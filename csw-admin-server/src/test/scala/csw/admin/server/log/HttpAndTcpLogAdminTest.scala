package csw.admin.server.log

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import csw.admin.server.log.http.HttpParameter
import csw.admin.server.wiring.AdminWiring
import csw.commons.http.{ErrorMessage, ErrorResponse}
import csw.config.server.commons.{ConfigServiceConnection, TestFileUtils}
import csw.config.server.mocks.MockedAuthentication
import csw.config.server.{ServerWiring, Settings}
import csw.location.models.Connection.TcpConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.logging.client.internal._
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.prefix.{Prefix, Subsystem}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class HttpAndTcpLogAdminTest extends AdminLogTestSuite with HttpParameter with MockedAuthentication {

  private val adminWiring: AdminWiring = AdminWiring.make(Some(7888))
  import adminWiring.actorRuntime._
  import csw.admin.server.log.http.HttpCodecs._
  import csw.commons.http.codecs.ErrorCodecs._

  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  private val serverWiring  = ServerWiring.make(adminWiring.locationService, securityDirectives)
  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  private var loggingSystem: LoggingSystem = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    serverWiring.svnRepo.initSvnRepo()
    Await.result(serverWiring.httpService.registeredLazyBinding, 20.seconds)

    loggingSystem = LoggingSystemFactory.start("logging", "version", hostName, adminWiring.actorSystem)
    loggingSystem.setAppenders(List(testAppender))

    logBuffer.clear()
    Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
    // this will start seed on port 3653 and log admin server on 7888
    adminWiring.locationService
  }

  override def afterEach(): Unit = logBuffer.clear()

  override def afterAll(): Unit = {
    testFileUtils.deleteServerFiles()
    Await.result(adminWiring.actorRuntime.shutdown(), 10.seconds)
    super.afterAll()
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-160: Config(HTTP) Service can receive and handle runtime update for logging characteristics
  test("get component log meta data for http service not supported") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = 7888,
      path = s"/admin/logging/${ConfigServiceConnection.value.name}/level"
    )

    val getLogMetadataRequest  = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse = Await.result(Http()(typedSystem.toClassic).singleRequest(getLogMetadataRequest), 5.seconds)
    getLogMetadataResponse.status shouldBe StatusCodes.BadRequest
    val response = Await.result(Unmarshal(getLogMetadataResponse).to[ErrorResponse], 5.seconds)
    response shouldBe ErrorResponse(ErrorMessage(400, ConfigServiceConnection.value.toString ++ " is not supported"))
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-160: Config(HTTP) Service can receive and handle runtime update for logging characteristics
  test("set component log level for http service not supported") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = 7888,
      path = s"/admin/logging/${ConfigServiceConnection.value.name}/level",
      queryString = Some("value=debug")
    )

    val setLogLevelRequest  = HttpRequest(HttpMethods.POST, uri = getLogMetadataUri)
    val setLogLevelResponse = Await.result(Http()(typedSystem.toClassic).singleRequest(setLogLevelRequest), 5.seconds)
    setLogLevelResponse.status shouldBe StatusCodes.BadRequest
    val response = Await.result(Unmarshal(setLogLevelResponse).to[ErrorResponse], 5.seconds)
    response shouldBe ErrorResponse(ErrorMessage(400, ConfigServiceConnection.value.toString ++ " is not supported"))
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-160: Config(HTTP) Service can receive and handle runtime update for logging characteristics
  test("get component log meta data and set log level for tcp service not supported") {

    val tcpConnection = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = 7888,
      path = s"/admin/logging/${tcpConnection.name}/level"
    )

    val getLogMetadataRequest  = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse = Await.result(Http()(typedSystem.toClassic).singleRequest(getLogMetadataRequest), 5.seconds)
    getLogMetadataResponse.status shouldBe StatusCodes.BadRequest
    val response = Await.result(Unmarshal(getLogMetadataResponse).to[ErrorResponse], 5.seconds)
    response shouldBe ErrorResponse(ErrorMessage(400, tcpConnection.toString ++ " is not supported"))
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-160: Config(HTTP) Service can receive and handle runtime update for logging characteristics
  test("set component log level for tcp service not supported") {

    val tcpConnection = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = Networks().hostname,
      port = 7888,
      path = s"/admin/logging/${tcpConnection.name}/level",
      queryString = Some("value=debug")
    )

    val setLogLevelRequest  = HttpRequest(HttpMethods.POST, uri = getLogMetadataUri)
    val setLogLevelResponse = Await.result(Http()(typedSystem.toClassic).singleRequest(setLogLevelRequest), 5.seconds)
    setLogLevelResponse.status shouldBe StatusCodes.BadRequest
    val response = Await.result(Unmarshal(setLogLevelResponse).to[ErrorResponse], 5.seconds)
    response shouldBe ErrorResponse(ErrorMessage(400, tcpConnection.toString ++ " is not supported"))
  }
}
