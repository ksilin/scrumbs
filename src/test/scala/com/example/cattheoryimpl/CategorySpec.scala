package com.example.cattheoryimpl

import org.scalatest.FreeSpec
import org.scalatest.prop.PropertyChecks

// implementing a category, no big deal:
// https://hseeberger.wordpress.com/2010/11/25/introduction-to-category-theory-in-scala/

// not only functions can build a category, so we need a more general notion of one:

// using ->> as an alias for any type constructor
trait GenericCategory[->>[_, _]] {
  def id[A]: A ->> A
  def compose[A, B, C](g: B ->> C, f: A ->> B): A ->> C // ->>[A,C] == A ->> C
}

// other categories could be for partial functions or monoids
object Category extends GenericCategory[Function] {
  def id[A]: Function[A, A] /* identical to A => A */ = a => a
  def compose[A, B, C](g: B => C, f: A => B): A => C  = f andThen g // g compose f
}

class CategorySpec extends FreeSpec with PropertyChecks {

  "a category should" - {

    val f = (i: Int) => i.toString
    val g = (s: String) => s.length
    val h = (i: Int) => i * i

    "be associative" in {
      forAll { (i: Int) =>
        Category.compose(Category.compose(g, f), h)(i) == Category.compose(g, Category.compose(f, h))(i)
      }
    }

    "identity" in {
      forAll { (i: Int) =>
        Category.compose(f, Category.id[Int])(i) == Category.compose(Category.id[String], f)(i)
      }
    }

    "identity 2" in {
      forAll { (i: Int) =>
        Category.compose(f, Category.id[Int])(i) == f(i)
      }
      forAll { (i: Int) =>
        Category.compose(h, Category.id[Int])(i) == h(i)
      }
      forAll { (s: String) =>
        Category.compose(g, Category.id[String])(s) == g(s)
      }
    }
  }

  // lets go for functors now - forst the general one
  trait GenericFunctor[->>[_, _], ->>>[_, _], F[_]] {
    def fmap[A, B](f: A ->> B): F[A] ->>> F[B]
  }

  // every object A ∈ C1 is mapped to to an object F(A) ∈ C2 and
  //  every map A → B between two objects A, B ∈ C1 is mapped to a map F(A) → F(B) between two objects F(A), F(B) ∈ C2.

  // preserving cat structure - identity maps & composition
  // F(1A) = 1F(A) ∀ A ∈ C1
  // F(g ο f) = F(g) ο F(f) ∀ f: A → B, g: B → C where A, B, C ∈ C1

  trait Functor[F[_]] extends GenericFunctor[Function, Function, F] {
    final def fmap[A, B](as: F[A])(f: A => B): F[B] = fmap(f)(as) // delegates to fmap of GenericFunctor, no impl yet!
  }

  // lets do it as a typeclass!

  object Functor {

    def fmap[A, B, F[_]](as: F[A])(f: A => B)(implicit functor: Functor[F]): F[B] = functor.fmap(as)(f)

    implicit object ListFunctor extends Functor[List] {

      // caveat - application order reversed:
      // Functor.fmap(strings)(stringID)
      // Functor.ListFunctor.fmap(stringID)(strings)
      def fmap[A, B](f: A => B): List[A] => List[B] = as => as map f
    }
  }

  "list functor should" - {
    import Functor._

    "preserve identity" in {
      val stringID     = (s: String) => s
      val stringListID = (s: List[String]) => s
      forAll { strings: List[String] =>
        Functor.fmap(strings)(stringID) == stringListID(strings)
        Functor.fmap(strings)(stringID) == strings
        // not entirely sure since we are comparing results anyway,
        // stringListID is purely illustrative
      }
    }

  }

}
