package util

import com.typesafe.config.{Config, ConfigFactory}
import model.MarathonRequest

import scala.collection.{immutable, mutable}

/**
  * Created by nuno on 07-07-2017.
  */
object AppConf {

  val conf: Config = ConfigFactory.load()

  lazy val clusterEndpoints: mutable.HashMap[String, mutable.Map[String, String]] = loadClusterEndpoints()

  lazy val clusterNodes : mutable.HashMap[String,mutable.Map[String, String]] = loadClusterNodes()


  lazy val clusterApplications : mutable.HashMap[String,MarathonRequest] = loadMarathonApplications()

  def loadMarathonApplications() : mutable.HashMap[String,MarathonRequest] = {

    val clusterApplications = new mutable.HashMap[String, MarathonRequest]()

    val apps = conf.getObjectList("athena.cluster.applications")

    for(i <- 0 until apps.size()){
      val obj = apps.get(i).toConfig

      val id = obj.getString("id")
      val cmd = obj.getString("cmd")
      val cpus = obj.getInt("cpus")
      val ram = obj.getInt("ram")
      val env = obj.getObject("env")
      val portDef = obj.getObject("portDefinitions")


    }


    clusterApplications
  }


  def loadClusterEndpoints(): mutable.HashMap[String, mutable.Map[String, String]] = {
    val clustersList = new mutable.HashMap[String, mutable.Map[String, String]]()

    val clusters = conf.getObjectList("athena.cluster")

    for (i <- 0 until clusters.size()) {
      val cluster = clusters.get(i).toConfig
      val name = cluster.getString("name")
      val commandsInfo = cluster.getObjectList("urls")

      val tmpMap = new mutable.HashMap[String, String]()

      for (j <- 0 until commandsInfo.size()) {
        val command = commandsInfo.get(j).toConfig
        tmpMap ++= Map(command.getString("command") -> command.getString("url"))
      }

      clustersList ++= Map(name -> tmpMap)
    }
    clustersList
  }

  def loadClusterNodes(): mutable.HashMap[String, mutable.Map[String, String]] = {
    val clustersList = new mutable.HashMap[String, mutable.Map[String, String]]()

    val clusters = conf.getObjectList("athena.cluster")

    for (i <- 0 until clusters.size()) {
      val cluster = clusters.get(i).toConfig

      val commandsOnOffInfo = cluster.getObjectList("node")
      val tmpMap = new mutable.HashMap[String, String]()
      val tmpMap1 = new mutable.HashMap[String, String]()

      for (j <- 0 until commandsOnOffInfo.size()) {
        val command = commandsOnOffInfo.get(j).toConfig
        tmpMap1 ++= Map(command.getString("hostname") -> command.getString("cmd"))
      }
      clustersList ++= Map("nodes" -> tmpMap1)
    }
    clustersList
  }
}
