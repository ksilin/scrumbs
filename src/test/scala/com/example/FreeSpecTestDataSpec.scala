package com.example

import org.scalatest._

class FreeSpecTestDataSpec extends FreeSpec with Matchers with BeforeAndAfter {

  println(suiteName)
  println(testNames)
  println(tags)

  override def run(testName: Option[String], args: Args): Status = {
    println(s"running test: $testName with args: $args")
    // running test: None with args: Args(org.scalatest.DispatchReporter@d4342c2,org.scalatest.Stopper$DefaultStopper@2bbf180e,org.scalatest.Filter@163e4e87,Map(),None,org.scalatest.Tracker@5276e6b0,Set(),false,None,None)
    super.run(testName, args)
  }

  "The test" - {
    "contain testdata" in {
      // TODO - tbc
       true should be(false)
    }
  }
}
