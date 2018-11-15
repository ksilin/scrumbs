package com.example.task

import java.io.Serializable
import java.time.LocalDateTime

import cats.effect.IO
import monix.catnap.FutureLift
import monix.execution.Cancelable
import monix.execution.schedulers.TestScheduler
import org.scalatest.{FreeSpec, Matchers, MustMatchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random
import monix.execution.CancelableFuture
import monix.eval.Task
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.util.{Failure, Success}

class MonixTaskFromFutureSpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience{

  // https://monix.io/blog/2018/11/08/tutorial-future-to-task.html

  val msc = monix.execution.Scheduler.Implicits.global

  def sum(numbers: List[Int])(implicit ec: ExecutionContext): Future[Long] = {
    Future(numbers.foldLeft(0L)(_ + _))
  }

  val l = List(1, 2, 3)

  "create monix tasks from scala futures" - {

    "simple task conversion" in {

      // this may not be what we want since the processing here is strict -
      // not a pure fn, side fx are not suspended
      def sumTask(num: List[Int])(implicit ec: ExecutionContext): Task[Long] = Task.fromFuture(sum(num))

      val t: Task[Long] = sumTask(l)
      val s = t.runToFuture.futureValue
      s mustBe 6L

      // what we can do instead is to suspend explicitly:
      def sumTaskSuspend(num: List[Int])(implicit ec: ExecutionContext): Task[Long] = Task.suspend { Task.fromFuture(sum(num))}

      // a shortcut to suspend / fromFuture
      def sumTaskDefer(num: List[Int])(implicit ec: ExecutionContext): Task[Long] = Task.deferFuture(sum(num))

      // injects scheduler on exec, not invocation
      def sumTaskAction(num: List[Int]): Task[Long] = Task.deferFutureAction{ implicit scheduler => sum(num)}

     def sumTaskLift(num: List[Int])(implicit ec: ExecutionContext): Task[Long] = FutureLift.from(Task(sum(num)))

      def sumTaskLiftDefer(num: List[Int]): Task[Long] = Task.deferAction{ implicit s => FutureLift.from(Task(sum(num)))}

      def sumIOLift(num: List[Int])(implicit ec: ExecutionContext): IO[Long] = FutureLift.from(IO(sum(num)))
    }

  }

}
