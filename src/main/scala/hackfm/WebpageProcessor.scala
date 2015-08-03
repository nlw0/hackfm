package hackfm

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.language.postfixOps
import scala.xml.Node


class WebpageProcessor extends Actor with ActorLogging {
  implicit val materializer = ActorMaterializer()

  val voter = context.actorOf(Props[FuncmesVoter], "voter")

  def receive = {
    case WebpageProcessor.WebpageSource(source, req) =>
      val html = HtmlParser(source)
      voter ! processWebpage(html, req)
  }

  def processWebpage(html: Node, req: ActorRef) = {
    val employeeRegex = """(\w*) \((\d*) pontos\)""".r
    val targetEmployee = """[Jj][Uu]""".r
    // val targetEmployee = "Nic".r
    // val targetEmployee = "Asp".r
    // val targetEmployee = "Maroja".r
    // val targetEmployee = "Edu".r

    def employees = for {
      node <- (html \\ "div").toStream
      if (node \ "@class").text == "row" && (node \\ "@type").text == "radio"
      label = node \\ "label"
      employeeRegex(name, points) <- Some(label.text.trim)
      uid <- (node \\ "@value").headOption
    } yield Employee(uid.text.toInt, name, points.toInt)

    val (dataB, dataA) = employees partition { ee: Employee =>
      val name = ee.name
      targetEmployee.findFirstIn(name).isDefined
    }

    dataB.headOption match {
      case None =>
        log.warning("NO TARGET FOUND")
        FuncmesVoter.VotesToCast(-1, 0, req)

      case Some(target) =>
        val data = (dataA ++ dataB.tail) sortBy (-_.points) toList
        val maxOthers = data.head.points
        // val maxOthers = data.drop(2).head.points // third
        // val maxOthers = data.reverse.drop(2).head.points
        // val maxOthers = data.reverse.head.points // second to last

        val error = target.points - (maxOthers + 1)
        val actionRaw = -error
        val action = clamp(0, 10)(actionRaw)

        log.info(s"target:$target max:$maxOthers votes:$action rapa:$data")

        FuncmesVoter.VotesToCast(target.uid, action, req)
    }
  }

  def clamp(lo: Int, hi: Int)(x: Int) = if (x < lo) lo else if (x > hi) hi else x
}

object WebpageProcessor {

  case class WebpageSource(value: Source[ByteString, Any], req: ActorRef)

}
