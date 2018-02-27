package csw.messages.location

import acyclic.skipped
import csw.messages.TMTSerializable
import csw.messages.location.ConnectionType.{AkkaType, HttpType, TcpType}
import play.api.libs.json._

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TMTSerializable { self: TypedConnection[_] ⇒

  //TODO: add doc for why we need this
  type L <: Location

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  //TODO: add doc for significance
  def connectionInfo: ConnectionInfo = ConnectionInfo(componentId.name, componentId.componentType, connectionType)

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = connectionInfo.toString

  //TODO: add doc for significance
  def of[T <: Location]: TypedConnection[T] = self.asInstanceOf[TypedConnection[T]]
}

//TODO: add doc for significance
abstract sealed class TypedConnection[T <: Location](connectionType: ConnectionType) extends Connection(connectionType) {
  override type L = T
}

object Connection {

  //TODO: add doc for significance
  def from(input: String): Connection = input.split("-") match {
    case Array(name, componentType, connectionType) ⇒
      from(ConnectionInfo(name, ComponentType.withName(componentType), ConnectionType.withName(connectionType)))
    case _ ⇒ throw new IllegalArgumentException(s"Unable to parse '$input' to make Connection object")
  }

  //TODO: add doc for significance
  def from(connectionInfo: ConnectionInfo): Connection = from(
    ComponentId(connectionInfo.name, connectionInfo.componentType),
    connectionInfo.connectionType
  )

  //TODO: add doc for significance
  private def from(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType ⇒ AkkaConnection(componentId)
    case TcpType  ⇒ TcpConnection(componentId)
    case HttpType ⇒ HttpConnection(componentId)
  }

  implicit val connectionReads: Reads[Connection]   = ConnectionInfo.connectionInfoFormat.map(Connection.from)
  implicit val connectionWrites: Writes[Connection] = Writes[Connection](c ⇒ Json.toJson(c.connectionInfo))

  /**
   * Represents a connection offered by remote Actors
   */
  case class AkkaConnection(componentId: ComponentId) extends TypedConnection[AkkaLocation](AkkaType)

  /**
   * Represents a http connection provided by the component
   */
  case class HttpConnection(componentId: ComponentId) extends TypedConnection[HttpLocation](HttpType)

  /**
   * represents a tcp connection provided by the component
   */
  case class TcpConnection(componentId: ComponentId) extends TypedConnection[TcpLocation](TcpType)
}
