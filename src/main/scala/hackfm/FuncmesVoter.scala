package hackfm

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

class FuncmesVoter extends Actor with ActorLogging {

  import FuncmesVoter._

  implicit val materializer = ActorMaterializer()

  import context.{dispatcher, system}

  def receive = castingVotes()

  def castingVotes(): Receive = {
    case VotesToCast(uid, 0, req) =>
      log.info(s"NO MORE VOTES TO CAST")
      req ! FinishedCastingVotes

    case VotesToCast(uid, n, req) =>
      requestVote(uid) pipeTo self
      context.become(processHtml(uid, n, req))

    case akka.actor.Status.Failure(ee) =>
      log.error(ee, "WTF error received on voter but should be in another state")

    case x =>
      println("WTF????")
      println(x)

  }

  def processHtml(uid: Int, n: Int, req: ActorRef): Receive = {
    case response: HttpResponse =>
      val qq = response.status
      val success = response.status == StatusCode.int2StatusCode(302)

      if (success) log.info("VOTE REQUEST SUCCESSFUL")
      else log.warning("VOTE REQUEST UNSUCCESSFUL")

      req ! (if (success) VoteAck(uid, n) else VoteAck(uid, n, status = false))
      self ! VotesToCast(uid, n - 1, req)
      context.become(castingVotes())

    case akka.actor.Status.Failure(ee) =>
      log.error(ee, "VOTE REQUEST FAILED MISERABLY")
      req ! VoteAck(uid, n, status = false)
      self ! VotesToCast(uid, n - 1, req)
      context.become(castingVotes())

    case x =>
      println("WTF???????")
      println(x)

  }

  def requestVote(uid: Int) = {
    val headers = List(Cookie(List(
      HttpCookiePair("sessionid", "s2rizhb5mxthkd7mhegsmjkjj9vjcnyj"),
      HttpCookiePair("csrftoken", "dyCS1Kat7CxJ8w00ANDWutvin7V2dNDm")
    )))

    val formDataRaw = FormData(Map(
      "csrfmiddlewaretoken" -> "dyCS1Kat7CxJ8w00ANDWutvin7V2dNDm",
      "vote" -> s"$uid"
    ))

    val connectionFlow = Http().outgoingConnection("funcmes.herokuapp.com")

    for {
      formData <- Marshal(formDataRaw).to[RequestEntity]
      request = HttpRequest(uri = "/vote/", headers = headers, method = HttpMethods.POST, entity = formData)
      response <- Source.single(request) via connectionFlow runWith Sink.head
    } yield response
  }
}

object FuncmesVoter {

  case class VotesToCast(uid: Int, n: Int, requester: ActorRef)

  case class VoteAck(uid: Int, n: Int, status: Boolean = true)

  case object FinishedCastingVotes

}
