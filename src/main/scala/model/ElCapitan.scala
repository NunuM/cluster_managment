package model

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.stream.ActorMaterializer
import akka.pattern.{ask, pipe}
import akka.util.ByteString
import com.google.gson.Gson
import spray.json._
import DefaultJsonProtocol._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by nuno on 03-07-2017.
  */
class ElCapitan
  extends Actor with ActorLogging {

  val cache = new mutable.HashMap[String, Map[String, Message]]()
  val getaway: ActorRef = context.actorOf(Props(new GetawayActor()))

  implicit val dispatcher = context.dispatcher
  implicit val materializer = ActorMaterializer()

  override def preStart(): Unit = {
    super.preStart()
    requests()
  }


  override def receive: Receive = {
    case cs: ClusterState => {
      val mesgToReply = cache.get(cs.getClass.getSimpleName) match {
        case Some(e) => {
          e.get(cs.name.toLowerCase()) match {
            case Some(el) => el
            case _ => Message("Sorry, we cannot find the values to this request. Please, make sure you cluster name is right!")
          }
        }
        case _ => Message("Command not found.")
      }
      sender() ! mesgToReply
    }
    case cs: ClusterInfo => {
      val mesgToReply = cache.get(cs.getClass.getSimpleName) match {
        case Some(e) => {
          e.get(cs.name.toLowerCase()) match {
            case Some(el) => el
            case _ => Message("Sorry, we cannot find the values to this request. Please, make sure you cluster name is right!")
          }
        }
        case _ => Message("Command not found.")
      }
      sender() ! mesgToReply
    }
    case cs: ClusterOn => {
      //turnOnNodes()
    }
  }


  private def turnOnNodes(): Unit = {

    import util.AppConf._

    clusterNodes.foreach {
      case (key, map) => {
        import sys.process._
        map.foreach(_._2.!)
      }
    }
  }


  private def requests(): Unit = {

    import util.AppConf._

    clusterEndpoints.foreach {
      case (name, commandDetails) => {
        commandDetails.foreach {
          case (command, url) => {
            command match {
              case "ClusterState" => {
                val gson = new Gson()
                val convert = (jsValue: String) => gson.fromJson(jsValue, classOf[ClusterStateResponse])
                HttpRequests[ClusterStateResponse](url, convert, va => {
                  cache ++= Map(command -> Map(va._cluster.toLowerCase -> va.clusterState()))
                })
              }
              case "ClusterInfo" => {
                HttpRequests[JsValue](url, s => s.parseJson, va => {

                  val listOfValues = va.asJsObject.fields

                  val cpus = listOfValues("system/cpus_total").convertTo[Int]
                  val memFree = listOfValues("system/mem_free_bytes").convertTo[Long]
                  val memTotal = listOfValues("system/mem_total_bytes").convertTo[Long]
                  val loadLastMinute = listOfValues("system/load_5min").convertTo[Float]
                  val msg = s"The Alexander has $cpus CPUs available and ${humanReadableByteSize(memFree)} free memory in a total of an ${humanReadableByteSize(memTotal)}. In last minute the system was under a average load of ${loadLastMinute}."
                  cache ++= Map(command -> Map(name.toLowerCase -> Message(msg)))
                })
              }
              case _ => log.info("Not known command")
            }
          }
          case _ => log.info("Something Happened")
        }
      }
      case _ => log.info("Error")
    }

  }

  private def HttpRequests[T](url: String,
                              f: String => T,
                              insert: T => Unit
                             ): Unit = {

    implicit val timeout = 10 seconds
    val res: Future[HttpResponse] = getaway.ask(GetawayRequest(url))(timeout).mapTo[HttpResponse]

    res.onComplete {
      case Success(response) => {
        val strictEntity: Future[HttpEntity.Strict] = response.entity.toStrict(10.seconds)

        val status = strictEntity flatMap { e =>
          e.dataBytes
            .runFold(ByteString.empty) { case (acc, b) => acc ++ b }
            .map(_.utf8String)
            .map(f(_))
        }
        status.onComplete {
          case Success(s) => {
            insert(s)
          }
          case Failure(e) => log.error("error", e.printStackTrace())
        }
      }
      case Failure(exception) => {
        log.error("Future", exception.printStackTrace())
      }
    }
  }

  def humanReadableByteSize(fileSize: Long): String = {
    if (fileSize <= 0) return "0 B"
    // kilo, Mega, Giga, Tera, Peta, Exa, Zetta, Yotta
    val units: Array[String] = Array("B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val digitGroup: Int = (Math.log10(fileSize) / Math.log10(1024)).toInt
    f"${fileSize / Math.pow(1024, digitGroup)}%3.2f ${units(digitGroup)}"
  }

}
