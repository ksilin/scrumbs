package com.example

import org.scalatest.{FunSpec, Matchers}

import scala.reflect.runtime.universe._

class ClassTagsSpec extends FunSpec with Matchers {

  def listMatching[A: TypeTag](lst: List[A]): String = lst match {
    // non-variable type argument String in type pattern List[String] (the underlying of List[String])
    // is unchecked since it is eliminated by erasure
    case str: List[String] if typeOf[A] =:= typeOf[String] => "String"
    case int: List[Int] if typeOf[A] =:= typeOf[Int]       => "Int"
    case _                                                 => "Unknown"
  }

  def cascadedAnyMatching[B: TypeTag](a: Any): String = a match {
    case str: List[B] =>
      val tag = typeTag[B] // TypeTag[Nothing]
      println(s"got typetag of B: $tag")
      listMatching(str)(tag)
    //    case map: Map[A, B] => mapMatching[A, B](map)
    case _ => "first Unknown"
  }

  def mapMatching[A: TypeTag, B: TypeTag](map: Map[A, B]): String = map match {
    case str: Map[String, List[_]] if typeOf[B] =:= typeOf[List[_]] => "ListMap"
    case str: Map[String, Any] if typeOf[B] =:= typeOf[Any]         => "AnyMap"
    case _                                                          => "Unknown"
  }

  def naiveMatching[A](lst: List[A]): String = lst match {
    case str: List[String] => "String"
    case int: List[Int]    => "Int" // unreachable
    case _                 => "Unknown"
  }

  def anyMatching[A: TypeTag](any: Any): String = any match {
    case str: List[String] if typeOf[A] =:= typeOf[String] => "String"
    case int: List[Int] if typeOf[A] =:= typeOf[Int]       => "Int"
    case _                                                 => "Unknown"
  }

  def tMatching[T, A: TypeTag](t: T): String = t match {
    case str: List[String] if typeOf[A] =:= typeOf[String] => "String"
    case int: List[Int] if typeOf[A] =:= typeOf[Int]       => "Int"
    case _                                                 => "Unknown"
  }

  def simpleTMatching[T](t: T): String = t match {
    case str: List[String] => "String"
    case int: List[Int]    => "Int"  // unreachable
    case _                 => "Unknown"
  }

  // compiler error - Any does not take parameters
  //  def anyGenMatching[A: TypeTag](any: Any[A]): String = typeOf[A] match {
  //    case str if str =:= typeOf[String] => "String"
  //    case int if int =:= typeOf[Int] => "Int"
  //    case _ => "Unknown"
  //  }

  def anyMatching2[A: TypeTag](any: Any): String = any match {
    case str: List[String] if typeOf[A] =:= typeOf[List[String]] => "String"
    case int: List[Int] if typeOf[A] =:= typeOf[List[Int]]       => "Int"
    case _                                                       => "Unknown"
  }

  case class StringList(value: List[String])

  case class IntList(value: List[Int])

  def holderMatching(any: Any): String = any match {
    case str: StringList => "String"
    case int: IntList    => "Int"
    case _               => "Unknown"
  }

  describe("naive matching fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = naiveMatching(strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = naiveMatching(intList)
      mat should be("String") // should actually be int, but because of erasure, the first match kicks in
    }
  }

  describe("list matching") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = listMatching(strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = listMatching(intList)
      mat should be("Int")
    }
  }

  describe("any matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = anyMatching(strList)
      mat should be("Unknown")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = anyMatching(intList)
      mat should be("Unknown")
    }
  }

  describe("t matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = tMatching(strList)
      mat should be("Unknown")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = tMatching(intList)
      mat should be("Unknown")
    }
  }

  describe("parametrized t matching - works") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = tMatching[List[String], String](strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = tMatching[List[Int], Int](intList)
      mat should be("Int")
    }
  }

  describe("simple t matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = simpleTMatching(strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = simpleTMatching(intList)
      mat should be("String") // erasure again - first List matches
    }
  }

  describe("simple parametrized t matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = simpleTMatching[List[String]](strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = simpleTMatching[List[Int]](intList)
      mat should be("String") // erasure still - first List matches
    }
  }

  describe("cascaded any matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = cascadedAnyMatching(strList)
      mat should be("String")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = cascadedAnyMatching(intList)
      mat should be("Int") // erasure still - first List matches
    }
  }

  //  describe("any generic matching - fails") {
  //    it("first match") {
  //      val strList = List("str", "wsaerf")
  //      val mat: String = anyGenMatching[String](strList.asInstanceOf[Any[String]])
  //      mat should be("Unknown")
  //    }
  //    it("second match") {
  //      val intList = List(1, 2)
  //      val mat: String = anyGenMatching[String](intList.asInstanceOf[Any[String]])
  //      mat should be("Unknown")
  //    }
  //  }

  describe("any matching 2 - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = anyMatching2(strList)
      mat should be("Unknown")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = anyMatching2(intList)
      mat should be("Unknown")
    }
  }

  describe("holder matching - fails") {
    it("first match") {
      val strList = List("str", "wsaerf")
      val mat: String = holderMatching(strList)
      mat should be("Unknown")
    }
    it("second match") {
      val intList = List(1, 2)
      val mat: String = holderMatching(intList)
      mat should be("Unknown")
    }
  }

}
