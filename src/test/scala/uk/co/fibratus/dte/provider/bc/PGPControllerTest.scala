package uk.co.fibratus.dte.provider.bc

import java.io.File

import org.bouncycastle.openpgp.{PGPPublicKeyRing, PGPSecretKeyRing}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar

/**
  * Created by rjs on 15/11/2016.
  */
class PGPControllerTest extends FunSuite with MockitoSugar {

  def dummyOp(i: File, o: File, p: List[PGPPublicKeyRing], s: PGPSecretKeyRing, w: Array[Char]): Unit = {}

  test("doCore executes and sets timestamp") {
    val inputFile = mock[File]
    val parentFile = mock[File]
    when(parentFile.mkdirs).thenReturn(true)
    when(inputFile.getParentFile).thenReturn(parentFile)
    when(inputFile.lastModified).thenReturn(1)

    val outputFile = mock[File]
    when(outputFile.setLastModified(1)).thenReturn(true)

    val context = PGPContext(List.empty, List[PGPSecretKeyRing](mock[PGPSecretKeyRing]), "", "".toCharArray)

    PGPController.doCore(inputFile, outputFile, context, dummyOp)

    verify(inputFile).getParentFile
    verify(inputFile, times(1)).lastModified
    verify(outputFile, times(1)).setLastModified(1)
  }
}
