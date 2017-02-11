package com.example.task

import java.time.LocalDateTime

import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Await

// A Future type that is also Cancelable
import monix.reactive._
import concurrent.duration._
import concurrent.Future

class ObservablePrimerSpec extends FunSpec with Matchers {

  describe("dipping my toes into monix observable") {

    it("should work") {

      val tick = Observable.interval(1.second)
        .filter( _ % 2 == 0)
        .map(_ * 2)
        .flatMap { x =>
          val now: LocalDateTime = LocalDateTime.now()
          Observable.fromIterable(Seq(s"${now} : $x", s"${now} : ${x + 1}"))
        }.take(6)
        .dump(s"Out") // string interpolated once

      val t: Cancelable = tick.subscribe()

      val f = Future { Thread.sleep(8000); 10 }
      val r = Await.result(f, 10.seconds)
      r should be(10)
    }
  }

}
