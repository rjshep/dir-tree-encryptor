package uk.co.fibratus.dte.provider.bc

import java.io.File
import java.util.Base64

import com.typesafe.scalalogging.Logger
import org.bouncycastle.openpgp.{PGPPublicKeyRing, PGPSecretKeyRing}
import uk.co.fibratus.dte.Controller

/**
  * Created by rjs on 13/12/2016.
  */
case class PGPContext(pubRingList: List[PGPPublicKeyRing], secRingList: List[PGPSecretKeyRing], id: String, passphrase: Array[Char])

class PGPController(val config: (String) => String, val logger: Logger) extends Controller {

  private val pubring: List[PGPPublicKeyRing] = PGP.publicKeyringsFromCollection(config("publicKeyRing"), Some(config("encryptToUser")))
  private val secring: List[PGPSecretKeyRing] = PGP.secretKeyringsFromCollection(config("secretKeyRing"))

  private val passphrase: Array[Char] = new String(Base64.getDecoder.decode(config("secretKeyPassword"))).toCharArray

  private val context = PGPContext(pubring, secring, config("encryptToUser"), passphrase)

  def doEncrypt(inputFile: File, outputFile: File): Boolean =
    PGPController.doEncrypt(inputFile, outputFile, context)

  def doDecrypt(inputFile: File, outputFile: File): Boolean =
    PGPController.doDecrypt(inputFile, outputFile, context)
}

object PGPController {

  def doEncrypt(inputFile: File, outputFile: File, context: PGPContext): Boolean =
    doCore(inputFile, outputFile, context, PGP.encrypt)

  def doDecrypt(inputFile: File, outputFile: File, context: PGPContext): Boolean =
    doCore(inputFile, outputFile, context, PGP.decrypt)

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
}
