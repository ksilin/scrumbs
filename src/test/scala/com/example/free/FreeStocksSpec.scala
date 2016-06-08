package com.example.free

import org.scalatest.{FunSpec, Matchers}

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

    it("should allow chaining ops"){
      val trade: Free[Orders, Response] = buy("APPL", 100).flatMap(_ => sell("GOOG", 100))
      println(trade) // Gosub(Suspend(Buy(APPL,100)),<function1>)

      val trade2 = for {
        _ <- buy("APPL", 50)
        _ <- buy("MSFT", 10)
        rsp <- sell("GOOG", 200)
      } yield rsp
      println(trade2)  // Gosub(Suspend(Buy(APPL,50)),<function1>) <- why not the sell op ?


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

      //  the result of computation, if not success, will be the first failure and the computation is short-cirquiteed
      val res = trade2.foldMap(stringInterpreter)
      println(res) // Left("dont sell that)
      }

    }
}
