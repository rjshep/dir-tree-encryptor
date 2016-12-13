package uk.co.fibratus.dte

import java.io._

import com.typesafe.scalalogging.Logger

/**
  * Created by rjs on 15/11/2016.
  */

trait Controller {
  val config: (String) => String

  val logger: Logger

  def doEncrypt(inputFile: File, outputFile: File): Boolean

  def doDecrypt(inputFile: File, outputFile: File): Boolean

  def process(): Unit = {
    val srcDirName = config("srcDir")
    val destDirName = config("destDir")
    val verbose = config("verbose").equals("true")
    val noop = config("noop").equals("true")

    val r = Compare.compare(new File(srcDirName), new File(destDirName))

    def srcFileFromPath(path: String): File = new File(srcDirName + path)

    def destFileFromPath(path: String): File = new File(destDirName + path)

    def srcFileFromFileDetails(fileDetails: FileDetails): File = srcFileFromPath(fileDetails.path)

    def destFileFromFileDetails(fileDetails: FileDetails): File = destFileFromPath(fileDetails.path)

    r.newSrc foreach {
      f =>
        val src = srcFileFromFileDetails(f)
        if (verbose) logger.info(s"Encrypting new file: ${src.getAbsolutePath}")
        if (!noop) doEncrypt(src, destFileFromFileDetails(f))
    }

    r.newDest foreach {
      f =>
        val dest = destFileFromFileDetails(f)
        if (verbose) logger.info(s"Decrypting new file: ${dest.getAbsolutePath}")
        if (!noop) doDecrypt(dest, srcFileFromFileDetails(f))
    }

    r.updated foreach {
      f =>
        if (f.srcTimestamp > f.destTimestamp) {
          val src = srcFileFromPath(f.path)
          if (verbose) logger.info(s"Encrypting updated file: ${src.getAbsolutePath}")
          if (!noop) doEncrypt(src, destFileFromPath(f.path))
        } else {
          val dest = destFileFromPath(f.path)
          if (verbose) logger.info(s"Decrypting updated file: ${dest.getAbsolutePath}")
          if (!noop) doDecrypt(dest, srcFileFromPath(f.path))
        }
    }
  }
}


