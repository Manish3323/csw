package csw.location.client.utils
import java.net.ServerSocket

import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class LocationServerStatusTest extends FunSuite with Matchers with BeforeAndAfterAll {
  val serverPort = 7654
  val serverSocket = new ServerSocket(serverPort)

  override protected def afterAll(): Unit = serverSocket.close()

  test("should return true when server is running on given host and port") {
    noException shouldBe thrownBy(LocationServerStatus.requireUp("localhost"))
    noException shouldBe thrownBy(LocationServerStatus.requireUpLocally())
  }

  test("should throw exception when server is not running on given host and port") {
    a[IllegalArgumentException] shouldBe thrownBy(LocationServerStatus.requireUp("invalid-ip"))
  }

}
