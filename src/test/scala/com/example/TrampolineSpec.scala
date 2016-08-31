package com.example

import org.scalatest.{ FunSpec, Matchers }

import scala.annotation.tailrec
import scalaz.StreamT.Done

class TrampolineSpec extends FunSpec with Matchers {

  describe("trampoline blog post by rich dougherty") {

    // http://blog.richdougherty.com/2009/04/tail-calls-tailrec-and-trampolines.html
//    also see http://eed3si9n.com/learning-scalaz/Stackless+Scala+with+Free+Monads.html

    // does not break on 99999
    val reps: Int = 99999

    it("recursion overflows the stack") {

      def even(n: Int): Boolean = if (n == 0) true else odd(n - 1)
      def odd(n: Int): Boolean  = if (n == 0) false else even(n - 1)
      intercept[java.lang.StackOverflowError] { even(reps) }
    }

    it("trampoline works fine") {

      sealed trait Bounce[+A]
      case class Done[+A](result: A)              extends Bounce[A]
      case class Call[+A](thunk: () => Bounce[A]) extends Bounce[A]

      @tailrec
      def trampoline[A](bounce: Bounce[A]): A = bounce match {
        case Call(thunk) => trampoline(thunk())
        case Done(x)     => x
      }

      def even2(n: Int): Bounce[Boolean] = {
        if (n == 0) Done(true)
        else Call(() => odd2(n - 1))
      }
      def odd2(n: Int): Bounce[Boolean] = {
        if (n == 0) Done(false)
        else Call(() => even2(n - 1))
      }

      val res = trampoline(even2(reps))
      res should be(false)
    }
  }
}
