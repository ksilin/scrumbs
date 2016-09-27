package com.example.kleisli

import org.scalatest.{ FreeSpec, MustMatchers }

import cats.FlatMap
import cats.implicits._

// from the cats docs: http://typelevel.org/cats/tut/kleisli.html

class HomebrewKleisliSpec extends FreeSpec with MustMatchers {

  final case class Kleisli[F[_], A, B](run: A => F[B]) {

    // compose and andThen need a FlatMap instance

    def compose[Z](k: Kleisli[F, Z, A])(implicit F: FlatMap[F]): Kleisli[F, Z, B] =
      Kleisli[F, Z, B](z => k.run(z).flatMap(run))

    // note the changed generic types
    def andThen[Z](k: Kleisli[F, B, Z])(implicit F: FlatMap[F]): Kleisli[F, A, Z] =
      Kleisli[F, A, Z](a => run(a).flatMap(k.run))

    // map only requires a Functor instance:
    import cats.Functor

    def map[Z](f: B => Z)(implicit F: Functor[F]): Kleisli[F, A, Z] =
      // map[B, Z](fa: F[B])(f: B => Z): F[Z]
      Kleisli[F, A, Z](a => F.map(run(a))(f))
  }

  "test it" - {

    "with Option" in {

      // Bring in cats.FlatMap[Option] instance
      import cats.implicits._

      val parse: Kleisli[Option, String, Int] =
        Kleisli((s: String) => try { Some(s.toInt) } catch { case _: NumberFormatException => None })

      // A = String, B = Int

      val reciprocal: Kleisli[Option, Int, Double] = Kleisli((i: Int) => if (i == 0) None else Some(1.0 / i))

      // A = Int, B = Double

      val parseAndReciprocal = reciprocal.compose(parse)

      println(parseAndReciprocal.run("8"))

      val parseAndReciprocal2 = parse.andThen(reciprocal)

      println(parseAndReciprocal2.run("8"))
    }

  }

}
