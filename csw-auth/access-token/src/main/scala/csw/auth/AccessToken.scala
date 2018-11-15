package csw.auth

import java.security.PublicKey
import java.util.Base64

import csw.auth.Conversions._
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

case class AccessToken(
    //standard checks
    sub: Option[String],
    iat: Option[Long],
    exp: Option[Long],
    iss: Option[String],
    aud: Option[String],
    jti: Option[String],
    //additional information
    given_name: Option[String],
    family_name: Option[String],
    name: Option[String],
    preferred_username: Option[String],
    email: Option[String],
    scope: Option[String],
    //auth
    realm_access: Option[RealmAccess],
    resource_access: Option[Map[String, ResourceAccess]],
    authorization: Option[Authorization]
) {
  def hasPermission(scope: String, resource: String): Boolean = {

    this.authorization match {
      case Some(auth) =>
        auth.permissions match {
          case Some(permissions) =>
            permissions.exists {
              case Permission(_, r, Some(scopes)) if r == resource => scopes.contains(scope)
              case _ => {
                System.err.println(s"user doesn't have '$scope' permission for '$resource' resource")
                false
              }
            }
          case None => {
            System.err.println("token doesn't contain permissions")
            false
          }
        }
      case None => {
        System.err.println("token doesn't authorization claim")
        false
      }
    }
  }

  def hasRole(role: String, resource: String): Boolean = {

    val allRealmRoles: Set[String] = this.realm_access match {
      case Some(realmAccess) =>
        realmAccess.roles match {
          case Some(realmRoles) => realmRoles
          case None             => Set.empty
        }
      case None => Set.empty
    }

    val allResourceRoles: Set[String] = this.resource_access match {
      case Some(resourceAccesses) =>
        resourceAccesses.get(resource) match {
          case Some(resourceAccess) =>
            resourceAccess.roles match {
              case Some(roles) => roles
              case None        => Set.empty
            }
          case None => Set.empty
        }
      case None => Set.empty
    }

    allRealmRoles.union(allResourceRoles).contains(role)
  }
}

object AccessToken {

  implicit val accessTokenFormat: OFormat[AccessToken] =
    Json.format[AccessToken]

  def decode(token: String): Try[AccessToken] = {
    getKeyId(token) match {
      case Failure(exception) => Failure(exception)
      case Success(kid) =>
        val publicKey = PublicKey.fromAuthServer(kid)
        decode(token, publicKey)
    }
  }

  private def getKeyId(token: String): Try[String] = {

    val format = "^(.+?)\\.(.+?)\\.(.+?$)".r

    val mayBeHeaderString = token match {
      case format(header, _, _) =>
        val jsonHeaderString = Base64.getDecoder.decode(header).map(_.toChar).mkString
        Success(jsonHeaderString)
      case _ =>
        Failure(new RuntimeException("invalid token format"))
    }

    val mayBeHeader = mayBeHeaderString match {
      case Success(headerString) => Success(JwtJson.parseHeader(headerString))
      case Failure(_)            => Failure(new RuntimeException("invalid token format"))
    }

    mayBeHeader match {
      case Failure(exception) => Failure(exception)
      case Success(header) =>
        header.keyId match {
          case Some(keyId) => Success(keyId)
          case None        => Failure(new RuntimeException("token does not have a key Id"))
        }
    }
  }

  private def decode(token: String, publicKey: PublicKey): Try[AccessToken] = {

    val verification =
      JwtJson.decodeJson(token, publicKey, Seq(JwtAlgorithm.RS256))

    verification match {
      case Failure(exception) =>
        System.err.println(exception)
        Failure(exception)
      case Success(jsObject) =>
        val jsResult = accessTokenFormat.reads(jsObject)

        jsResult match {
          case JsSuccess(accessToken, _) => Success(accessToken)
          case e: JsError =>
            System.err.println(toFailure(e).exception)
            e
        }
    }
  }
}
