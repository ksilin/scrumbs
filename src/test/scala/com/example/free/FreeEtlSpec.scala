package com.example.free

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.free.Free
import cats.free.Free._
import cats.std.list._
import cats.syntax.traverse._


class FreeEtlSpec extends FunSpec with Matchers {

  describe("free monads in an etl application") {

    type Response = String
    case class Record(id: String, payload: String)
    sealed trait Ops[A]
    type OpsF[A] = Free[Ops, A]

    it("should work with both methods accepting lists and returning futures") {

      case class Fetch(offset: Int, amount: Int) extends Ops[Future[List[Record]]]
      case class Store(recs: List[Record]) extends Ops[Future[List[Response]]]
      case class CalcBatches(offset: Int, amount: Int, batchSize: Int) extends Ops[List[Fetch]]

      def fetch(offset: Int, amount: Int): OpsF[Future[List[Record]]] = liftF[Ops, Future[List[Record]]](Fetch(offset, amount))
      def store(recs: List[Record]): OpsF[Future[List[Response]]] = liftF[Ops, Future[List[Response]]](Store(recs))
      def calcBatches(offset: Int, amount: Int, batchSize: Int): OpsF[List[Fetch]] = liftF[Ops, List[Fetch]](CalcBatches(offset, amount, batchSize))


//      Error: type mismatch;
//      found   : scala.concurrent.Future[OpsF[scala.concurrent.Future[List[Response]]]]
//      (which expands to)  scala.concurrent.Future[cats.free.Free[Ops,scala.concurrent.Future[List[String]]]]
//      required: OpsF[scala.concurrent.Future[List[Response]]]
//      (which expands to)  cats.free.Free[Ops,scala.concurrent.Future[List[String]]]
//      val getResponse: OpsF[Future[List[Response]]] = rf map { r: List[Record] =>
//        ^

//      def simpleEtl(offset: Int, amount: Int): Free[Ops, Future[List[Response]]] = fetch(offset, amount).flatMap { rf: Future[List[Record]] =>
//        val getResponse: OpsF[Future[List[Response]]] = rf map { r: List[Record] =>
//          val resf: OpsF[Future[List[Response]]] = store(r)
//          resf
//        }
//        getResponse
//      }
//
//      val etl = for {
//        b <- calcBatches(0, 1000, 10)
//        rs <- b.traverseU(f => simpleEtl(f.offset, f.amount)) //f => fetch(f.offset, f.amount).flatMap(r => store(r)))
//      } yield rs
//      println(etl)
//
//      import cats.{Id, ~>}
//
//      def etlPrinter: Ops ~> Id =
//        new (Ops ~> Id) {
//          def apply[A](fa: Ops[A]): Id[A] = fa match {
//            case Fetch(offset, amount) =>
//              println(s"fetching $amount records with offset $offset")
//              List(Record(offset.toString, "fake"))
//            case Store(recs) =>
//              println(s"storing $recs")
//              List("ok")
//            case CalcBatches(offset, amount, batchSize) =>
//              println(s"calculating batches: $offset, $amount, $batchSize")
//              List(Fetch(1, 50), Fetch(2, 60))
//          }
//        }
//      // execute by invoking foldMap
//      etl.foldMap(etlPrinter)
    }

    it("should work with both methods working with lists") {

      case class Fetch(offset: Int, amount: Int) extends Ops[List[Record]]
      case class Store(recs: List[Record]) extends Ops[List[Response]]
      case class CalcBatches(offset: Int, amount: Int, batchSize: Int) extends Ops[List[Fetch]]

      def fetch(offset: Int, amount: Int): OpsF[List[Record]] = liftF[Ops, List[Record]](Fetch(offset, amount))
      def store(recs: List[Record]): OpsF[List[Response]] = liftF[Ops, List[Response]](Store(recs))
      def calcBatches(offset: Int, amount: Int, batchSize: Int): OpsF[List[Fetch]] = liftF[Ops, List[Fetch]](CalcBatches(offset, amount, batchSize))

      def simpleEtl(offset: Int, amount: Int): Free[Ops, List[Response]] = fetch(offset, amount).flatMap(r => store(r))

      val etl = for {
        b <- calcBatches(0, 1000, 10)
        rs <- b.traverseU(f => simpleEtl(f.offset, f.amount)) //f => fetch(f.offset, f.amount).flatMap(r => store(r)))
      } yield rs
      println(etl)

      import cats.{Id, ~>}

      def etlPrinter: Ops ~> Id =
        new (Ops ~> Id) {
          def apply[A](fa: Ops[A]): Id[A] = fa match {
            case Fetch(offset, amount) =>
              println(s"fetching $amount records with offset $offset")
              List(Record(offset.toString, "fake"))
            case Store(recs) =>
              println(s"storing $recs")
              List("ok")
            case CalcBatches(offset, amount, batchSize) =>
              println(s"calculating batches: $offset, $amount, $batchSize")
              List(Fetch(1, 50), Fetch(2, 60))
          }
        }
      // execute by invoking foldMap
      etl.foldMap(etlPrinter)
    }

    it("should work with Store accepting List and returning Future of List ") {
      case class Fetch(offset: Int, amount: Int) extends Ops[List[Record]]
      case class Store(recs: List[Record]) extends Ops[Future[List[Response]]]
      case class CalcBatches(offset: Int, amount: Int, batchSize: Int) extends Ops[List[Fetch]]

      def fetch(offset: Int, amount: Int): OpsF[List[Record]] = liftF[Ops, List[Record]](Fetch(offset, amount))
      def store(recs: List[Record]): OpsF[Future[List[Response]]] = liftF[Ops, Future[List[Response]]](Store(recs))
      def calcBatches(offset: Int, amount: Int, batchSize: Int): OpsF[List[Fetch]] = liftF[Ops, List[Fetch]](CalcBatches(offset, amount, batchSize))

      def simpleEtl(offset: Int, amount: Int): Free[Ops, Future[List[Response]]] = fetch(offset, amount).flatMap(r => store(r))

      val etl = for {
        b <- calcBatches(0, 1000, 10)
        rs <- b.traverseU(f => simpleEtl(f.offset, f.amount)) //f => fetch(f.offset, f.amount).flatMap(r => store(r)))
      } yield rs
      println(etl)

      import cats.{Id, ~>}

      def etlPrinter: Ops ~> Id =
        new (Ops ~> Id) {
          def apply[A](fa: Ops[A]): Id[A] = fa match {
            case Fetch(offset, amount) =>
              println(s"fetching $amount records with offset $offset")
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

    it("should work with Store accepting and returning Future of List") {

      case class Fetch(offset: Int, amount: Int) extends Ops[Future[List[Record]]]
      case class Store(recs: Future[List[Record]]) extends Ops[Future[List[Response]]]
      case class CalcBatches(offset: Int, amount: Int, batchSize: Int) extends Ops[List[Fetch]]
      // combining fetch & store
      case class Batch(offset: Int, amount: Int) extends Ops[Future[List[Response]]]

      def fetch(offset: Int, amount: Int): OpsF[Future[List[Record]]] = liftF[Ops, Future[List[Record]]](Fetch(offset, amount))
      def batch(offset: Int, amount: Int): OpsF[Future[List[Response]]] = liftF[Ops, Future[List[Response]]](Batch(offset, amount))
      def store(recs: Future[List[Record]]): OpsF[Future[List[Response]]] = liftF[Ops, Future[List[Response]]](Store(recs))
      def calcBatches(offset: Int, amount: Int, batchSize: Int): OpsF[List[Fetch]] = liftF[Ops, List[Fetch]](CalcBatches(offset, amount, batchSize))

      def simpleEtl(offset: Int, amount: Int): Free[Ops, Future[List[Response]]] =
        for {
          fl <- fetch(offset, amount)
          rs <- store(fl)
        } yield rs

      import cats.{Id, ~>}

      def etlPrinter: Ops ~> Id =
        new (Ops ~> Id) {
          def apply[A](fa: Ops[A]): Id[A] = fa match {
            case Fetch(offset, amount) =>
              println(s"fetching $amount records with offset $offset")
              Future.successful(List(Record(offset.toString, "fake")))
            case Store(getRecs) =>
              // TODO - map
              getRecs flatMap { recs =>
                println(s"storing $recs")
                Future.successful(List("ok"))
              }
            case CalcBatches(offset, amount, batchSize) =>
              println(s"calculating batches: $offset, $amount, $batchSize")
              List(Fetch(1, 50), Fetch(2, 60))
            case Batch(offset, amount) =>
              println(s"running batch: $offset, $amount")
              simpleEtl(offset, amount).foldMap(etlPrinter) // can we go recursive here?
          }
        }
      simpleEtl(0, 100).foldMap(etlPrinter)

      def fullEtl(offset: Int, amount: Int, batchSize: Int) = for {
        b <- calcBatches(offset, amount, batchSize)
        rs <- b.traverseU(f => simpleEtl(f.offset, f.amount)) //fetch(f.offset, f.amount).flatMap { r: Future[List[Record]] => store(r) })
      } yield rs

      println("full etl")
      fullEtl(0, 100, 10).foldMap(etlPrinter)


      def batchEtl(offset: Int, amount: Int, batchSize: Int) = for {
        b <- calcBatches(offset, amount, batchSize)
        rs <- b.traverseU(f => batch(f.offset, f.amount))
      } yield rs

      println("batch etl")
      batchEtl(0, 100, 10).foldMap(etlPrinter)
    }
  }
}
