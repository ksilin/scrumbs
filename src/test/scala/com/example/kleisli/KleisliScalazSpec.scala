package com.example.kleisli

import org.scalatest.{ FreeSpec, MustMatchers }

class KleisliScalazSpec extends FreeSpec with MustMatchers {

  // no need to repeat everything, jsut the basics

  import scalaz._
  import Scalaz._
  import Kleisli._

  def f(i: Int) = List(i, 2 * i)
  def g(j: Int) = {
    println(s"invoked g with $j")
    List(3 * j, 5 * j)
  }

  // http://eed3si9n.com/learning-scalaz/Composing+monadic+functions.html

  "composing with Kleisli" - {

    "simple" in {

      val h = Kleisli(f) >=> Kleisli(g)
      val r = h.run(1)
      println(r)
      r mustBe List(3, 5, 6, 10)

    }

  }

}
