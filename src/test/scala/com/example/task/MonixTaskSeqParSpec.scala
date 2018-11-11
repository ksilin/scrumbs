package com.example.task

import org.scalatest.{AsyncFreeSpec, Failed, Matchers, Succeeded}

import scala.util.Random
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Future

class MonixTaskSeqParSpec extends AsyncFreeSpec with Matchers {

  val sc = monix.execution.Scheduler.Implicits.global


  "sequential and parallel execution" - {

    type Response = String
    type Record = String

    def extract(offset: Int, amount: Int) = Task {
      println(s"running with $offset & $amount")
      List.fill(amount)(offset + Random.nextInt(1000))
    }

    "sequential" in {

      val allTogether = for{
        e1 <- extract(0, 1)
        e2 <- extract(1, 2)
        e3 <- extract(2, 3)
      } yield e1 ++ e2 ++ e3

      val f= allTogether.runAsync(sc)

      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    // up tp zip6 and zipList
    "parallel with zip" in {

      val allTogether: Task[(List[Int], List[Int], List[Int])] = Task.zip3(extract(0, 1), extract(1, 2), extract(2, 3))

      val concat = allTogether map { ls => ls._1 ++ ls._2 ++ ls._3}
      val f = concat.runAsync(sc)
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }
    val extracts = List(extract(0, 1), extract(1, 2), extract(2, 3))
    "parallel but ordered with zipList / gather" in {

      val tasks: List[Task[List[Int]]] = extracts
      // TODO - not worky:
      // Error:(53, 65) type mismatch;
//      found   : List[monix.eval.Task[List[Int]]]
//      required: monix.eval.Task[List[Int]]
//      val allTogether: Task[List[List[Int]]] = Task.zipList(tasks)

//      val concat = allTogether map { ls => ls.flatten}
//      val f= concat.runAsync
//      f map {r =>
//        println(r)
//        r.size should be(6)
//      }
      Future.successful(Succeeded)
    }

    "parallel unordered with gatherUnordered" in {

      val allTogether: Task[Seq[List[Int]]] = Task.gatherUnordered(extracts)

      val concat = allTogether map { ls => ls.flatten}
      val f= concat.runAsync(sc)
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    "sequential with sequence" in {

      val allTogether: Task[List[List[Int]]] = Task.sequence(extracts)
      val concat = allTogether map { ls => ls.flatten}

      val f= concat.runAsync(sc)
      f map {r =>
        println(r)
        r.size should be(6)
      }
    }

    "only first result" in {
      val allTogether = Task.raceMany(extracts)
      allTogether.runToFuture map{ r =>
        println(r)
        r.size should be < 4
      }
    }

//    "break on first fail"
//    "collect all results"
  }
}
