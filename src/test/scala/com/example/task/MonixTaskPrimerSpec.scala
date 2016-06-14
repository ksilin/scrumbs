package com.example.task

// In order to evaluate tasks, we'll need a Scheduler
import java.time.LocalDateTime

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FunSpec, Matchers}

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task
import scala.util.{Success, Failure}

// https://monix.io/docs/2x/eval/task.html
// https://vimeo.com/channels/flatmap2016/165922572
// https://www.dropbox.com/s/9yz6bdzumzk0y1p/Monix-Task.pdf

// what about cats?
// https://github.com/typelevel/cats/issues/32
// https://github.com/typelevel/cats/issues/21

class MonixTaskPrimerSpec extends FunSpec with Matchers {

  describe("dipping my toes into monix task") {

    it("should work") {
      // Executing a sum, which (due to the semantics of apply)
      // will happen on another thread. Nothing happens on building
      // this instance though, this expression is pure, being
      // just a spec! Task by default has lazy behavior ;-)
      val task = Task {1 + 1}

      // Tasks get evaluated only on runAsync!
      // Callback style:
      val cancelable = task.runAsync { result =>
        result match {
          case Success(value) =>
            println(value)
          case Failure(ex) =>
            System.out.println(s"ERROR: ${ex.getMessage}")
        }
      }
      //=> 2

      // Or you can convert it into a Future
      val future: CancelableFuture[Int] =
        task.runAsync

      // Printing the result asynchronously
      future.foreach(println)
    }

    it("should run examples from the talk"){
      // strict / eager
      Task.now { println("effect eager"); "immediate" }

      // lazy / memoized
      val once = Task.evalOnce{
        println(s"effect once + ${LocalDateTime.now}"); LocalDateTime.now
      }
      once.runAsync
      once.runAsync // no effect

      // equiv to a fn - will try to execute synchronously
      val getResultAlways: Task[String] = Task.evalAlways{ println("effect fn"); "always"}
      getResultAlways.runAsync
      getResultAlways.runAsync

      // turning into evalOnce
      val memoized = getResultAlways.memoize
      println("memoized: " + memoized)


      // task factory
      val tf = Task.defer{ Task.now { println("effect fact")}}
      val ra: CancelableFuture[Unit] = tf.runAsync
      println(ra)
      println(ra) // same
      println(tf.runAsync) // new instance

      // guarantees async exec
      Task.fork(Task.evalAlways(println("effect fn")))
    }
  }

  describe("simple tl with tasks"){

  }

}
