package uk.co.fibratus.dte.provider.bc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPException
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.{Failure, Success}

/**
  * Created by rjs on 03/11/2016.
  */
class PGPTest extends FunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    Security.addProvider(new BouncyCastleProvider())
  }

  test("publicKeyringsFromCollection returns all keyrings") {
    val c = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring.gpg"), None)
    assert(c.size === 2)
  }

  test("publicKeyringsFromCollection returns specified keyring") {
    val c = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring.gpg"), Some("DTE Test User 2 <dte2@local>"))
    assert(c.size === 1)
    assert(c.head.getPublicKey.getUserIDs.next === "DTE Test User 2 <dte2@local>")
  }

  test("secretKeyringsFromCollection returns all keyrings") {
    val c = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring.gpg"), None)
    assert(c.size === 1)
  }

  test("secretKeyringsFromCollection returns specified keyring") {
    val c = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring.gpg"), Some("DTE Test User 1 <dte1@local>"))
    assert(c.size === 1)
    assert(c.head.getPublicKey.getUserIDs.next === "DTE Test User 1 <dte1@local>")
  }

  test("secretKeyringsFromCollection returns no keyring") {
    val c = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring.gpg"), Some("DTE Test User 2 <dte2@local>"))
    assert(c.size === 0)
  }

  test("decrypt successfully decrypts a file") {
    val out = new ByteArrayOutputStream()
    val secKey = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring2.gpg"), Some("DTE Test User 2 <dte2@local>"))
    val pubKeyRingList = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring.gpg"), None)
    PGP.decrypt(this.getClass.getResourceAsStream("/files/testfile.txt.gpg"), out, pubKeyRingList, secKey.head, "passwd".toCharArray)
    assert(out.toString === "testfile.txt contents\n")
  }

  test("decrypt fails to decrypts a file given incorrect secret key passphrase") {
    val out = new ByteArrayOutputStream()
    val secKey = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring2.gpg"), Some("DTE Test User 2 <dte2@local>"))
    val pubKeyRingList = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring.gpg"), None)
    val d = PGP.decrypt(this.getClass.getResourceAsStream("/files/testfile.txt.gpg"), out, pubKeyRingList, secKey.head, "notthepasswd".toCharArray)
    assert(d.isFailure)
    d match {
      case Failure(f) =>
        assert(f.isInstanceOf[PGPException])
        assert(f.getMessage === "checksum mismatch at 0 of 20")
      case Success(u) =>
    }
  }

  test("decrypt throws validation exception when unable to verify due to absence of key in public keyring") {
    val out = new ByteArrayOutputStream()
    val secKey = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring2.gpg"), Some("DTE Test User 2 <dte2@local>"))
    val pubKeyRingList = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring2.gpg"), None)

    val d = PGP.decrypt(this.getClass.getResourceAsStream("/files/testfile.txt.gpg"), out, pubKeyRingList, secKey.head, "passwd".toCharArray)
    assert(d.isFailure)
    d match {
      case Failure(f) =>
        assert(f.isInstanceOf[PGPException])
        assert(f.getMessage === "No public keys match message signer")
      case Success(u) =>
    }
  }

  test("encrypt successfully encrypts and signs a message") {
    val msg = "a test message"
    val in = new ByteArrayInputStream(msg.getBytes)
    val out = new ByteArrayOutputStream()

    val secKeyRing = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring2.gpg"), Some("DTE Test User 2 <dte2@local>"))
    val pubKeyRingList = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring.gpg"), Some("DTE Test User 1 <dte1@local>"))

    PGP.encrypt(in, out, pubKeyRingList, secKeyRing.head, "passwd".toCharArray)

    val decin = new ByteArrayInputStream(out.toByteArray)
    val decout = new ByteArrayOutputStream()
    val decsecKey = PGP.secretKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/secring.gpg"), Some("DTE Test User 1 <dte1@local>"))
    val decpubKeyRingList = PGP.publicKeyringsFromCollection(this.getClass.getResourceAsStream("/keys/pubring2.gpg"), None)
    PGP.decrypt(decin, decout, decpubKeyRingList, decsecKey.head, "passwd".toCharArray)
    assert(decout.toString === msg)
    println(decout.toString)
  }
}
