package com.example.task

import java.io.Serializable

import monix.execution.Scheduler.Implicits.global
import org.scalatest.{AsyncFreeSpec, FreeSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.util.Random

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task

// In order to evaluate tasks, we'll need a Scheduler

// A Future type that is also Cancelable

// Task is in monix.eval

// https://monix.io/docs/2x/eval/task.html
// https://vimeo.com/channels/flatmap2016/165922572
// https://www.dropbox.com/s/9yz6bdzumzk0y1p/Monix-Task.pdf

// what about cats?
// https://github.com/typelevel/cats/issues/32
// https://github.com/typelevel/cats/issues/21

class MonixTaskSeqParSpec extends AsyncFreeSpec with Matchers {

  "sequential and parallel execution" - {

    type Response = String
    type Record = String

    def extract(offset: Int, amount: Int) = Task {
      println(s"running with $offset & $amount")
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

    val intToString: (Int) => Record = (i: Int) => i.toString

    "sequential" in {

      val allTogether = for{
        e1 <- extract(0, 1)
        e2 <- extract(1, 2)
        e3 <- extract(2, 3)
      } yield (e1 ++ e2 ++ e3)

      val f= allTogether.runAsync

      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    // up tp zip6 and zipList
    "parallel with zip" in {

      val allTogether: Task[(List[Int], List[Int], List[Int])] = Task.zip3(extract(0, 1), extract(1, 2), extract(2, 3))

      val concat = allTogether map { ls => ls._1 ++ ls._2 ++ ls._3}

      val f= concat.runAsync

      f map {r =>
        println(r)
        r.size should be(6)
      }
    }
    "parallel but ordered with zipList / gather" in {

      val allTogether: Task[List[List[Int]]] = Task.zipList(List(extract(0, 1), extract(1, 2), extract(2, 3)))

      val concat = allTogether map { ls => ls.flatten}
      val f= concat.runAsync
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    "parallel unordered with gatherUnordered" in {

      val allTogether: Task[Seq[List[Int]]] = Task.gatherUnordered(List(extract(0, 1), extract(1, 2), extract(2, 3)))

      val concat = allTogether map { ls => ls.flatten}
      val f= concat.runAsync
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    "sequential with sequence" in {

      val allTogether: Task[List[List[Int]]] = Task.sequence(List(extract(0, 1), extract(1, 2), extract(2, 3)))
      val concat = allTogether map { ls => ls.flatten}

      val f= concat.runAsync
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    "only first result" in {
      val allTogether: Task[List[Int]] = Task.chooseFirstOfList(List(extract(0, 1), extract(1, 2), extract(2, 3)))
      allTogether.runAsync map{ r =>
        println(r)
        r.size should be < 4
      }
    }

//    "break of first fail"
//    "collect all results"
  }

}
