package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Authorization(permissions: Set[Permission] = Set.empty)

private[auth] object Authorization {
  val empty: Authorization = Authorization()

  implicit val authorizationFormat: OFormat[Authorization] = Json.using[Json.WithDefaultValues].format
}
