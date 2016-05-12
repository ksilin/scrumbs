package com.example

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class EitherVsTrySpec extends FunSpec with Matchers {

  // TODO - Either & Try dont implement Product and Serializable, so the type inference runs wild with them ,but I dont get why the two traits are even needed here
  // relevant discussion
  // https://groups.google.com/forum/#!topic/scala-internals/p1OETcWOo8Y
  // https://github.com/scala/scala-abide/issues/41

  describe("Either") {

    // TODO - use right projection for bias

      it("either") {

        val result: Future[Int] = Future { throw new Exception("boom!") }


        val maybeDataFuture: Future[Either[Throwable, Int] with Product with Serializable] = result.map(Right(_)).recover { case x => Left(x) }

        val maybeData: Either[Throwable, Int] with Product with Serializable = Await.result(maybeDataFuture, 10 seconds)

        val recovered = maybeData match {
          case Left(t)  => s"failure: $t"
          case Right(x) => x
        }
        println(s"found $recovered ")
      }

      it("try") {

        val result: Future[Int] = Future { throw new Exception("boom!") }

        val maybeDataFuture: Future[Try[Int] with Product with Serializable] = result.map(Success(_)).recover { case x => Failure(x) }

        val maybeData: Try[Int] with Product with Serializable = Await.result(maybeDataFuture, 10 seconds)

        // TODO - ok, but how do we get the failure
        val mapped: String = maybeData.map(i => i.toString).getOrElse(s"failure")

        val mapped2: Try[String] = maybeData.map(i => i.toString).recover { case x: Throwable => s"failure: $x" }

        println(mapped)
      }
  }

}