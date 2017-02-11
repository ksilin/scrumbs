package com.example

import org.scalatest.{ FreeSpec, FunSpec, Matchers, MustMatchers }

class FunctionalCompositionSpec extends FreeSpec with MustMatchers {

  "composing instead of nesting" - {

    case class Foo(a: String, b: Int)
    val fetcher: (Int, String) => Foo   = (x, y) => Foo(y + "muahaha", x + 99)
    val mapper: Foo => Map[String, Any] = f => Map("a" -> f.a, "b" -> f.b)

    val expected = mapper(fetcher(3, "hi"))

    "traditional - andThen & compose" in {
      val combo: ((Int, String)) => Map[String, Any]  = fetcher.tupled andThen mapper
      val combo2: ((Int, String)) => Map[String, Any] = mapper compose fetcher.tupled

      // must use tuples as andThen & compose are defined for Function1 only
      combo((3, "hi")) mustBe expected
      combo2((3, "hi")) mustBe expected
    }

    "using scalaz FunctorSyntax" in {
      import scalaz._
      import Scalaz._

      // map from functorSyntax
      val comboZ: (Int, String) => Map[String, Any] = fetcher map mapper
      comboZ(3, "hi") mustBe expected
    }

    "using cats.Functor" in {
      import cats._
      import cats.implicits._

      implicit def function1Functor[In]: Functor[Function1[In, ?]] =
        new Functor[Function1[In, ?]] {
          def map[A, B](fa: In => A)(f: A => B): Function1[In, B] = fa andThen f
        }

      val comboC: ((Int, String)) => Map[String, Any] = fetcher.tupled map mapper
      comboC(3, "hi") mustBe expected
    }

  }

}
