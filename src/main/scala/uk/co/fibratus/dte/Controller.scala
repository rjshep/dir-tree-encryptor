package uk.co.fibratus.dte

import java.io._
import java.util.Date

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
    val client = config("clientId")
    val syncState = new SyncState(client, destDirName)
    val syncTimestamp = syncState.timestamp

    val r = Compare.compare(new File(srcDirName), new File(destDirName))

    def srcFileFromPath(path: String): File = new File(srcDirName + path)

    def destFileFromPath(path: String): File = new File(destDirName + path)

    def srcFileFromFileDetails(fileDetails: FileDetails): File = srcFileFromPath(fileDetails.path)

    def destFileFromFileDetails(fileDetails: FileDetails): File = destFileFromPath(fileDetails.path)

    def delete(file: File) = if(!noop) file.delete()

    def encrypt(inputFile: File, outputFile: File) = if(!noop) doEncrypt(inputFile, outputFile)

    def decrypt(inputFile: File, outputFile: File) = if(!noop) doDecrypt(inputFile, outputFile)

    def log(msg: String) = if(verbose) logger.info(msg)

    r.newSrc foreach {
      f =>
        val src = srcFileFromFileDetails(f)
          if(syncTimestamp.isDefined &&
            (f.crTimestamp > syncTimestamp.get ||
              f.modTimestamp > syncTimestamp.get)
          ) {
            log(s"Encrypting new file: ${src.getAbsolutePath}")
            encrypt(src, destFileFromFileDetails(f))
          } else {
            log(s"Deleting: [${new Date(syncTimestamp.get)}] [${new Date(f.crTimestamp)}] [${new Date(f.modTimestamp)}] ${src.getAbsolutePath}")
            delete(src)
          }
    }

    r.newDest foreach {
      f =>
        val dest = destFileFromFileDetails(f)
          if(syncTimestamp.isDefined && f.modTimestamp < syncTimestamp.get) {
            log(s"Deleting: ${dest.getAbsolutePath}")
            delete(dest)
          } else {
            log(s"Decrypting new file: ${dest.getAbsolutePath}")
            decrypt(dest, srcFileFromFileDetails(f))
          }
    }

    r.updated foreach {
      f =>
        if (f.srcTimestamp > f.destTimestamp) {
          val src = srcFileFromPath(f.path)
          log(s"Encrypting updated file: ${src.getAbsolutePath}")
          encrypt(src, destFileFromPath(f.path))
        } else {
          val dest = destFileFromPath(f.path)
          log(s"Decrypting updated file: ${dest.getAbsolutePath}")
          decrypt(dest, srcFileFromPath(f.path))
        }
    }

    if(!noop) syncState.update()
  }
}


