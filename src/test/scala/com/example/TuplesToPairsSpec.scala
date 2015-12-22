package com.example

import com.example.helpers.TuplesToPairs
import org.scalatest._
import shapeless.Nat

// see 2015.08.20 notes
class TuplesToPairsSpec extends FlatSpec with Matchers {

  val tup = ("a", 1, Some("key"), Some("value"))
  val tup2 = ("a", 1, None, None)
  val tup3 = ("a", 1, None, Some("orphan value"))
  val tup4 = ("a", 1, Some("lonely key"), None)
  val l = List(tup, tup2, tup3, tup4)
  final val keyIndex = 2


  "TuplesToPairs" should "have tests" in {
    val d: Set[(String, Seq[String])] = TuplesToPairs.toDetails(l, keyIndex, 3)
    d should contain theSameElementsAs Set(("key", List("value")), ("lonely key", List("")), ("", List("", "orphan value")))
  }

  "TuplesToPairs" should "have more tests" in {
    val d = TuplesToPairs.toDetails3(l, keyIndex, 3)
    d should contain theSameElementsAs Set(("key", List("value")))
  }

  "TuplesToPairs" should "fail on indexing Product" in {

    // number passed to Nat cannot be a variable/value. It has to be a compile-time constant - i.e. literal or final val
    val index = Nat(2)
    val d = TuplesToPairs.getAtIndex(l.head, index)
    println(d)
  }

}
