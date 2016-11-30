package uk.co.fibratus.dte

import java.io._
import java.security.Security

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{DefaultParser, Options}
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Created by rjs on 30/10/2016.
  */

//case class FileDetails(path: String, timestamp: Long)

object Main {

  def options: Options = {
    val o = new Options
    o.addOption("c", true, "config file")
    o.addOption("v", false, "verbose logging")
    o.addOption("n", false, "no-op - dummy run")
    o
  }

  def getConfigItem(config: Config)(item: String): String = {
    config.getString(item)
  }

  def main(args: Array[String]): Unit = {
    Security.addProvider(new BouncyCastleProvider())

    val cmd = new DefaultParser().parse(options, args)

    lazy val configFile =
      if (cmd.hasOption("c")) cmd.getOptionValue("c")
      else System.getProperty("user.home") + File.separator + "dte.cfg"

    val config = ConfigFactory.parseFile(new File(configFile))
    val config2 = config
      .withValue("verbose", ConfigValueFactory.fromAnyRef(cmd.hasOption("v")))
      .withValue("noop", ConfigValueFactory.fromAnyRef(cmd.hasOption("n")))

    val conf = getConfigItem(config2)(_)

    new Controller(conf, Logger(this.getClass)).process()
  }
}