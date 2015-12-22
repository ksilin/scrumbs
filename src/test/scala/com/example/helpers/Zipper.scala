package com.example.helpers


// the left list is required to be the 'reverse' of the left part of the original list
class Zipper[T](focus: T, left: List[T], right: List[T]) {

  def moveRight: Option[Zipper[T]] = right match {
    case x :: xs => Some(new Zipper[T](x, focus :: left, xs))
    case _ => None
  }

  def moveLeft: Option[Zipper[T]] = left match {
    case x :: xs => Some(new Zipper[T](x, xs, focus :: right))
    case _ => None
  }

  def toList: List[T] = left.reverse ::: (focus :: right)


}
