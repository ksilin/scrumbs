package com.example

import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.{ Await, Future }

class FutureCombinationSpec extends AsyncFunSpec with Matchers with BeforeAndAfterEach {

  import scala.concurrent.duration._

  implicit val sys = ActorSystem("test")

  var successful: Future[String] = _
  val errorMsg: String           = "boom!"
  var throwing: Future[String]   = _

  override def beforeEach() = {
    successful = Future.successful("hi")
    throwing = Future { throw new Exception(errorMsg) }
  }

  describe("Futures combination & recovery") {

    it("should explode") {
      val resFuture = for {
        s <- successful
        f <- throwing
      } yield (s, f)

//      recoverToSucceededIf[Exception] {resFuture}
      val ex = recoverToExceptionIf[Exception] { resFuture }

//      val ex: Exception = intercept[Exception] {Await.result(resFuture, 1.second)}
      ex map { e =>
        e should have message (errorMsg)
      }
    }
    // an [Exception] should be thrownBy {Await.result(resFuture, 1.second)} works as well, bit returns an Assertion
    // or, simpler: assertThrows[Exception], also returns an Assertion

    it("should recover with another future") {
      // recoverWith if we are doing to do something with the error
      val resFuture: Future[(String, String)] = for {
        s <- successful

        f <- throwing.recoverWith {
          case e => Future.successful(s"failed with: $e")
        }
      } yield (s, f)

      resFuture map (_ should be(("hi", "failed with: java.lang.Exception: boom!")))
    }

    it("should fallback") {
      val recoveryText: String = "failed, but its ok"
      val resFuture: Future[(String, String)] = for {
        s <- successful
        // fallbackTo if we dont care for error details
        f <- throwing.fallbackTo {
          Future.successful(recoveryText)
        }
      } yield (s, f)
      // while mapping / returning Future[TestResult] works fine, Await.result times out. Why?
      resFuture map (e => e should be(("hi", recoveryText)))
    }

    it("completing a future of list, correctly mapping recovery and successes") {
      val futures = List(successful, throwing)

      val withRecovery = futures map {
        _.recoverWith { case e => Future(s"failed with: $e") }
      }
//      A Future produced by Future.sequence completes when either:
//      all the futures have completed successfully, or
//      one of the futures has failed

      val sequence: Future[List[String]] = Future.sequence(withRecovery)

//      val res: List[String] = Await.result(sequence, 1.second) // times out. Why?
      sequence.map(l => l should be(List("hi", "failed with: java.lang.Exception: boom!")))
    }
  }
}
