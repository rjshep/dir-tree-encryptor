package uk.co.fibratus.dte

import java.io._
import java.security.SecureRandom
import java.util.Date

import org.bouncycastle.bcpg.{CompressionAlgorithmTags, HashAlgorithmTags, SymmetricKeyAlgorithmTags}
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.bc._
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.openpgp.{PGPCompressedData, PGPOnePassSignatureList, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by rjs on 02/11/2016.
  */

case class Context(pubKeyRingList: List[PGPPublicKeyRing], factory: PGPObjectFactory, outputStream: OutputStream, onePassSignature: Option[PGPOnePassSignature])

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

  def decrypt(encStream: InputStream, clearStream: OutputStream, pubKeyRingList: List[PGPPublicKeyRing], secKeyRing: PGPSecretKeyRing, pass: String, checkSig: Boolean = false): Try[Unit] = {
    try {

      val edo = getEncryptedDataObject(encStream)

      val secKey = secKeyRing.getSecretKey(edo.getKeyID)

      if (secKey == null)
        throw new PGPException("File was not encrypted to the public key associated with the supplied private key")

      val privKey = secKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass.toCharArray))

      val clear = edo.getDataStream(new BcPublicKeyDataDecryptorFactory(privKey))

      val plainFact = new JcaPGPObjectFactory(clear)
      val context = Context(pubKeyRingList, plainFact, clearStream, None)

      processMessage(context)
      Success(Unit)
    }
    catch {
      case t: Throwable => Failure(t)
    }
  }

  def encrypt(clearStream: InputStream, encStream: OutputStream, pubKeyRingList: List[PGPPublicKeyRing], secKeyRing: PGPSecretKeyRing, pass: String) = {
    val publicKey = pubKeyRingList.head.getPublicKey

    val rand = new SecureRandom()
    rand.setSeed(System.currentTimeMillis())

    val encryptedDataGenerator = new PGPEncryptedDataGenerator(new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).setWithIntegrityPacket(true).setSecureRandom(rand))
    encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey))

    val compressedOut = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP).open(encryptedDataGenerator.open(encStream, new Array[Byte](4096)))

    val signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(publicKey.getAlgorithm, HashAlgorithmTags.SHA512))

    val privKey = secKeyRing.getSecretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass.toCharArray))

    signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privKey)
    signatureGenerator.generateOnePassVersion(true).encode(compressedOut)

    val finalOut = new PGPLiteralDataGenerator().open(compressedOut, PGPLiteralData.BINARY, "", new Date(), new Array[Byte](4096))

    val bytes = new Array[Byte](4096)
    Iterator
      .continually(clearStream.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read => {
        finalOut.write(bytes, 0, read)
        signatureGenerator.update(bytes, 0, read)
      })

    finalOut.close()
    clearStream.close()
    compressedOut.close()
    encryptedDataGenerator.close()
    encStream.close()
  }

  @tailrec
  private def processMessage(context: Context): Unit = {
    context.factory.nextObject() match {
      case x: PGPCompressedData =>
        processMessage(context.copy(factory = new JcaPGPObjectFactory(x.getDataStream)))
      case x: PGPOnePassSignatureList =>
        processMessage(processOPS(context, x.get(0)))
      case x: PGPLiteralData =>
        val is = x.getInputStream
        transformStream(context, is)
        processMessage(context)
      case x: PGPSignatureList =>
        context.onePassSignature.foreach(y => verifySignature(x.get(0), y))
        processMessage(context)
      case _ => // message == null so we're done
        if (context.onePassSignature.isEmpty) {
          throw new PGPException("No signature found")
        }
    }
  }

  private def processOPS(context: Context, ops: PGPOnePassSignature): Context = {
    val krl = context.pubKeyRingList
    val pubKeys = krl.map(_.getPublicKey(ops.getKeyID)).filter(_ != null)
    if (pubKeys.isEmpty) {
      throw new PGPException("No public keys match message signer")
    }
    ops.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKeys.head)
    context.copy(onePassSignature = Some(ops))
  }

  private def verifySignature(signature: PGPSignature, onePassSignature: PGPOnePassSignature) = {
    if (!onePassSignature.verify(signature))
      throw new PGPException("Signature verification failed")
  }

  private def transformStream(context: Context, inStream: InputStream) = {
    val bytes = new Array[Byte](1024)
    Iterator
      .continually(inStream.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read => {
        context.outputStream.write(bytes, 0, read)
        context.onePassSignature.foreach(_.update(bytes, 0, read))
      })
    context.outputStream.close()
  }

  private def getEncryptedDataObject(encStream: InputStream): PGPPublicKeyEncryptedData = {
    val pgpDecoder = PGPUtil.getDecoderStream(encStream)

    val pgpObjectFactory = new PGPObjectFactory(pgpDecoder, new BcKeyFingerprintCalculator())

    // Assumes only 1 object per encrypted stream
    val pgpObject = pgpObjectFactory.nextObject()

    val encList = pgpObject.asInstanceOf[PGPEncryptedDataList]

    encList.getEncryptedDataObjects.next.asInstanceOf[PGPPublicKeyEncryptedData]
  }
}