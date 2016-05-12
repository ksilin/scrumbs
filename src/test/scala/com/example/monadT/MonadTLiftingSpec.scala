package com.example.monadT


import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MonadTLiftingSpec extends FunSpec with Matchers with LazyLogging {

  describe("lifting values into monad transformers") {

    // http://stackoverflow.com/questions/31517895/scala-futureoptiont-un-packing

    it(" with scalaz") {

      import scalaz._
      import Scalaz._
      import scalaz.OptionT._

      val getResult: Future[Option[String]] = Future(Some("firstResult"))
      val first: OptionT[Future, String] = OptionT(getResult)

      val fut: Future[String] = Future.successful("justTheFuture")
      // in order to create an OptionT from a future, you need to use liftM
      val lifted: OptionT[Future, String] = fut.liftM[OptionT]
      val lifted2: OptionT[Future, String] = OptionT(fut.map(Some(_)))

      // simply calling point wont work, though I dont quite get why not, probably because only a single type param is accepted?
      //      Error:(30, 73) scalaz.OptionT[scala.concurrent.Future,String] takes no type parameters, expected: one
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[OptionT[Future, String]]
      //      ^

      type Res[A] = OptionT[Future, A]

      // it also wont work if you try to 'help' by providing String as the type for Res
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res[String]]

      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res]
    }

    it(" with cats") {
      import cats.data.OptionT
      import cats.data.OptionT._
      import cats.data._
      import cats.syntax.transLift._

      val getResult: Future[Option[String]] = Future(Some("firstResult"))
      val first: OptionT[Future, String] = OptionT(getResult)

      val fut: Future[String] = Future.successful("justTheFuture")
      // in order to create an OptionT from a future, you need to use liftM
      val lifted: OptionT[Future, String] = fut.liftT[OptionT]
      val lifted2: OptionT[Future, String] = OptionT(fut.map(Some(_)))

      // simply calling point wont work, though I dont quite get why not, probably because only a single type param is accepted?
      //      Error:(30, 73) scalaz.OptionT[scala.concurrent.Future,String] takes no type parameters, expected: one
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[OptionT[Future, String]]
      //      ^

      type Res[A] = OptionT[Future, A]

      // it also wont work if you try to 'help' by providing String as the type for Res
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res[String]]

      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res]
    }

  }
}