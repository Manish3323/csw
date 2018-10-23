package csw.alarm
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{ActorSystem, typed}
import akka.stream.ActorMaterializer
import com.typesafe.config._
import csw.alarm.api.models.AlarmSeverity.Okay
import csw.alarm.api.models.Key.{AlarmKey, ComponentKey, SubsystemKey}
import csw.alarm.api.models.{AlarmHealth, AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import csw.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.alarm.client.AlarmServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.models.Subsystem.{IRIS, NFIRAOS}

import scala.async.Async._
import scala.concurrent.ExecutionContext

object AlarmServiceClientExampleApp {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  implicit val mat: ActorMaterializer   = ActorMaterializer()
  private val locationService           = HttpLocationServiceFactory.makeLocalClient

  private def behaviour[T]: Behaviors.Receive[T] = Behaviors.receive { (ctx, msg) ⇒
    println(msg)
    Behaviors.same
  }

  //#create-scala-api
  // create alarm client using host and port of alarm server
  private val clientAPI1 = new AlarmServiceFactory().makeClientApi("localhost", 5225)

  // create alarm client using location service
  private val clientAPI2 = new AlarmServiceFactory().makeClientApi(locationService)

  // create alarm admin using host and port of alarm server
  private val adminAPI1 = new AlarmServiceFactory().makeAdminApi("localhost", 5226)

  // create alarm admin using location service
  private val adminAPI2 = new AlarmServiceFactory().makeAdminApi(locationService)
  //#create-scala-api

  val clientAPI: AlarmService     = clientAPI1
  val adminAPI: AlarmAdminService = adminAPI1

  //#setSeverity-scala
  val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")

  async {
    await(clientAPI.setSeverity(alarmKey, Okay))
  }
  //#setSeverity-scala

  //#initAlarms
  async {
    val resource             = "test-alarms/valid-alarms.conf"
    val alarmsConfig: Config = ConfigFactory.parseResources(resource)
    await(adminAPI.initAlarms(alarmsConfig))
  }
  //#initAlarms

  //#acknowledge
  async {
    await(adminAPI.acknowledge(alarmKey))
  }
  //#acknowledge

  //#shelve
  async {
    await(adminAPI.shelve(alarmKey))
  }
  //#shelve

  //#unshelve
  async {
    await(adminAPI.unshelve(alarmKey))
  }
  //#unshelve

  //#reset
  async {
    await(adminAPI.reset(alarmKey))
  }
  //#reset

  //#getMetadata
  async {
    val metadata: AlarmMetadata = await(adminAPI.getMetadata(alarmKey))
  }
  //#getMetadata

  //#getStatus
  async {
    val status: AlarmStatus = await(adminAPI.getStatus(alarmKey))
  }
  //#getStatus

  //#getCurrentSeverity
  async {
    val severity: FullAlarmSeverity = await(adminAPI.getCurrentSeverity(alarmKey))
  }
  //#getCurrentSeverity

  //#getAggregatedSeverity
  async {
    val componentKey                          = ComponentKey(NFIRAOS, "tromboneAssembly")
    val aggregatedSeverity: FullAlarmSeverity = await(adminAPI.getAggregatedSeverity(componentKey))
  }
  //#getAggregatedSeverity

  //#getAggregatedHealth
  async {
    val subsystemKey        = SubsystemKey(IRIS)
    val health: AlarmHealth = await(adminAPI.getAggregatedHealth(subsystemKey))
  }
  //#getAggregatedHealth

  //#subscribeAggregatedSeverityCallback
  adminAPI.subscribeAggregatedSeverityCallback(
    ComponentKey(NFIRAOS, "tromboneAssembly"),
    aggregatedSeverity ⇒ { /* do something*/ }
  )
  //#subscribeAggregatedSeverityCallback

  //#subscribeAggregatedSeverityActorRef
  val severityActorRef = typed.ActorSystem(behaviour[FullAlarmSeverity], "fullSeverityActor")
  adminAPI.subscribeAggregatedSeverityActorRef(SubsystemKey(NFIRAOS), severityActorRef)
  //#subscribeAggregatedSeverityActorRef

  //#subscribeAggregatedHealthCallback
  adminAPI.subscribeAggregatedHealthCallback(
    ComponentKey(IRIS, "ImagerDetectorAssembly"),
    aggregatedHealth ⇒ { /* do something*/ }
  )
  //#subscribeAggregatedHealthCallback

  //#subscribeAggregatedHealthActorRef
  val healthActorRef = typed.ActorSystem(behaviour[AlarmHealth], "healthActor")
  adminAPI.subscribeAggregatedHealthActorRef(SubsystemKey(IRIS), healthActorRef)
  //#subscribeAggregatedHealthActorRef
}
