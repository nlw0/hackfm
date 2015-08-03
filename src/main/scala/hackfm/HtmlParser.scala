package hackfm

import java.io.{PipedInputStream, PipedOutputStream}

import akka.stream.ActorMaterializer
import akka.stream.io.OutputStreamSink
import akka.stream.scaladsl._
import akka.util.ByteString
import org.xml.sax.InputSource

import scala.language.postfixOps


object HtmlParser {
  val parser = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl().newSAXParser()
  val adapter = new scala.xml.parsing.NoBindingFactoryAdapter

  def apply(source: Source[ByteString, Any])
           (implicit context: akka.actor.ActorRefFactory,
            materializer: akka.stream.Materializer) = {
    val xmlSource = convertSource(source)
    adapter.loadXML(xmlSource, parser)
  }

  private def convertSource(source: Source[ByteString, Any])
                           (implicit context: akka.actor.ActorRefFactory,
                            materializer: akka.stream.Materializer): InputSource = {
    val pipedIn = new PipedInputStream()
    val pipedOut = new PipedOutputStream(pipedIn)
    source.to(OutputStreamSink(() => pipedOut)).run()
    xml.Source.fromInputStream(pipedIn)
  }
}
