package com.example

import akka.actor.ActorSystem
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class FutureToTrySpec extends AsyncFunSpec with Matchers with BeforeAndAfterEach {

  //  http://stackoverflow.com/questions/29344430/scala-waiting-for-sequence-of-futures

  //  One common approach to waiting for all results (failed or not) is to "lift" failures into a new representation
  // inside the future, so that all futures complete with some result (although they may complete with a result
  // that represents failure). One natural way to get that is lifting to a Try.
  //    Twitter's implementation of futures provides a liftToTry method

  import scala.concurrent.duration._

  implicit val sys = ActorSystem("test")
  implicit val ec = sys.dispatcher

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = f.map(Success(_)).recover { case x => Failure(x) }

  var resFuture: List[Future[(String)]] = _
  var seq: Future[List[Try[String]]] = _
  var tries: List[Future[Try[String]]] = _

  override def beforeEach() = {
    resFuture = List(Future {
      "hi"
    },
      Future.failed {
        new Exception("boom")
      })
    tries = resFuture.map { x => futureToFutureTry(x) }
    seq = Future.sequence(tries)
  }

  describe("Future sequence reaping") {

    it("completing a future of list with individual timeouts - blocking") {
      val res: List[Try[String]] = Await.result(seq, 20 seconds)
      val successes: List[String] = res.collect { case Success(x) => x }
      val failures: List[Throwable] = res.collect { case Failure(x) => x }
      successes should be(List("hi"))
      failures should have size 1
      failures.head should have message "boom"
    }

    it("completing a future of list with individual timeouts - async") {
      seq map { s => s.collect { case Success(x) => x } } map { x => x should be(List("hi")) }
      seq map { s => s.collect { case Failure(x) => x } } map { x =>
        x should have size 1
        x.head should have message "boom"
      }
    }
  }

  // TODO - try scalaz disjunction (\/) and Validation

  // TODO - try scalaz Task: http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/

  // TODO - combining Future & Either from cats: http://eed3si9n.com/herding-cats/stacking-future-and-either.html
}
