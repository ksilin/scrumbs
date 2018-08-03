package com.example.helpers

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.Future

object FutureTimeoutHelper {

  //  http://stackoverflow.com/questions/17466889/run-multiple-futures-in-parallel-return-default-value-on-timeout

  implicit class FutureWithTimeout[T](val f: Future[T]) extends AnyVal {

    import akka.pattern.after

    // default must be of same type
    def orDefault(t: Timeout, default: => T)(implicit system: ActorSystem): Future[T] = {
      implicit val ec = system.dispatcher
      val delayed     = after(t.duration, system.scheduler)(Future.successful(default))
      Future firstCompletedOf Seq(f, delayed)
    }

    // default can be of any type, but must use a disjunction here
    def orEitherDefault[U](t: Timeout, default: => U)(implicit system: ActorSystem): Future[Either[U, T]] = {
      implicit val ec            = system.dispatcher
      val delayed                = after(t.duration, system.scheduler)(Future.successful(default))
      val completed = Future firstCompletedOf Seq(f, delayed)
      val res = completed map {
        //  abstract type pattern U is unchecked since it is eliminated by erasure
        case dflt: U =>
          println(s"matching $completed with ${dflt.getClass}")
          Left(dflt)
          //  abstract type pattern T is unchecked since it is eliminated by erasure
        case originalSuccess: T =>
          println(s"matching $completed with ${originalSuccess.getClass}")
          Right(originalSuccess)
      }
      res
    }
  }

}
