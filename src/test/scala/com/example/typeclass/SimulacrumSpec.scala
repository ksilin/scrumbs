package com.example.typeclass

import org.scalatest.{FreeSpec, MustMatchers}

import simulacrum._

//https://github.com/mpilquist/simulacrum

class SimulacrumSpec extends FreeSpec with MustMatchers {

  "creating typecalsses the new way" - {

    @typeclass trait Semigroup[A] {
      @op("|+|") def append(x: A, y: A): A
    }

    "defining and using a typeclass with a custom op" in {


      implicit val semigroupInt: Semigroup[Int] = new Semigroup[Int] {
        def append(x: Int, y: Int) = x + y
      }

      import Semigroup.ops._

      val res = 2 |+| 3
      res mustBe 5
    }


    "subtyping" in {
      @typeclass trait Monoid[A] extends Semigroup[A] {
        def id: A
      }
    }

    // TODO - and so much more - // https://github.com/mpilquist/simulacrum/blob/master/examples/src/test/scala/simulacrum/examples/examples.scala
  }

}
