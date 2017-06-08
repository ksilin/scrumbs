package com.example.genericmatching

import org.scalatest.{ FreeSpec, MustMatchers }
import util.Timed

class TypeableTypeCaseSpec extends FreeSpec with MustMatchers with Timed {

  import shapeless._
  import reflect.runtime.universe._

  "typeable init" in {

    case class Person(name: String, age: Int, wage: Double)

    val stringTypeable = Typeable[String]
    val personTypeable = Typeable[Person]

    // typeable enables casting to options

    stringTypeable.cast("foo": Any) mustBe Some("foo")
    stringTypeable.cast(1: Any) mustBe None
    personTypeable.cast(Person("John", 40, 30000.0): Any) mustBe Some(Person("John", 40, 30000.0))
    personTypeable.cast("John": Any) mustBe None
  }

  private val intL = List(1, 2, 3)

  "typeCase is an extractor for Typeable" in {
    val stringList: TypeCase[List[String]] = TypeCase[List[String]]
    val intList: TypeCase[List[Int]]       = TypeCase[List[Int]]

    def handle(a: Any): String = a match {
      case stringList(vs) => "strings: " + vs.map(_.size).sum
      case intList(vs)    => "ints: " + vs.sum
      case _              => "default"
    }

    handle(List("hello", "world")) mustBe "strings: 10"
    handle(intL) mustBe "ints: 6"

    val ints: List[Int] = Nil
    // wait... what? We'll get back to this.
    handle(ints) mustBe "strings: 0"
  }

  "keeping generic types generic" in {
    def extractCollection[T: Typeable](a: Any): Option[Iterable[T]] = {
      val list = TypeCase[List[T]]
      val set  = TypeCase[Set[T]]

      a match {
        case list(l) => Some(l)
        case set(s)  => Some(s)
        case _       => None
      }
    }

    val intSet = Set(1, 2, 3)

    val emptyList = List.empty[Int]

    extractCollection[Int](intSet) mustBe Some(intSet)
    extractCollection[String](intSet) mustBe None
    extractCollection[Int](intL) mustBe Some(intL)
    extractCollection[String](intL) mustBe None
    extractCollection[Int](emptyList) mustBe Some(emptyList)

    // typeable casts are basen on the values in the list - it is subject to type erasure as any other code
    extractCollection[String](emptyList) mustBe Some(List()) // should be None - well get to that
  }

  "typeable depends on the values in the collection -> will take longer on larger collection" in {

    val list1 = (1 to 100).toList
    val list2 = (1 to 1000).toList
    val list3 = (1 to 10000).toList
    val list4 = (1 to 100000).toList
    val list5 = (1 to 1000000).toList
    val list6 = (1 to 10000000).toList

    val listTypeable = Typeable[List[Int]]
    timed() { listTypeable.cast(list1: Any) } // 2 ms
    timed() { listTypeable.cast(list2: Any) } // 1 ms
    timed() { listTypeable.cast(list3: Any) } // 3 ms
    timed() { listTypeable.cast(list4: Any) } // 11 ms
    timed() { listTypeable.cast(list5: Any) } // 5 ms
    timed() { listTypeable.cast(list6: Any) } // 50 ms
  }

  // moving typedefs out of test -
  // does not work for nested case classes - case class must be defined at top level
  // Error:(83, 32) No TypeTag available for FunkyCollection[A,B]
  // val selfTypeTag = typeTag[FunkyCollection[A, B]] // aka implicitly[TypeTag[T]]

  class Funky[A, B](val foo: A, val bar: B) {
    override def toString: String = s"Funky($foo, $bar)"
  }

  implicit def funkyIsTypeable[A: Typeable, B: Typeable]: Typeable[Funky[A, B]] =
    new Typeable[Funky[A, B]] {
      private val typA = Typeable[A]
      private val typB = Typeable[B]

      def cast(t: Any): Option[Funky[A, B]] = {
        if (t == null) None
        else if (t.isInstanceOf[Funky[_, _]]) {
          val o = t.asInstanceOf[Funky[_, _]]
          for {
            _ <- typA.cast(o.foo)
            _ <- typB.cast(o.bar)
          } yield o.asInstanceOf[Funky[A, B]]
        } else None
      }

      def describe: String = s"Funky[${typA.describe}, ${typB.describe}]"
    }
  "cannot use Typeable for regular generic classes (not case classes)" in {
    // now it works
    val f             = new Funky[String, Int]("sadf", 634)
    val funkyTypeable = Typeable[Funky[String, Int]]
    val cast          = funkyTypeable.cast(f)
    cast mustBe Some(f)
  }

  // Type tags

  final case class FunkyCollection[A: TypeTag, B: TypeTag](seq: Seq[Funky[A, B]]) {
    val selfTypeTag: TypeTag[FunkyCollection[A, B]] = typeTag[FunkyCollection[A, B]] // aka implicitly[TypeTag[T]]

    // shouldnt this be a subtype aka 'conforms' <:< ?
    def hasType[O: TypeTag]: Boolean = typeOf[O] =:= selfTypeTag.tpe

    def cast[O: TypeTag]: Option[O] = if (hasType[O]) Some(this.asInstanceOf[O]) else None
  }

  "casting and type recog must work withSelfTypeTAg " in {
    val a: FunkyCollection[String, Int] = FunkyCollection(Seq(new Funky("foo", 2)))
    val b: FunkyCollection[_, _] = a

    b.hasType[FunkyCollection[String, Int]] mustBe true
    b.hasType[FunkyCollection[Int, String]] mustBe false
    b.cast[FunkyCollection[String, Int]]    mustBe Some(a)
    b.cast[FunkyCollection[Int, String]]    mustBe None
  }

  // special extractor we can use
  object FunkyCollection {
    def extractor[A: TypeTag, B: TypeTag] = new FunkyExtractor[A, B]
  }

  class FunkyExtractor[A: TypeTag, B: TypeTag] {
    def unapply(a: Any): Option[FunkyCollection[A, B]] = a match {
      case kvs: FunkyCollection[_, _] => kvs.cast[FunkyCollection[A, B]]
      case _                          => None
    }
  }

  "extractor / unapply" in {
    val stringIntExt: FunkyExtractor[String, Int] = FunkyCollection.extractor[String, Int]
    val funkies                                   = Seq(new Funky("foo", 123))
    val funCol: FunkyCollection[String, Int]      = FunkyCollection(funkies)
    val funCol2: FunkyCollection[_, _]            = funCol

    funCol2 match {
      case stringIntExt(col) => println("stringInt")
      case _                 => println("sth else")
    }
  }

  trait TypeTaggedTrait[Self] { self: Self =>
    val selfTypeTag: TypeTag[Self]

    def hasType[O: TypeTag]: Boolean = typeOf[O] =:= selfTypeTag.tpe
    def cast[O: TypeTag]: Option[O]  = if (hasType[O]) Some(this.asInstanceOf[O]) else None
  }

  // TODO - does not compile bc 'No TypTag available ... - why? it is available some lines before for FunkyCollection?
//  final case class SomeClass[A](a: A) extends TypeTaggedTrait[SomeClass[A]] {
//    override val selfTypeTag: TypeTag[SomeClass[A]] = implicitly[TypeTag[SomeClass[A]]] // typeTag[SomeClass[A]]
//  }

  class TypeTaggedExtractor[T: TypeTag] {
    def unapply(a: Any): Option[T] = a match {
      case t: TypeTaggedTrait[_] => t.cast[T]
      case _                     => None
    }
  }

  "typetag boilerplate" in {
//    val v = SomeClass[String]("foo")
//    println(v.selfTypeTag)
  }

  // unapply is superfast

}
