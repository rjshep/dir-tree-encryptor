package uk.co.fibratus.dte.provider.gpg

/**
  * Created by rjs on 13/12/2016.
  */

import java.io.File
import java.util.Date

import com.freiheit.gnupg.GnuPGKey
import com.typesafe.scalalogging.Logger
import uk.co.fibratus.dte.Controller

case class GPGContext(recipients: Array[GnuPGKey])

class GPGController(val config: (String) => String, val logger: Logger) extends Controller {

  private val context = GPGContext(GPG.keyForRecipients(Seq(config("encryptToUser"))))

  def doEncrypt(inputFile: File, outputFile: File): Boolean =
    GPGController.doEncrypt(inputFile, outputFile, context)

  def doDecrypt(inputFile: File, outputFile: File): Boolean =
    GPGController.doDecrypt(inputFile, outputFile, context)
}

object GPGController {

  def doEncrypt(inputFile: File, outputFile: File, context: GPGContext): Boolean =
    doCore(inputFile, outputFile, context, GPG.encrypt)

  def doDecrypt(inputFile: File, outputFile: File, context: GPGContext): Boolean =
    doCore(inputFile, outputFile, context, GPG.decrypt)

  def doCore(inputFile: File, outputFile: File, context: GPGContext,
             f: (File, File, Array[GnuPGKey]) => Unit): Boolean = {
    outputFile.getParentFile.mkdirs

    f(
      inputFile,
      outputFile,
      context.recipients
    )

    outputFile.setLastModified(inputFile.lastModified())
  }
}
