package csw.services.integtration.tests

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import csw.services.integtration.apps.TromboneHCD
import csw.services.integtration.apps.TromboneHCD.Unregister
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.exceptions.OtherLocationIsRegistered
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest._

class LocationServiceIntegrationTest
    extends TestKit(ActorSystem("location-testkit"))
    with ImplicitSender
    with FunSuiteLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  val locationService = LocationServiceFactory.make()

  override protected def afterAll(): Unit = {
    locationService.shutdown()
    system.terminate()
  }

  test("should not allow duplicate akka registration") {
    val tromboneHcdActorRef = system.actorOf(Props[TromboneHCD], "trombone-hcd")
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)

    val registration = AkkaRegistration(connection, tromboneHcdActorRef)
    Thread.sleep(4000)
    intercept[OtherLocationIsRegistered]{
      locationService.register(registration).await
    }
  }

  test("should able to resolve and communicate with remote HCD started on another container") {
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val hcdLocation = locationService.resolve(connection).await.get

    hcdLocation shouldBe a[AkkaLocation]
    hcdLocation.connection shouldBe connection

    val hcdAkkaLocation = hcdLocation.asInstanceOf[AkkaLocation]

    hcdAkkaLocation.actorRef ! Unregister

    Thread.sleep(3000)

    locationService.list.await should have size 1
  }

  test("list all components"){
    val listOfLocations = locationService.list.await

    listOfLocations should have size 1
  }

  test("should able to resolve remote Service started on another container") {

    val componentId = ComponentId("redisservice", ComponentType.Service)
    val connection = HttpConnection(componentId)

    val hcdLocation = locationService.resolve(connection).await.get

    hcdLocation shouldBe a[HttpLocation]
    hcdLocation.connection shouldBe connection
  }
}
