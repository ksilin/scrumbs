package com.example.helpers

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.Future

object FutureTimeoutHelper {

//  http://stackoverflow.com/questions/17466889/run-multiple-futures-in-parallel-return-default-value-on-timeout

  implicit class FutureWithTimeout[T](val f: Future[T]) extends AnyVal {

    import akka.pattern.after

    def orDefault(t: Timeout, default: => T)(implicit system: ActorSystem): Future[T] = {
      implicit val ec = system.dispatcher
      val delayed = after(t.duration, system.scheduler)(Future.successful(default))
      Future firstCompletedOf Seq(f, delayed)
    }

    def orEitherDefault[U](t: Timeout, default: => U)(implicit system: ActorSystem): Future[Either[U, T]]  = {
      implicit val ec = system.dispatcher
      val delayed = after(t.duration, system.scheduler)(Future.successful(default))
      val completed: Future[Any] = Future firstCompletedOf Seq(f, delayed)
      val res: Future[Either[U, T]] = completed match {
        case succ: Future[T] => succ.map(Right(_))
        case dflt: Future[U] => dflt.map(Left(_))
      }
      res
    }
  }
}