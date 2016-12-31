package uk.co.fibratus.dte

import java.io.File
import java.nio.file.attribute.FileTime
import java.nio.file.spi.FileSystemProvider
import java.nio.file.{FileSystem, LinkOption, Path}
import java.util.concurrent.TimeUnit

import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

/**
  * Created by rjs on 05/11/2016.
  */
class CompareTest extends FunSuite with MockitoSugar {

  private def folder(path: String, name: String, files: List[File]) = {
    val f = mock[File]
    when(f.getAbsolutePath).thenReturn(path+name)
    when(f.isDirectory).thenReturn(true)
    when(f.listFiles).thenReturn(files.toArray)
    f
  }

  private def leafFile(path: String, name: String, lastModified: Long, created: Long) = {
    val m = new java.util.HashMap[String,AnyRef]()
    m.put("creationTime", FileTime.from(created, TimeUnit.MILLISECONDS))

    val pr = mock[FileSystemProvider]
    when(pr.readAttributes(any(), anyString(), any())).thenReturn(m)

    val fs = mock[FileSystem]
    when(fs.provider()).thenReturn(pr)

    val p = mock[Path]
    when(p.getFileSystem).thenReturn(fs)

    val f = mock[File]
    when(f.getAbsolutePath).thenReturn(path+name)
    when(f.isDirectory).thenReturn(false)
    when(f.lastModified).thenReturn(lastModified)
    when(f.getName).thenReturn(name)
    when(f.toPath).thenReturn(p)
    f
  }

  test("Compare identical directories") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List.empty, Set.empty))
  }

  test("Compare directories and ignore .DS_Store in src") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1),
          leafFile("/z/q/w/e/r/a/b/c/", ".DS_Store", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List.empty, Set.empty))
  }

  test("Compare directories and ignore .DS_Store in dest") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1),
          leafFile("/z/q/w/e/r/a/b/c/", ".DS_Store", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List.empty, Set.empty))
  }

  test("Compare directories where src has a new file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1),
          leafFile("/z/q/w/e/r/a/b/c/", "e.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List(FileDetails("/b/c/e.txt",1,1)), List.empty, Set.empty))
  }

  test("Compare directories where dest has a new file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1),
          leafFile("/a/b/c/", "e.txt", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List(FileDetails("/b/c/e.txt",1,1)), Set.empty))
  }

  test("Compare directories where src has updated file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 2, 2)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List.empty, Set(UpdatedFileDetails("/b/c/d.txt",2,1))))
  }

  test("Compare directories where dest has updated file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "d.txt", 2, 2)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List.empty, List.empty, Set(UpdatedFileDetails("/b/c/d.txt",1,2))))
  }

  test("Compare directories where both src and dest have new files") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "e.txt", 2, 2)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List(FileDetails("/b/c/d.txt",1,1)), List(FileDetails("/b/c/e.txt",2,2)), Set.empty))
  }

  test("Compare directories where both src and dest have new files and src an updated file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1),
          leafFile("/z/q/w/e/r/a/b/c/", "f.txt", 4, 4),
          leafFile("/z/q/w/e/r/a/b/c/", "g.txt", 6, 6)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "e.txt", 2, 2),
          leafFile("/a/b/c/", "f.txt", 3, 3),
          leafFile("/a/b/c/", "g.txt", 6, 6)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List(FileDetails("/b/c/d.txt",1,1)), List(FileDetails("/b/c/e.txt",2,2)), Set(UpdatedFileDetails("/b/c/f.txt",4,3))))
  }

  test("Compare directories where both src and dest have new files and dest an updated file") {
    val src = folder("/z/q/w/e/r/", "a", List(
      folder("/z/q/w/e/r/a/", "b", List(
        folder("/z/q/w/e/r/a/b/", "c", List(
          leafFile("/z/q/w/e/r/a/b/c/", "d.txt", 1, 1),
          leafFile("/z/q/w/e/r/a/b/c/", "f.txt", 3, 3),
          leafFile("/z/q/w/e/r/a/b/c/", "g.txt", 6, 6)
        ))
      ))
    ))

    val dest = folder("/", "a", List(
      folder("/a/", "b", List(
        folder("/a/b/", "c", List(
          leafFile("/a/b/c/", "e.txt", 2, 2),
          leafFile("/a/b/c/", "f.txt", 4, 4),
          leafFile("/a/b/c/", "g.txt", 6, 6)
        ))
      ))
    ))

    assert(Compare.compare(src, dest) === CompareResult(List(FileDetails("/b/c/d.txt",1,1)), List(FileDetails("/b/c/e.txt",2,2)), Set(UpdatedFileDetails("/b/c/f.txt",3,4))))
  }
}
