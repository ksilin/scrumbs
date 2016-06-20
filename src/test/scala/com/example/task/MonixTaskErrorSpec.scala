package com.example.task

import java.io.Serializable

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.util.Random

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task

class MonixTaskErrorSpec extends FreeSpec with Matchers {

  // TODO - onErrorRestart / If

  "retry and error handling" - {

    type Response = String
    type Record = String

    def extract(offset: Int, amount: Int) = Task {
      List.fill(amount)(offset + Random.nextInt(1000))
    }

    def transform[T, U](sourceRecs: List[T], f: T => U): Task[List[U]] = Task {
      sourceRecs map f
    }

    def load[T](targetRecs: List[T]): Task[List[Response]] = Task {
      targetRecs.map(r => s"${r.toString} stored")
    }

    val tl: (List[Int], Int => Record) => Task[List[Response]] =
      (sourceRecs: List[Int], f: Int => Record) => transform(sourceRecs, f) flatMap load

    "simply failing" in {

      val failing = Task {
        println("hi, I am about to fail")
        throw new IllegalStateException("kaboom")
      }

      val d: Task[Nothing] = failing.delayExecution(1 second)

      val f = failing.runAsync //(r => println(r))

      // will get printed
      f.onComplete(_ => println("task complete"))

      Await.result(f, 10 seconds)
    }


    "failing again" in {

      val t = Task(2).flatMap { r =>
        throw new IllegalStateException(s"kablammo! $r")
      }
      // Even though Monix expects for the arguments given to its operators, like flatMap,
      // to be pure or at least protected from errors, it still catches errors, signaling them on runAsync:
      val f = t.runAsync
      val r = Await.result(f, 10 seconds)
      println(s"printing the result: $r") // will never be printed
    }

    // In case an error happens in the callback provided to runAsync,
    // then Monix can no longer signal an onError, because it would be a contract violation (see Callback).
    // But it still logs the error

    // handling and recovering
    // Handle -> full function
    // recover -> partial function

    // ..Handle.. expects a complete function,
    // ..recover.. expects a partial function
    // both are an equivalent to a flatMap, but for exceptions only

    // with -> flatMap
    // . -> map


    "fails with a timeout" in {

      val delayed = Task("hi").delayExecution(10 seconds).timeout(3 seconds)
      val f: CancelableFuture[Record] = delayed.runAsync
      f.onComplete(r => println(s"completed: $r"))

      // java.util.concurrent.TimeoutException: Task timed-out after 3 seconds of inactivity
      val r = Await.result(f, 5 seconds)
    }

    "fails with a timeout and recovers using HandleWith" in {

      val delayed = Task("hi").delayExecution(10 seconds)
      val timedout: Task[Record] = delayed.timeout(3 seconds)

      val recovered = timedout.onErrorHandleWith {
        case _: TimeoutException => Task.now("recovered!")
        case other => Task.raiseError(other)
      }

      val f: CancelableFuture[Record] = recovered.runAsync
      f.onComplete(r => println(s"completed: $r"))

      // java.util.concurrent.TimeoutException: Task timed-out after 3 seconds of inactivity
      val r = Await.result(f, 5 seconds)
      println(s"result: $r")
    }

    "fails with a timeout and recovers using Handle " in {

      val delayed = Task("hi").delayExecution(10 seconds).timeout(3 seconds)

      val recovered = delayed.onErrorHandle {
        case _: TimeoutException => "recovered!"
        case other => throw other
      }
      val f = recovered.runAsync

      f.onComplete(r => println(s"completed: $r"))

      // java.util.concurrent.TimeoutException: Task timed-out after 3 seconds of inactivity
      val r = Await.result(f, 5 seconds)
      println(s"result: $r")
    }


    "recovering composed" in {

      val etl: Task[List[Response]] = extract(0, 3) flatMap //(tl(_, intToString)) flatMap
        { _ =>
        println("failing in flatMap")
        throw new IllegalStateException("failing in flatMap")
      }

      val failTask: Task[Nothing] = Task {
        println("failing")
        throw new IllegalStateException("failing")
      }

      val composedFail: Task[Nothing] = Task { "x" } flatMap { _ => throw new IllegalStateException("failing")}

      // does not recover
//      val withRecovery: Task[List[Response]] = etl.onErrorRecoverWith { case t: Throwable =>
//        println(s"failed with $t")
//        etl
//      }

      val withHandling: Task[Serializable] = failTask.onErrorHandleWith {
        case e: IllegalStateException =>
          println("as expected")
          Task.now("recovered from ISE")
//        case t: Throwable =>
//        println(s"failed with $t. Handling error")
//        Task.now("recovered")
      case other =>
        println(s"unexpected error: $other")
        Task.raiseError(other)
      }

      val f = withHandling.runAsync
      f.onComplete(r => println(s"completed: $r"))

      val r = Await.result(f, 10 seconds)
      println(s"result: $r")
    }

  }

}
