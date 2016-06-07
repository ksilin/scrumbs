package com.example.traverse

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scalaz._

class DisjunctionFuturerSpec extends FunSpec with Matchers with LazyLogging {

  // http://stackoverflow.com/questions/37327257/how-to-transform-xor-of-future-to-future-of-xor

  describe("monad transformation as explained by unserscore") {

    it("should provide correct Xor"){
      import scalaz.{Functor, \/}

      val f = \/.right(Future.successful(1))
      val futurized: Future[\/[Int, Int]] = Functor[Future].counzip(f)

      futurized map { x: \/[Int, Int] =>
        println(x)
        x should be(-\/(1))
      }
    }

    it("same with cats"){
      import cats.data.Xor
//      import cats.std.future._
      import cats.syntax.bitraverse._

      val f: Xor[Future[Int], Future[String]] = Xor.left(Future.successful(1))
      // bisequence works on applicatives - NestedBitraverseOps in NesterTraversesyntax
      // cats.syntax.bitraverse.scala
      val getX: Future[Xor[Int, String]] = f.bisequence

      getX map { x: Xor[Int, String] =>
        println(x)
        x should be(Xor.left(1))
      }
    }
  }
}