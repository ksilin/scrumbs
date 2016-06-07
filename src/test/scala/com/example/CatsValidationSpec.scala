package com.example

import cats.data.ValidatedNel
import cats.syntax.CartesianBuilder
import org.scalatest.{FunSpec, Matchers}

class CatsValidationSpec extends FunSpec with Matchers {

  describe("cats Validated & Xor") {

    trait Read[A] {def read(s: String): Option[A]}

    object Read {
      def apply[A](implicit A: Read[A]): Read[A] = A

      implicit val stringRead: Read[String] = new Read[String] {
        def read(s: String): Option[String] = Some(s)
      }
      implicit val intRead: Read[Int] = new Read[Int] {
        def read(s: String): Option[Int] =
          if (s.matches("-?[0-9]+")) Some(s.toInt)
          else None
      }
    }

    sealed abstract class ConfigError // TODO - why not trait?
    final case class MissingConfig(field: String) extends ConfigError
    final case class ParseError(field: String) extends ConfigError


    import cats.data.Validated
    import cats.data.Validated.{Invalid, Valid}

    case class Conf(map: Map[String, String]) {

      // TODO - this A: Read is implicit, change to other notation
      def parse[A: Read](key: String): Validated[ConfigError, A] =
        map.get(key) match {
          case None => Invalid(MissingConfig(key))
          case Some(value) => Read[A].read(value) match {
            case None => Invalid(ParseError(key))
            case Some(a) => Valid(a)
          }
        }
    }

    it("validates") {

      val c = Conf(Map(("endpoint", "127.0.0.1"), ("port", "not an int")))
      println(c)
      val port: Validated[ConfigError, Int] = c.parse[Int]("port")
      port should be(Invalid(ParseError("port")))
    }

    it("supports parallel validation") {
      val c = Conf(Map(("endpoint", "127.0.0.1"), ("port", "not an int")))

      import cats.Semigroup
      import cats.SemigroupK
      import cats.data.NonEmptyList
      import cats.std.list._ // For semigroup (append) on List

      def parallelValidate[E: Semigroup, A, B, C](v1: Validated[E, A], v2: Validated[E, B])(f: (A, B) => C): Validated[E, C] =
        (v1, v2) match {
          case(Valid(a), Valid(b)) => Valid(f(a, b))
            case(Valid(_), i@Invalid(_)) => i
            case(i@Invalid(_), Valid(_)) => i
            case(Invalid(e1), Invalid(e2)) => Invalid(Semigroup[E].combine(e1, e2))
        }

      val port: ValidatedNel[ConfigError, Int] = c.parse[Int]("port").toValidatedNel
      val host: ValidatedNel[ConfigError, String] = c.parse[String]("host").toValidatedNel

      implicit val nelSemigroup: Semigroup[NonEmptyList[ConfigError]] = SemigroupK[NonEmptyList].algebra[ConfigError]

      println(parallelValidate(host, port)((host, port) => s"$host:$port"))



      val v1: Validated[List[String], Int] = Valid(1)
      val v2: Validated[List[String], Int] = Invalid(List("Accumulates this"))

      val v3: ValidatedNel[List[String], Int] = Invalid(List("And this")).toValidatedNel
      val v4: ValidatedNel[List[String], Int] = Invalid(List("boom")).toValidatedNel

// TODO - why it no worky?

      // value flatMap is not a member of cats.data.Validated...
//      val res = for {
//      x <- v1
//      y <- v2
//      z <- v3
//      } yield (x, y, z)
//      println(res)

      import cats.std.all._
      import cats.syntax.cartesian._

      // it no worky either
      // no |@| syntax for Validated or Xor
//      (v1 |@| v2)
      //      val xors = (v1.toXor |@| v2.toXor)
      //      val nels = v3 |@| v4

      val eithers = (v1.toEither |@| v2.toEither)
//      println(eithers) // cats.syntax.CartesianBuilder$

      // Error:(108, 31) value |@| is not a member of List[Int]
//      val lists  = (v1.toList |@| v2.toList) // cats.syntax.CartesianBuilder
//      println(lists)
    }
  }

}