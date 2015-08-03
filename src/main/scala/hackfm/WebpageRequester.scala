package hackfm

import akka.actor.{ActorLogging, Actor, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

class WebpageRequester extends Actor with ActorLogging {
  implicit val materializer = ActorMaterializer()

  import context.{dispatcher, system}

  val headers = List(Cookie(List(
    HttpCookiePair("sessionid", "s2rizhb5mxthkd7mhegsmjkjj9vjcnyj"),
    HttpCookiePair("csrftoken", "dyCS1Kat7CxJ8w00ANDWutvin7V2dNDm")
  )))

  val pageProcessor = context.actorOf(Props[WebpageProcessor], "processor")

  def receive = awaitingCommand()

  def awaitingCommand(): Receive = {
    case WebpageRequester.RequestWebpage =>
      val request = HttpRequest(uri = "/", headers = headers)
      val connectionFlow = Http().outgoingConnection("funcmes.herokuapp.com")
      val responseFuture = Source.single(request) via connectionFlow runWith Sink.head

      responseFuture map (r => WebpageProcessor.WebpageSource(r.entity.dataBytes, self)) pipeTo pageProcessor
      context.become(awaitingVoteAcks())
  }

  def awaitingVoteAcks(): Receive = {
    case FuncmesVoter.VoteAck(uid, n, status) =>
      log.info(s"Acked vote $n to $uid (status:$status)")

    case FuncmesVoter.FinishedCastingVotes =>
      log.info(s"Awaiting next iteration")
      context.become(awaitingCommand())

    case WebpageRequester.RequestWebpage =>
      log.info("Someone seems to be looking for a request right now... not gonna happen")

  }
}


object WebpageRequester {

  case object RequestWebpage

}
