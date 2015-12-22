package com.example

import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

class FutureTimeoutSpec extends FunSpec with Matchers {

  import scala.concurrent.duration._

  implicit val sys = ActorSystem("test")
  implicit val ec = sys.dispatcher
  val successful: Future[String] = Future {"hi"}

  describe("Futures combination & timeouts") {

    import com.example.helpers.FutureTimeoutHelper._

    // actually, you should not block at all when using futures, so having them time out is not an optimal technique
    // rolling your own timeout: http://stackoverflow.com/questions/16304471/scala-futures-built-in-timeout/16305056#16305056

    describe("individual timeouts") {

      it("completing a future of list with individual timeouts") {

        val resFuture = for {
          d <- successful.orDefault(10 seconds, "timed out 1")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "timeout default")
        } yield (d, f)

        // leave some allowance - if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
        val res: (String, String) = Await.result(resFuture, 20 seconds)
        res should be(("hi", "timeout default"))
      }

      it("completing a future tuple with individual timeout - Either") {

        val resFuture: Future[(String, String)] = for {
          d <- successful.orDefault(10 seconds, "unexpected timeout")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "expected timeout")
        } yield (d, f)

        val combinedWithDefault: Future[Either[String, (String, String)]] = resFuture.orEitherDefault(10 seconds, "combined future timed out")

        // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
        val res: Either[String, (String, String)] = Await.result(combinedWithDefault, 20 seconds)

        res should be(Right("hi", "expected timeout"))
      }
    }

    describe("combination timeout") {

      it("completing a future of list with individual timeouts - common timeout fail") {

        val resFuture = for {
          d <- successful.orDefault(10 seconds, "timed out 1")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "timeout default")
        } yield (d, f)

        // without allowance - all futures fail
        intercept[java.util.concurrent.TimeoutException] {Await.result(resFuture, 1 seconds)}
      }

      it("completing a future of list with individual timeouts - common timeout fail - either") {

        val resFuture = for {
          d <- successful.orDefault(10 seconds, "timed out 1")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "timeout default")
        } yield (d, f)

        val res: Either[String, (String, String)] = try {
          Right(Await.result(resFuture, 1 seconds))
        } catch {
          case NonFatal(ex) => Left(s"failed with $ex")
        }
        res should be(Left("failed with java.util.concurrent.TimeoutException: Futures timed out after [1 second]"))
      }

      it("completing a future of list with individual timeouts - common timeout fail - try") {

        val resFuture = for {
          d <- successful.orDefault(10 seconds, "timed out 1")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "timeout default")
        } yield (d, f)

        // Try is basically an error-specific Either
        val res: Try[(String, String)] = Try {Await.result(resFuture, 1 seconds)}

        res should be('failure) // isFailure
        res shouldBe a [Failure[(String, String)]]
        // yeah, we shoudl actually be matching
        val x: Throwable = res.asInstanceOf[Failure[(String, String)]].exception
        x should have message "Futures timed out after [1 second]"
      }

      it("completing a future tuple with combined timeout try") {

        val resFuture: Future[(String, String)] = for {
          d <- successful.orDefault(10 seconds, "unexpected timeout")
          f <- {Future {Thread.sleep(2000); "awake"}}.orDefault(1 second, "expected timeout")
        } yield (d, f)

        val combinedWithDefault: Future[Either[String, (String, String)]] = resFuture.orEitherDefault(10 seconds, "combined future timed out")

        // if the timeout of Await is shorter than the timeout of the failed Future, all futures fail
        val res: Either[String, (String, String)] = Await.result(combinedWithDefault, 20 seconds)

        res should be (Right("hi", "expected timeout"))
      }

      // TODO - try scalaz disjunction (\/) and Validation

      // TODO - try scalaz Task: http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/

      // TODO - combining Future & Either from cats: http://eed3si9n.com/herding-cats/stacking-future-and-either.html

    }
  }
}
