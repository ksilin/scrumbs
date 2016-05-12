package com.example.monadT

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.Scalaz._
import scalaz._

class CustomMonadTSpec extends FunSpec with Matchers with LazyLogging {

  describe("monad transformation as explained by unserscore") {

    //  describe("working with monad transformers"){

    //    https://medium.com/coding-with-clarity/practical-monads-dealing-with-futures-of-options-8260800712f8#.3gclb2g9h

    trait Monad[T[_]] {
      def map[A, B](value: T[A])(f: A => B): T[B]

      def flatMap[A, B](value: T[A])(f: A => T[B]): T[B]

      def pure[A](x: A): T[A]
    }

    implicit val futureMonad = new Monad[Future] {
      def map[A, B](value: Future[A])(f: (A) => B) = value.map(f)

      def flatMap[A, B](value: Future[A])(f: (A) => Future[B]) = value.flatMap(f)

      def pure[A](x: A): Future[A] = Future(x)
    }

    case class OptionTransformer[T[_], A](value: T[Option[A]])(implicit m: Monad[T]) {

      def map[B](f: A => B): OptionTransformer[T, B] = {
        OptionTransformer[T, B](m.map(value) { a: Option[A] => a.map(f) })
      }

      def flatMap[B](f: A => OptionTransformer[T, B]): OptionTransformer[T, B] = {
        val result: T[Option[B]] = m.flatMap(value) { a: Option[A] =>
          a.map(b => f(b).value)
            .getOrElse(m.pure(None))
        }
        OptionTransformer[T, B](result)
      }
    }

    it("should handle future of option well") {
      val getRes1: Future[Option[String]] = Future(Some("firstResult"))
      val getRes2: Future[Option[String]] = Future(Some("secondResult"))

      val first: OptionTransformer[Future, String] = OptionTransformer(getRes1)
      val second: OptionTransformer[Future, String] = OptionTransformer(getRes2)

      // TODO - why is this an error?
//      Error:(58, 32) value filter is not a member of OptionTransformer[scala.concurrent.Future,String]
//        firstResult: String <- first
//      ^
//      val sum: OptionTransformer[Future, String] = for {
//        firstResult: String <- first
//        secondResult: String <- second
//      } yield {
//        firstResult + secondResult
//      }
//      sum.value
    }

  }
}