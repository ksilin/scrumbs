package com.example.future

import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class FutureTimeoutSpec extends AsyncFunSpec with Matchers with BeforeAndAfterEach {

  import com.example.helpers.FutureTimeoutHelper._

  import scala.concurrent.duration._

  implicit val sys = ActorSystem("test")
  implicit val ec  = sys.dispatcher

  val expectedTimeoutMsg: String   = "expected timeout"
  val unexpectedTimeoutMsg: String = "unexpected timeout"

  def succeedingFuture         = Future.successful("hi").orDefault(10.seconds, unexpectedTimeoutMsg)
  def twoSecondsFuture            = Future { Thread.sleep(2000); 42 }
  def timeoutFutureWithDefault = (Future { Thread.sleep(200); 42 }).orDefault(1 millisecond, 23)

  def combinedFuture: Future[(String, String)] =
    for {
      d <- Future.successful("hi").orDefault(10.seconds, unexpectedTimeoutMsg)
      f <- (Future { Thread.sleep(2000); "awake" }).orDefault(1.second, expectedTimeoutMsg)
    } yield (d, f)

  describe("Futures combination & timeouts") {

    // actually, you should not block at all when using futures, so having them time out is not an optimal technique
    // rolling your own timeout: http://stackoverflow.com/questions/16304471/scala-futures-built-in-timeout/16305056#16305056

    describe("individual timeouts") {

      it("completing a future of list with individual timeouts - simple") {
        // leave some allowance - if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
        val res: (String, String) = Await.result(combinedFuture, 2.seconds)
        res should be(("hi", expectedTimeoutMsg))
      }
    }

    describe("common timeout") {

      it("common timeout fail") {
        // without allowance - all futures fail
        val ex = intercept[java.util.concurrent.TimeoutException] {
          Await.result(twoSecondsFuture, 1.seconds)
        }
        ex should have message ("Futures timed out after [1.second]") // needs Future[Assertion]
      }

      describe("Either") {

        it("mapping to Either on timeout fail") {
          val res: Either[String, Int] = try {
            Right(Await.result(twoSecondsFuture, 1 milli))
          } catch {
            case NonFatal(ex) => Left(s"failed with $ex")
          }
          res should be(
            Left("failed with java.util.concurrent.TimeoutException: Futures timed out after [1 millisecond]")
          )
        }

        it("completing a future tuple with combined timeout - default") {

          val combinedWithDefault: Future[Either[String, Int]] =
            twoSecondsFuture.orEitherDefault[String](1.second, "future timed out")
          val res: Either[String, Int] = Await.result(combinedWithDefault, 2.seconds)
          res should be(Left("future timed out"))
        }

        it("failing a future tuple with combined timeout - default") {
          val combinedWithDefault: Future[Either[String, (String, String)]] =
            combinedFuture.orEitherDefault[String](1 millisecond, "combined future timed out")
          // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
          val res: Either[String, (String, String)] = Await.result(combinedWithDefault, 20.seconds)
          res should be(Left("combined future timed out"))
        }
      }

      // Try is an error-specific Either
      describe("Try") {
        it("common timeout fail") {
          val res: Try[(String, String)] = Try {
            Await.result(combinedFuture, 1.seconds)
          }
          res should be('failure) // isFailure
          res shouldBe a[Failure[(String, String)]]
          val x: Throwable = res.asInstanceOf[Failure[(String, String)]].exception
          x should have message "Futures timed out after [1.second]"
        }

        it("common timeout success") {
          val res: Try[(String, String)] = Try {
            Await.result(combinedFuture, 10.seconds)
          }
          res shouldBe a[Success[(String, String)]]
          res should be(Success("hi", expectedTimeoutMsg))
        }
      }
    }

    // TODO - try scalaz disjunction (\/) and Validation

    // TODO - try scalaz Task: http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/

    // TODO - combining Future & Either from cats: http://eed3si9n.com/herding-cats/stacking-future-and-either.html
  }
}
