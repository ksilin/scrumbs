package com.example.genericmatching

import org.scalatest.{ FreeSpec, MustMatchers }

class PatternMatchingTypeErasureSpec extends FreeSpec with MustMatchers {

  import reflect.runtime.universe._

  // http://slides.com/enverosmanov
  // http://www.cakesolutions.net/teamblogs/ways-to-pattern-match-generic-types-in-scala

  "simple types must not work correctly with pattern matching & erasure" in {
    case class Pipe(value: Int)

    def matching[A, B](sth: B) = sth match {
      case y: A => "c'est une A"
      case n: B => "c'est pas une A"
    }
    // type erasure
    matching[Pipe, Int](42) mustBe "c'est une A"
  }

  "type tags must work correctly with pattern matching & erasure" in {
    case class Pipe(value: Int)

    import scala.reflect.ClassTag
    def matching[A: ClassTag, B: ClassTag](sth: B) = sth match {
      case y: A => "c'est une A"
      case n: B => "c'est pas une A"
    }
    matching[Pipe, Int](42) mustBe "c'est pas une A"
  }

  // collection type erasure
  "collections must not work properly with pattern matching & type erasure" in {

    val res = List("a") match {
      case ints: List[Int]       => "ints" // IJ warns about the fruitless test here
      case strings: List[String] => "strings" // unreachable code
    }
    res mustBe "ints"
  }

  "collections must not work properly with pattern matching with naive TypeTags" in {

    val l = List("a")
    def matchColl[A: TypeTag](l: List[A]) = l match {
      case ints: List[Int]       => "ints"
      case strings: List[String] => "strings" // unreachable code
    }
    matchColl(l) mustBe "ints"
  }

  "collections must work properly with pattern matching with TypeTags and guards" in {

    val l = List("a")
    def matchColl[A: TypeTag](l: List[A]) = l match {
      case ints: List[Int] if typeOf[A] <:< typeOf[Int]          => "ints"
      case strings: List[String] if typeOf[A] <:< typeOf[String] => "strings"
    }
    matchColl(l) mustBe "strings"
  }

  "type tags must works even with empty collections" in {

    val l = List.empty[String]
    def matchColl[A: TypeTag](l: List[A]) = l match {
      case ints: List[Int] if typeOf[A] <:< typeOf[Int]          => "ints"
      case strings: List[String] if typeOf[A] <:< typeOf[String] => "strings"
    }
    matchColl(l) mustBe "strings"
  }
}
