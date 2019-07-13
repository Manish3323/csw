package csw.aas.core.deployment
import java.net.URI

import com.typesafe.config.{ConfigException, ConfigFactory}
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType, HttpLocation}
import org.scalatest.{FunSuite, Matchers}

import scala.jdk.CollectionConverters._

class AuthConfigTest extends FunSuite with Matchers {
  test("should create KeycloakDeployment from config") {
    val config     = ConfigFactory.load()
    val authConfig = AuthConfig.create()

    val deployment = authConfig.getDeployment

    deployment.getResourceName shouldBe config.getConfig("auth-config").getString("client-id")
    deployment.getRealm shouldBe config.getConfig("auth-config").getString("realm")
    deployment.getAuthServerBaseUrl shouldBe config.getConfig("auth-config").getString("auth-server-url")
  }

  test("should create KeycloakDeployment from config and auth server url from location service") {
    val config        = ConfigFactory.load()
    val authServerUrl = "http://somehost:someport"
    val httpLocation  = HttpLocation(HttpConnection(ComponentId("testComponent", ComponentType.Service)), new URI(authServerUrl))
    val authConfig    = AuthConfig.create(authServerLocation = Some(httpLocation))

    val deployment = authConfig.getDeployment

    deployment.getResourceName shouldBe config.getConfig("auth-config").getString("client-id")
    deployment.getRealm shouldBe config.getConfig("auth-config").getString("realm")
    deployment.getAuthServerBaseUrl shouldBe authServerUrl
  }

  test("should throw exception if client-id is not present in config") {
    val map: Map[String, String] = Map("auth-config.realm" -> "test")
    val config                   = ConfigFactory.parseMap(map.asJava)

    val authConfig = AuthConfig.create(config)

    intercept[ConfigException.Missing] {
      authConfig.getDeployment
    }
  }
}
