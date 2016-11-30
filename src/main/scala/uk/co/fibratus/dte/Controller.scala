package uk.co.fibratus.dte

import java.io._
import java.util.Base64

import com.typesafe.scalalogging.Logger
import org.bouncycastle.openpgp.{PGPPublicKeyRing, PGPSecretKeyRing}

/**
  * Created by rjs on 15/11/2016.
  */

case class PGPContext(pubRingList: List[PGPPublicKeyRing], secRingList: List[PGPSecretKeyRing], id: String, passphrase: Array[Char])

object Controller {

   def doCore(inputFile: File, outputFile: File, context: PGPContext,
                     f: (File, File, List[PGPPublicKeyRing], PGPSecretKeyRing, Array[Char]) => Unit): Boolean = {
    inputFile.getParentFile.mkdirs

    f(
      inputFile,
      outputFile,
      context.pubRingList,
      context.secRingList.head,
      context.passphrase
    )

    outputFile.setLastModified(inputFile.lastModified())
  }

  def doEncrypt(inputFile: File, outputFile: File, context: PGPContext): Boolean =
    doCore(inputFile, outputFile, context, PGP.encrypt)

  def doDecrypt(inputFile: File, outputFile: File, context: PGPContext): Boolean =
    doCore(inputFile, outputFile, context, PGP.decrypt)
}

class Controller(config: (String) => String, logger: Logger) {

  def process(): Unit = {
    val srcDirName = config("srcDir")
    val destDirName = config("destDir")

    val r = Compare.compare(new File(srcDirName), new File(destDirName))

    val pubring: List[PGPPublicKeyRing] = PGP.publicKeyringsFromCollection(config("publicKeyRing"), Some(config("encryptToUser")))
    val secring: List[PGPSecretKeyRing] = PGP.secretKeyringsFromCollection(config("secretKeyRing"))

    val passphrase = new String(Base64.getDecoder.decode(config("secretKeyPassword"))).toCharArray

    val context = PGPContext(pubring, secring, config("encryptToUser"), passphrase)

    def srcFileFromPath(path: String):File = new File(srcDirName + path)
    def destFileFromPath(path: String):File = new File(destDirName + path)

    def srcFileFromFileDetails(fileDetails: FileDetails):File = srcFileFromPath(fileDetails.path)
    def destFileFromFileDetails(fileDetails: FileDetails):File = destFileFromPath(fileDetails.path)

    val verbose = config("verbose").equals("true")
    val noop = config("noop").equals("true")

    r.newSrc foreach {
      f =>
        val src = srcFileFromFileDetails(f)
        if(verbose) logger.info(s"Encrypting new file: ${src.getAbsolutePath}")
        if(!noop) Controller.doEncrypt(src, destFileFromFileDetails(f), context)
    }

    r.newDest foreach {
      f =>
        val dest = destFileFromFileDetails(f)
        if(verbose) logger.info(s"Decrypting new file: ${dest.getAbsolutePath}")
        if(!noop) Controller.doDecrypt(dest, srcFileFromFileDetails(f), context)
    }

    r.updated foreach {
      f =>
        if (f.srcTimestamp > f.destTimestamp) {
          val src = srcFileFromPath(f.path)
          if(verbose) logger.info(s"Encrypting updated file: ${src.getAbsolutePath}")
          if(!noop) Controller.doEncrypt(src, destFileFromPath(f.path), context)
        } else {
          val dest = destFileFromPath(f.path)
          if(verbose) logger.info(s"Decrypting updated file: ${dest.getAbsolutePath}")
          if(!noop) Controller.doDecrypt(dest, srcFileFromPath(f.path), context)
        }
    }
  }
}
