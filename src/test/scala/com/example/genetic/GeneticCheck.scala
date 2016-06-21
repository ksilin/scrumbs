package com.example.genetic

import org.scalacheck._
import org.scalacheck.Prop.forAll

class GeneticCheck { // extends Specification with ScalaCheck {

  val propConcatLists = forAll { (l1: List[Int], l2: List[Int]) =>
    l1.size + l2.size == (l1 ::: l2).size }

  val propSqrt = forAll { (n: Int) => scala.math.sqrt(n*n) == n }


}
