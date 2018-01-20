package model

import akka.actor.ActorRef
import akka.pattern.ask
import spray.json._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import DefaultJsonProtocol._

/**
  * Created by nuno on 03-07-2017.
  */
trait Command {
  def execute: Future[APIAiResponse]
}

case class StateCommand(clusterName: String, cacheActor: ActorRef) extends Command {

  override def execute: Future[APIAiResponse] = {
    val toSendBack = cacheActor.ask(ClusterState(clusterName))(5 seconds).mapTo[Message]

    toSendBack.map(message => {
      import spray.json._
      APIAiResponse(message.msg, message.msg, JsObject(), List(), "")
    })
  }
}

case class ClusterInfoCommand(clusterName: String, cacheActor: ActorRef) extends Command {
  override def execute: Future[APIAiResponse] = {
    val toSendBack = cacheActor.ask(ClusterInfo(clusterName))(5 seconds).mapTo[Message]

    toSendBack.map(message => {
      import spray.json._
      APIAiResponse(message.msg, message.msg, JsObject(), List(), "")
    })
  }
}

case class TurnOnClusterCommand() extends Command {
  override def execute: Future[APIAiResponse] = ???
}

case class LaunchCommand() extends Command {
  override def execute: Future[APIAiResponse] = ???
}

case class NodeCommand(clusterName: String, cacheActor: ActorRef) extends Command {
  override def execute: Future[APIAiResponse] = {
    cacheActor ! ClusterOn(clusterName)

    Future {
      APIAiResponse("In moments your cluster is on.",
        "In moments your cluster is on.",
        JsObject(),
        List(),
        "")
    }
  }
}

case class ApplicationInfoCommand() extends Command {
  override def execute: Future[APIAiResponse] = ???
}

case object UnknownCommand extends Command {
  override def execute: Future[APIAiResponse] = {
    Future {
      APIAiResponse("Sorry, I cannot understand you command!",
        "Sorry, I cannot understand your command!",
        JsObject(),
        List(),
        "")
    }
  }
}

object RequestFactory {
  def apply(intent: JsValue)(implicit cacheActor: ActorRef): Command = {

    val action = intent match {
      case JsObject(results) => {
        results("result").asJsObject.fields("action").convertTo[String]
      }
      case _ => "none"
    }

    action toLowerCase match {
      case "input.cluster.ramp" => {
        NodeCommand("", cacheActor)
      }
      case "add" => NodeCommand("", cacheActor)
      case "input.cluster.state" => {

        val requestClusterName = intent match {
          case JsObject(results) => {
            results("result").asJsObject.fields("parameters").asJsObject.fields("given-name").convertTo[String]
          }
          case _ => "none"
        }

        StateCommand(requestClusterName, cacheActor)
      }
      case "input.cluster.info" => {
        val requestClusterName = intent match {
          case JsObject(results) => {
            results("result").asJsObject.fields("parameters").asJsObject.fields("given-name").convertTo[String]
          }
          case _ => "none"
        }
        ClusterInfoCommand(requestClusterName, cacheActor)
      }
      case "input.cluster.application" => {

        LaunchCommand()
      }
      case "info" => ApplicationInfoCommand()
      case _ => UnknownCommand
    }
  }
}
