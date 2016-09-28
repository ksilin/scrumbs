package com.example.logging

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FreeSpec, MustMatchers}
import util.Timed

class LoggingSpec extends FreeSpec with MustMatchers with Timed with LazyLogging {

  "logging speed" - {

//    after reading these I decided to do a hslf-assed approx benchmark

    // http://manuel.bernhardt.io/2016/08/31/akka-anti-patterns-logging-the-wrong-way/
//    http://blog.takipi.com/how-to-instantly-improve-your-java-logging-with-7-logback-tweaks/
//    http://logback.qos.ch/manual/appenders.html#AsyncAppender
    //http://www.nurkiewicz.com/2013/04/siftingappender-logging-different.html
//    https://dzone.com/articles/siftingappender-logging

    "interpolating" in {
      val range: Range = 0 until 100000
      timed("simple logging") {
        range foreach (_ => logger.info("hi, logging on info"))
      }
      // STDOUT - 624.970362ms
      // ASYNC to STTDOUT - 888.793873ms
      // FILE - 593.341766ms
      // ASYNC to FILE - 839.705545ms
      // SIFT to FILE - single thread - 634.137058ms

      // seems to be irrelevant for me ATM
    }

  }

}
