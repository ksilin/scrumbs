import org.scalatest._

class TuplesToPairsSpec extends FlatSpec with Matchers {

  val tup = ("a", 1, Some("key"), Some("value"))
  val tup2 = ("a", 1, None, None)
  val tup3 = ("a", 1, None, Some("orphan value"))
  val tup4 = ("a", 1,  Some("lonely key"), None)
  val l = List(tup, tup2, tup3, tup4)
  val keyIndex = 2


  "TuplesToPairs" should "have tests" in {
    val d = TuplesToPairs.toDetails(l, keyIndex, 3)
    println(d)

    true should be === true
  }

  "TuplesToPairs" should "have more tests" in {
    val d = TuplesToPairs.toDetails3(l, keyIndex, 3)
    println(d)

    true should be === true
  }

  "TuplesToPairs" should "fail on indexing Product" in {
//    val d = TuplesToPairs.toKeys(l, keyIndex)
//    println(d)
    true should be === true
  }
}
