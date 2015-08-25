import scalaz._
import scalaz.std.option._
import scalaz.std.tuple._
import scalaz.syntax.bitraverse._

// see 2015.08.20 notes
object TuplesToPairs {

  import shapeless._
  import ops.hlist._
  

  def getAtIndex[T, L <: HList, N <: Nat](tpl: T, index: N)(implicit
                                                     gen: Generic.Aux[T, L],
                                                     at: At[L, N])
  : at.Out =
    at(gen.to(tpl))

  def getFirstStringOption[T, L <: HList](tpl: T)(implicit
                                    gen: Generic.Aux[T, L],
                                    at: Selector[L, Option[String]])
  : at.Out =
    at(gen.to(tpl))

//  def toKeys(entities: List[Product], keyIndex: Int): List[String] = {
//  import shapeless._
//  import syntax.std.tuple._
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
