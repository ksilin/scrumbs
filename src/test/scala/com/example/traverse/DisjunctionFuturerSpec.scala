package com.example.traverse

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class DisjunctionFuturerSpec extends FunSpec with Matchers with LazyLogging {

  // http://stackoverflow.com/questions/37327257/how-to-transform-xor-of-future-to-future-of-xor

  describe("monad transformation as explained by unserscore") {

    it("should provide correct Xor"){
      import scalaz.{Functor, \/}
      import scalaz.Scalaz._

      val f = \/.right(Future.successful(1))
      val futurized: Future[\/[Int, Int]] = Functor[Future].counzip(f)

      futurized map { x: \/[Int, Int] =>
        println(x)
        x should be(-\/(1))
      }
    }

    it("same with cats"){
      import cats.data.Xor
      import cats.implicits._
      import cats.std.future._
      import cats.syntax.bitraverse._
      import cats.syntax._

      val f: Xor[Future[Int], Future[String]] = Xor.left(Future.successful(1))
      // bisequence works on applicatives - NestedBitraverseOps in NesterTraversesyntax
      // cats.syntax.bitraverse.scala

      // TODO - why?
      // Error:(39, 46) value bisequence is not a member of cats.data.Xor[scala.concurrent.Future[Int],scala.concurrent.Future[String]]
//      val getX: Future[Xor[Int, String]] = f.bisequence
//      ^
//      val getX: Future[Xor[Int, String]] = f.bisequence

//      getX map { x: Xor[Int, String] =>
//        println(x)
//        x should be(Xor.left(1))
//      }
    }
  }
}