package com.example.task

import java.io.Serializable

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{AsyncFreeSpec, Matchers}

import scala.concurrent.TimeoutException
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task

class MonixTaskErrorSpec extends AsyncFreeSpec with Matchers {

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

      val d: Task[Nothing] = failing.delayExecution(1.second)
      recoverToSucceededIf[IllegalStateException] {failing.runAsync} //(r => println(r)))
    }


    "failing again" in {

      val t = Task(2).flatMap { r =>
        throw new IllegalStateException(s"kablammo! $r")
      }
      // Even though Monix expects for the arguments given to its operators, like flatMap,
      // to be pure or at least protected from errors, it still catches errors, signaling them on runAsync:
      recoverToSucceededIf[IllegalStateException](t.runAsync)
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

      val delayed = Task("hi").delayExecution(10.seconds).timeout(3.seconds)

      recoverToSucceededIf[TimeoutException] {
        val f: CancelableFuture[Record] = delayed.runAsync
        f.onComplete(r => println(s"completed: $r"))
        // java.util.concurrent.TimeoutException: Task timed-out after 3.seconds of inactivity
        f
      }
    }

    "fails with a timeout and recovers using HandleWith" in {

      val delayed = Task("hi").delayExecution(10.seconds)
      val timedout: Task[Record] = delayed.timeout(3.seconds)

      val recovered = timedout.onErrorHandleWith {
        case _: TimeoutException => Task.now("recovered!")
        case other => Task.raiseError(other)
      }

      val f: CancelableFuture[Record] = recovered.runAsync
      f.onComplete(r => println(s"completed: $r")) // Success(recovered!)
      f map { res: Serializable =>
        res should be("recovered!")
      }
    }

    "fails with a timeout and recovers using Handle " in {

      val delayed = Task("hi").delayExecution(10.seconds).timeout(3.seconds)

      val recovered = delayed.onErrorHandle {
        case _: TimeoutException => "recovered!"
      }

      val f = recovered.runAsync
      f.onComplete(r => println(s"completed: $r")) // Success(recovered!)
      f map { res: Serializable =>
        res should be("recovered!")
      }
    }

    val recoverFromISE: (Throwable) => Task[String] = {
      //(t: Throwable) => t match {
      case e: IllegalStateException => Task.now("recovered from ISE")
      case other => Task.raiseError(other)
    }

    "recovering composed" in {

      // TODO - seems to be another bug in 2.0-RC7, waiting for confirmation

      val composedFail: Task[Nothing] = Task.now("x") flatMap { _ => throw new IllegalStateException("failing") }
      //      val composedFail: Task[Nothing] = Task("x") flatMap { _ => throw new IllegalStateException("failing")}
      val withHandling: Task[Serializable] = composedFail.onErrorHandleWith(recoverFromISE)
      withHandling.runAsync map {_ should be("recovered from ISE")}
    }

    "root cause of bug" in {
      val f = CancelableFuture.successful(1).map(_+1)
      f map{ r => r should be(2)}
    }

  }
}
