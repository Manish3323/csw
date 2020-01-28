package csw.location.models.scaladsl

import csw.location.models.ComponentType
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentTypeTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data model
  test(
    "ComponentType should be any one of this types : 'container', 'hcd', 'assembly', 'sequence', 'sequence_component' and 'service'"
  ) {
    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service", "machine", "sequencer", "sequence_component")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
