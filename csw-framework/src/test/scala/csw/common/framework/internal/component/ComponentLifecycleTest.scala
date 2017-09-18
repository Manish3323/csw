package csw.common.framework.internal.component

import akka.typed.PostStop
import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.framework.FrameworkTestMocks.TypedActorMock
import csw.common.framework.FrameworkTestSuite
import csw.common.framework.models.FromComponentLifecycleMessage.Running
import csw.common.framework.models.IdleMessage.Initialize
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.{ComponentMessage, FromComponentLifecycleMessage, ToComponentLifecycleMessage}
import csw.common.framework.scaladsl.ComponentHandlers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

//DEOPSCSW-177-Hooks for lifecycle management
//DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

  class RunningComponent(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {
    private val ctx = new StubbedActorContext[ComponentMessage]("test-component", 100, system)

    val sampleHcdHandler: ComponentHandlers[ComponentDomainMessage] = mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.onShutdown()).thenReturn(Future.unit)
    when(sampleHcdHandler.componentName).thenReturn("test-component")
    val behavior = new ComponentBehavior[ComponentDomainMessage](ctx, supervisorProbe.ref, sampleHcdHandler)
    with TypedActorMock[ComponentMessage]

    val runningComponentBehavior: ComponentBehavior[ComponentDomainMessage] = {
      behavior.onMessage(Initialize)
      supervisorProbe.expectMsgType[Running]
      behavior
    }
  }

  test("A running component should handle RunOffline lifecycle message") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(true)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler).onGoOffline()
    verify(sampleHcdHandler).isOnline
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should not accept RunOffline lifecycle message when it is already offline") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOffline))
    verify(sampleHcdHandler, never).onGoOffline()
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should handle RunOnline lifecycle message when it is Offline") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    val previousComponentLifecyleState = runningComponentBehavior.lifecycleState
    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler).onGoOnline()
    previousComponentLifecyleState shouldBe runningComponentBehavior.lifecycleState
  }

  test("A running component should not accept RunOnline lifecycle message when it is already Online") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._

    when(sampleHcdHandler.isOnline).thenReturn(true)

    runningComponentBehavior.onMessage(Lifecycle(ToComponentLifecycleMessage.GoOnline))
    verify(sampleHcdHandler, never).onGoOnline()
  }

  test("A running component should clean up using onShutdown handler before stopping") {
    val supervisorProbe  = TestProbe[FromComponentLifecycleMessage]
    val runningComponent = new RunningComponent(supervisorProbe)
    import runningComponent._

    runningComponentBehavior.onSignal(PostStop)
    verify(sampleHcdHandler).onShutdown()
  }
}
