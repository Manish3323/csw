package csw.contract.generator
import csw.location.api.codec.LocationServiceCodecs

class FilesGeneratorTest{}

//  override def afterAll(): Unit = {
//    val dir = new Directory(new File("testOutput"))
//    if (dir.exists) {
//      dir.deleteRecursively()
//    }
//  }
//  test("should generate samples for given services") {
//    val componentId: ComponentId       = ComponentId(Prefix("tcs.filter.wheel"), ComponentType.HCD)
//    val akkaConnection: AkkaConnection = AkkaConnection(componentId)
//    val akkaLocation: Location         = AkkaLocation(akkaConnection, new URI("some_path"))
//    val httpEndpoints: List[Endpoint] = List(
//      Endpoint(
//        request = "Register",
//        response = "RegistrationFailed",
//        errors = Nil
//      )
//    )
//    val websocketEndpoints: List[Endpoint] = List(
//      Endpoint(
//        request = "Track",
//        response = "TrackingEvent",
//        errors = List("ServiceError")
//      )
//    )
//    val models: Map[String, ModelType] = Map(
//      "Registration" -> ModelType(akkaLocation)
//    )
//    val services: Services = Services(
//      Map(
//        "location" -> Service(
//          httpEndpoints = httpEndpoints,
//          webSocketEndpoints = websocketEndpoints,
//          models
//        )
//      )
//    )
//    val testOutput = "testOutput"
//    FilesGenerator.generate(services, testOutput)
//    val path = Paths.get(testOutput)
//    Files.exists(path) shouldBe true
//    Files.size(path) should be > 0L
//  }