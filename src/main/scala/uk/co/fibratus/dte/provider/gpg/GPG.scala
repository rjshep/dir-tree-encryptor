package uk.co.fibratus.dte.provider.gpg

import java.io.{File, FileInputStream, FileOutputStream}

import com.freiheit.gnupg.{GnuPGContext, GnuPGKey}

/**
  * Created by rjs on 13/12/2016.
  */
object GPG {
  val ctx = new GnuPGContext

  def encrypt(clearFile: File, encFile: File, recipients: Array[GnuPGKey]): Unit = {
    val clear = ctx.createDataObject()
    val enc = ctx.createDataObject()

    clear.read(new FileInputStream(clearFile))
    ctx.encryptAndSign(recipients, clear, enc)

    val fos = new FileOutputStream(encFile)
    enc.write(fos)
    fos.close()
  }

  def decrypt(encFile: File, clearFile: File, recipients: Array[GnuPGKey]): Unit = {
    val enc = ctx.createDataObject()
    val clear = ctx.createDataObject()

    enc.read(new FileInputStream(encFile))
    ctx.decryptVerify(enc, clear)

    val fos = new FileOutputStream(clearFile)
    clear.write(fos)
    fos.close()
  }

  def keyForRecipients(recipients: Seq[String]): Array[GnuPGKey] = {
    val keys: Seq[GnuPGKey] = recipients.flatMap {
      r =>
        ctx.searchKeys(r)
    }
    keys.toArray
  }
}