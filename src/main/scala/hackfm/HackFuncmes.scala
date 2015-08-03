package hackfm

import akka.actor.{ActorSystem, Props}
import scala.concurrent.duration._
import scala.language.postfixOps

object HackFuncmes extends App {
  val REQUEST_PERIOD = 10 seconds
  val RESET_PERIOD = 10 minutes

  implicit val system = ActorSystem()

  import system.dispatcher

  val requester = system.actorOf(Props[WebpageRequester], "requester")

  system.scheduler.schedule(0 seconds, REQUEST_PERIOD, requester, WebpageRequester.RequestWebpage)
  system.scheduler.schedule(RESET_PERIOD, RESET_PERIOD, requester, FuncmesVoter.FinishedCastingVotes)
}

