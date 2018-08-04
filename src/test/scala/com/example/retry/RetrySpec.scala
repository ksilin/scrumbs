package com.example.retry

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import retry.{Defaults, Success}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.control.NonFatal

class RetrySpec extends FreeSpec with MustMatchers with ScalaFutures {

  // https://github.com/softwaremill/retry

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(20, Millis))

  // retry only works with futures, not with other concurrency types like Task or IO

  // an implicit execution context for executing futures
  // a definition of Success encode what "success" means for the type of your future
  // a block of code that results in a Scala Future.

  // Retry provides a set of defaults that provide retry.Success definitions for Option, Either, Try,
  // and a partial function (defined with Success.definedAt(partialFunction)) out of the box.

  "simplest retry" in {

    // Cannot find an implicit retry.Success for the given type of Future, either require one yourself or import retry.Success._
    // Success[-T](pred: T => Boolean)

    implicit val succ = Success[Unit](_ == ())

    val r: Future[Unit] = retry
      .Backoff(max = 8, delay = 500.millis, base = 2) // defaults with 0.3.0
      .apply(
        () =>
          Future {
            println("succeeded")
        }
      )
    val result = r.futureValue
    result mustBe ()
  }

  "simplest fail with backoff" in {

    // will fail and retry after 500ms & 500*2 ms

    implicit val succ = Success[Unit](_ == ())

    val r: Future[Unit] = retry
      .Backoff(max = 2, delay = 500.millis, base = 2) // defaults with 0.3.0
      .apply(
        () =>
          Future {
            println(s"trying: ${System.currentTimeMillis()}")
            throw new Exception("boom")
        }
      )

    // The future returned an exception of type: java.lang.Exception, with message: boom.
    val result = r.recover { case ex: Throwable => println(ex.getMessage) }.futureValue
    println(result)
  }

  "composable success defs" in {

    var i = 0
    var j = 0

    val succ1 = Success[(Int, Int)] {
      case (i: Int, _: Int) =>
        val res = i > 3
        println(s"i: $i, $res")
        res
    }
    val succ2 = Success[(Int, Int)] {
      case (_: Int, j: Int) =>
        val res = j > 5
        println(s"j: $j, $res")
        res
    }

    implicit val fullSucc = succ1.and(succ2)

    val run = retry
      .Pause(max = 7, delay = 100.millis)
      .apply({ () =>
        Future {
          i += 1
          j += 1
          (i, j)
        }

      })

    val done = run.futureValue

  }

  // policies - Directly, Pause, Backoff

  "custom policies" in {
    val policy = retry.When {
      case NonFatal(e) => retry.Pause(3, 1.second)
    }

    val f = () => Future{
      println(s"retrying: ${System.currentTimeMillis()}")
      throw new Exception("boom")
      None // using the built-in value for Success[Option]
    }

    val x: Future[Option[_]] = policy(f)
    x.futureValue
  }

}
