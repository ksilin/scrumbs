import shapeless.syntax.std.tuple._

import scalaz._, std.option._, std.tuple._, syntax.bitraverse._
import scalaz._
import std.option._
import std.tuple._
import syntax.bitraverse._


object TuplesToPairs {

//  def toKeys(entities: List[Product], keyIndex: Int): List[String] = {
//    val keys: List[Option[String]] = entities.map { prod => prod(keyIndex) }
//    keys.flatten
//  }

  /*
Error:(13, 66) Expression keyIndex does not evaluate to an Int constant
  val keys: List[Option[String]] = entities.map { prod => prod(keyIndex) }
                                                               ^
 */

  /*
  Error:(13, 66) type mismatch;
 found   : Int
 required: shapeless.Nat
    val keys: List[Option[String]] = entities.map { prod => prod(keyIndex) }
                                                                 ^
   */

  def toDetails(entities: Seq[Product], keyIndex: Int, valIndex: Int): Set[(String, Seq[String])] = {

    val detailOpts: Seq[(Option[String], Option[String])] = entities.map { prod =>
      (abomination(prod, keyIndex), abomination(prod, valIndex))
    }

    val flattened = detailOpts.map { case (k, v) => (k.getOrElse(""), v.getOrElse("")) }

    val flouped: Map[String, Seq[String]] = flattened.groupBy(k => k._1).mapValues(v => v.map(_._2))

    flouped.to[Set]
  }

  def toDetails3(entities: Seq[Product], keyIndex: Int, valIndex: Int): Set[(String, Seq[String])] = {

    val detailOpts: Seq[(Option[String], Option[String])] = entities.map { prod =>
      (abomination(prod, keyIndex), abomination(prod, valIndex))
    }

    val optTup: Seq[Option[(String, String)]] = detailOpts.map(d => d.bisequence[Option, String, String])

    val flouped: Map[String, Seq[String]] = optTup.flatten.groupBy(_._1).mapValues(_.map(_._2))
    flouped.to[Set]
  }

  def abomination(prod: Product, index: Int): Option[String] = {
    prod.productElement(index).asInstanceOf[Option[String]]
  }
}
