package utils

import scala.language.reflectiveCalls

import java.io._

trait FileUtils {

  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    } finally {
      Option(resource).map(_.close())
    }
  }

  def writeToFile(fileName: String, data: String): Unit = {
    writeToFile(new File(fileName), data)
  }

  def writeToFile(file: File, data: String): Unit = {
    using(new FileWriter(file)) {
      fileWriter => fileWriter.write(data)
    }
  }

  def writeToFile(file: File, data: Array[Byte]): Unit = {
    using(new FileOutputStream(file)) {
      fileStream => fileStream.write(data)
    }
  }

  def appendToFile(fileName: String, data: String): Unit = {
    appendToFile(new File(fileName), data)
  }

  def appendToFile(file: File, data: String): Unit = {
    using(new FileWriter(file, true)) {
      fileWriter =>
        using(new PrintWriter(fileWriter)) {
          printWriter => printWriter.println(data)
        }
    }
  }

  def isFileExtension(file: File, extension: String): Boolean = {
    isFileExtension(file.getName, extension)
  }

  def isFileExtension(filename: String, extension: String): Boolean = {
    fileExtension(filename) match {
      case Some(ext) if ext.toLowerCase == extension.toLowerCase => true
      case _ => false
    }
  }

  def fileExtension(file: File): Option[String] = {
    fileExtension(file.getName)
  }

  def fileExtension(filename: String): Option[String] = {
    filename.toLowerCase.split('.').drop(1).lastOption
  }
}

object FileUtils extends FileUtils
