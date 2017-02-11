package com.example.monadT

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

trait Monad[T[_]] {
  def map[A, B](value: T[A])(f: A => B): T[B]

  def flatMap[A, B](value: T[A])(f: A => T[B]): T[B]

  def pure[A](x: A): T[A]
}

case class OptionTransformer[T[_], A](value: T[Option[A]])(implicit m: Monad[T]) {

  def map[B](f: A => B): OptionTransformer[T, B] = {
    OptionTransformer[T, B](m.map(value)(_.map(f)))
  }

  def flatMap[B](f: A => OptionTransformer[T, B]): OptionTransformer[T, B] = {
    val result: T[Option[B]] = m.flatMap(value)(
      a =>
        a.map(b => f(b).value)
          .getOrElse(m.pure(None))
    )
    OptionTransformer[T, B](result)
  }
}

class CustomMonadTSpec extends FunSpec with Matchers with LazyLogging {

  describe("monad transformation as explained by matt") {

    //    https://medium.com/coding-with-clarity/practical-monads-dealing-with-futures-of-options-8260800712f8#.3gclb2g9h
    implicit val futureMonad = new Monad[Future] {
      def map[A, B](value: Future[A])(f: (A) => B) = value.map(f)

      def flatMap[A, B](value: Future[A])(f: (A) => Future[B]) = value.flatMap(f)

      def pure[A](x: A): Future[A] = Future(x)
    }


    it("original test") {
      // TODO - Some instead of Option fails:
//      https://github.com/mattfowler/MonadTransformers/issues/2
//      val one: OptionTransformer[Future, Int] = OptionTransformer(Future(Some(1)))
      val one: OptionTransformer[Future, Int] = OptionTransformer(Future(Option(1)))
      val two: OptionTransformer[Future, Int] = OptionTransformer(Future.successful(Option(2)))

      val getFO: (Int) => Future[Option[Int]] =
        (in) => Future.successful(Some(in + 1))

      // TODO - adding Int as type like resultOne: Int breaks it:
      // value filter is not a member of
      // com.example.monadT.OptionTransformer[scala.concurrent.Future,Int] [error] resultOne: Int <- one
      // https://github.com/mattfowler/MonadTransformers/issues/1
      val summed = for {
        resultOne <- OptionTransformer(getFO(0))
        resultTwo <- two
      } yield {
        resultOne + resultTwo
      }

      val result = Await.result(summed.value, Duration.Inf)

      result should be(Some(3))
    }

    it("should handle future of option well") {
      val getFO: (String) => Future[Option[String]] =
        (in) => Future.successful(Some(in + Random.alphanumeric.take(5).mkString))

      val getRes: OptionTransformer[Future, String] = for {
        f <- OptionTransformer(getFO("one"))
        g <- OptionTransformer(getFO(f))
      } yield g

      val res = Await.result(getRes.value, 10.seconds)

      println(res)
    }

  }
}
