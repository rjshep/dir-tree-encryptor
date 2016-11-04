package uk.co.fibratus.dte

import java.io._
import java.security.{SecureRandom, Security}
import java.util.Date

import org.bouncycastle.bcpg.{CompressionAlgorithmTags, SymmetricKeyAlgorithmTags}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.bc._

import scala.annotation.tailrec
import org.bouncycastle.openpgp.{PGPEncryptedDataList, _}
import org.bouncycastle.openpgp.operator.jcajce.{JcaKeyFingerprintCalculator, JcePBESecretKeyDecryptorBuilder, JcePublicKeyDataDecryptorFactoryBuilder};

/**
  * Created by rjs on 30/10/2016.
  */

case class FileDetails(path: String, timestamp: Long)

object Main {
  def readSecretKeyFromCol(in1: InputStream, keyId: Long): PGPSecretKey = {
    val in = PGPUtil.getDecoderStream(in1)
    val pgpSec = new PGPSecretKeyRingCollection(in, new BcKeyFingerprintCalculator())

    val key = pgpSec.getSecretKey(keyId)

    if (key == null) {
      throw new IllegalArgumentException("1Can't find encryption key in key ring.")
    }

    key
  }

  def readPublicKeyFromCol(in1: InputStream): PGPPublicKey = {
    val in = PGPUtil.getDecoderStream(in1)
    val pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator())
    var key: PGPPublicKey = null
    val rIt = pgpPub.getKeyRings()
    while (key == null && rIt.hasNext()) {
      val kRing = rIt.next().asInstanceOf[PGPPublicKeyRing]
      val kIt = kRing.getPublicKeys()
      while (key == null && kIt.hasNext()) {
        val k = kIt.next().asInstanceOf[PGPPublicKey]
        if (k.isEncryptionKey() && k.getKeyID!=4036557336601280739L && k.getKeyID()!= -8810353164988946729L && k.getKeyID()!= -2159824979917209833L) {
          key = k
        }
      }
    }
    if (key == null) {
      throw new IllegalArgumentException("2Can't find encryption key in key ring.")
    }
    key
  }

  def decryptFile(in1: InputStream, secKeyIn: InputStream, pubKeyIn: InputStream, pass: Array[Char]) = {
    Security.addProvider(new BouncyCastleProvider())

    val pubKey = readPublicKeyFromCol(pubKeyIn)

    var secKey = readSecretKeyFromCol(secKeyIn, pubKey.getKeyID())

    val in = PGPUtil.getDecoderStream(in1)

    var pgpFact: JcaPGPObjectFactory = null


    val  pgpF = new PGPObjectFactory(in, new BcKeyFingerprintCalculator())

    val o = pgpF.nextObject()
    var encList: PGPEncryptedDataList = null

    if (o.isInstanceOf[PGPEncryptedDataList]) {

      encList = o.asInstanceOf[PGPEncryptedDataList]

    } else {

      encList = pgpF.nextObject().asInstanceOf[PGPEncryptedDataList]
    }

    val itt = encList.getEncryptedDataObjects()
    var sKey: PGPPrivateKey  = null;
    var encP: PGPPublicKeyEncryptedData = null
    while (sKey == null && itt.hasNext()) {
      encP = itt.next().asInstanceOf[PGPPublicKeyEncryptedData]
//      secKey = readSecretKeyFromCol(new FileInputStream("PrivateKey.asc"), encP.getKeyID())
      secKey = readSecretKeyFromCol(new FileInputStream("c:\\users\\rjs\\git\\gpg\\secring.gpg"), encP.getKeyID())
      sKey = secKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass))
    }
    if (sKey == null) {
      throw new IllegalArgumentException("Secret key for message not found.")
    }

    val clear = encP.getDataStream(new BcPublicKeyDataDecryptorFactory(sKey))

    pgpFact = new JcaPGPObjectFactory(clear)

    val c1: PGPCompressedData  = pgpFact.nextObject().asInstanceOf[PGPCompressedData]

    val pgpFact2 = new JcaPGPObjectFactory(c1.getDataStream())

    val ld = pgpFact2.nextObject().asInstanceOf[PGPLiteralData]
    val bOut = new ByteArrayOutputStream()

    val inLd = ld.getDataStream()

    var ch:Int = 0

    do {
      ch = inLd.read()
      if (ch != -1) bOut.write(ch)
    }
    while(ch!= -1)

   // bOut.writeTo(new FileOutputStream(ld.getFileName()))
    bOut.writeTo(System.out)
  }

  def encryptFile(out: OutputStream, fileName: String, encKey: PGPPublicKey)= {
    Security.addProvider(new BouncyCastleProvider())

    val bOut = new ByteArrayOutputStream()

    val comData = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP)

    PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY, new File(fileName))

    comData.close()

    val cPk = new PGPEncryptedDataGenerator(new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.TRIPLE_DES).setSecureRandom(new SecureRandom()).setWithIntegrityPacket(true))

    cPk.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encKey))

    val bytes = bOut.toByteArray()

    val cOut = cPk.open(out, bytes.length)

    cOut.write(bytes)

    cOut.close()

    out.close()
  }


  def main(args: Array[String]): Unit = {
//    println("Hello, world!")

//    decryptFile(new FileInputStream("c:\\users\\rjs\\git\\gpg\\test.txt.gpg"),
//      new FileInputStream("c:\\users\\rjs\\git\\gpg\\secring.gpg"),
//      new FileInputStream("c:\\users\\rjs\\git\\gpg\\pubring.gpg"),
//    "3Uz941L@@m06".toCharArray)
//
//    val pubKey = readPublicKeyFromCol(new FileInputStream("c:\\users\\rjs\\git\\gpg\\pubring.gpg"))
//
//    encryptFile(new FileOutputStream("c:\\users\\rjs\\git\\gpg\\out.txt.gpg"),
//      "c:\\users\\rjs\\git\\gpg\\out.txt",
//      pubKey)

//
//    PGP.publicKeyringsFromCollection("c:\\users\\rjs\\git\\gpg\\pubring.gpg").foreach {
//      x =>
//        val kr = x._2
//
//        val it = kr.getPublicKeys()
//        while(it.hasNext) {
//          val pk = it.next
//
//          println(if (pk.getUserIDs.hasNext) pk.getUserIDs.next else "")
//        }
//    }
//
//    return

    val srcDirName = "c:\\temp\\phantom"
    val tgtDirName = "c:\\temp\\phantom - copy"

    val src: List[File] = scan(new File(srcDirName))
    val tgt: List[File] = scan(new File(tgtDirName))

    val x = src.map(f=>FileDetails(f.getAbsolutePath.substring(srcDirName.length), f.lastModified())).toSet
    val y = tgt.map(f=>FileDetails(f.getAbsolutePath.substring(tgtDirName.length), f.lastModified())).toSet

    //println(x.head)

//    println(src.length)
//    println(tgt.length)
//    println(src.toSet.filterNot(tgt.toSet).size)

    val a=x.filterNot(y)//.map(f=> f.path +" "+new Date(f.timestamp).toString)
    val b=y.filterNot(x)//.map(f=> f.path +" "+new Date(f.timestamp).toString)

    println(a)
    println(b)

    println()
    val c = a.map(_.path)
    val d = b.map(_.path)

    println("Update: "+c.filter(d))
    println("New src: "+c.filterNot(d))
    println("New tgt: "+d.filterNot(c))
  }

  def scan(file: File): List[File] = {

    @scala.annotation.tailrec
    def sc(acc: List[File], files: List[File]): List[File] = {
      files match {
        case Nil => acc
        case x :: xs => {
          x.isDirectory match {
            case false => sc(x :: acc, xs)
            case true => sc(acc, xs ::: x.listFiles.toList)
          }
        }
      }
    }

    sc(List(), List(file))
  }
}