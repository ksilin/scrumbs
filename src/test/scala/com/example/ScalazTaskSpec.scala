package com.example

import java.util.concurrent.{ScheduledExecutorService, Executors, TimeoutException}

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{AsyncFunSpec, FunSpec, Matchers}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scalaz.concurrent.Task

class ScalazTaskSpec extends AsyncFunSpec with Matchers {

  describe("Task") {

    it("should be resolvable into a Disjunction") {

      val t: Task[String] = Task {"foo"}

      t.unsafePerformSync should be("foo")
      val attempt: \/[Throwable, String] = t.unsafePerformSyncAttempt
      attempt should be(\/-("foo"))

      val tWithErrorHandling: Task[\/[Throwable, String]] = t.attempt
      tWithErrorHandling.unsafePerformSync should be(\/-("foo"))
    }

    it("should lift strict values") {

      val foo: Task[String] = Task.now("foo")

      foo.unsafePerformAsync(r => r should be(\/-("foo")))

      val exceptionMsg: String = "boom"
      val ex: Exception = new scala.Exception(exceptionMsg)
      val recoveredBar: Task[String] = Task.fail(ex) or Task.now("bar")

      val r: String = recoveredBar.unsafePerformSync

      r should be("bar") // the 'or' acts as a recovery

      val failedBar: Task[String] = Task.fail(ex)

      val r2: \/[Throwable, String] = failedBar.unsafePerformSyncAttempt
      r2 should be(-\/(ex))
    }

    it("should delay / suspend execution") {

      // TODO - try memoizing - see docs
      val delayed: Task[String] = Task.delay("foo")

      val res = delayed.unsafePerformSync
      res should be("foo")

      // looks like the names are confusing. Delay and suspend neither shift the execution along the timeline
      // nor do they halt the execution. suspend excutes the passed fn in a new trampoline loop

      // Task.fork - executes on a separate thread
    }

    it("should gather individual Task instances") {

      val tasks: IndexedSeq[Task[Int]] = (1 to 5) map (n => Task {Thread.sleep(100); n})

      val gathered: Task[List[Int]] = Task.gatherUnordered(tasks)

      val res: List[Int] = gathered.unsafePerformSync

      println(s"res: $res")

      res should contain theSameElementsAs ((1 to 5))
    }

    it("should map and flatMap") {

      val t = Task.now {1}

      val iceT = t map { v => v * 2 } flatMap { v => Task {v * 3} }

      iceT.unsafePerformSync should be(6)
    }

    it("should reduce the tasks") {

      val tasks: IndexedSeq[Task[Int]] = (1 to 5) map (n => Task {Thread.sleep(100); n})

      import Reducer.VectorReducer // provides implicit reducer instance for Vector
      val gathered: Task[Vector[Int]] = Task.reduceUnordered(tasks)

      gathered.unsafePerformSync should contain theSameElementsAs ((1 to 5))
    }

    it("should start immediately"){

      // if we really want to emulate the behavior of Future with Task
      val start: Task[String] = Task.unsafeStart { Thread.sleep(100); "huzza!"}

      start.unsafePerformSync should be("huzza!")
    }


    it("should accept an upper time limit of execution") {

      implicit val service: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

      val longRunning: Task[String] = Task {Thread.sleep(1000); "and done"}

       val timed: Task[String] = longRunning.unsafePerformTimed(50)

      // TODO - why is no exception is thrown?
      val ex = intercept[Exception] {
        val res: String = timed.unsafePerformSync
        println(s"result from timing out Task: $res")
      }
      true should be(true)
    }


    // a more general abstraction on Task combination
    it("should run Nondeterministically"){
      import scalaz.Nondeterminism

      val ts: IndexedSeq[Task[Int]] = (1 to 5) map (n => Task {Thread.sleep(100); n})

      val gathered: Task[List[Int]] = Nondeterminism[Task].nmap5(ts(0), ts(1), ts(2), ts(3), ts(4))(List(_, _, _, _, _))

      gathered.unsafePerformSync should contain theSameElementsAs ((1 to 5))
    }

    it("should recover with handleWith"){
      val exploding = Task(10 / 0)

      val withFallback: Task[Int] = exploding.handle{ case ex: Throwable => 0 }

      val withTaskFallback: Task[Int] = exploding.handleWith { case ex: Throwable => Task(0) }

      withFallback.unsafePerformSync should be(0)
      withTaskFallback.unsafePerformSync should be(0)
    }

    // TODO - Async.await


  }

}