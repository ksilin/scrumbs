package com.example

import org.scalatest.{FunSpec, Matchers}

import scala.annotation.tailrec
import scalaz.StreamT.Done

class TrampolineSpec extends FunSpec with Matchers {

  describe("unreproducible blog post by rich dougherty") {

    // http://blog.richdougherty.com/2009/04/tail-calls-tailrec-and-trampolines.html

    // actually, it does nto break, probably scala has figured out a way to do tail call optimizatiobn
    it("breaks") {

      def even(n: Int): Boolean = if (n == 0) true else odd(n - 1)
      def odd(n: Int): Boolean = if (n == 0) false else even(n - 1)

      val res = even(9999)
      res should be(false)
    }

    it("trampolines") {

      // this one actually throws a java.lang.StackOverflowError - hä?

      sealed trait Bounce[+A]
      case class Done[+A](result: A) extends Bounce[A]
      case class Call[+A](thunk: () => Bounce[A]) extends Bounce[A]

      // does not compile with the tailrec annotation
      // Error:(31, 49) could not optimize @tailrec annotated method trampoline: it contains a recursive call not in tail position
      //      def trampoline[A](bounce: Bounce[A]): A = bounce match {
      //      ^
      //      @tailrec
      def trampoline[A](bounce: Bounce[A]): A = bounce match {
        case Call(thunk) => trampoline(thunk())
        case Done(x) => x
      }

      def even2(n: Int): Bounce[Boolean] = {
        if (n == 0) Done(true)
        else Call(() => odd2(n - 1))
      }
      def odd2(n: Int): Bounce[Boolean] = {
        if (n == 0) Done(false)
        else Call(() => even2(n - 1))
      }

      val res = trampoline(even2(9999))
      res should be(false)
    }

    // to me this example, edapted from http://eed3si9n.com/learning-scalaz/Stackless+Scala+with+Free+Monads.html
    // is essentially the same as above. Why doesnt it work?

    it("trampolines2") {

      sealed trait Bounce[+A] {
        case class Done[+A](result: A) extends Bounce[A]
        case class Call[+A](thunk: () => Bounce[A]) extends Bounce[A]

        @tailrec
        final def trampoline[A](bounce: Bounce[A]): A = bounce match {
          case Call(thunk) => trampoline(thunk())
          case Done(x) => x
        }

        // TODO - hwo can I access the definitions outside the class/trait?
        def even2(n: Int): Bounce[Boolean] = {
          if (n == 0) Done(true)
          else Call(() => odd2(n - 1))
        }
        def odd2(n: Int): Bounce[Boolean] = {
          if (n == 0) Done(false)
          else Call(() => even2(n - 1))
        }

        val res = trampoline(even2(9999))
        res should be(false)
      }
    }


  }

}