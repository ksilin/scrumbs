package com.example.future

import org.scalatest._

import scala.concurrent.{Await, Future}

class FutureForCompParallelismSpec extends FunSpec with Matchers {

  // because of it's `flatMap`-based desugaring
  // even independent futures in a for-comp are supposed to be instantiated consecutively

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  describe("Futures combination & recovery - short tasks") {

    // 525
    it("should execute futures sequentially") {
      val start = System.currentTimeMillis
      // TODO - would returning only a change the elapsed time?
      val resFuture = for {
        a <- Future { Thread.sleep(100); "one"}
        b <- Future { Thread.sleep(100); "two"}
        c <- Future { Thread.sleep(100); "three"}
        d <- Future { Thread.sleep(100); "four"}
        e <- Future { Thread.sleep(100); "five"}
      } yield (a, b, c, d, e)

      val res: (String, String, String, String, String) = Await.result(resFuture, 10.second)

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
      elapsed should be > 500L
    }

    // 503
    it("should not optimize") {

      val start = System.currentTimeMillis
      val resFuture = for {
        a <- Future { Thread.sleep(100); "one"}
        b <- Future { Thread.sleep(100); "two"}
        c <- Future { Thread.sleep(100); "three"}
        d <- Future { Thread.sleep(100); "four"}
        e <- Future { Thread.sleep(100); "five"}
      } yield (a)

      val res: String = Await.result(resFuture, 10.second)

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
      elapsed should be > 500L
    }

    // 103
    it("should execute in parallel when the futures are already initialized") {
      val start = System.currentTimeMillis

      val a = Future { Thread.sleep(100); "one"}
      val b = Future { Thread.sleep(100); "two"}
      val c = Future { Thread.sleep(100); "three"}
      val d = Future { Thread.sleep(100); "four"}
      val e = Future { Thread.sleep(100); "five"}
      val resFuture = for {
        aa <- a
        bb <- b
        cc <- c
        dd <- d
        ee <- e
      } yield (aa, bb, cc, dd, ee)

      val res: (String, String, String, String, String) = Await.result(resFuture, 10.second)

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed pre for comp: $elapsed")
      elapsed should be < 500L
    }

    // 102
    it("should execute in parallel within a list") {
      val start = System.currentTimeMillis

      val a = Future { Thread.sleep(100); "one"}
      val b = Future { Thread.sleep(100); "two"}
      val c = Future { Thread.sleep(100); "three"}
      val d = Future { Thread.sleep(100); "four"}
      val e = Future { Thread.sleep(100); "five"}
      val futures = Future.sequence(List(a, b, c, d, e))

      val res: List[String] = Await.result(futures, 10.second)

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed list: $elapsed")
      elapsed should be < 500L
    }

  // 101
    it("should not improve performance without intermediate valuesr") {
      val start = System.currentTimeMillis

      val futures = Future.sequence(List(
        Future { Thread.sleep(100); "one"},
        Future { Thread.sleep(100); "two"},
        Future { Thread.sleep(100); "three"},
        Future { Thread.sleep(100); "four"},
        Future { Thread.sleep(100); "five"}
      ))

      val res: List[String] = Await.result(futures, 10.second)

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed list: $elapsed")
      elapsed should be < 500L
    }
  }
}
