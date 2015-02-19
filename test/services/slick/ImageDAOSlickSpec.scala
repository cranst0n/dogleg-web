package services.slick

import java.io.ByteArrayOutputStream

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

import com.sksamuel.scrimage.{ Image => Scrimage }

import play.api.Play
import play.api.Play.current

import models.Image

import test.Helpers._

object ImageDAOSlickSpec extends Specification with ThrownMessages {

  "ImageDAOSlick" should {

    "insert a new image" in DoglegTestApp { implicit module =>
      val dao = new ImageDAOSlick

      val insertedImage = dao.insert(SimpleImage)
      insertedImage.id must beSome(1)

      insertedImage.data must be equalTo SimpleImage.data
    }

    "find an image by ID" in DoglegTestApp { implicit module =>
      val dao = new ImageDAOSlick

      val insertedImage = dao.insert(SimpleImage)
      insertedImage.id must beSome(1)

      dao.findById(insertedImage.id.get) must beSome.which { byId =>
        byId.data must be equalTo SimpleImage.data
      }

      dao.findById(1234) must beNone
    }

    "update an image" in DoglegTestApp { implicit module =>
      val dao = new ImageDAOSlick

      val insertedImage = dao.insert(SimpleImage)
      insertedImage.id must beSome(1)

      val toUpdate = insertedImage.copy(data = AnotherImage.data)
      val updatedImage = dao.update(toUpdate)

      dao.findById(insertedImage.id.get) must beSome.which { image =>
        image.data must be equalTo AnotherImage.data
      }
    }

    "delete an image" in DoglegTestApp { implicit module =>
      val dao = new ImageDAOSlick

      val insertedImage = dao.insert(SimpleImage)
      insertedImage.id must beSome(1)

      dao.delete(insertedImage.id.get) must be equalTo 1

      dao.findById(insertedImage.id.get) must beNone
    }
  }

  lazy val SimpleImage = image("golf_cup.jpg")
  lazy val AnotherImage = image("orange_ball.jpg")

  private[this] def image(path: String) = {
    Play.getExistingFile(s"test/resources/images/$path").map { file =>
      val byteStream = new ByteArrayOutputStream(file.length.toInt)
      Scrimage(file).write(byteStream)

      Image(None, byteStream.toByteArray)
    } getOrElse fail(s"Failed to load image: $path")
  }
}
