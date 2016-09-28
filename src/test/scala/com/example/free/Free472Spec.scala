package com.example.free

import cats._
import cats.data._
import cats.free._
import org.scalatest.{FreeSpec, MustMatchers}

import scala.collection.mutable.ListBuffer

class Free472Spec extends FreeSpec with MustMatchers {

  //http://www.47deg.com/blog/fp-for-the-average-joe-part3-free-monads

  /** An application as a Coproduct of it's ADTs */
  type Application[A] = Coproduct[Interact, DataOp, A]

  /** User Interaction Algebra */
  sealed trait Interact[A]
  case class Ask(prompt: String) extends Interact[String]
  case class Tell(msg: String)   extends Interact[Unit]

  /** Data Operations Algebra */
  sealed trait DataOp[A]
  case class AddCat(a: String) extends DataOp[String]
  case class GetAllCats()      extends DataOp[List[String]]

  /** Smart Constructors */
  class Interacts[F[_]](implicit I: Inject[Interact, F]) {
    def tell(msg: String): Free[F, Unit]     = Free.inject[Interact, F](Tell(msg))
    def ask(prompt: String): Free[F, String] = Free.inject[Interact, F](Ask(prompt))
  }

  object Interacts {
    implicit def interacts[F[_]](implicit I: Inject[Interact, F]): Interacts[F] = new Interacts[F]
  }

  class DataOps[F[_]](implicit I: Inject[DataOp, F]) {
    def addCat(a: String): Free[F, String] = Free.inject[DataOp, F](AddCat(a))
    def getAllCats: Free[F, List[String]]  = Free.inject[DataOp, F](GetAllCats())
  }

  object DataOps {
    implicit def dataOps[F[_]](implicit I: Inject[DataOp, F]): DataOps[F] = new DataOps[F]
  }

  def program(implicit I: Interacts[Application], D: DataOps[Application]): Free[Application, Unit] = {

    import D._
    import I._

    for {
      cat  <- ask("What's the kitty's name?")
      _    <- addCat(cat)
      cats <- getAllCats
      _    <- tell(cats.toString)
    } yield ()

  }

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

  "Applications as coproduct of free algrebras" - {

    "interpret" in {

      val interpreter: Application ~> Id = InteractInterpreter or InMemoryDataOpInterpreter

      program foldMap interpreter
    }

    "add another algebra" in {

      sealed trait LogOp[A]
      case class Debug(a: String) extends LogOp[Unit]
      case class Info(a: String)  extends LogOp[Unit]

      class LogOps[F[_]](implicit I: Inject[LogOp, F]) {
        def debug(a: String): Free[F, Unit] = Free.inject[LogOp, F](Debug(a))
        def info(a: String): Free[F, Unit]  = Free.inject[LogOp, F](Info(a))
      }

      object LogOps {
        implicit def logOps[F[_]](implicit I: Inject[LogOp, F]): LogOps[F] = new LogOps[F]
      }

      type C01        = Coproduct[Interact, DataOp, A]
      type Application2[A] = Coproduct[LogOps, C01, A]

      object LogOpsInterpreter extends (LogOp ~> Id) {
        def apply[A](i: LogOp[A]) = i match {
          case Debug(msg) => println(msg)
          case Info(msg)  => println(msg)
        }
      }

      val c01Interpreter: C01 ~> Id      = InteractInterpreter or InMemoryDataOpInterpreter

      // Error:(117, 63) type mismatch;
//      found   : cats.arrow.FunctionK[[γ]cats.data.Coproduct[LogOp,C01,γ],cats.Id]
//      required: cats.~>[Application2,cats.Id]
//      (which expands to)  cats.arrow.FunctionK[Application2,cats.Id]
      val interpreter: Application2 ~> Id = LogOpsInterpreter or c01Interpreter
    }

  }

}
