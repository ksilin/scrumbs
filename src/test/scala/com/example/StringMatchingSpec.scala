package com.example

import org.scalatest.{FunSpec, Matchers}

import scala.util.matching.Regex

class StringMatchingSpec extends FunSpec with Matchers {

  describe("pattern matching strings with regexes") {

    it("should match precompiled regexes") {

      val rx1 = "abc.*".r

      val res1 = "abcdefg" match {
        case rx1() => "found" // you DO need to invoke the regex with ()
        case _ => "missed"
      }
      res1 should equal("found")

      val res2 = "bcdefg" match {
        case rx1() => "found"
        case _ => "missed"
      }
      res2 should equal("missed")
    }

    it("precompiled regex with capture") {

      val rx1 = """abc(\d+).*""".r

      val res1 = "abc123defg" match {
        case rx1(amount) => amount // capturing
        case _ => "missed"
      }
      res1 should equal("123")
    }

    it("should match ad hoc regexes") {
      val res = "abcdefg" match {
        //        case "abc.*".r => "found" // not worky - ("abc.*".r)() and ("abc.*".r).apply() dont neither
        case _ => "missed"
      }
      res should equal("missed")
    }

    it("should match guards") {
      val res = "abcdefg" match {
        case r if r.matches("abc.*") => "found" // unable to capture, boolean return value
        case _ => "missed"
      }
      res should equal("found")

      val res2 = "defg" match {
        case r if r.matches("abc.*") => "found"
        case _ => "missed"
      }
      res2 should equal("missed")
    }

    implicit class RegexContext(sc: StringContext) {
      def r = new Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
    }

    it("should match with implicit conversion") {

      val res = "abcdefg" match {
        case r"abc.*" => "found"
        case _ => "missed"
      }
      res should equal("found")

      val res2 = "defg" match {
        case r"abc.*" => "found"
        case _ => "missed"
      }
      res2 should equal("missed")
    }

    it("implicit conversion with capturing") {

      val res = "abc123defg" match {
        case r"abc(\d+)$amount.*" => amount
        case _ => "missed"
      }
      res should equal("123")
    }
  }
}
