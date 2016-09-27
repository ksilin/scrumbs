package util

trait Timed {

  def timed[A](name: String = "")(f: => A) = {
    val s = System.nanoTime
    val ret = f
    println(s"time for $name " + (System.nanoTime - s) / 1e6 + "ms")
    ret
  }

}
