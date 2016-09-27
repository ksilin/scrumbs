package com.example.free


import cats._
import cats.data._
import cats.free._
import cats.implicits._
import monix.eval.Task
import simulacrum.typeclass
import monix.cats._

import scala.util.Try
import org.scalatest.{FreeSpec, MustMatchers}

import scala.collection.mutable.ListBuffer

class Free472Spec extends FreeSpec with MustMatchers {

  //http://www.47deg.com/blog/fp-for-the-average-joe-part3-free-monads

  /** An application as a Coproduct of it's ADTs */
  type Application[A] = Coproduct[Interact, DataOp, A]

  /** User Interaction Algebra */
  sealed trait Interact[A]
  case class Ask(prompt: String) extends Interact[String]
  case class Tell(msg: String) extends Interact[Unit]

  /** Data Operations Algebra */
  sealed trait DataOp[A]
  case class AddCat(a: String) extends DataOp[String]
  case class GetAllCats() extends DataOp[List[String]]

  /** Smart Constructors */
  class Interacts[F[_]](implicit I: Inject[Interact, F]) {
    def tell(msg: String): Free[F, Unit] = Free.inject[Interact, F](Tell(msg))
    def ask(prompt: String): Free[F, String] = Free.inject[Interact, F](Ask(prompt))
  }

  object Interacts {
    implicit def interacts[F[_]](implicit I: Inject[Interact, F]): Interacts[F] = new Interacts[F]
  }

  class DataOps[F[_]](implicit I: Inject[DataOp, F]) {
    def addCat(a: String): Free[F, String] = Free.inject[DataOp, F](AddCat(a))
    def getAllCats: Free[F, List[String]] = Free.inject[DataOp, F](GetAllCats())
  }

  object DataOps {
    implicit def dataOps[F[_]](implicit I: Inject[DataOp, F]): DataOps[F] = new DataOps[F]
  }

  def program(implicit I: Interacts[Application], D: DataOps[Application]): Free[Application, Unit] = {

    import I._, D._

    for {
      cat <- ask("What's the kitty's name?")
      _ <- addCat(cat)
      cats <- getAllCats
      _ <- tell(cats.toString)
    } yield ()

  }


  "Applications as coproduct of free algrebras" - {

    "interpret" in {

      object InteractInterpreter extends (Interact ~> Id) {
        def apply[A](i: Interact[A]) = i match {
          case Ask(prompt) => println(prompt); "Tom" // StdIn.readLine()
          case Tell(msg) => println(msg)
        }
      }

      object InMemoryDataOpInterpreter extends (DataOp ~> Id) {
        private[this] val memDataSet = new ListBuffer[String]

        def apply[A](fa: DataOp[A]) = fa match {
          case AddCat(a) => memDataSet.append(a); a
          case GetAllCats() => memDataSet.toList
        }
      }

      val interpreter: Application ~> Id = InteractInterpreter or InMemoryDataOpInterpreter

      program foldMap interpreter
    }


  }

}
