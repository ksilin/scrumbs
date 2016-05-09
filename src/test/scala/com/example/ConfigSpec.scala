package com.example

import com.typesafe.config.{ConfigValueFactory, ConfigObject, Config, ConfigFactory}
import org.scalatest.{FunSpec, Matchers}
import scala.collection.JavaConverters._

class ConfigSpec extends FunSpec with Matchers {

  describe("complex substitution") {

    it("should read config") {

      val conf = ConfigFactory.load("substitution")
      println(conf)
      val dbConf = conf.getConfig("db")
      println(dbConf)

      val dbLoc = "localhost"
      val dbName = "test_db"

      val locSpecificConf: Config = dbConf.getConfig(s"locations.$dbLoc")
      val dbSpecificConf: Config = dbConf.getConfig(dbName)

      val protocol: String = dbConf.getString("protocol")

      val url = s"$protocol${dbSpecificConf.getString("host")}:${locSpecificConf.getString("port")}/${dbSpecificConf.getString("db")}"

      val user = locSpecificConf.getString("user")
      val pw = locSpecificConf.getString("password")

      user should be("root")
      pw should be("secret")

      println(url)
    }
  }

  describe("synthetic config") {

    it("should create flat config programmatically") {

      val confMap: Map[String, Any] = Map("connectionPool" -> "disabled",
        "url" -> "jdbc:mysql://localhost:6603",
        "driver" -> "com.mysql.jdbc.Driver",
        "keepAliveConnection" -> "true")

      val outerMap: Map[String, Any] = Map("test_db" -> confMap)

      val j: java.util.Map[java.lang.String, java.lang.Object] = confMap.asJava.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]

      val jWrap: java.util.Map[java.lang.String, java.lang.Object] = outerMap.asJava.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]

      val syntheticConfig: Config = ConfigFactory.parseMap(j)

      val cv: ConfigObject = ConfigValueFactory.fromMap(j)

      println(syntheticConfig)
      println(cv)

      val cv2: Exception = intercept[Exception] {ConfigValueFactory.fromMap(jWrap)}

    }

    it("does not need to be explicitly typed as Strings") {
      val confMap: Map[String, Any] = Map("string" -> "disabled", "number" -> 123, "boolean" -> true)

      val j: java.util.Map[java.lang.String, java.lang.Object] = confMap.asJava.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]
      val syntheticConfig: Config = ConfigFactory.parseMap(j)
      println(syntheticConfig)

    }

    it("should break on creating nested conf") {
      val simpleMap: Map[String, Any] = Map("connectionPool" -> "disabled",
        "url" -> "jdbc:mysql://localhost:6603",
        "driver" -> "com.mysql.jdbc.Driver",
        "keepAliveConnection" -> "true")

      val wrapMap: Map[String, Any] = Map("test_db" -> simpleMap)
      val jMap: java.util.Map[java.lang.String, java.lang.Object] = wrapMap.asJava.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]

      val ex: Exception = intercept[Exception] {ConfigFactory.parseMap(jMap)}
      println(ex)
    }

    it("should be transformable into a value") {

      // TODO - can we make the value into a value of another Config?

    }

    it("should support fake nesting") {

      val fakeNesting: Map[String, Any] = Map("mydb.connectionPool" -> "disabled",
        "mydb.url" -> "jdbc:mysql://localhost:6603",
        "mydb.driver" -> "com.mysql.jdbc.Driver",
        "mydb.keepAliveConnection" -> "true")
    }
  }


}