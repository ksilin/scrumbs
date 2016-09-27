package com.example.kleisli

import org.scalatest.{ FreeSpec, MustMatchers }
import cats.{ FlatMap, Id }
import cats.implicits._
import cats.data.{ Kleisli, Writer, WriterT }

class KleisliFirstSpec extends FreeSpec with MustMatchers {

  "function composition" - {

    val f = (i: Int) => List(i, i * 2)
    val g = (j: Int) => List(j, j * 3)

    "we can combine the functions ad hoc or create a combined fn if F defines flatMap" in {
      val gr = f(5) flatMap g
      println(gr) // 5, 15, 10, 30

      val h: (Int) => List[Int] = (i: Int) => f(i) flatMap g
      println(h(5))
    }

    // but we cannot create a function which is a combination of both using regular rfunction composition:

    "andThen dows not work, b accepts a single int" in {
      "val h = f.andThen(g)" mustNot typeCheck //compile
    }

    "compose dows not work, b accepts a single int" in {
      "val h = g.compose(f)" mustNot typeCheck //compile
    }

    "we can create a kleisli definition for our purpose" in {
      val fk: Kleisli[List, Int, Int] = Kleisli(g).compose(Kleisli(f))
      println("as kleisli: " + fk.run(5))
    }

    //can we do the same for a tuple? do we have to define a Flatmap instance?

    "there is a flatMap op provided for tuple - the 'raw' Writer monad" in {

      val fs: (Int) => (String, Int) = (i: Int) => (s"f on $i ", i + 1)
      val gs: (Int) => (String, Int) = (j: Int) => (s"g on $j ", j * 2)

      // https://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/instances/tuple.scala
      // def flatMap[A, B](fa: (X, A))(f: A => (X, B)): (X, B)

      // looks like flatMap for tuple2 uses only the second element and discards the first one

      //    t flatMap( (i: Int, s: String) => (i * 2, "$s multiplied"))
      //    t flatMap( (i: Int) => (i * 2, "multiplied"))
      val withFlatMap = fs(7) flatMap ((i: Int) => (s"multiplied $i by 2", i * 2))
      println(withFlatMap)

//      val kfs: Kleisli[(String, Int), Int, Int] = Kleisli(fs) // no type param, expected one
//      val kfs: Kleisli[Tuple2, Int, Int] = Kleisli(fs) // two type params, expected one
//      val kfs: Kleisli[Any, Int, Int] = Kleisli(fs) // Kleisli is invariant in F
//      val kfs: Kleisli[Nothing, Nothing, Nothing] = Kleisli(fs) // Kleisli is invariant in F - inferred by IJ
      val kfs = Kleisli(fs)

      val x = kfs.run(6)
      println("running kleisli: " + x)

      val kgs = Kleisli(gs)
      val y   = kgs.run(6)
      println("running kleisli: " + y)

      // Error:(75, 27) type mismatch;
//      found   : cats.data.Kleisli[[+(some other)T2(in class Tuple2)](String, (some other)T2(in class Tuple2)),Int,Int]
//      required: Int => cats.data.Kleisli[[+T2(in class Tuple2)](String, T2(in class Tuple2)),Int,?]
//      val h = kfs.flatMap(kgs)

      // TODO - I understand how the integers are composed, but not how the strings are getting passen and concatenated

      val h              = kgs.compose(kfs)
      val composedResult = h.run(3)
      println(composedResult)
      composedResult mustBe ("f on 3 g on 4 ", 8)

      val i             = kfs.andThen(kgs)
      val andThenResult = i.run(3)
      println(andThenResult)
      andThenResult mustBe ("f on 3 g on 4 ", 8)
    }

    "same composition using the cats Writer" in {

      val w: WriterT[Id, String, Int] = Writer("Smallish gang", 3)

      val v: Writer[String, Int]  = Writer.value[String, Int](3)
      val l: Writer[String, Unit] = Writer.tell[String]("Log something")

      w.run mustBe ("Smallish gang", 3)
      v.run mustBe ("", 3)
      l.run mustBe ("Log something", ()) // why doesnt ("Log something", Unit) work? because Unit is a type and () is its value
    }
  }


}
