package csw.services.alarm.client.internal

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
import csw.services.alarm.api.exceptions.{
  InactiveAlarmException,
  InvalidSeverityException,
  KeyNotFoundException,
  ResetOperationNotAllowed
}
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.{Disconnected, Okay}
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmSubscription}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.configparser.ConfigParser
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.concurrent.Future

class AlarmServiceImpl(
    redisConnectionsFactory: RedisConnectionsFactory,
    shelveTimeoutActorFactory: ShelveTimeoutActorFactory,
    settings: Settings
)(implicit actorSystem: ActorSystem)
    extends AlarmAdminService {

  import redisConnectionsFactory._
  import settings._
  implicit val mat: Materializer = ActorMaterializer()

  private val log = AlarmServiceLogger.getLogger

  private lazy val metadataApiF = wrappedAsyncConnection(MetadataCodec)
  private lazy val severityApiF = wrappedAsyncConnection(SeverityCodec)
  private lazy val statusApiF   = wrappedAsyncConnection(StatusCodec)

  private lazy val shelveTimeoutRef = shelveTimeoutActorFactory.make(key ⇒ unShelve(key, cancelShelveTimeout = false))

  override def initAlarms(inputConfig: Config, reset: Boolean): Future[Unit] = async {
    log.debug(s"Initializing alarm store with reset [$reset]")
    val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)
    if (reset) await(resetAlarmStore())
    await(setAlarmStore(alarmMetadataSet))
  }

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    log.debug(s"Setting severity [${severity.name}] for alarm [${key.value}] with expire timeout [$ttlInSeconds] seconds")

    // get alarm metadata
    val alarm = await(getMetadata(key))

    // validate if the provided severity is supported by this alarm
    if (!alarm.allSupportedSeverities.contains(severity))
      logAndThrow(InvalidSeverityException(key, alarm.allSupportedSeverities, severity))

    // get the current severity of the alarm
    val severityApi      = await(severityApiF)
    val previousSeverity = await(severityApi.get(key)).getOrElse(Disconnected)

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    log.info(s"Updating current severity [${severity.name}] in alarm store")
    await(severityApi.setex(key, ttlInSeconds, severity))

    // get alarm status
    val status    = await(getStatus(key))
    var newStatus = status

    def shouldUpdateLatchStatus: Boolean                     = alarm.isLatchable && severity.latchable
    def shouldUpdateLatchedSeverityWhenLatchable: Boolean    = shouldUpdateWhenLatched || shouldUpdateWhenUnLatched
    def shouldUpdateWhenLatched: Boolean                     = alarm.isLatchable && severity.latchable && severity > status.latchedSeverity
    def shouldUpdateWhenUnLatched: Boolean                   = alarm.isLatchable && status.latchStatus == UnLatched && severity.latchable
    def shouldUpdateLatchedSeverityWhenNotLatchable: Boolean = !alarm.isLatchable && severity != previousSeverity

    if (shouldUpdateLatchStatus) newStatus = newStatus.copy(latchStatus = Latched)

    if (shouldUpdateLatchedSeverityWhenLatchable || shouldUpdateLatchedSeverityWhenNotLatchable)
      newStatus = newStatus.copy(latchedSeverity = severity, alarmTime = Some(AlarmTime()))

    // derive acknowledgement status
    if (newStatus.latchedSeverity == Okay || alarm.isAutoAcknowledgeable)
      newStatus = newStatus.copy(acknowledgementStatus = Acknowledged)
    else if (severity != previousSeverity) newStatus = newStatus.copy(acknowledgementStatus = UnAcknowledged)

    // update alarm status (with recent time) only when severity changes
    if (newStatus != status) await(setStatus(key, newStatus))
  }

  override def getCurrentSeverity(key: AlarmKey): Future[AlarmSeverity] = async {
    log.debug(s"Getting severity for alarm [${key.value}]")
    val metadataApi = await(metadataApiF)
    val severityApi = await(severityApiF)

    if (await(metadataApi.exists(key))) await(severityApi.get(key)).getOrElse(Disconnected)
    else logAndThrow(KeyNotFoundException(key))
  }

  override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    val statusApi = await(statusApiF)

    log.debug(s"Getting status for alarm [${key.value}]")
    await(statusApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = async {
    log.debug(s"Getting metadata for alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  override def getMetadata(key: Key): Future[List[AlarmMetadata]] = async {
    log.debug(s"Getting metadata for alarms matching [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))
    await(metadataApi.mget(metadataKeys)).map(_.getValue)
  }

  override def acknowledge(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Acknowledge alarm [${key.value}]")
    val metadataApi = await(metadataApiF)
    val statusApi   = await(statusApiF)

    if (await(metadataApi.exists(key))) {
      val status = await(statusApi.get(key)).getOrElse(AlarmStatus())

      if (status.acknowledgementStatus == UnAcknowledged) // save the set call if status is already Acknowledged
        await(statusApi.set(key, status.copy(acknowledgementStatus = Acknowledged)))
    } else logAndThrow(KeyNotFoundException(key))
  }

  // reset is only called when severity is `Okay`
  override def reset(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Reset alarm [${key.value}]")
    val metadataApi   = await(metadataApiF)
    val statusApi     = await(statusApiF)
    val maybeMetadata = await(metadataApi.get(key))

    maybeMetadata match {
      case Some(_) ⇒
        val currentSeverity = await(getCurrentSeverity(key))
        if (currentSeverity != Okay) logAndThrow(ResetOperationNotAllowed(key, currentSeverity))

        val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
        val resetStatus = status.copy(
          acknowledgementStatus = Acknowledged,
          latchStatus = if (maybeMetadata.get.isLatchable) Latched else UnLatched,
          latchedSeverity = Okay,
          alarmTime = alarmTime(status)
        )
        if (status != resetStatus) await(statusApi.set(key, resetStatus))

      case None ⇒ logAndThrow(KeyNotFoundException(key))
    }
  }

  override def shelve(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Shelve alarm [${key.value}]")
    val statusApi = await(statusApiF)

    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
    if (status.shelveStatus != Shelved) {
      await(statusApi.set(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  override def unShelve(key: AlarmKey): Future[Unit] = unShelve(key, cancelShelveTimeout = true)

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    log.debug(s"Un-shelve alarm [${key.value}]")
    val statusApi = await(statusApiF)

    //TODO: decide whether to  unshelve an alarm when it goes to okay
    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
    if (status.shelveStatus != UnShelved) {
      await(statusApi.set(key, status.copy(shelveStatus = UnShelved)))
      // if in case of manual un-shelve operation, cancel the scheduled timer for this alarm
      // this method is also called when scheduled timer for shelving of an alarm goes off (i.e. default 8 AM local time) with
      // cancelShelveTimeout as false
      // so, at this time `CancelShelveTimeout` should not be sent to `shelveTimeoutRef` as it is already cancelled
      if (cancelShelveTimeout) shelveTimeoutRef ! CancelShelveTimeout(key)
    }
  }

  override def activate(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Activate alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (!metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Active)))
  }

  override def deActivate(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Deactivate alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Inactive)))
  }

  override def getAggregatedSeverity(key: Key): Future[AlarmSeverity] = async {
    log.debug(s"Get aggregated severity for alarm [${key.value}]")
    val metadataApi = await(metadataApiF)
    val severityApi = await(severityApiF)

    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))

    val metadataList = await(metadataApi.mget(metadataKeys)).filter(_.getValue.isActive)
    if (metadataList.isEmpty) logAndThrow(InactiveAlarmException(key))

    val severityKeys   = metadataKeys.map(SeverityKey.fromMetadataKey)
    val severityValues = await(severityApi.mget(severityKeys))
    val severityList = severityValues.collect {
      case kv if kv.hasValue => kv.getValue
      case _                 => Disconnected
    }

    severityList.reduceRight((previous, current: AlarmSeverity) ⇒ previous max current)
  }

  override def getAggregatedHealth(key: Key): Future[AlarmHealth] = {
    log.debug(s"Get aggregated health for alarm [${key.value}]")
    getAggregatedSeverity(key).map(AlarmHealth.fromSeverity)
  }

  override def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key)
      .to(Sink.foreach(callback))
      .run()
  }

  override def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key)
      .map(AlarmHealth.fromSeverity)
      .to(Sink.foreach(callback))
      .run()
  }

  override def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with an actor")
    subscribeAggregatedSeverityCallback(key, actorRef ! _)
  }

  override def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with an actor")
    subscribeAggregatedHealthCallback(key, actorRef ! _)
  }

  // PatternMessage gives three values:
  // pattern: e.g  __keyspace@0__:status.nfiraos.*.*,
  // channel: e.g. __keyspace@0__:status.nfiraos.trombone.tromboneAxisLowLimitAlarm,
  // message: event type as value: e.g. set, expire, expired
  private def subscribeAggregatedSeverity(key: Key): Source[AlarmSeverity, AlarmSubscription] = {
    val redisStreamApi     = statusApiF.flatMap(statusApi ⇒ redisKeySpaceApi(statusApi)(AlarmCodec.StatusCodec)) // create new connection for every client
    val keys: List[String] = List(StatusKey.fromAlarmKey(key).value)

    Source
      .fromFutureSource(
        redisStreamApi.map(
          _.watchKeyspaceFieldAggregation[AlarmSeverity](keys, OverflowStrategy.LATEST, _.latchedSeverity, _.maxBy(_.level))
        )
      )
      .mapMaterializedValue { mat =>
        new AlarmSubscription {
          override def unsubscribe(): Future[Unit] = mat.flatMap(_.unsubscribe())
          override def ready(): Future[Unit]       = mat.flatMap(_.ready())
        }
      }
  }

  private def setAlarmStore(alarmMetadataSet: AlarmMetadataSet) = {
    val alarms      = alarmMetadataSet.alarms
    val metadataMap = alarms.map(metadata ⇒ MetadataKey.fromAlarmKey(metadata.alarmKey) → metadata).toMap
    val statusMap   = alarms.map(metadata ⇒ StatusKey.fromAlarmKey(metadata.alarmKey) → AlarmStatus()).toMap

    log.info(s"Feeding alarm metadata in alarm store for following alarms: [${alarms.map(_.alarmKey.value).mkString("\n")}]")
    Future.sequence(
      List(
        metadataApiF.flatMap(_.mset(metadataMap)),
        statusApiF.flatMap(_.mset(statusMap))
      )
    )
  }

  private def resetAlarmStore() = {
    log.debug("Resetting alarm store")
    Future
      .sequence(
        List(
          metadataApiF.flatMap(_.pdel(GlobalKey)),
          statusApiF.flatMap(_.pdel(GlobalKey)),
          severityApiF.flatMap(_.pdel(GlobalKey))
        )
      )
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Unit] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    statusApiF.flatMap(_.set(alarmKey, alarmStatus))
  }

  private[alarm] def setMetadata(alarmKey: AlarmKey, alarmMetadata: AlarmMetadata): Future[Unit] =
    metadataApiF.flatMap(_.set(alarmKey, alarmMetadata))

  private def alarmTime(status: AlarmStatus) = if (status.latchedSeverity != Okay) Some(AlarmTime()) else status.alarmTime

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
