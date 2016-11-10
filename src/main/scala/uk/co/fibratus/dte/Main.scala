package uk.co.fibratus.dte

import java.io.File

/**
  * Created by rjs on 30/10/2016.
  */

//case class FileDetails(path: String, timestamp: Long)

object Main {
  def main(args: Array[String]): Unit = {

    //    val srcDirName = "c:\\temp\\test\\lib"
    //    val tgtDirName = "c:\\temp\\test\\two\\lib"
    //
    val srcDirName = "C:\\temp\\phantom"
    val tgtDirName = "C:\\temp\\phantom - Copy"

    val r = Compare.compare(new File(srcDirName), new File(tgtDirName))
    println(r)

  }


}