package org.bigbluebutton

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.routing.directives.BasicDirectives._
import spray.routing.Directive.pimpApply
import shapeless._
import spray.json.DefaultJsonProtocol
import akka.actor.ActorRef
import spray.json.JsObject
import spray.httpx.SprayJsonSupport
import spray.json.JsValue
import akka.actor.Props
import org.bigbluebutton.apps.protocol.Header
import org.bigbluebutton.apps.protocol.HeaderAndPayload
import org.bigbluebutton.apps.protocol.HeaderAndPayload
import spray.json.JsString
import org.bigbluebutton.apps.protocol.MessageTransformer
import org.bigbluebutton.apps.protocol.HeaderAndPayload
import akka.event.LoggingAdapter
import akka.actor.ActorLogging
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._
import org.bigbluebutton.apps.protocol.CreateMeetingRequestReply
import org.bigbluebutton.apps.protocol.Ok
import scala.util.{Success, Failure}

class RestEndpointServiceActor(val msgReceiver: ActorRef) extends Actor with RestEndpointService with ActorLogging {

  def actorRefFactory = context

  def receive = runRoute(restApiRoute)
}

object HeaderAndPayloadJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {  
  implicit val headerFormat = jsonFormat4(Header)
  implicit val headerAndPayloadFormats = jsonFormat2(HeaderAndPayload)
}

trait RestEndpointService extends HttpService {
  import MessageTransformer._
  import HeaderAndPayloadJsonSupport._

  val msgReceiver: ActorRef
  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5 seconds)
  
  val supportedContentTypes = List[ContentType](`application/json`, `text/plain`)
  
  val restApiRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { 
          complete {
            <html>
              <body>
                <h1>Welcome to BigBlueButton!</h1>
              </body>
            </html>
          }
        }
      }
    } ~
    path("meeting") {
      post {
        respondWithMediaType(`application/json`) {
	        entity(as[HeaderAndPayload]) { hp =>
	          val msg = processMessage(hp.header, hp.payload.asJsObject)
	          if (msg != None) {
	            /**
	             * TODO: Use Future to handle response if meeting has been created or
	             *       if it is already running.
	             */
	            val response = (msgReceiver ? msg.get).mapTo[CreateMeetingRequestReply]
				response onComplete {
				   case Success(result) => {
				     println("RESULT: " + result)
				    // result match {
				   //    case ok:Ok => println(ok)
				    //   case 
				    // }
				   }
				   case Failure(failure) =>  println("FAIL: " + failure)
				  } 
	            complete("{OK}")
	          } else {
	            complete("{Failed to process message.}")
	          }
	          
	        }
        }
      }
    }
}

