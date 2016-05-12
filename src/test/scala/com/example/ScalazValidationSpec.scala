package com.example

import org.scalatest.{FunSpec, Matchers}

class ScalazValidationSpec extends FunSpec with Matchers {

  describe("scalaz Validation & Either") {

    import scalaz._
    import Scalaz._

    it("combine") {

      // Extracting success or failure values
      val s: Validation[String, Int] = 1.success
      val f: Validation[String, Int] = "error".failure

      val res = for {
        sRes <- s.disjunction
        fRes <- s.disjunction
      } yield sRes + fRes

      res should be(\/-(2))
      res.validation should be(Success(2))

      val combo: Validation[String, Int] = s |+| s
      combo should be(Success(2))

      val combo2 = s |@| s
      println(combo2)
      // scalaz.syntax.ApplyOps$$anon$1@6cd24612 was not equal to Success(2)
      //      combo2 should be(Success(2))

      // ApplicativeBuilder[M[_], A, B]
      val comboFail2 = f |@| f
      println(comboFail2)
      comboFail2.a should be(Failure("error"))
      comboFail2.b should be(Failure("error"))

      val comboFail: Validation[String, Int] = f |+| f
      println(comboFail)
      comboFail should be(Failure("errorerror"))
    }


    it("SO") {
      // http://stackoverflow.com/questions/32358719/validation-usage-with-in-scalaz
      case class HttpConnectionParams(url: String, user: String, password: String)

      val a = Map("url" -> "http://example.com", "user" -> "bob", "pass" -> "12345")

      def lookup[K, V](m: Map[K, V], k: K, message: String): ValidationNel[String, V] =
        m.get(k).toSuccess(NonEmptyList(message))

      val validated: ValidationNel[String, HttpConnectionParams] = (
        lookup(a, "url", "Url must be supplied") |@|
          lookup(a, "username", "Username must be supplied") |@|
          lookup(a, "password", "Password must be supplied")
        ) (HttpConnectionParams.apply)

      //      Failure(NonEmpty[Username must be supplied,Password must be supplied])
      println(validated)

    }

    it("disjunction") {
      // https://speakerdeck.com/bwmcadams/scaladays-sf-2015-a-skeptics-guide-to-scalaz-gateway-drugs
      // \/ is right-biased (success)
      // can be used on for-comp/map/flatMap -\/ abort, \/- continue

      // prefer infix notation Error \/ Success oder  \/[Error, Success]
      def query(key: String): Error \/ String = ???

      "success".right should be(\/-("success"))
      "failure".left should be(-\/("failure"))

      // \/> is an alias for toRightDisjunction

      val notFound = None \/> "not found"
      notFound should be(-\/("not found"))

      val found = Some("found") \/> "not found"
      found should be(\/-("found"))
    }

    it("more validation") {
      // https://speakerdeck.com/bwmcadams/scaladays-sf-2015-a-skeptics-guide-to-scalaz-gateway-drugs
      // Validation is not a monad
      // Validation is an applicative functor

      case class Address(street: String, house: Int)

      case class User(name: String, surname: String, address: Option[Address])

      def validAddress(user:Option[User]): ValidationNel[String, Address] = {
        user match {
          case Some(User(_, _, Some(address))) => address.success
          case Some(User(_, _, None)) => "user has no address".failureNel
          case None => "no user".failureNel
        }
      }

        // applicative operators:
        // *> - right hand
        // <* - left hand
        // errors always win
      // TODO - where could we want to apply this?

        1.some *> 2.some should be(Some(2))
        1.some <* 2.some should be(Some(1))

        None <* 2.some should be(None)
        None *> 2.some should be(None)

      // in order to collect errors, use NEL:
      // Validation[NonEmptyList[L], R] or ValidationNel[L, R]

      val res = validAddress(None) *> validAddress(Some(User("umberto", "eco", None)))
      println(res)
      res should be(Failure(NonEmptyList("no user","user has no address")))

      val succ = validAddress(None) |@| validAddress(Some(User("umberto", "eco", None))) |@| validAddress(Some(User("han", "solo", Some(Address("elm str.", 13)))))
      println(succ)
      println(succ.c)
//      succ.b should be(Failure(NonEmptyList("no user","user has no address")))
      }

      it("fromTryCatchThrowable"){
        val res: Disjunction[Throwable, Int] = \/.fromTryCatchNonFatal{ "abc".toInt}
        println(res) // -\/(java.lang.NumberFormatException For input string: "abc")

        // can be NumberFormatException for precise matching instead of Exception
        val res2 = \/.fromTryCatchThrowable[Int, Exception]{ "abc".toInt}
        println(res2) // -\/(java.lang.NumberFormatException For input string: "abc")
      }

    // |@| - oink, cinnabun, tie fighter, princess leia, scream
    }
}