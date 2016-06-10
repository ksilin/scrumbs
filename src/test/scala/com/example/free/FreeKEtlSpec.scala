package com.example.free

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future

class FreeKEtlSpec extends FunSpec with Matchers {

  // http://perevillega.com/freek-and-free-monads
  // https://github.com/ProjectSeptemberInc/freek

  describe("free monads in an etl application") {

    type Symbol = String
    type Response = String
    case class Record(id: String, payload: String)

    sealed trait Ops[A]
//    case class Fetch(offset: Int, amount: Int) extends Ops[Future[List[Record]]]
    case class Fetch(offset: Int, amount: Int) extends Ops[List[Record]]
    case class Store(recs: List[Record]) extends Ops[Future[List[Response]]]
    case class CalcBatches(offset: Int, amount: Int, batchSize: Int) extends Ops[List[Fetch]]

    import cats.free.Free
    import cats.free.Free._

    // turning our Fetch and Store into sth we can run
    type OpsF[A] = Free[Ops, A]

    // mapping our case classes to instances of Free
//    def fetch(offset: Int, amount: Int): OpsF[Future[List[Record]]] = liftF[Ops, Future[List[Record]]](Fetch(offset, amount))
    def fetch(offset: Int, amount: Int): OpsF[List[Record]] = liftF[Ops, List[Record]](Fetch(offset, amount))
    def store(recs: List[Record]): OpsF[Future[List[Response]]] = liftF[Ops, Future[List[Response]]](Store(recs))
    def calcBatches(offset: Int, amount: Int, batchSize: Int): OpsF[List[Fetch]] = liftF[Ops, List[Fetch]](CalcBatches(offset, amount, batchSize))

    it("should allow chaining ops") {

      val simpleEtl: Free[Ops, Future[List[Response]]] = fetch(0, 100).flatMap(r => store(r))
      println(simpleEtl)

      import cats.std.list._
      import cats.syntax.traverse._

      val etl = for {
        b <- calcBatches(0, 1000, 10)
        rs <- b.traverseU(f => fetch(f.offset, f.amount).flatMap(r => store(r)))
      } yield rs
      println(etl)

      // now we need an interpreter (natural transformation) to actually run things
      // use an interpreter to obtain a monad with yout aresult (option, future, xor)

      // Id is the simplest monad - effect of having no effect
      // ~> is syntax sugar for natural transformation
      import cats.{Id, ~>}

      def etlPrinter: Ops ~> Id =
        new (Ops ~> Id) {
          def apply[A](fa: Ops[A]): Id[A] = fa match {
            case Fetch(offset, amount) =>
              println(s"fetching $amount records with offset $offset")
              // IntelliJ is not amused: Expression of type String does not conform to expected type cats.Id[A]
              List(Record(offset.toString, "fake"))
            case Store(recs) =>
              println(s"storing $recs")
              Future.successful(List("ok"))
            case CalcBatches(offset, amount, batchSize) =>
              println(s"calculating batches: $offset, $amount, $batchSize")
              List(Fetch(1, 50), Fetch(2, 60))
          }
        }
      // execute by invoking foldMap
      etl.foldMap(etlPrinter)
    }
  }
}
