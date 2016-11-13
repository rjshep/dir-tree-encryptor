package uk.co.fibratus.dte

import java.io.File
import java.util.Date

/**
  * Created by rjs on 05/11/2016.
  */

case class FileDetails(path: String, timestamp: Long) {
//  override def equals(o: Any) = o match {
//    case that: FileDetails => that.path.equalsIgnoreCase(this.path)
//    case _ => false
//  }
}

case class UpdatedFileDetails(path: String, srcTimestamp: Long, destTimestamp: Long)

case class CompareResult(newSrc: Seq[FileDetails], newDest: Seq[FileDetails], updated: Set[UpdatedFileDetails]) {
  override def toString: String = {
    val sb = new StringBuilder()
    sb ++= "New src:\n"
    newSrc.foreach(x=>sb.append(s"\t${x.path}, ${new Date(x.timestamp)}\n"))
    sb ++= "\nNew dest:\n"
    newDest.foreach(x=>sb.append(s"\t${x.path}, ${new Date(x.timestamp)}\n"))
    sb ++= "\nUpdated:\n"
    updated.map(x=>sb.append(s"\t${x.path}, ${new Date(x.srcTimestamp)}, ${new Date(x.destTimestamp)}\n"))
    sb.toString()
  }
}

object Compare {

  private def fileDetailsSorter(x: FileDetails, y:FileDetails) = x.path < y.path

  def compare(src: File, dest: File):CompareResult = {
    val srcSet = scan(src).map(f=>FileDetails(f.getAbsolutePath.substring(src.getAbsolutePath.length), f.lastModified())).toSet
    val destSet = scan(dest).map(f=>FileDetails(f.getAbsolutePath.substring(dest.getAbsolutePath.length), f.lastModified())).toSet

    compare(srcSet, destSet)
  }

  private def compare(src: Set[FileDetails], dest: Set[FileDetails]):CompareResult = {

    val srcPaths = src.map(_.path)
    val destPaths = dest.map(_.path)

    val newSrcFiles=src.filterNot(x=> destPaths.contains(x.path)).toList.sortWith(fileDetailsSorter)
    val newDestFiles=dest.filterNot(x=> srcPaths.contains(x.path)).toList.sortWith(fileDetailsSorter)

    val changedSrcFiles=src.filter(x=> destPaths.contains(x.path)).toList.sortWith(fileDetailsSorter)
    val changedDestFiles=dest.filter(x=> srcPaths.contains(x.path)).toList.sortWith(fileDetailsSorter)

    val changedFiles = changedSrcFiles.zip(changedDestFiles) flatMap {
      x=>
        if(x._1.timestamp!=x._2.timestamp) Some(UpdatedFileDetails(x._1.path, x._1.timestamp, x._2.timestamp)) else None
    }

    CompareResult(newSrcFiles, newDestFiles, changedFiles.toSet)
  }


  private def scan(file: File): List[File] = {
    @scala.annotation.tailrec
    def sc(acc: List[File], files: List[File]): List[File] = {
      files match {
        case Nil => acc
        case x :: xs =>
          x.isDirectory match {
            case false => sc(x :: acc, xs)
            case true => sc(acc, xs ::: x.listFiles.toList)
          }
      }
    }

    sc(List(), List(file))
  }
}