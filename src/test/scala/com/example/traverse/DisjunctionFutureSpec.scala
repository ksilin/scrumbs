package com.example.traverse

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DisjunctionFutureSpec extends FunSpec with Matchers with LazyLogging {

  // http://stackoverflow.com/questions/37327257/how-to-transform-xor-of-future-to-future-of-xor

  describe("monad transformation as explained by unserscore") {

    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    it("should provide correct Either") {
      import scalaz.{ Functor, \/ }
      import scalaz._
      import scalaz.Scalaz._

      val f                               = \/.right(Future.successful(1))
      val futurized: Future[\/[Int, Int]] = Functor[Future].counzip(f)

      futurized map { x: \/[Int, Int] =>
        println(x)
        x should be(-\/(1))
      }
    }
  }

  it("same with cats") {
    import cats.implicits._
    import cats.Bitraverse

//      val f: Either[Future[Int], Future[String]] = Either.left(Future.successful(1))
    val f: Either[Future[Int], Future[String]] = Right(Future.successful("x"))
    // bisequence works on applicatives - NestedBitraverseOps in NesterTraversesyntax
//       cats.syntax.bitraverse.scala

    val bt = implicitly[Bitraverse[Either]]
    println(bt)
    val flipped = bt.bisequence(f)
    println(s"flipped: $flipped")

    def flip[A, B](x: Either[Future[A], Future[B]]): Future[Either[A, B]] = x.bisequence
    val getX: Future[Either[Int, String]]                                 = flip(f)

    getX map { x: Either[Int, String] =>
      println(x)
      x should be(Left(1))
    }
  }
}
