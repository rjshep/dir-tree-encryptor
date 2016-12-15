package uk.co.fibratus.dte

import java.io._
import java.security.Security

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, DefaultParser, HelpFormatter, Options}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import uk.co.fibratus.dte.provider.bc.PGPController
import uk.co.fibratus.dte.provider.gpg.GPGController

/**
  * Created by rjs on 30/10/2016.
  */

//case class FileDetails(path: String, timestamp: Long)

object Main {

  def options: Options = {
    val o = new Options
    o.addOption("?", false, "help")
    o.addOption("c", true, "config file")
    o.addOption("v", false, "verbose logging")
    o.addOption("n", false, "no-op - dummy run")
    o.addOption("p", "provider", true, "BC or GPG")
    o
  }

  def getConfigItem(config: Config)(item: String): String = {
    config.getString(item)
  }

  def main(args: Array[String]): Unit = {
    val cmd = new DefaultParser().parse(options, args)

    if (cmd.hasOption("?")) {
      new HelpFormatter().printHelp("main", options)
    }
    else {
      process(cmd)
    }
  }

  def process(cmd: CommandLine):Unit = {
    lazy val configFile =
      if (cmd.hasOption("c")) cmd.getOptionValue("c")
      else System.getProperty("user.home") + File.separator + "dte.cfg"

    val config = ConfigFactory.parseFile(new File(configFile))
    val config2 = config
      .withValue("verbose", ConfigValueFactory.fromAnyRef(cmd.hasOption("v")))
      .withValue("noop", ConfigValueFactory.fromAnyRef(cmd.hasOption("n")))

    val conf = getConfigItem(config2)(_)

    val provider = cmd.getOptionValue('p')

    val logger = Logger(this.getClass)

    val controller = if (provider != null && provider.equals("BC")) {
      Security.addProvider(new BouncyCastleProvider())
      new PGPController(conf, logger)
    } else {
      new GPGController(conf, logger)
    }

    controller.process()
  }
}