package uk.co.fibratus.dte

import java.io._

import org.bouncycastle.openpgp.{PGPOnePassSignatureList, _}
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.bc.{BcKeyFingerprintCalculator, BcPBESecretKeyDecryptorBuilder, BcPGPDigestCalculatorProvider, BcPublicKeyDataDecryptorFactory}
import org.bouncycastle.openpgp.operator.jcajce.{JcaPGPContentSignerBuilder, JcaPGPContentVerifierBuilderProvider}

import scala.collection.JavaConverters._

/**
  * Created by rjs on 02/11/2016.
  */
object PGP {

  def publicKeyringsFromCollection(file: String, userId: Option[String] = None): List[PGPPublicKeyRing] = {
    publicKeyringsFromCollection(new FileInputStream(file), None)
  }

  def publicKeyringsFromCollection(in: InputStream, userId: Option[String]): List[PGPPublicKeyRing] = {
    val col = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator())
    val it = userId match {
      case Some(u) => col.getKeyRings(u, false, false).asScala
      case _ => col.getKeyRings.asScala
    }
    it.toList
  }

  def secretKeyringsFromCollection(file: String, userId: Option[String] = None): List[PGPSecretKeyRing] = {
    secretKeyringsFromCollection(new FileInputStream(file), None)
  }

  def secretKeyringsFromCollection(in: InputStream, userId: Option[String]): List[PGPSecretKeyRing] = {
    val col = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator())
    val it: Iterator[PGPSecretKeyRing] = userId match {
      case Some(u) => col.getKeyRings(u, false, false).asScala
      case _ => col.getKeyRings.asScala
    }
    it.toList
  }

  def decrypt(encStream: InputStream, decStream: OutputStream, secKeyRing: PGPSecretKeyRing, pass: Array[Char], checkSig: Boolean = false) = {

    val edo = getEncryptedDataObject(encStream)

    val secKey = secKeyRing.getSecretKey(edo.getKeyID)

    if (secKey == null)
      throw new IllegalArgumentException("File was not encrypted to the public key associated with the supplied private key")

    val privKey = secKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass))

    val (dataStream, signatureList) = getDataStreamAndSignatureList(edo, privKey)

    val sig = if(checkSig) {
      if (signatureList.isEmpty || signatureList.size == 0) throw new IllegalArgumentException("Signature required but not found")
      val s = signatureList.get.get(0)
        s.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), secKey.getPublicKey)
        Some(s)
    }
    else None

    transformStream(dataStream, decStream, sig)

    if(sig.isDefined) {

    }
  }


  private def getDataStreamAndSignatureList(edo: PGPPublicKeyEncryptedData, privKey: PGPPrivateKey): (InputStream, Option[PGPOnePassSignatureList]) = {
    val pgpFact = new JcaPGPObjectFactory(edo.getDataStream(new BcPublicKeyDataDecryptorFactory(privKey)))

    val plainFactory = new JcaPGPObjectFactory(pgpFact.nextObject.asInstanceOf[PGPCompressedData].getDataStream)

    val sig = plainFactory.nextObject.asInstanceOf[PGPOnePassSignatureList]

    val dataStream = plainFactory.nextObject.asInstanceOf[PGPLiteralData].getDataStream

    (dataStream, if(sig == null) None else Some(sig))
  }

  private def transformStream(inStream: InputStream, outStream: OutputStream, sig: Option[PGPOnePassSignature]) = {
    val bytes = new Array[Byte](1024)
    Iterator
      .continually(inStream.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read => {
        outStream.write(bytes,0,read)
        if(sig.isDefined) sig.get.update(bytes, 0, read)
      })
    outStream.close()
  }

  private def getEncryptedDataObject(encStream: InputStream): PGPPublicKeyEncryptedData = {
    val pgpDecoder = PGPUtil.getDecoderStream(encStream)

    val pgpObjectFactory = new PGPObjectFactory(pgpDecoder, new BcKeyFingerprintCalculator())

    // Assumes only 1 object per encrypted stream
    val pgpObject = pgpObjectFactory.nextObject()

    val encList = pgpObject.asInstanceOf[PGPEncryptedDataList]

    val edo = encList.getEncryptedDataObjects.next.asInstanceOf[PGPPublicKeyEncryptedData]
    edo
  }
}