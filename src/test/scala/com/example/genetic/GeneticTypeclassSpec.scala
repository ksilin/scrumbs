package com.example.genetic

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.Prop.AnyOperators
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FreeSpec, Matchers, PropSpec}

// a typeclass can be practivally viewed as a parametrized implicit param

class GeneticTypeclassSpec extends FreeSpec with Matchers with PropertyChecks {

  // TODO - nice topic, nice talk, but no code available yet - will wait a bit for it
  // http://noelmarkham.github.io/genetics-scaladays/#/ ~ slide 30
  // https://www.youtube.com/watch?v=lTd3Ep8jGrw - about 26min

  "genetic algorithms" - {

    type Population[A] = Vector[A]
    type MeasuredChromosome = (Chromo, Long)

    def iterate[A](fitness: A => Long, nextDouble: () => Double = () => scala.util.Random.nextDouble())
                  (pop: Population[A])(implicit g: Genetic[A]): Population[A] = ???

    "uses scalacheck - success" in {
      forAll { (n: Int) => scala.math.sqrt(n*n) == n }
    }

    "uses scalacheck - fail" in {
        forAll { (n: Int) => scala.math.sqrt(n*n) != n*2 }
    }

    "more scalacheck 2" in {
        forAll { // adding or leaving out forAll seems to have no effect
          (a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size + 1
      }
    }

    "and an actual test" in {
      forAll(listOf(arbitrary[MeasuredChromosome]),
        arbitrary[Chromo],
        listOf(arbitrary[MeasuredChromosome])) { (lefts, mid, rights) =>

        val middleScore = 1 + (lefts ::: rights).map(_._2).sum
        val midWithWeight = (mid, middleScore)
        val measuredPop = lefts.toVector ++ (midWithWeight +: rights.toVector)

        genRoulette(measuredPop)(0.5) ?= Some(mid)
      }
    }

  }
}
