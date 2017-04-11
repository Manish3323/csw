package csw.services.config.api.commons

import java.io.File
import java.security.MessageDigest

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

object ShaUtils {

  def generateSHA1(file: File)(implicit mat: Materializer): Future[String] = {
    FileIO.fromPath(file.toPath).runWith(sha1Sink)
  }

  def generateSHA1(string: String)(implicit mat: Materializer): Future[String] = {
    Source.single(ByteString(string.getBytes)).runWith(sha1Sink)
  }

  //Keep this a def so that the digester is created anew each time.
  def sha1Sink: Sink[ByteString, Future[String]] = {
    val sha1Digester = MessageDigest.getInstance("SHA-1")
    Flow[ByteString]
      .fold(sha1Digester) { (digester, bs) =>
        digester.update(bs.toArray)
        digester
      }
      .mapConcat(_.digest().toList)
      .map(_ & 0xFF)
      .map("%02x" format _)
      .toMat(Sink.fold("")(_ + _))(Keep.right)
  }

}
