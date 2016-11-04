package uk.co.fibratus.dte

import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite

/**
  * Created by rjs on 03/11/2016.
  */
class PGPTest extends FunSuite {

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
    PGP.decrypt(this.getClass.getResourceAsStream("/files/testfile.txt.gpg"), out, secKey.head, "passwd".toCharArray)
    assert(out.toString === "testfile.txt contents\n")
  }
}
