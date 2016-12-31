package uk.co.fibratus.dte

import java.io.File
import java.nio.file.{Files, LinkOption}
import java.nio.file.attribute.FileTime
import java.util.Date

/**
  * Created by rjs on 05/11/2016.
  */

case class FileDetails(path: String, modTimestamp: Long, crTimestamp: Long)

case class UpdatedFileDetails(path: String, srcTimestamp: Long, destTimestamp: Long)

case class CompareResult(newSrc: Seq[FileDetails], newDest: Seq[FileDetails], updated: Set[UpdatedFileDetails]) {
  override def toString: String = {
    val sb = new StringBuilder()
    sb ++= "New src:\n"
    newSrc.foreach(x=>sb.append(s"\t${x.path}, ${new Date(x.modTimestamp)}\n"))
    sb ++= "\nNew dest:\n"
    newDest.foreach(x=>sb.append(s"\t${x.path}, ${new Date(x.modTimestamp)}\n"))
    sb ++= "\nUpdated:\n"
    updated.map(x=>sb.append(s"\t${x.path}, ${new Date(x.srcTimestamp)}, ${new Date(x.destTimestamp)}\n"))
    sb.toString()
  }
}

object Compare {

  private def fileDetailsSorter(x: FileDetails, y:FileDetails) = x.path < y.path

  def compare(src: File, dest: File):CompareResult = {
    val srcSet = scan(src).map(
      f=>FileDetails(
        f.getAbsolutePath.substring(src.getAbsolutePath.length),
        f.lastModified(),
        Files.getAttribute(f.toPath, "basic:creationTime", LinkOption.NOFOLLOW_LINKS).asInstanceOf[FileTime].toMillis
      )
    ).toSet
    val destSet = scan(dest).map(
      f=>FileDetails(
        f.getAbsolutePath.substring(dest.getAbsolutePath.length),
        f.lastModified(),
        Files.getAttribute(f.toPath, "basic:creationTime", LinkOption.NOFOLLOW_LINKS).asInstanceOf[FileTime].toMillis
      )
    ).toSet

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
        if(x._1.modTimestamp!=x._2.modTimestamp) Some(UpdatedFileDetails(x._1.path, x._1.modTimestamp, x._2.modTimestamp)) else None
    }

    CompareResult(newSrcFiles, newDestFiles, changedFiles.toSet)
  }

  private def scan(file: File): List[File] = {
    @scala.annotation.tailrec
    def sc(acc: List[File], files: List[File]): List[File] = {
      files match {
        case Nil => acc
        case x :: xs =>
          if(x.isDirectory) sc(acc, xs ::: x.listFiles.toList.filterNot(f => ignoreList.contains(f.getName)))
          else sc(x :: acc, xs)
      }
    }

    sc(List(), List(file))
  }

  private val ignoreList=List(".DS_Store", ".syncstate")
}
