import org.scalatest.{Matchers, FunSpec}

class FunctionalCompositionSpec extends FunSpec with Matchers {

  describe("composing instead of nesting") {

    it("combine") {

      case class Foo(a: String, b: Int)
      def fetcher(x: Int, y: String): Foo = Foo(y + "muahaha", x + 99)
      def mapper(f: Foo): Map[String, Any] = Map("a" -> f.a, "b" -> f.b)

      val combo: ((Int, String)) => Map[String, Any] = (fetcher _).tupled.andThen(mapper _)
      val combo2: ((Int, String)) => Map[String, Any] = (mapper _).compose((fetcher _).tupled)

      import scalaz._
      import Scalaz._

      val comboZ: (Int, String) => Map[String, Any] = (fetcher _).map(mapper _)

    }
  }

}