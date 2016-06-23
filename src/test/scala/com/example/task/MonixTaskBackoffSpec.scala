package com.example.task

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{AsyncFreeSpec, Matchers}

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

class MonixTaskBackoffSpec extends AsyncFreeSpec with Matchers {

  // TODO - allow more than 2 splits,
  // TODO - allow splits running sequentially
  // in order to reduce workload, need to pass the params of the original task as well
  def retryBackoff[A](getTask: (List[Int]) => Task[List[A]], unitsOfWork: List[Int], minBatch: Int,
                      maxRetries: Int, firstDelay: FiniteDuration): Task[List[A]] = {

    println(s"running with ${unitsOfWork.size} units of work, min size: $minBatch. retries: $maxRetries, delay: $firstDelay")

    getTask(unitsOfWork).onErrorHandleWith {
      case ex: Exception => {
        println(s"encountered exception: $ex")
        if (maxRetries > 0 && unitsOfWork.size >= minBatch) {
          splitAndZip(getTask, unitsOfWork, minBatch, maxRetries, firstDelay)
        }
        else
          Task.raiseError(new IllegalStateException(s"unable to complete task because of $ex"))
      }
    }
  }

  def retryBackoffTry[A](getTask: (List[Int]) => Task[List[A]], unitsOfWork: List[Int], minBatch: Int,
                         maxRetries: Int, firstDelay: FiniteDuration): Task[List[A]] = {
    println(s"running with ${unitsOfWork.size} units of work, min size: $minBatch. retries: $maxRetries, delay: $firstDelay")

    val asTry: Task[Try[List[A]]] = getTask(unitsOfWork).materialize
    asTry flatMap { t: Try[List[A]] =>
      t match {
        case Success(res) => Task.now(res)
        case Failure(ex) => {
          if (maxRetries > 0 && unitsOfWork.size >= minBatch) {
            splitAndZip(getTask, unitsOfWork, minBatch, maxRetries, firstDelay)
          }
          else
            Task.raiseError(new IllegalStateException(s"unable to complete task because of $ex"))
        }
      }
    }
  }

  def splitAndZip[A](getTask: (List[Int]) => Task[List[A]], unitsOfWork: List[Int], minBatch: Int, maxRetries: Int, firstDelay: FiniteDuration): Task[List[A]] = {
    val workSplits = unitsOfWork.splitAt(unitsOfWork.size / 2)
    val zipped: Task[(List[A], List[A])] =
      Task.zip2(
        retryBackoff(getTask, workSplits._1, minBatch, maxRetries - 1, firstDelay * 2)
          .delayExecution(firstDelay),
        retryBackoff(getTask, workSplits._2, minBatch, maxRetries - 1, firstDelay * 2)
          .delayExecution(firstDelay))
    zipped.map(l => l._1 ++ l._2)
  }

  "retry and backoff" - {

    def failingTask(unitsOfWork: List[Int]) = {
      println(s"creating new task with $unitsOfWork units of work")
      Task {
        if (unitsOfWork.size > 64) throw new TimeoutException(s"$unitsOfWork is too much man, too much")
        else unitsOfWork map (_ => Random.nextInt(1000))
      }
    }

    "backing off explonentially" in {

      val unitsOfWork: List[Int] = List.fill(500)(Random.nextInt(1000))
      val t: Task[List[Int]] = retryBackoff[Int](failingTask, unitsOfWork, 8, 10, 10 millisecond)

      val f = t.runAsync

      f map {
        r =>
          println(s"completed task: $r")
          r.size should be(500)
      }
    }

    "failing once max retries reached" in {

      val unitsOfWork: List[Int] = List.fill(500)(Random.nextInt(1000))
      val t: Task[List[Int]] = retryBackoff[Int](failingTask, unitsOfWork, 8, 2, 10 millisecond)

      val f = t.runAsync

      val ex: Future[IllegalStateException] = recoverToExceptionIf[IllegalStateException](f)
      ex map {
        r: IllegalStateException =>
          println(s"completed task: \n$r")
          r.getMessage should startWith("unable to complete task because of java.util.concurrent.TimeoutException")
      }
    }

    "failing once min units of work reached" in {

      val unitsOfWork: List[Int] = List.fill(500)(Random.nextInt(1000))
      val t: Task[List[Int]] = retryBackoff[Int](failingTask, unitsOfWork, 300, 10, 10 millisecond)

      val f = t.runAsync

      val ex: Future[IllegalStateException] = recoverToExceptionIf[IllegalStateException](f)
      ex map {
        r: IllegalStateException =>
          println(s"completed task: \n$r")
          r.getMessage should startWith("unable to complete task because of java.util.concurrent.TimeoutException")
      }
    }

  "backing off explonentially with try" in {

    val unitsOfWork: List[Int] = List.fill(500)(Random.nextInt(1000))
    val t: Task[List[Int]] = retryBackoffTry[Int](failingTask, unitsOfWork, 8, 10, 10 millisecond)

    val f = t.runAsync

    f map {
      r =>
        println(s"completed task: $r")
        r.size should be(500)
    }
  }
  }

}
