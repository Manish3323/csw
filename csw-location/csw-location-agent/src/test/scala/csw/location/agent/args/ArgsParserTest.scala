package csw.location.agent.args

import java.io.ByteArrayOutputStream

import csw.prefix.Prefix
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ArgsParserTest extends FunSuite with Matchers with BeforeAndAfterEach {

  //Capture output/error generated by the parser, for cleaner test output. If interested, errCapture.toString will return capture errors.
  val outCapture = new ByteArrayOutputStream
  val errCapture = new ByteArrayOutputStream

  override protected def afterEach(): Unit = {
    outCapture.reset()
    errCapture.reset()
  }

  def silentParse(args: Array[String]): Option[Options] =
    Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        new ArgsParser("csw-location-agent").parser.parse(args, Options())
      }
    }

  test("test parser with valid arguments") {
    val port     = 5555
    val services = "csw.redis,csw.alarm,csw.watchdog"
    val args     = Array("--prefix", services, "--port", port.toString, "--command", "sleep 5")

    val x: Option[Options] = silentParse(args)
    x should contain(
      Options(List("csw.redis", "csw.alarm", "csw.watchdog").map(Prefix(_)), Some("sleep 5"), Some(port), None, None)
    )
  }

  // DEOPSCSW-628: Add support for registering service as HTTP in location agent
  test("test parser for http command line argument") {
    val port     = 5555
    val services = "csw.aas"
    val args     = Array("--prefix", services, "--port", port.toString, "--command", "sleep 5", "--http", "testPath")

    val x: Option[Options] = silentParse(args)
    x should contain(Options(List(Prefix("csw.aas")), Some("sleep 5"), Some(port), None, None, httpPath = Some("testPath")))
  }

  test("test parser with invalid service name combinations") {
    val port = 5555
    val listOfInvalidServices: List[String] =
      List("re-dis,alarm", "redis, alarm", " redis,alarm-service", "redis, alarm ", "redis, ,alarm")

    listOfInvalidServices.foreach { service =>
      val args = Array("--prefix", service, "--port", port.toString, "--command", "sleep 5")
      silentParse(args) shouldEqual None
    }
  }

  test("test parser with only --prefix option, should be allowed") {
    val args               = Array[String]("--prefix", "csw.myService")
    val x: Option[Options] = silentParse(args)

    x should contain(Options(List("csw.myService").map(Prefix(_)), None, None, None, None))
  }

  test("test parser without --prefix option, should error out") {
    val args               = Array[String]("csw.abcd")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with prefix containing '-' character, should error out") {
    val args               = Array[String]("--prefix", "csw.alarm-service")
    val x: Option[Options] = silentParse(args)
    x shouldEqual None
  }

  test("test parser with list of services containing leading/trailing whitespace, should error out") {
    val args               = Array[String]("--prefix", "   csw.redis-server,   csw.alarm,csw.watchdog   ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with prerfix containing leading whitespace, should error out") {
    val args               = Array[String]("--prefix", " csw.someService")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with prefix containing trailing whitespace, should error out") {
    val args               = Array[String]("--prefix", "csw.someService ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with prefix containing both leading and trailing whitespaces, error is shown") {
    val args               = Array[String]("--prefix", " csw.someService ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }
}
