package com.example

package object genetic {

  type Chromo = Vector[Boolean]

  trait Genetic[A] {
    def asA(c: Chromo): A
  }

  type GeneticRoulette = Double => Option[Chromo]

  def genRoulette(chromoFit: Vector[(Chromo, Long)]): GeneticRoulette = ???

}
