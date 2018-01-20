package model

import spray.json._

import scala.collection.JavaConverters._

/**
  * Created by nuno on 03-07-2017.
  */
case class ClusterOn(name: String)

case class ClusterState(name: String)

case class ClusterInfo(name: String)

case class GetawayRequest(url: String)

abstract case class MarathonRequest() {
  def id: String

  def cmd: String

  def cpus: Int

  def ram: Int

  def disk: Int

  def instances: Int

  def acceptedResourceRoles: List[String]

  def env: Map[String, String]
}

class NativeMarathonRequest(id: String,
                                 cmd: String,
                                 cpus: Int,
                                 ram: Int,
                                 disk: Int,
                                 instances: Int,
                                 acceptedResourceRoles: List[String] = List("*"),
                                 portDefinitions: PortDefinitions,
                                 env: Map[String, String] = Map()
                                ) 

class DockerMarathonRequest(
                                  id: String,
                                  cmd: String,
                                  cpus: Int,
                                  ram: Int,
                                  disk: Int,
                                  instances: Int,
                                  acceptedResourceRoles: List[String] = List("*"),
                                  ports: List[Int],
                                  env: Map[String, String] = Map(),
                                  container: Container
                                )


class ClusterStateResponse(
                            hostname: String,
                            cluster: String,
                            slaves: java.util.ArrayList[ClusterSlave],
                            frameworks: java.util.ArrayList[Frameworks]
                          ) {

  def _hostname = hostname

  def _cluster = cluster

  def _slaves = slaves

  def _frameworks = frameworks


  override def toString: String = {
    s"${this.slaves}"

  }

  def clusterState(): Message = {

    val msgBuffer = StringBuilder.newBuilder

    msgBuffer.append(s"The cluster $cluster is running")

    val slavesSize = slaves.size()

    val slavesMsg: String = slavesSize match {
      case 0 => "but it is sad, it doesn't have any friends."
      case 1 => "with one agent"
      case e => s"have $e agents"
    }

    val frameworkMsg: String = frameworks.size() match {
      case 0 => if (slavesSize > 0) "and they are hungry for applications." else "."
      case 1 => "there are 1 framework running."
      case e => s"there are $e frameworks running."
    }

    msgBuffer append " " + slavesMsg
    msgBuffer append " " + frameworkMsg

    Message(msgBuffer.mkString)
  }

  def clusterFrameworksNames(): Message = {

    frameworks.size() match {
      case 0 => Message("Currently there is none")
      case 1 => Message(s"The cluster is running ${frameworks.get(0)._name} framework.")
      case 2 => {
        val framework = frameworks.asScala.toList

        val msgBuffer = StringBuilder.newBuilder

        def loop(msg: StringBuilder, arr: List[Frameworks]): StringBuilder = arr match {
          case Nil => msg.append(".")
          case x :: xs => if (arr.length > 1) {
            if (xs.length == 1) {
              loop(msg.append(x._name + " and "), xs)
            } else {
              loop(msg.append(x._name + ","), xs)
            }
          } else loop(msg.append(x._name), xs)
        }

        val msg =
          s"""
             |The cluster is running frameworks with the following names ${loop(msgBuffer, framework)}
       """.stripMargin

        Message(msg)
      }
    }
  }
}

case class Message(msg: String) {

}


class ClusterSlave(
                    id: String = "",
                    hostname: String = "",
                    port: Int,
                    attributes: JsObject,
                    pid: String = "",
                    registered_time: String,
                    reregistered_time: String,
                    resources: Resources,
                    used_resources: Resources,
                    offered_resources: Resources,
                    unreserved_resources: Resources,
                    active: Boolean,
                    version: String = "",
                    capabilities: java.util.ArrayList[String],
                    TASK_STAGING: Int,
                    TASK_STARTING: Int,
                    TASK_RUNNING: Int,
                    TASK_KILLING: Int,
                    TASK_FINISHED: Int,
                    TASK_KILLED: Int,
                    TASK_FAILED: Int,
                    TASK_LOST: Int,
                    TASK_ERROR: Int,
                    TASK_UNREACHABLE: Int,
                    framework_ids: java.util.ArrayList[String]
                  )

class Resources(
                 disk: Float = 0.0f,
                 mem: Float = 0.0f,
                 gpus: Float = 0.0f,
                 cpus: Float = 0.0f,
                 ports: String = ""
               )

class Frameworks(
                  id: String = "",
                  name: String = "",
                  pid: String = "",
                  used_resources: Resources,
                  offered_resources: Resources,
                  capabilities: List[String],
                  hostname: String = "",
                  weburi: String = "",
                  active: Boolean,
                  connected: Boolean,
                  recovered: Boolean,
                  TASK_STAGING: Int,
                  TASK_STARTING: Int,
                  TASK_RUNNING: Int,
                  TASK_KILLING: Int,
                  TASK_FINISHED: Int,
                  TASK_KILLED: Int,
                  TASK_FAILED: Int,
                  TASK_LOST: Int,
                  TASK_ERROR: Int,
                  TASK_UNREACHABLE: Int,
                  slave_ids: List[String]
                ) {
  def _name = name
}

case class APIAiResponse(speech: String,
                         display: String,
                         data: JsObject,
                         contextOut: List[String],
                         source: String
                        )


case class Container(`type`: String, docker: Docker)

case class Labels()

case class PortDefinitions(port: Int, protocol: String, labels: Labels)

case class Docker(image: String, network: String)


