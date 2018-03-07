package csw.framework.components.assembly;

import akka.typed.javadsl.ActorContext;
import akka.typed.javadsl.Adapter;
import csw.framework.exceptions.FailureRestart;
import csw.framework.exceptions.FailureStop;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.TopLevelActorMessage;
import csw.messages.ccs.CommandIssue;
import csw.messages.ccs.commands.CommandResponse;
import csw.messages.ccs.commands.ControlCommand;
import csw.messages.ccs.commands.Observe;
import csw.messages.ccs.commands.Setup;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.*;
import csw.services.ccs.scaladsl.CommandResponseManager;
import csw.services.config.api.javadsl.IConfigClientService;
import csw.services.config.api.models.ConfigData;
import csw.services.config.client.javadsl.JConfigClientFactory;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JComponentType;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


//#jcomponent-handlers-class
public class JAssemblyComponentHandlers extends JComponentHandlers {

    private final ActorContext<TopLevelActorMessage> ctx;
    private final ComponentInfo componentInfo;
    private final CommandResponseManager commandResponseManager;
    private final CurrentStatePublisher currentStatePublisher;
    private final ILocationService locationService;
    private ILogger log;
    private IConfigClientService configClient;

    public JAssemblyComponentHandlers(
            akka.typed.javadsl.ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            CommandResponseManager commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            ILocationService locationService,
            JLoggerFactory loggerFactory

    ) {
        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory);
        this.ctx = ctx;
        this.componentInfo = componentInfo;
        this.commandResponseManager = commandResponseManager;
        this.currentStatePublisher = currentStatePublisher;
        this.locationService = locationService;
        log = loggerFactory.getLogger(this.getClass());
        configClient = JConfigClientFactory.clientApi(Adapter.toUntyped(ctx.getSystem()), locationService);
    }
    //#jcomponent-handlers-class

    //#jInitialize-handler
    @Override
    public CompletableFuture<Void> jInitialize() {
        /*
         * Initialization could include following steps :
         * 1. fetch config (preferably from configuration service)
         * 2. create a worker actor which is used by this assembly
         * 3. find a Hcd connection from the connections provided in componentInfo
         * 4. If an Hcd is found as a connection, resolve its location from location service and create other
         *    required worker actors required by this assembly
         * */

        return new CompletableFuture<>();
    }
    //#jInitialize-handler

    //#validateCommand-handler
    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup) {
            // validation for setup goes here
            return new CommandResponse.Accepted(controlCommand.runId());
        } else if (controlCommand instanceof Observe) {
            // validation for observe goes here
            return new CommandResponse.Accepted(controlCommand.runId());
        } else {
            return new CommandResponse.Invalid(controlCommand.runId(), new CommandIssue.AssemblyBusyIssue("Command not supported"));
        }
    }
    //#validateCommand-handler

    //#onSubmit-handler
    @Override
    public void onSubmit(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            submitSetup((Setup) controlCommand); // includes logic to handle Submit with Setup config command
        else if (controlCommand instanceof Observe)
            submitObserve((Observe) controlCommand); // includes logic to handle Submit with Observe config command
    }
    //#onSubmit-handler

    //#onOneway-handler
    @Override
    public void onOneway(ControlCommand controlCommand) {
        if (controlCommand instanceof Setup)
            onewaySetup((Setup) controlCommand); // includes logic to handle Oneway with Setup config command
        else if (controlCommand instanceof Observe)
            onewayObserve((Observe) controlCommand); // includes logic to handle Oneway with Observe config command
    }
    //#onOneway-handler

    //#onGoOffline-handler
    @Override
    public void onGoOffline() {
        // do something when going offline
    }
    //#onGoOffline-handler

    //#onGoOnline-handler
    @Override
    public void onGoOnline() {
        // do something when going online
    }
    //#onGoOnline-handler

    //#onShutdown-handler
    @Override
    public CompletableFuture<Void> jOnShutdown() {
        // clean up resources
        return new CompletableFuture<>();
    }
    //#onShutdown-handler

    //#onLocationTrackingEvent-handler
    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        if (trackingEvent instanceof LocationUpdated) {
            // do something for the tracked location when it is updated
        } else if (trackingEvent instanceof LocationRemoved) {
            // do something for the tracked location when it is no longer available
        }
    }
    //#onLocationTrackingEvent-handler

    private void processSetup(Setup sc) {
        switch (sc.commandName().name()) {
            case "forwardToWorker":
            default:
                log.error("Invalid command [" + sc + "] received.");
        }
    }

    private void processObserve(Observe oc) {
        switch (oc.commandName().name()) {
            case "point":
            case "acquire":
            default:
                log.error("Invalid command [" + oc + "] received.");
        }
    }

    /**
     * in case of submit command, component writer is required to update commandResponseManager with the result
     */
    private void submitSetup(Setup setup) {
        processSetup(setup);
    }

    private void submitObserve(Observe observe) {
        processObserve(observe);
    }

    private void onewaySetup(Setup setup) {
        processSetup(setup);
    }

    private void onewayObserve(Observe observe) {
        processObserve(observe);
    }

    /**
     * Below methods are just here to show how exceptions can be used to either restart or stop supervisor
     * This are snipped in paradox documentation
     * */

    // #failureRestart-Exception
    class HcdNotFoundException extends FailureRestart {
        HcdNotFoundException() {
            super("Could not resolve hcd location. Initialization failure.");
        }
    }

    private void resolveHcd() {
        // find a Hcd connection from the connections provided in componentInfo
        Optional<Connection> mayBeConnection = componentInfo.getConnections().stream()
                .filter(connection -> connection.componentId().componentType() == JComponentType.HCD)
                .findFirst();

        // If an Hcd is found as a connection, resolve its location from location service and create other
        // required worker actors required by this assembly
        mayBeConnection.ifPresent(connection -> {
            CompletableFuture<Optional<AkkaLocation>> eventualHcdLocation = locationService.resolve(connection.<AkkaLocation>of(), FiniteDuration.apply(5, TimeUnit.SECONDS));
            try {
                AkkaLocation hcdLocation = eventualHcdLocation.get().orElseThrow(HcdNotFoundException::new);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
    // #failureRestart-Exception

    // #failureStop-Exception
    class ConfigNotAvailableException extends FailureStop {
        ConfigNotAvailableException() {
            super("Configuration not available. Initialization failure.");
        }
    }

    private CompletableFuture<ConfigData> getAssemblyConfigs() {
        // required configuration could not be found in the configuration service. Component can choose to stop until the configuration is made available in the
        // configuration service and started again
        return configClient.getActive(Paths.get("tromboneAssemblyContext.conf")).thenApply((maybeConfigData) -> {
            return maybeConfigData.orElseThrow(ConfigNotAvailableException::new);
        });
    }
    // #failureStop-Exception

}
