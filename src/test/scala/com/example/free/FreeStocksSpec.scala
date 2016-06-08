package com.example.free

import java.util.UUID

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future

// not sure why start with this one, but I do:

// http://perevillega.com/understanding-free-monads
// https://github.com/pvillega/free-monad-sample

// separating prog def from exec, interface from impl and using a business language. sounds good

class FreeStocksSpec extends FunSpec with Matchers {

  describe("free monads in a stock application") {

    // could be our standard approach
    //    object Orders {
    type Symbol = String
    type Response = String

    // looks like a command pattern at first
    sealed trait Orders[A]
    case class Buy(stock: Symbol, amount: Int) extends Orders[Response]
    case class Sell(stock: Symbol, amount: Int) extends Orders[Response]

    // instead of defining methods, we define case classes
    //      def buy(stock: Symbol, amount: Int): Response = ???
    //      def sell(stock: Symbol, amount: Int): Response = ???
    //    }

    import cats.free.Free
    import cats.free.Free._

    // turning our Buy and Sell into sth we can run
    type OrdersF[A] = Free[Orders, A]

    // mapping our case classes to instances of Free
    def buy(stock: Symbol, amount: Int): OrdersF[Response] = liftF[Orders, Response](Buy(stock, amount))
    def sell(stock: Symbol, amount: Int): OrdersF[Response] = liftF[Orders, Response](Sell(stock, amount))

    it("should allow chaining ops") {

      val trade: Free[Orders, Response] = buy("APPL", 100).flatMap(_ => sell("GOOG", 100))
      println(trade) // Gosub(Suspend(Buy(APPL,100)),<function1>)

      val trade2 = for {
        _ <- buy("APPL", 50)
        _ <- buy("MSFT", 10)
        rsp <- sell("GOOG", 200)
      } yield rsp
      println(trade2) // Gosub(Suspend(Buy(APPL,50)),<function1>) <- why not the sell op ?


      // now we need an interpreter (natural transformation) to actually run things
      // use an interpreter to obtain a monad with yout aresult (option, future, xor)

      // Id is the simplest monad - effect of having no effect
      // ~> is syntax sugar for natural transformation
      import cats.{Id, ~>}

      def orderPrinter: Orders ~> Id =
        new (Orders ~> Id) {
          def apply[A](fa: Orders[A]): Id[A] = fa match {
            case Buy(stock, amount) =>
              println(s"buying $amount of $stock")
              // IntelliJ is not amused: Expression of type String does not conform to expected type cats.Id[A]
              "ok"
            case Sell(stock, amount) =>
              println(s"selling $amount of $stock")
              "ok"
            case ListStocks() =>
              val stocks = List("FB", "TWTR")
              println(s"getting list of stocks: $stocks")
              stocks
          }
        }

      // execute by invoking foldMap
      trade2.foldMap(orderPrinter)

      import cats.data.Xor
      import cats.syntax.xor._

      // sth like railway oriented programming - using Xor
      // same issue as with monad transformers & Xor
      // ~> expects G[_], but Xor is Xor[+A, +B]
      type ErrorOr[A] = Xor[String, A]

      def stringInterpreter: Orders ~> ErrorOr =
        new (Orders ~> ErrorOr) {
          def apply[A](fa: Orders[A]): ErrorOr[A] = fa match {
            case Buy(stock, amount) =>
              // expression fo type Xor[Nothing, String] does not conform to expected type ErrorOn[A]
              s"$stock - $amount".right
            case Sell(stock, amount) =>
              "dont sell that!".left
          }
        }

      //  the result of computation, if not success, will be the first failure and the computation is short-cirquited
      val res = trade2.foldMap(stringInterpreter)
      println(res) // Left("dont sell that)

      // TODO - so what do we do with a free monad if we want to get all the errors intead of shortcutting?
      import cats.std.list._
      import cats.syntax.traverse._


      // to get a list of stocks, we need to define a new case class and listing it to a free monad instance
      case class ListStocks() extends Orders[List[Symbol]]

      def listStocks(): OrdersF[List[Symbol]] = liftF[Orders, List[Symbol]](ListStocks())
      //      def listStocks() = Future.successful(List("APPL", "MSFT", "GOOG"))

      // nice trick, but I would prefer to somehow provide the list of stocks myselfm instead of the method pulling it
      // OTOH I can provide any impl I want through the interpreter
      val tradeDynamicStocks: Free[Orders, Response] = for {
        st <- listStocks()
        _ <- st.traverseU(buy(_, 100))
        rsp <- sell("AAPL", 100)
      } yield rsp

      println("--- dynamic stocks")
      val r = tradeDynamicStocks.foldMap(orderPrinter)
      println(r)

      // logging without side effects- we need another free monad just for the logs
      sealed trait Log[A]

      case class Info(msg: String) extends Log[Unit]
      case class Error(msg: String) extends Log[Unit]

      type LogF[A] = Free[Log, A]

      def info(msg: String): LogF[Unit] = liftF[Log, Unit](Info(msg))
      def error(msg: String): LogF[Unit] = liftF[Log, Unit](Error(msg))

      def logPrinter: Log ~> Id =
        new (Log ~> Id) {
          def apply[A](fa: Log[A]): Id[A] =
            fa match {
              case Info(msg) => println(s"[Info] - $msg")
              case Error(msg) => println(s"[Error] - $msg")
            }
        }

      println("trade with logs")
      // this wont compile - monads must be of the same type
      // here we are mixing OrdersF and LogF
      //      val tradeWithLogs = for {
      //        _ <- info("starting trade")
      //        _ <- buy("APPL", 50)
      //        rsp <- sell("GOOG", 200)
      //      } yield rsp

      // in order to flatMap over different monads, we need a more complex structure
      // not understanding what goes on now, take it as a boilerplate for the moment
      import cats.free.Inject

      // the implicit Inject will resolve to a type binding different monads together on compilation
      class OrderI[F[_]](implicit I: Inject[Orders, F]) {
        def buyI(stock: Symbol, amount: Int): Free[F, Response] = Free.inject[Orders, F](Buy(stock, amount))

        def sellI(stock: Symbol, amount: Int): Free[F, Response] = Free.inject[Orders, F](Sell(stock, amount))
      }

      implicit def orderI[F[_]](implicit I: Inject[Orders, F]): OrderI[F] = new OrderI[F]

      // and the same for logs
      class LogI[F[_]](implicit I: Inject[Log, F]) {
        def infoI(msg: String): Free[F, Unit] = Free.inject[Log, F](Info(msg))

        def errorI(msg: String): Free[F, Unit] = Free.inject[Log, F](Error(msg))
      }

      implicit def logI[F[_]](implicit I: Inject[Log, F]): LogI[F] = new LogI[F]


      // in order to use both laguages in a for-comp, we need to combine them with Coproduct
      import cats.data.Coproduct

      // TODO - do we need a coproduct for each pair of free monads?
      type TradeApp[A] = Coproduct[Orders, Log, A]

      // this compiles at least, but I still have no idea how to use it
      // the implicit Inject will help us build a compat layer between both monads
      // see the data types a la carte paper: http://www.cs.ru.nl/~W.Swierstra/Publications/DataTypesALaCarte.pdf
      // also Emm or Eff (extensible effects) monad
      def tradeWithLogs(implicit O: OrderI[TradeApp], L: LogI[TradeApp]): Free[TradeApp, Response] = {
        import O._
        // buyI & sellI
        import L._

        for {
          _ <- infoI("starting trade")
          _ <- buyI("APPL", 100)
          _ <- infoI("even more trading")
          _ <- buyI("MSFT", 100)
          rsp <- sellI("GOOG", 100)
          _ <- errorI("we should not be here")
        } yield rsp
      }

      // we can create the new interpreter by combining existing ones
      // 'or' comes from the NaturalTransformtion trait, uses Coproduct & Xor
      def orderAndLogInterpreter: TradeApp ~> Id = orderPrinter or logPrinter

      println(" --- trading and logging")
      val rC = tradeWithLogs.foldMap(orderAndLogInterpreter)
      println(rC)

      // so, now we have 2 languages, but what about 3? - audit as the 3rd one

      type UserId = String
      type JobId = String
      type Values = String

      sealed trait Audit[A]
      case class UserActionAudit(user: UserId, action: String, values: List[Values]) extends Audit[Unit]
      case class SystemActionAudit(job: JobId, action: String, values: List[Values]) extends Audit[Unit]

      class AuditI[F[_]](implicit I: Inject[Audit, F]) {
        def userActionAuditI(user: UserId, action: String, values: List[Values]) = Free.inject[Audit, F](UserActionAudit(user, action, values))

        def systemActionAuditI(job: JobId, action: String, values: List[Values]) = Free.inject[Audit, F](SystemActionAudit(job, action, values))
      }

      implicit def AuditI[F[_]](implicit I: Inject[Audit, F]): AuditI[F] = new AuditI[F]

      def auditPrinter: Audit ~> Id =
        new (Audit ~> Id) {
          def apply[A](fa: Audit[A]): Id[A] = fa match {
            case UserActionAudit(user, action, values) => println(s"[User action] - user $user called $action with $values")
            case SystemActionAudit(job, action, values) => println(s"[System action] - job $job called $action with $values")
          }
        }

      // now we need a Coproduct for 3 monads while it only accepts 2
      // lets take th eone Coproduct we have alredy created for Order & Log and use it as a single type/monad

      // due to the implementation of Inject, TradeApp need to be on the right side or -> no compile
      type AuditableTradeApp[A] = Coproduct[Audit, TradeApp, A]

      // the order is also important here, due to impl of 'or'
      def auditableInerpreter: AuditableTradeApp ~> Id = auditPrinter or orderAndLogInterpreter

      def tradeWithAuditsAndLogs(implicit O: OrderI[AuditableTradeApp], L: LogI[AuditableTradeApp], A: AuditI[AuditableTradeApp]): Free[AuditableTradeApp, Response] = {

        import O._
        import L._
        import A._

        for {
          _ <- infoI("starting trade")
          _ <- userActionAuditI("id1", "buy", List("APPL", "100"))
          _ <- buyI("APPL", 100)
          _ <- infoI("even more trading")
          _ <- userActionAuditI("id1", "buy", List("MSFT", "100"))
          _ <- buyI("MSFT", 100)
          _ <- userActionAuditI("id1", "sell", List("GOOG", "100"))
          rsp <- sellI("GOOG", 100)
          _ <- errorI("we should not be here")
        } yield rsp
      }

      println("--- trading with log and audit")
      tradeWithAuditsAndLogs.foldMap(auditableInerpreter)


      // how would we actually send an order to someone

      type ChannelId = String
      type SourceId = String
      type MessageId = String
      type Payload = String
      type Condition = String

      sealed trait Messaging[A]

      case class Publish(channel: ChannelId, source: SourceId, messageId: MessageId, payload: Payload) extends Messaging[Response]
      case class Subscribe(channel: ChannelId, filterBy: Condition) extends Messaging[Payload]

      type MessagingF[A] = Free[Messaging, A]

      def publish(channel: ChannelId, source: SourceId, messageId: MessageId, payload: Payload): MessagingF[Response] = liftF[Messaging, Response](Publish(channel, source, messageId, payload))
      def subscribe(channel: ChannelId, filterBy: Condition): MessagingF[Payload] = liftF[Messaging, Payload](Subscribe(channel, filterBy))

      def messagingPrinter: Messaging ~> Id =
        new (Messaging ~> Id) {
          def apply[A](fa: A): Id = fa match {
            case Publish(channel, source, messageId, payload) =>
              println(s"publishing [$channel] from $source id $messageId payload $payload")
              "ok"
            case Subscribe(channel, filterBy) =>
              println(s"subscribing [$channel] filtered by $filterBy")
              "event fired"
          }
        }

      // now that we have defined the language, how do we define our orders in terms of ops agains a pub-sub network?
      // natural transformation of course

      def orderToMessageInterpreter: Orders ~> MessagingF =
        new (Orders ~> MessagingF) {
          def apply[A](fa: Orders[A]): MessagingF[A] = {
            case ListStocks() =>
              for {
                _ <- publish("001", "Orders", UUID.randomUUID().toString, "get stocks list")
                payload <- subscribe("001", "*")
              } yield payload
            case Buy(stock, amount) => publish("001", "Orders", UUID.randomUUID().toString, s"buy $stock $amount")
            case Sell(stock, amount) => publish("001", "Orders", UUID.randomUUID().toString, "get stocks list")
          }
        }

      // and how do we get from Orders to Id while using MessagingF?
      // we need an interpreter bridge


    }

  }

  // voices against free monads

  //  Alexandru Nedelcu - https://twitter.com/alexelcu/status/736090380999888898
  // decoupling the interpreter is cool, but
  // Free is a frozen flatMap -> imposed ordering & lost potential to rearrange ops
  // not crossing async boundaries, only plain trampolines  - huh?
  // why not use Task - the purpose is very similar ?

  // Kelley Robinson - free monad isnt free
  // http://www.slideshare.net/KelleyRobinson1/why-the-free-monad-isnt-free-61836547


}
