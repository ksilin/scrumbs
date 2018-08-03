package com.example.genericmatching

import org.scalatest.{ FreeSpec, MustMatchers }

sealed trait Params

case class GenParams[T](ts: List[T], p: T => Boolean) extends Params

class PatMatGenericCaseClass extends FreeSpec with MustMatchers {

  "type erasure make all matches beyond the first one unreachable" in {

    def run(p: Params): String = p match {
      case GenParams(ts: List[String], f: (String => Boolean)) => "string case"
      case GenParams(ts: List[Int], f: (Int => Boolean))       => "int case" // unreachable code
      case _                                                   => "default"
    }
    run(GenParams[Int](List(1), x => x % 2 == 0)) mustBe "string case"
  }

  "type tags do not help" in {

    import reflect.runtime.universe._

    def run[A: TypeTag](p: GenParams[A]): String = p match {
      case GenParams(ts: List[String], f: (String => Boolean)) if typeOf[A] <:< typeOf[String] => "string case"
      case GenParams(ts: List[Int], f: (Int => Boolean)) if typeOf[A] <:< typeOf[Int]          => "int case"
      case _                                                                                   => "default"
    }
    run(GenParams[Int](List(1), x => x % 2 == 0)) mustBe "string case"
  }

//  http://www.cakesolutions.net/teamblogs/ways-to-pattern-match-generic-types-in-scala

  "cowards way out - degenerify/specialize" in {

    // case-to-case inheritance ins prohibited (for reasons of of broken equality)
//    case class IntParams(override val ts: List[Int], override val p: Int => Boolean) extends GenParams[Int](ts, p)
    case class IntParams(ts: List[Int], p: Int => Boolean)          extends Params
    case class StringParams(ts: List[String], p: String => Boolean) extends Params

    def run(p: Params): String = p match {
      case StringParams(ts: List[String], f: (String => Boolean)) => "string case"
      case IntParams(ts: List[Int], f: (Int => Boolean))          => "int case"
      case _                                                      => "default"
    }
    run(IntParams(List(1), x => x % 2 == 0)) mustBe "int case"
  }

}
