
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import model.{APIAiResponse, ElCapitan, RequestFactory}
import spray.json._
import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.io.StdIn

/**
  * Created by nuno on 02-07-2017.
  */

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat5(APIAiResponse)
}


class Server(port:Int,hostname:String) extends JsonSupport {

  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end
  implicit val executionContext = system.dispatcher

  val password: Array[Char] = "".toCharArray // do not store passwords in code, read them from somewhere safe!

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("mazikeen.ml.p12")

  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)

  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

  val cacheActor : ActorRef = system.actorOf(Props(new ElCapitan()))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(id) if p.verify("p4ssw0rd") => Some(id)
      case _ => None
    }

  val html =
    """
      |<iframe
      |    width="350"
      |    height="430"
      |    src="https://console.api.ai/api-client/demo/embedded/2d0a1c2a-fafa-42fb-8930-84381962674d">
      |</iframe>
    """.stripMargin

  val response =
    """
      |{
      |"speech": "Barack Hussein Obama II was the 44th and current President of the United States.",
      |"displayText": "Barack Hussein Obama II was the 44th and current President of the United States, and the first African American to hold the office. Born in Honolulu, Hawaii, Obama is a graduate of Columbia University   and Harvard Law School, where ",
      |"data": {},
      |"contextOut": [],
      |"source": "DuckDuckGo"
      |}
    """.stripMargin


  val ree =
    """
      |{"hostname":"192.168.1.6","cluster":"TheJournalist","slaves":[{"id":"2f5fc6d8-0bc7-4421-b99a-a02de079338e-S2","hostname":"192.168.1.6","port":5051,"attributes":{},"pid":"slave(1)@192.168.1.6:5051","registered_time":1499101661.24975,"reregistered_time":1499101661.24995,"resources":{"disk":95381.0,"mem":10942.0,"gpus":0.0,"cpus":8.0,"ports":"[31000-32000]"},"used_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"offered_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"reserved_resources":{},"unreserved_resources":{"disk":95381.0,"mem":10942.0,"gpus":0.0,"cpus":8.0,"ports":"[31000-32000]"},"active":true,"version":"1.3.0","capabilities":["MULTI_ROLE"],"TASK_STAGING":0,"TASK_STARTING":0,"TASK_RUNNING":0,"TASK_KILLING":0,"TASK_FINISHED":1,"TASK_KILLED":0,"TASK_FAILED":0,"TASK_LOST":0,"TASK_ERROR":0,"TASK_UNREACHABLE":0,"framework_ids":["2f5fc6d8-0bc7-4421-b99a-a02de079338e-0002"]}],"frameworks":[{"id":"6d88fe3e-18e3-4835-aa38-d3746df383b4-0000","name":"marathon","pid":"scheduler-5429a4b4-f0e5-4512-8bdd-22e10053dd1c@127.0.1.1:40561","used_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"offered_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"capabilities":["PARTITION_AWARE"],"hostname":"192.168.1.6","webui_url":"http:\/\/192.168.1.6:8080","active":false,"connected":false,"recovered":false,"TASK_STAGING":0,"TASK_STARTING":0,"TASK_RUNNING":0,"TASK_KILLING":0,"TASK_FINISHED":0,"TASK_KILLED":0,"TASK_FAILED":0,"TASK_LOST":0,"TASK_ERROR":0,"TASK_UNREACHABLE":0,"slave_ids":[]},{"id":"2f5fc6d8-0bc7-4421-b99a-a02de079338e-0002","name":"Getting Stated with Spark and Cassandra Notebook","used_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"offered_resources":{"disk":0.0,"mem":0.0,"gpus":0.0,"cpus":0.0},"capabilities":[],"hostname":"nuno","webui_url":"http:\/\/192.168.1.6:4040","active":false,"connected":false,"recovered":true,"TASK_STAGING":0,"TASK_STARTING":0,"TASK_RUNNING":0,"TASK_KILLING":0,"TASK_FINISHED":1,"TASK_KILLED":0,"TASK_FAILED":0,"TASK_LOST":0,"TASK_ERROR":0,"TASK_UNREACHABLE":0,"slave_ids":["2f5fc6d8-0bc7-4421-b99a-a02de079338e-S2"]}]}
    """.stripMargin


  implicit val um: Unmarshaller[HttpEntity, JsValue] = {
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      data.utf8String.parseJson
    }
  }

  val route =
    path("") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
      } ~
        post {
          entity(as[JsValue]) { request =>
            complete {
              RequestFactory(request)(cacheActor).execute.map(ai => {
                ai
              })
            }
          }
        }
    }

  Http().setDefaultServerHttpContext(https)
  //connectionContext = https
  val bindingFuture = Http().bindAndHandle(route, hostname, port)
  println(s"Server online at https://$hostname:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}


object Server {

  def main(args: Array[String]): Unit = {
    new Server(args(0).toInt,args(1))
  }
}
