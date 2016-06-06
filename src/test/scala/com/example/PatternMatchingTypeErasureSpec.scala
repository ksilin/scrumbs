package com.example

import org.scalatest.{FlatSpec, FunSpec, Matchers}

class PatternMatchingTypeErasureSpec extends FlatSpec with Matchers {

  // http://slides.com/enverosmanov

  // TODO ask @SUG

  "simple types" should "not work correctly with pattern matching & erasure" in {
    case class Pipe(value: Int)

    def matching[A, B](sth: B) = sth match {
      case y: A => "c'est une A"
      case n: B => "c'est pas une A"
    }
    matching[Pipe, Int](42) should be("c'est une A") // TODO - no idea how this can happen but it does
  }

  "type tags" should "work correctly with pattern matching & erasure" in {
    case class Pipe(value: Int)

    import scala.reflect.ClassTag
    def matching[A: ClassTag, B: ClassTag](sth: B) = sth match {
      case y: A => "c'est une A"
      case n: B => "c'est pas une A"
    }
    matching[Pipe, Int](42) should be("c'est pas une A")
  }

  // collection type erasure
  "collections" should "not work properly with pattern matching & type erasure" in {

    val res = List("a") match {
      case ints: List[Int] => "ints"
      case strings: List[String] => "strings"
    }
    res should be("ints")
  }

  "collections" should "not work properly with pattern matching with naive TypeTags" in {

    import reflect.runtime.universe._

    val l = List("a")
    def matchColl[A: TypeTag](l: List[A]) = l match
    {
      case ints: List[Int] => "ints"
      case strings: List[String] => "strings"
    }
    matchColl(l) should be("ints")
  }

  "collections" should "work properly with pattern matching with TypeTags and guards" in {

    import reflect.runtime.universe._

    val l = List("a")
    def matchColl[A: TypeTag](l: List[A]) = l match
    {
      case ints: List[Int] if typeOf[A] <:< typeOf[Int] => "ints"
      case strings: List[String] if typeOf[A] <:< typeOf[String] => "strings"
    }
    matchColl(l) should be("strings")
  }

}
