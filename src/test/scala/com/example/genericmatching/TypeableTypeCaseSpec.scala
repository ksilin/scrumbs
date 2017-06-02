package com.example.genericmatching

import org.scalatest.{ FreeSpec, MustMatchers }

class TypeableTypeCaseSpec extends FreeSpec with MustMatchers {

  import shapeless._

  "typable init" in {

    case class Person(name: String, age: Int, wage: Double)

    val stringTypeable = Typeable[String]
    val personTypeable = Typeable[Person]

    // typeable enables casting to options

    stringTypeable.cast("foo": Any) mustBe Some("foo")
    stringTypeable.cast(1: Any) mustBe None
    personTypeable.cast(Person("John", 40, 30000.0): Any) mustBe Some(Person("John", 40, 30000.0))
    personTypeable.cast("John": Any) mustBe None
  }

  "typeCase is an extractor for Typeable" in {
    val stringList: TypeCase[List[String]] = TypeCase[List[String]]
    val intList: TypeCase[List[Int]]       = TypeCase[List[Int]]

    def handle(a: Any): String = a match {
      case stringList(vs) => "strings: " + vs.map(_.size).sum
      case intList(vs)    => "ints: " + vs.sum
      case _              => "default"
    }

    handle(List("hello", "world")) mustBe "strings: 10"
    handle(List(1, 2, 3)) mustBe "ints: 6"

    val ints: List[Int] = Nil
    // wait... what? We'll get back to this.
    handle(ints) mustBe "strings: 0"
  }

}
