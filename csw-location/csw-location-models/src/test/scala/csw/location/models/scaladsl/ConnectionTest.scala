package csw.location.models.scaladsl

import csw.location.models
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models.{ComponentId, ComponentType, Connection}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConnectionTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  test("should able to form a string representation for akka connection for trombone HCD") {
    val expectedAkkaConnectionName = "tromboneHcd-hcd-akka"
    val akkaConnection = AkkaConnection(ComponentId("tromboneHcd", ComponentType.HCD))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a string representation for tcp connection for redis") {
    val expectedTcpConnectionName = "redis-service-tcp"
    val tcpConnection = TcpConnection(ComponentId("redis", ComponentType.Service))
    tcpConnection.name shouldBe expectedTcpConnectionName
  }

  test("should able to form a string representation for http connection for config service") {
    val expectedHttpConnectionName = "config-service-http"
    val httpConnection = HttpConnection(models.ComponentId("config", ComponentType.Service))
    httpConnection.name shouldBe expectedHttpConnectionName
  }

  test("should able to form a string representation for akka connection for trombone container") {
    val expectedAkkaConnectionName = "tromboneContainer-container-akka"
    val akkaConnection = AkkaConnection(models.ComponentId("tromboneContainer", ComponentType.Container))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a string representation for akka connection for trombone assembly") {
    val expectedAkkaConnectionName = "tromboneAssembly-assembly-akka"
    val akkaConnection = AkkaConnection(models.ComponentId("tromboneAssembly", ComponentType.Assembly))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a connection for components from a valid string representation") {
    Connection.from("tromboneAssembly-assembly-akka") shouldBe
    AkkaConnection(models.ComponentId("tromboneAssembly", ComponentType.Assembly))

    Connection.from("tromboneHcd-hcd-akka") shouldBe
    AkkaConnection(models.ComponentId("tromboneHcd", ComponentType.HCD))

    Connection.from("redis-service-tcp") shouldBe
    TcpConnection(models.ComponentId("redis", ComponentType.Service))

    Connection.from("configService-service-http") shouldBe
    HttpConnection(models.ComponentId("configService", ComponentType.Service))
  }

  test("should not be able to form a connection for components from an invalid string representation") {
    val connection = "tromboneAssembly_assembly_akka"
    val exception = intercept[IllegalArgumentException] {
      Connection.from(connection)
    }

    exception.getMessage shouldBe s"Unable to parse '$connection' to make Connection object"

    val connection2 = "trombone-hcd"
    val exception2 = intercept[IllegalArgumentException] {
      Connection.from(connection2)
    }

    exception2.getMessage shouldBe s"Unable to parse '$connection2' to make Connection object"
  }
}
