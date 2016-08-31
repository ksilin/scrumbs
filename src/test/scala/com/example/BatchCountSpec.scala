package com.example

import org.scalatest.{FreeSpec, MustMatchers}

class BatchCountSpec extends FreeSpec with MustMatchers {

  def runCount(offset: Int = 0,
               count: Int = 10,
               limit: Int = 10,
               parallelism: Int = 1): Int = {
    (count - offset - 1) / (limit * parallelism) + Math.min(1,
                                                            Math.max(count, 0))
  }

  def runIndices(batchNum: Int = 0,
                 threadNum: Int = 0,
                 baseOffset: Int = 0,
                 batchSize: Int = 10): Range = {
    val start: Int = startId(baseOffset, batchNum, 1, batchSize, 0)
    (start until start + batchSize)
  }

  def startId(baseOffset: Int, batchNum: Int, parallelism: Int, batchSize: Int, thread: Int) = baseOffset + batchNum * (parallelism * batchSize) + (thread * batchSize)

  "counting parallel batches from sizes, offsets and parallelism" - {

    "min of 1 run" in {
      runCount(0, 10, 10, 1) mustBe 1
    }

    "2 runs if over limit" in {
      runCount(0, 11, 10, 1) mustBe 2
    }

    "1 runs if over limit reduced by offset" in {
      runCount(1, 11, 10, 1) mustBe 1
    }

    "no runs if  count = 0" in {
      runCount(0, 0, 10, 1) mustBe 0
    }

    "no runs if count < 0" in {
      runCount(0, -5, 10,  1) mustBe 0
    }

    "first run" in {
      runIndices(0, 0, 0, 10) mustBe (0 to 9)
    }

    "no runs" in {
      runIndices(0, 0, 0, 0) mustBe 'empty //(0 until 0)
    }
  }

}
