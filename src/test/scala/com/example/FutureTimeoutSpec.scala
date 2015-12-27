package com.example

import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Failure, Try}

class FutureTimeoutSpec extends AsyncFunSpec with Matchers with BeforeAndAfterEach {

  import scala.concurrent.duration._
  import com.example.helpers.FutureTimeoutHelper._

  implicit val sys = ActorSystem("test")
  implicit val ec = sys.dispatcher

  val expectedTimeoutMsg: String = "expected timeout"
  val unexpectedTimeoutMsg: String = "unexpected timeout"

  var resFuture: Future[(String, String)] = _

  override def beforeEach() = {
    resFuture = for {
      d <- (Future {"hi"}).orDefault(10 seconds, unexpectedTimeoutMsg)
      f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, expectedTimeoutMsg)
    } yield (d, f)
  }

  describe("Futures combination & timeouts") {

    // actually, you should not block at all when using futures, so having them time out is not an optimal technique
    // rolling your own timeout: http://stackoverflow.com/questions/16304471/scala-futures-built-in-timeout/16305056#16305056


    describe("individual timeouts") {

      it("completing a future of list with individual timeouts - simple") {
        // leave some allowance - if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
        val res: (String, String) = Await.result(resFuture, 20 seconds)
        res should be(("hi", expectedTimeoutMsg))
      }
    }

    describe("common timeout") {

      it("common timeout fail") {
        // without allowance - all futures fail
        val ex = intercept[java.util.concurrent.TimeoutException] {
          Await.result(resFuture, 1 seconds)
        }
        ex should have message ("Futures timed out after [1 second]") // needs Future[Assertion]
      }

      describe("Either") {

        it("completing a future of list with individual timeouts - common timeout fail - either") {
          val res: Either[String, (String, String)] = try {
            Right(Await.result(resFuture, 1 seconds))
          } catch {
            case NonFatal(ex) => Left(s"failed with $ex")
          }
          res should be(Left("failed with java.util.concurrent.TimeoutException: Futures timed out after [1 second]"))
        }

        it("completing a future tuple with combined timeout - default") {
          val combinedWithDefault: Future[Either[String, (String, String)]] =
            resFuture.orEitherDefault[String](10 seconds, "combined future timed out")
          // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
          val res: Either[String, (String, String)] = Await.result(combinedWithDefault, 20 seconds)
          // TODO  see failed impl for orEitherDefault
          res should be(Right("hi", expectedTimeoutMsg))
        }

        it("failing a future tuple with combined timeout - default") {
          val combinedWithDefault: Future[Either[String, (String, String)]] =
            resFuture.orEitherDefault[String](1 millisecond, "combined future timed out")
          // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
          val res: Either[String, (String, String)] = Await.result(combinedWithDefault, 20 seconds)
          res should be(Left("combined future timed out"))
        }
      }

      describe("Try") {

        it("common timeout fail") {
          // Try is basically an error-specific Either
          val res: Try[(String, String)] = Try {
            Await.result(resFuture, 1 seconds)
          }

          res should be('failure) // isFailure
          res shouldBe a[Failure[(String, String)]]
          // yeah, we should actually be matching
          val x: Throwable = res.asInstanceOf[Failure[(String, String)]].exception
          x should have message "Futures timed out after [1 second]"
        }

        it("common timeout success") {

          val res: Try[(String, String)] = Try {
            Await.result(resFuture, 10 seconds)
          }
          res shouldNot be('failure) // isFailure
          res shouldBe a[Success[(String, String)]]
          // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail

          res should be(Success("hi", expectedTimeoutMsg))
        }
      }
    }

      // TODO - try scalaz disjunction (\/) and Validation

      // TODO - try scalaz Task: http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/

      // TODO - combining Future & Either from cats: http://eed3si9n.com/herding-cats/stacking-future-and-either.html

  }
}
