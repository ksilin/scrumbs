package com.example.task

// In order to evaluate tasks, we'll need a Scheduler
import java.io.Serializable
import java.time.LocalDateTime

import monix.execution.{ Cancelable, Scheduler }

import monix.execution.schedulers.TestScheduler
import org.scalatest.{ FreeSpec, FunSpec, Matchers }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task
import scala.util.{ Failure, Success }

// https://monix.io/docs/2x/eval/task.html
// https://vimeo.com/channels/flatmap2016/165922572
// https://www.dropbox.com/s/9yz6bdzumzk0y1p/Monix-Task.pdf

// what about cats?
// https://github.com/typelevel/cats/issues/32
// https://github.com/typelevel/cats/issues/21

class MonixTaskPrimerSpec extends FreeSpec with Matchers {

  val sc = monix.execution.Scheduler.Implicits.global

  "dipping my toes into monix task" - {

    "should work" in {
      // Executing a sum, which (due to the semantics of apply)
      // will happen on another thread. Nothing happens on building
      // this instance though, this expression is pure, being
      // just a spec! Task by default has lazy behavior ;-)
      val task = Task {
        1 + 1
      }

      // Tasks get evaluated only on runAsync!
      // Callback style:
      val cancelable: Cancelable = task.runOnComplete {
        case Success(value) =>
          println(value)
        case Failure(ex) =>
          System.out.println(s"ERROR: ${ex.getMessage}")
      }(sc)
      //=> 2

      // Or you can convert it into a Future
      val future: CancelableFuture[Int] =
        task.runAsync(sc)

      // Printing the result asynchronously
      future.foreach(println)(sc)
    }

    "should run examples from the talk" in {
      // strict / eager
      Task.now {
        println("effect eager");
        "immediate"
      }

      // lazy / memoized
      val once = Task.evalOnce {
        println(s"effect once + ${LocalDateTime.now}");
        LocalDateTime.now
      }
      once.runAsync(sc)
      once.runAsync(sc) // no effect

      // equiv to a fn - will try to execute synchronously
      val getResultAlways: Task[String] = Task.eval {
        println("effect fn");
        "always"
      }
      getResultAlways.runAsync(sc)
      getResultAlways.runAsync(sc)

      // turning into evalOnce
      val memoized = getResultAlways.memoize
      println("memoized: " + memoized)

      // task factory
      val tf = Task.defer {
        Task.now {
          println("effect fact")
        }
      }
      val ra: CancelableFuture[Unit] = tf.runAsync(sc)
      println(ra)
      println(ra)          // same
      println(tf.runAsync(sc)) // new instance

      // guarantees async exec
      Task.fork(Task.eval(println("effect fn")))
    }
  }

  "simple etl with tasks" - {

    type Response = String
    type Record   = String

    def extract(offset: Int, amount: Int) = Task {
      List.fill(amount)(offset + Random.nextInt(1000))
    }

    def transform[T, U](sourceRecs: List[T], f: T => U): Task[List[U]] = Task {
      sourceRecs map f
    }

    def load[T](targetRecs: List[T]): Task[List[Response]] = Task {
//      Thread.sleep(10000)
      //        Task.wait(10000) // scheduler cannot tick while task is waititng
      targetRecs.map(r => s"${r.toString} stored")
    }

    val tl: (List[Int], Int => Record) => Task[List[Response]] =
      (sourceRecs: List[Int], f: Int => Record) => transform(sourceRecs, f) flatMap load

    val intToString: (Int) => Record = (i: Int) => i.toString

    "should just work " in {

      val etl: Task[List[Response]] = extract(0, 3) flatMap (tl(_, intToString))

      val ts = TestScheduler()
      val f  = etl.runAsync(ts)

      // smallest possible tick?
      //      ts.tick(1 nanosecond)
      ts.tick(11.second)

      val r = Await.result(f, 10.seconds)
      println(r)
    }

    "recovering with backoff" in {

      val etl: Task[List[Response]] = extract(0, 3) flatMap (tl(_, intToString)) flatMap { _ =>
        println("failing")
        throw new IllegalStateException("failing in flatMap")
      }

      // does not recover
      val withRecovery: Task[List[Response]] = etl.onErrorRecoverWith {
        case t: Throwable =>
          println(s"failed with $t")
          etl
      }

      val withHandling: Task[Serializable] = etl.onErrorHandleWith {
        case t: Throwable =>
          println(s"failed with $t. Handling error")
          Task.now("recovered")
      }

      val ts = TestScheduler()
      val f  = withHandling.runAsync(ts)

      // smallest possible tick?
      //      ts.tick(1 nanosecond)
      ts.tick(1.second) // wihtRecovery fails here and does not recover
      ts.tick(1.second)
      ts.tick(1.second)

      val r = Await.result(f, 10.seconds)
    }

  }

}
