package csw.common.framework.internal.supervisor

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.common.components.SampleComponentState._
import csw.common.components.{ComponentDomainMessage, SampleComponentHandlers}
import csw.common.framework.ComponentInfos._
import csw.common.framework.exceptions.InitializeFailureStop
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.PubSub.{Publish, PublisherMessage}
import csw.common.framework.models.SupervisorCommonMessage.GetSupervisorMode
import csw.common.framework.models._
import csw.common.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.common.framework.{FrameworkTestSuite, TestMocks}
import csw.param.states.CurrentState
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.Answer

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// DEOPSCSW-178: Lifecycle success/failure notification
class SupervisorLifecyleFailureTest extends FrameworkTestSuite {

  val supervisorModeProbe: TestProbe[SupervisorMode]             = TestProbe[SupervisorMode]
  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]
  var supervisorBehavior: Behavior[SupervisorExternalMessage]    = _
  var supervisorRef: ActorRef[SupervisorExternalMessage]         = _

  test("should restart TLA when it throws InitializeFailureStop exception") {
    val testMockData = testMocks
    import testMockData._

    val componentHandlers = createComponentHandlers(testMockData, InitializeFailureStop.apply())
    createSupervisorAndStartTLA(testMockData, componentHandlers)

    supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Idle)

    supervisorRef ! Restart

    supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
    supervisorModeProbe.expectMsg(SupervisorMode.Restart)

    verify(locationService, never()).register(akkaRegistration)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    verify(registrationResult, never()).unregister()

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))
    containerIdleMessageProbe.expectMsg(SupervisorModeChanged(supervisorRef, SupervisorMode.Running))
  }

  private def createSupervisorAndStartTLA(
      testMocks: TestMocks,
      componentHandlers: ComponentHandlers[ComponentDomainMessage]
  ): Unit = {
    import testMocks._

    val componentBehaviorFactory = mock[ComponentBehaviorFactory[ComponentDomainMessage]]
    when(
      componentBehaviorFactory.make(
        ArgumentMatchers.any[ComponentInfo],
        ArgumentMatchers.any[ActorRef[FromComponentLifecycleMessage]],
        ArgumentMatchers.any[ActorRef[PublisherMessage[CurrentState]]]
      )
    ).thenCallRealMethod()

    when(
      componentBehaviorFactory.handlers(
        ArgumentMatchers.any[ActorContext[ComponentMessage]],
        ArgumentMatchers.any[ComponentInfo],
        ArgumentMatchers.any[ActorRef[PublisherMessage[CurrentState]]]
      )
    ).thenReturn(componentHandlers)

    supervisorBehavior = Actor
      .withTimers[SupervisorMessage](
        timerScheduler ⇒
          Actor
            .mutable[SupervisorMessage](
              ctx =>
                new SupervisorBehavior(
                  ctx,
                  timerScheduler,
                  Some(containerIdleMessageProbe.ref),
                  hcdInfo,
                  componentBehaviorFactory,
                  pubSubBehaviorFactory,
                  registrationFactory,
                  locationService
              )
          )
      )
      .narrow

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "comp-supervisor"), 5.seconds)
  }

  private def createComponentHandlers(testMocks: TestMocks, initializeFailureException: RuntimeException) = {
    import testMocks._

    val initializeAnswer: Answer[Future[Unit]] = (_) ⇒ {
      compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice))))
      Future.unit
    }
    val shutdownAnswer: Answer[Unit] = (_) ⇒
      compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice))))
    val runAnswer: Answer[Unit] = (_) ⇒
      compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(runChoice))))

    val componentHandlers = mock[SampleComponentHandlers]
    when(componentHandlers.initialize()).thenThrow(initializeFailureException).thenAnswer(initializeAnswer)
    when(componentHandlers.onShutdown()).thenAnswer(shutdownAnswer)
    when(componentHandlers.onRun()).thenAnswer(runAnswer)
    componentHandlers
  }
}
