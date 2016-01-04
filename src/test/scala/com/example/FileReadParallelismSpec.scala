package com.example

import java.io.{File => JFile}
import java.nio.file.{Path, Files}
import java.util.stream.Stream

import better.files._
import org.scalatest._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.immutable.IndexedSeq
import scala.collection.{Iterator, mutable}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Random

class FileReadParallelismSpec extends FunSpec with Matchers {

  val log = LoggerFactory.getLogger(getClass)
  val r = new Random(31)
  val chunkSize: Int = 100

  // TODO - open same file multiple times for separate streams

  def createFakeFile(lines: Int = 1000): File = {
    val file = Files.createTempFile("parallelreads", "")
    file.toJava.deleteOnExit()
    (1 to lines) foreach { i =>
      file << s"$i * ${r.nextString(10)}"
    }
    file
  }

  def timed[A](name: String = "")(f: => A) = {
    val s = System.nanoTime
    val ret = f
    println(s"time for $name " + (System.nanoTime - s) / 1e6 + "ms")
    ret
  }

  val f = createFakeFile()

  val bigFile = createFakeFile(1000000)

  def fakeProcessor(s: String): String = {
    Thread.sleep(10)
    "booya"
  }

  def fastProcessor(s: String): String = identity(s)

  describe("raw text file line by line") {

    it("sequential") {
      // 10150
      timed("sequential") {
        val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(f.toJava).getLines.grouped(chunkSize)
        chunks foreach {
          _ foreach fakeProcessor
        }
      }
    }

    it("par lines in chunks") {
      // 1890
      System.in.read()
      timed("par lines in chunks") {
        val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(f.toJava).getLines.grouped(chunkSize)
        chunks.foreach {
          _.par foreach fakeProcessor
        }
      }
    }

    it("par lines in par chunks") {
      // 2030
      timed("par lines in par chunks") {
        val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(f.toJava).getLines.grouped(chunkSize)
        chunks.toIndexedSeq.par.foreach {
          _.par foreach fakeProcessor
        }
      }
    }

    it("par lines") {
      // 1900
      timed("par lines") {
        val lines: IndexedSeq[String] = Source.fromFile(f.toJava).getLines.toIndexedSeq
        lines.par foreach fakeProcessor
      }
    }

    it("line futures") {
      //1290
      import scala.concurrent.ExecutionContext.Implicits.global
      timed("line futures") {
        val lines: scala.Iterator[String] = Source.fromFile(f.toJava).getLines
        val eventualStrings: scala.Iterator[Future[String]] = lines map { line => Future {
          fakeProcessor(line)
        }
        }
        Await.result(Future.sequence(eventualStrings), 20 seconds)
      }
    }
    it("line futures with traverse") {
      //1290
      import scala.concurrent.ExecutionContext.Implicits.global
      timed("more line futures") {
        val lines: scala.Iterator[String] = Source.fromFile(f.toJava).getLines
        val eventualStrings: Future[scala.Iterator[String]] = Future.traverse(lines)(l => Future {
          fakeProcessor(l)
        })
        Await.result(eventualStrings, 20 seconds)
      }
    }

    it("preloaded into memory") {
      timed("preloaded into memory") {
        // 2000
        val lines: mutable.Buffer[String] = f.lines.asInstanceOf[mutable.Buffer[String]]
        val chunks: Iterator[mutable.Buffer[String]] = lines.grouped(chunkSize)

        chunks.foreach {
          _.par.foreach { line => fakeProcessor(line) }
        }
      }
    }

    it("sequential stream") {
      timed("sequential stream") {
        // 10180
        val streams: ManagedResource[Stream[String]] = Files.lines(f.path).autoClosed

        streams foreach { stream: Stream[String] =>
          val chunks: Iterator[String]#GroupedIterator[String] = stream.iterator.asScala.grouped(chunkSize)
          chunks foreach {
            _ foreach fakeProcessor
          }
        }
      }
    }

    it("parallel stream") {
      timed("parallel stream") {
        // 1780
        val streams: ManagedResource[Stream[String]] = Files.lines(f.path).autoClosed

        streams foreach { stream: Stream[String] =>
          val chunks: Iterator[String]#GroupedIterator[String] = stream.iterator.asScala.grouped(chunkSize)
          chunks foreach {
            _.par foreach fakeProcessor
          }
        }
      }
    }
  }

  // memory info
  val mb = 1024 * 1024
  val runtime = Runtime.getRuntime


  describe("clumsy memory profiling") {
    pending

    it("sequential") {
      // 10150
      val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(f.toJava).getLines.grouped(chunkSize)
      chunks foreach {
        _ foreach { l =>
          log.info("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb)
          fakeProcessor(l)
        }
      }
    }

    it("par lines in chunks") {
      // 1890
      val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(bigFile.toJava).getLines.grouped(chunkSize)
      chunks.foreach {
        _.par foreach { l =>
          log.info("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb)
          fastProcessor(l)
        }
      }
    }

    it("par lines in par chunks") {
      // 2030
      pending
      val chunks: Iterator[String]#GroupedIterator[String] = Source.fromFile(f.toJava).getLines.grouped(chunkSize)
      chunks.toIndexedSeq.par.foreach {
        _.par foreach fakeProcessor
      }
    }

    it("par lines") {
      // 1900
      pending
      val lines: IndexedSeq[String] = Source.fromFile(f.toJava).getLines.toIndexedSeq
      lines.par foreach fakeProcessor
    }

    it("line futures") {
      //1280
      pending
      import scala.concurrent.ExecutionContext.Implicits.global
      val lines: scala.Iterator[String] = Source.fromFile(f.toJava).getLines
      val eventualStrings: scala.Iterator[Future[String]] = lines map { line => Future {
        fakeProcessor(line)
      }
      }
      Await.result(Future.sequence(eventualStrings), 20 seconds)
    }

    it("preloaded into memory") {
      pending
      val lines: mutable.Buffer[String] = f.lines.asInstanceOf[mutable.Buffer[String]]
      val chunks: Iterator[mutable.Buffer[String]] = lines.grouped(chunkSize)

      chunks.foreach {
        _.par.foreach { line => fakeProcessor(line) }
      }
    }

    it("sequential stream") {
      pending
      val streams: ManagedResource[Stream[String]] = Files.lines(bigFile.path).autoClosed

      streams foreach { stream: Stream[String] =>
        val chunks: Iterator[String]#GroupedIterator[String] = stream.iterator.asScala.grouped(chunkSize)
        var count = 0
        chunks foreach {
          _ foreach { l =>
            count += 1
            if (count % 100 == 0) log.info("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb)
            fastProcessor(l)
          }
        }
      }
    }

    it("parallel stream") {
      val streams: ManagedResource[Stream[String]] = Files.lines(bigFile.path).autoClosed

      streams foreach { stream: Stream[String] =>
        val chunks: Iterator[String]#GroupedIterator[String] = stream.iterator.asScala.grouped(chunkSize)
        var count = 0
        chunks foreach {
          _.par foreach { l =>
            count += 1
            if (count % 100 == 0) log.info("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb)
            fastProcessor(l)
          }
        }
      }
    }
  }

  describe("raw xml file") {

    //  val splitter = XmlStreamElementProcessor.collectElements(_.last == tableName.toUpperCase)
    //  val zipFile = new org.apache.commons.compress.archivers.zip.ZipFile(file)

    //  val elems: Iterator[Elem] = zipFile.getEntries.flatMap { e =>
    //    val stream: InputStream = zipFile.getInputStream(e)
    //    splitter.processInputStream(stream)
    //  }

    it("sequential") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }

    it("parallel") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }
  }

  describe("zipped text file - line by line") {

    it("sequential") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }

    it("parallel") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }
  }

  describe("zipped xml file") {

    it("sequential") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }

    it("parallel") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }
  }

  describe("gzipped text file - line by line") {

    it("sequential") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }

    it("parallel") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }
  }

  describe("gzipped xml file") {

    it("sequential") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }

    it("parallel") {
      val start = System.currentTimeMillis

      val elapsed = System.currentTimeMillis - start
      println(s"elapsed in for comp: $elapsed")
    }
  }
}
