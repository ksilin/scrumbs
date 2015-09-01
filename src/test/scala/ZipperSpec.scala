import org.scalatest._


class ZipperSpec extends FlatSpec with Matchers {


  val l = List(1, 2, 3, 4, 9, 7, 8, 10)
  val increasedPeak: List[Int] = List(1, 2, 3, 4, 10, 7, 8, 10)

  // finds the first numebr whis is greater than both it's predecessor and successor
  def firstPeak(list: List[Int]): Option[Int] = list match {
    case x :: y :: z :: tl if y > x && y > z => Some(y)
    case x :: tl => firstPeak(tl)
    case Nil => None
  }


  "simple case without zipper" should "work" in {
    firstPeak(l).get should ===(9)
  }

  "more complex case without zipper" should "work" in {


    // after finding the peak, return a list with the value fo the peak increased
    def raisePeak(list: List[Int]): Option[List[Int]] = {
      def rec(head: List[Int], tail: List[Int]): Option[List[Int]] = tail match {
        case x :: y :: z :: tl if y > x && y > z => Some((x :: head).reverse ::: ((y + 1) :: z :: tl))
        case x :: tl => rec(x :: head, tl)
        case Nil => None
      }
      rec(List.empty, list)
    }
    raisePeak(l).get should ===(increasedPeak)
  }

  // http://tech.schmitztech.com/scala/zippers.html
  "scalaz zipper" should "work wonders" in {

    import scalaz._
    import Scalaz._

    def raisePeak(list: List[Int]): Option[List[Int]] = {
      for {
        z <- list.toZipper
      // positions returns a Zipper[Zipper[T]] - combo of all possible zippers for the lsit
        peak <- z.positions.findNext({ f =>
          (f.previous, f.next) match {
            case (Some(p), Some(n)) => (p.focus < f.focus && n.focus < f.focus)
            case _ => false
          }
        })
      } yield peak.focus.modify(_ + 1).to[List]
    }

    raisePeak(l).get should ===(increasedPeak)
  }

  "scalaz zipper" should "help perform smoothing" in {

    // http://stackoverflow.com/questions/23984160/zipper-to-iterate-over-list-in-scala

    // official zipper exampel is kinda useless - shoows only the basics
    // https://github.com/dcapwell/scalaz-examples/blob/master/src/main/scala/com/github/scalaz_examples/util/ZipperExample.scala

    // and there is not so much about the zipper in the tutorial:
    // http://eed3si9n.com/learning-scalaz/Zipper.html

    import scalaz._
    import Scalaz._

    val weights: Stream[Double] = Stream.from(1).map(1.0 / math.pow(2, _))
    println(weights.take(10).toList)

    // fzipWith comes from here:
    // http://docs.typelevel.org/api/scalaz/stable/7.0.0/doc/scalaz/syntax/ZipOps.html
    def neighborWeights(neighbors: Stream[Double]): Stream[Double] =
      neighbors.fzipWith(weights)(_ * _)


    val lStream: Stream[Double] = List(1, 1, 1, 1, 1, 1, 1).map(_.toDouble).toStream

    val zipped: Stream[Double] = neighborWeights(lStream)
    println(zipped.to[List])

    // TODO - still dont get what cobind does. Looks like map to me, but perhaps not
    def smooth(data: NonEmptyList[Double]) = data.toZipper.cobind { z =>
      (z.focus + neighborWeights(z.lefts).sum + neighborWeights(z.rights).sum) / 3
    }

    println(smooth(NonEmptyList(0, 1, 2, 1, 0)).toList)

  }


  "scalaz zipper" should " have a comonad and know how to use it" in {

    // http://etorreborre.blogspot.de/2013/06/a-zipper-and-comonad-example.html

    // we are trying to specify the following method
//     def partition[A](seq: Seq[A])(relation: (A, A) => Boolean): Seq[NonEmptyList[A]]

    // for each element in a group, there exists at least another related element in the group
    // for each element in a group, there doesn't exist a related element in any other group
    // elements which are not related must end up in different groups


    val near = (n1: Int, n2: Int) => math.abs(n1 - n2) <= 1
//    partition(Seq(1, 2, 3, 7, 8, 9))(near)
  }



}
