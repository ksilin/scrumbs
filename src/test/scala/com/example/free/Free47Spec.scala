package com.example.free

import cats._
import cats.free._
import simulacrum.typeclass
import cats.data.Coproduct
import cats.implicits._
import monix.eval.Task

import scala.util.Try
import org.scalatest.{ FreeSpec, MustMatchers }

import scala.collection.mutable.ListBuffer
import scala.io.StdIn

class Free47Spec extends FreeSpec with MustMatchers {

  //http://www.47deg.com/blog/fp-for-the-average-joe-part3-free-monads

  "Applications as coproduct of free algrebras" - {

    // ADTs as verbs
    sealed trait Interact[A]
    case class Ask(prompt: String) extends Interact[String]
    case class Tell(msg: String)   extends Interact[Unit]

    object InteractOps {
      def ask(prompt: String): Free[Interact, String] = Free.liftF[Interact, String](Ask(prompt))
      def tell(msg: String): Free[Interact, Unit]     = Free.liftF[Interact, Unit](Tell(msg))
    }

    import InteractOps._

    val program: Free[Interact, Unit] = for {
      cat <- ask("what's the cats name?")
      _   <- tell(s"you said $cat")
    } yield ()

    def interpreter: Interact ~> Id = new (Interact ~> Id) {
      def apply[A](fa: Interact[A]): Id[A] = fa match {
        case Ask(prompt) => println(prompt); "Tom" // StdIn.readLine()
        case Tell(msg)   => println(msg)
      }
    }

    "simple program" in {
      println(program) // Free(...)

      val x: Id[Unit] = catsInstancesForId.pure(())
      println(x)

      val evaled = program foldMap interpreter
      println(evaled)
    }

    // building a program from ADTs is tricky as unrelated monads do not compose

    sealed trait DataOp[A]
    case class AddCat(a: String) extends DataOp[String]
    case class GetAllCats()      extends DataOp[List[String]]

    "unrelated monads do not compose" in {

      object DataOps {
        def addCat(a: String): Free[DataOp, String] = Free.liftF[DataOp, String](AddCat(a))
        def getAllCats: Free[DataOp, List[String]]  = Free.liftF[DataOp, List[String]](GetAllCats())
      }

      """val program = for {
        cat <- ask("kitty name?")
        addedCat <- addCat(cat)
        _ <-tell(s"$addedCAt has been stored")
      } yield ()""" mustNot typeCheck

    }

    // how do we combine the ADTs? By building a coproduct from them, as explained in 'Data types a la carte'

    // an application as a coproduct of its algebras
    type Application[A] = Coproduct[Interact, DataOp, A]

    // using inject intead of liftF, parametrized on F[_] instead of A
    class Interacts[F[_]](implicit I: Inject[Interact, F]) {
      def ask(prompt: String): Free[F, String] = Free.inject[Interact, F](Ask(prompt))
      def tell(msg: String): Free[F, Unit]     = Free.inject[Interact, F](Tell(msg))
    }

    object Interacts {
      implicit def interacts[F[_]](implicit I: Inject[Interact, F]): Interacts[F] = new Interacts[F]
    }

    class DataOps[F[_]](implicit I: Inject[DataOp, F]) {
      def addCat(a: String): Free[F, String]  = Free.inject[DataOp, F](AddCat(a))
      def getAllCats(): Free[F, List[String]] = Free.inject[DataOp, F](GetAllCats())
    }

    object DataOps {
      implicit def dataOps[F[_]](implicit I: Inject[DataOp, F]): DataOps[F] = new DataOps[F]
    }

    // parametrizing the ctors to F[_] allows to obtain Free mondas on demand that are parametrized to same Functor, in this case - Application

    // The required Inject instance is resposible for injecting the operation inside a coproduct

    // so, what do we pass to our foldMap now? Instead of Interact ~> Id or DataOps ~> Id it needs to have the shape of Application ~> Id

    object InteractInterpreter extends (Interact ~> Id) {
      def apply[A](i: Interact[A]) = i match {
        case Ask(prompt) => println(prompt); "Tom" // StdIn.readLine()
        case Tell(msg)   => println(msg)
      }
    }

    object InMemoryDataOpInterpreter extends (DataOp ~> Id) {
      private[this] val memDataSet = new ListBuffer[String]

      def apply[A](fa: DataOp[A]) = fa match {
        case AddCat(a)    => memDataSet.append(a); a
        case GetAllCats() => memDataSet.toList
      }
    }
    // courtesy of cats.arrow.FunktionK
    val interpreter2: Application ~> Id = InteractInterpreter or InMemoryDataOpInterpreter

    "now composition works" in {

      def prgrm(implicit I: Interacts[Application], D: DataOps[Application]): Free[Application, Unit] = {

        import I._, D._

        for {
          cat  <- ask("kitty name?")
          _    <- addCat(cat)
          cats <- getAllCats()
          _    <- tell("all cats: " + cats.mkString(", "))
        } yield ()
      }
      val evaluated = prgrm foldMap interpreter2
      println(s"combined algebras: $evaluated")
    }

    // excercise for the reader - add more algebras - they compose

    sealed trait LogOp[A]
    case class Error(msg: String) extends LogOp[Unit]
    case class Debug(msg: String) extends LogOp[Unit]

    class LogOps[F[_]](implicit I: Inject[LogOp, F]) {
      def error(msg: String) = Free.inject(Error(msg))
      def debug(msg: String) = Free.inject(Debug(msg))
    }

    object LogOps {
      implicit def LogOps[F[_]](implicit i: Inject[LogOp, F]) = new LogOps[F]
    }

    object LogInterpreter extends (LogOp ~> Id) {
      def apply[A](l: LogOp[A]) = l match {
        case Error(msg) => println(s"error: $msg")
        case Debug(msg) => println(s"debug: $msg")
      }
    }

    // coproducts & interpreters compose
    "and a third algebra is factored in" in {

      type C01[A]          = Coproduct[Interact, DataOp, A]
      type Application2[A] = Coproduct[LogOp, C01, A]

      val c01Interpreter: C01 ~> Id              = InteractInterpreter or InMemoryDataOpInterpreter
      val loggingInterpreter: Application2 ~> Id = LogInterpreter or c01Interpreter

      def prgrm(implicit I: Interacts[Application2],
                D: DataOps[Application2],
                L: LogOps[Application2]): Free[Application2, Unit] = {
        import I._, D._, L._
        for {
          cat  <- ask("kitty name?")
          _    <- debug(s"cat's name was $cat")
          _    <- addCat(cat)
          cats <- getAllCats()
          _    <- debug(s"fetched all cats: $cats")
          _    <- tell("all cats: " + cats.mkString(", "))
        } yield ()
      }
      val evaluated = prgrm foldMap loggingInterpreter
      println(s"combined algebras: $evaluated")
    }

    // TODO - we have used the Id for illustrative purposes - in reality we would rather be using Future or Task to capture effects

    // TODO - what do I do when I want to branch out into parallel execution? - Free Applicatives probably, but how do I do it?


    type Result[A] = Throwable Either A

    "now with typeclasses any type constructor with an instance can be used to capture eventful computation" in {
//      @typeclass
//      trait Capture[M[_]] {
//        def capture[A](a: => A): M[A]
//      }
//
//      class Interpreters[M[_] : Capture] {
//
//        def InteractInterpreter: Interact ~> M = new (Interact ~> M) {
//          def apply[A](i: Interact[A]) = i match {
//            case Ask(prompt) => Capture[M].capture({
//              println(prompt); "Tom"
//            }) // StdIn.readLine()
//            case Tell(msg) => Capture[M].capture({
//              println(msg)
//            })
//          }
//        }
//
//        def InMemoryDataOpInterpreter: DataOp ~> M = new (DataOp ~> M) {
//          private[this] val memDataSet = new ListBuffer[String]
//
//          def apply[A](fa: DataOp[A]) = fa match {
//            case AddCat(a) => Capture[M].capture({
//              memDataSet.append(a); a
//            })
//            case GetAllCats() => Capture[M].capture(memDataSet.toList)
//          }
//        }
//
//        def interpreter: Application ~> M =
//          InteractInterpreter or InMemoryDataOpInterpreter
//      }
//
//       M can be any type ctor for which a Capture instance exists, we can create interpreters for Task, Either, Try
//       or anything else that captures effectful computation
//
//      implicit val taskCaptureInstance = new Capture[Task] {
//        override def capture[A](a: => A): Task[A] = Task.evalOnce(a)
//      }
//
//      implicit val eitherCaptureInstance = new Capture[Result] {
//        override def capture[A](a: => A): Result[A] = Either.catchNonFatal(a)
//      }
//
//      implicit val tryCaptureInstance = new Capture[Try] {
//        override def capture[A](a: => A): Try[A] = Try(a)
//      }
//
//      val taskInterpreter = new Interpreters[Task].interpreter
//      val eitherInterpreter = new Interpreters[Result].interpreter
//      val tryInterpreter = new Interpreters[Try].interpreter
//
//      def prgrm2(implicit I: Interacts[Application], D: DataOps[Application]): Free[Application, Unit] = {
//
//        import I._, D._
//
//        for {
//          cat <- ask("kitty name?")
//          _ <- addCat(cat)
//          cats <- getAllCats()
//          _ <- tell("all cats: " + cats.mkString(", "))
//        } yield ()
//      }
//
//      val eitherProgram: Result[Unit] = prgrm2 foldMap eitherInterpreter
//      val taskProgram: Task[Unit] = prgrm2 foldMap taskInterpreter
//      val tryProgram: Try[Unit] = prgrm2 foldMap tryInterpreter
//
//      println("either: ")
//      println(eitherProgram)
//
//      println("task: ")
//      println(taskProgram)
//
//      println("try: ")
//      println(tryProgram)
//      throw new RuntimeException("ggkgkgu")
    }

  }

}
