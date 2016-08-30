package com.example.traverse

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DisjunctionFutureSpec extends FunSpec with Matchers with LazyLogging {

  // http://stackoverflow.com/questions/37327257/how-to-transform-xor-of-future-to-future-of-xor

  describe("monad transformation as explained by unserscore") {

    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    it("should provide correct Xor") {
      import scalaz.{Functor, \/}
      import scalaz._
      import scalaz.Scalaz._

      val f = \/.right(Future.successful(1))
      val futurized: Future[\/[Int, Int]] = Functor[Future].counzip(f)

      futurized map { x: \/[Int, Int] =>
        println(x)
        x should be(-\/(1))
      }
    }
  }

    it("same with cats"){
      import cats.data.Xor
      import cats.instances.future._
      import cats.syntax.bitraverse._
      import cats.Bitraverse

//      val f: Xor[Future[Int], Future[String]] = Xor.left(Future.successful(1))
      val f: Xor[Future[Int], Future[String]] = Xor.right(Future.successful("x"))
      // bisequence works on applicatives - NestedBitraverseOps in NesterTraversesyntax
//       cats.syntax.bitraverse.scala

      val bt = implicitly[Bitraverse[Xor]]
      println(bt)
      val flipped = bt.bisequence(f)
      println(s"flipped: $flipped")

      def flip[A, B](x: Xor[Future[A], Future[B]]): Future[Xor[A, B]] = x.bisequence
      val getX: Future[Xor[Int, String]] = flip(f)

      getX map { x: Xor[Int, String] =>
        println(x)
        x should be(Xor.left(1))
      }
    }
}