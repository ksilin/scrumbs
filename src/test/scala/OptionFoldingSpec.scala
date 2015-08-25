import org.scalatest._

class OptionFoldingSpec extends FlatSpec with Matchers {


  "Option" should "be foldable" in {

    val noString: Option[String] = None
    val someString: Option[String] = Some("str")


    val b = noString map (s => s"has a value $s") getOrElse "is empty"
    val b2 = noString.map(s => s"has a value $s").getOrElse("is empty")

    val check1: String = noString.fold("is empty")(s => s"has a value: $s")
    val check2: String = someString.fold("is empty")(s => s"has a value: $s")

    check1 should be === "is empty"
    check2 should startWith("has a value")

    /*
    odersky prefers pattern matching:

    opt match {
  case Some(x) => x + 1
  case None => 0}

  but is also OK with

  opt map { x => x + 1 } getOrElse 0

     */

  }

}
