package com.example.monadT

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scalaz._
import Scalaz._

class MonadTransformerSpec extends FunSpec with Matchers with LazyLogging {

  describe("monad transformation as explained by unserscore") {

    // http://underscore.io/blog/posts/2013/12/20/scalaz-monad-transformers.html

    it("maybe getting a value or an error") {

      type Result[+A] = String \/ Option[A]

      val rEither: Result[Int] = some(42).right
      logger.info("first result {}", rEither)

      // unwrapping twice to do sth with the numeric result is sth we want to avoid

      val transformed: Result[String] = for{
        rOpt: Option[Int] <- rEither
      } yield {
        for {
          r: Int <- rOpt
        } yield r.toString
      }

      // desugared: val transformed = result map { _ map { _.toString } }
      logger.info("transformed {}", transformed)

    }

    it("using optionT trainsformer"){

      type Error[+A] = \/[String, A]
      // why define the Error type? Type inference!
      // OptionT expects M[A] where A is a single type. but \/ has 2 types
      // we could have used a type lambda to work around it:
      // type Result[A] = OptionT[{ type l[X] = \/[String, X] }#l, A]

      // Result[+A] in original text - results in error:
      // Error:(43, 12) covariant type A occurs in invariant position in type [+A]scalaz.OptionT[Error,A] of type Result

      // OptionT[M, A] is a monad transformer. It will construct an Option[A] inside M
      // first important point is the monad transformers are built from the inside out
      type Result[A] = OptionT[Error, A]

      val result: Result[Int] = 42.point[Result]
      val transformed =
        for {
          value <- result
        } yield value.toString

      logger.info("transformed {}", transformed)

      // we cannot use x.point[Result] for empty options (right vals)
      val n = None.point[Result] // OptionT(\/-(Some(None)))
      logger.info("wrapping a None naively: {}", n)

      val pre: Error[Option[Int]] = none[Int].point[Error]
      val r2: Result[Int] = OptionT(pre) // OptionT(\/-(None))
      logger.info("constructed r2: {}", r2)

      val errorMsg: String = "Error message"
      val error: \/[String, Option[Int]] = errorMsg.left
      // what about errors (left vals?)
      val err: Result[Int] = OptionT(error : Error[Option[Int]]) // OptionT(-\/(Error message))
      logger.info("constructed err: {}", err)

      // now lets combine some results


    }

    it("should work the same with Future & OptionT"){


    }
  }
}