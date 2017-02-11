package com.example.monadT

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MonadTLiftingSpec extends FunSpec with Matchers with LazyLogging {

  describe("lifting values into monad transformers") {

    // http://stackoverflow.com/questions/31517895/scala-futureoptiont-un-packing

    it("with scalaz") {

      import scalaz._
      import Scalaz._
      import scalaz.OptionT._

      val getResult: Future[Option[String]] = Future(Some("firstResult"))
      val first: OptionT[Future, String] = OptionT(getResult)

      val fut: Future[String] = Future.successful("justTheFuture")
      // in order to create an OptionT from a future, you need to use liftM
      val lifted: OptionT[Future, String] = fut.liftM[OptionT]

      // does not compile if fut.map(Some(_)) is inlined into OptionT
      val map: Future[Option[String]] = fut.map(Some(_))
      val lifted2: OptionT[Future, String] = OptionT(map)

      // simply calling point wont work, though I dont quite get why not, probably because only a single type param is accepted?
      //      Error:(30, 73) scalaz.OptionT[scala.concurrent.Future,String] takes no type parameters, expected: one
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[OptionT[Future, String]]
      //      ^

      type Res[A] = OptionT[Future, A]

      // it also wont work if you try to 'help' by providing String as the type for Res
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res[String]]

      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res]
    }

    it("lifting with cats") {

      import cats.data.OptionT
      import cats.implicits._

      val getResult: Future[Option[String]] = Future(Some("firstResult"))
      val first: OptionT[Future, String] = OptionT(getResult)

      val fut: Future[String] = Future.successful("justTheFuture")
      // in order to create an OptionT from a future, you need to use liftM

//      Error:(62, 54) could not find implicit value for parameter extract: cats.syntax.TLExtract[cats.syntax.TLExtract.SingletonMT{type MT[F[_], B] = cats.data.OptionT[F,B]},cats.syntax.TLExtract.SingletonM{type M[B] = scala.concurrent.Future[B]}]
//      val lifted: OptionT[Future, String] = fut.liftT[OptionT]
   // Error:(58, 54) not enough arguments for method liftT: (implicit extract: cats.syntax.TLExtract[cats.syntax.TLExtract.SingletonMT{type MT[F[_], B] = cats.data.OptionT[F,B]},cats.syntax.TLExtract.SingletonM{type M[B] = scala.concurrent.Future[B]}])cats.data.OptionT[[X]scala.concurrent.Future[X],String].
//      Unspecified value parameter extract.
//      val lifted: OptionT[Future, String] = fut.liftT[OptionT]
      // perhaps I have to import sth else from cats? not sure


      // sth like this could help eventually?
      //      https://github.com/stew/cats/blob/c6750c3e9da6a1622ec5d0dcc0bcca3337565e33/tests/src/test/scala/cats/tests/TransLiftTests.s
      // https://github.com/typelevel/cats/issues/659


      val lifted: OptionT[Future, String] = OptionT.liftF(fut)

      val fOpt: Future[Option[String]] = fut.map(Some(_))
      val lifted2: OptionT[Future, String] = OptionT(fOpt)

      // simply calling point wont work, though I dont quite get why not, probably because only a single type param is accepted?
      //      Error:(30, 73) scalaz.OptionT[scala.concurrent.Future,String] takes no type parameters, expected: one
      //      val liftedPrimitive: OptionT[Future, String] = "someString".point[OptionT[Future, String]]
      //      ^

      type Res[A] = OptionT[Future, A]

      // there seems to be no point method in cats, at least not where I am looking for it, no pure either
//      val liftedPrimitive: OptionT[Future, String] = "someString".point[Res]
    }

  }
}