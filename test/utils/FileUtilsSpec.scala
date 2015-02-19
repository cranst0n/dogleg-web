package utils

import scala.collection.JavaConversions._

import java.io.File

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import com.google.common.base.Charsets
import com.google.common.io.Files

import utils.FileUtils._

object FileUtilsSpec extends Specification with ThrownMessages {

  "FileUtils" should {

    "write to a file" in {
      val tmpFile = File.createTempFile("utils", "WriteTest")
      writeToFile(tmpFile.getPath, testWriteLines.mkString("\n"))
      val actualLines = Files.readLines(tmpFile, Charsets.UTF_8).toList
      tmpFile.delete

      actualLines must have size (2)
      actualLines must be equalTo(testWriteLines)
    }

    "append to a file" in {

      val tmpFile = File.createTempFile("utils", "WriteTest")

      writeToFile(tmpFile.getPath, testWriteLines.mkString("\n"))
      appendToFile(tmpFile.getPath, testAppendLines.mkString("\n", "\n", ""))
      val actualLines = Files.readLines(tmpFile, Charsets.UTF_8).toList
      tmpFile.delete

      actualLines must have size (4)
      actualLines must be equalTo(testWriteLines ++ testAppendLines)
    }

    "determine file extension" in {

      val txtFile = new File("file.txt")
      val jsonFile = new File("file.json")
      val multipleDotFile = new File("a.dotted.file.wav")
      val noExtensionFile = new File("noExtension")

      fileExtension(txtFile) must beSome("txt")
      fileExtension(jsonFile) must beSome("json")
      fileExtension(multipleDotFile) must beSome("wav")
      fileExtension(noExtensionFile) must beNone
    }

    "check for extension type" in {

      val txtFile = new File("file.txt")
      val noExtensionFile = new File("noExtension")

      isFileExtension(txtFile, "txt") must beTrue
      isFileExtension(txtFile, "json") must beFalse
      isFileExtension(noExtensionFile, "noExtension") must beFalse
    }
  }

  private[this] val testWriteLines = List("1234", "5678")
  private[this] val testAppendLines = List("98765", "43210")
}