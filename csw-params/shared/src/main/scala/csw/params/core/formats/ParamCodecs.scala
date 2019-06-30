package csw.params.core.formats

import java.lang.{Byte ⇒ JByte}
import java.time.Instant

import csw.params.commands.CommandIssue._
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Coords._
import csw.params.core.models._
import csw.params.core.states.{CurrentState, DemandState, StateName, StateVariable}
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable.{WrappedArray ⇒ WArray}
import scala.reflect.ClassTag

object ParamCodecs extends CommonCodecs {
  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  // ************************ Base Type Codecs ********************

  //Ensure that Codec.forCaseClass is used ONLY for unary case classes see https://github.com/sirthias/borer/issues/26
  implicit lazy val choiceCodec: Codec[Choice] = Codec.forCaseClass[Choice]
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec[RaDec]

  implicit lazy val tagCodec: Codec[Coords.Tag]            = Codec.forCaseClass[Coords.Tag]
  implicit lazy val angleCodec: Codec[Angle]               = Codec.forCaseClass[Angle]
  implicit lazy val properMotionCodec: Codec[ProperMotion] = deriveCodec[ProperMotion]

  implicit lazy val eqFrameCodec: Codec[EqFrame] = CborHelpers.enumCodec[EqFrame]

  implicit lazy val solarSystemObjectCodec: Codec[SolarSystemObject] = CborHelpers.enumCodec[SolarSystemObject]

  implicit lazy val eqCoordCodec: Codec[EqCoord]                   = deriveCodec[EqCoord]
  implicit lazy val solarSystemCoordCodec: Codec[SolarSystemCoord] = deriveCodec[SolarSystemCoord]
  implicit lazy val minorPlanetCoordCodec: Codec[MinorPlanetCoord] = deriveCodec[MinorPlanetCoord]
  implicit lazy val cometCoordCodec: Codec[CometCoord]             = deriveCodec[CometCoord]
  implicit lazy val altAzCoordCodec: Codec[AltAzCoord]             = deriveCodec[AltAzCoord]
  implicit lazy val coordCodec: Codec[Coord]                       = deriveCodec[Coord]

  implicit lazy val tsCodec: Codec[Timestamp] = deriveCodec[Timestamp]

  implicit lazy val instantEnc: Encoder[Instant] = CborHelpers.targetSpecificEnc(
    cborEnc = tsCodec.encoder.contramap(Timestamp.fromInstant),
    jsonEnc = Encoder.forString.contramap(_.toString)
  )

  implicit lazy val instantDec: Decoder[Instant] = CborHelpers.targetSpecificDec(
    cborDec = tsCodec.decoder.map(_.toInstant),
    jsonDec = Decoder.forString.map(Instant.parse)
  )

  implicit lazy val utcTimeCodec: Codec[UTCTime] = Codec.forCaseClass[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = Codec.forCaseClass[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    CborHelpers.bimap[WArray[T], ArrayData[T]](ArrayData(_), _.data)

  implicit def matrixDataCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    CborHelpers.bimap[WArray[WArray[T]], MatrixData[T]](MatrixData(_), _.data)

  // ************************ Enum Codecs ********************

  implicit lazy val unitsCodec: Codec[Units]                   = CborHelpers.enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = CborHelpers.enumCodec[KeyType[_]]
  implicit def keyTypeCodec[T]: Codec[KeyType[T]]              = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  //Do not replace these with bimap, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val javaByteArrayEnc: Encoder[Array[JByte]] = Encoder.forByteArray.contramap(_.map(x ⇒ x: Byte))
  implicit lazy val javaByteArrayDec: Decoder[Array[JByte]] = Decoder.forByteArray.map(_.map(x ⇒ x: JByte))

  //Do not put the bytesEnc and bytesDec inside Codec, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit lazy val bytesEnc: Encoder[Array[Byte]] = CborHelpers.targetSpecificEnc(
    cborEnc = Encoder.forByteArray,
    jsonEnc = Encoder.forArray[Byte]
  )

  implicit lazy val bytesDec: Decoder[Array[Byte]] = CborHelpers.targetSpecificDec(
    cborDec = Decoder.forByteArray,
    jsonDec = Decoder.forArray[Byte]
  )

  implicit def waCodec[T: ArrayEnc: ArrayDec]: Codec[WArray[T]] =
    CborHelpers.bimap[Array[T], WArray[T]](x => x: WArray[T], _.array)

  implicit def paramCodec[T: ArrayEnc: ArrayDec]: Codec[Parameter[T]] = deriveCodec[Parameter[T]]

  implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val encoder: Encoder[Parameter[Any]] = value.keyType.paramEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    r.tryReadMapHeader(4) || r.tryReadMapStart() || r.tryReadArrayHeader(4) || r.tryReadArrayStart()
    r.tryReadString("keyName")
    val keyName = r.readString()
    r.tryReadString("keyType")
    val keyType = KeyType.withNameInsensitive(r.readString())
    r.tryReadString("values")
    val wa = keyType.waDecoder.read(r)
    r.tryReadString("units")
    val units = unitsCodec.decoder.read(r)
    if (r.target != Cbor) {
      r.tryReadBreak()
    }
    Parameter(keyName, keyType.asInstanceOf[KeyType[Any]], wa.asInstanceOf[WArray[Any]], units)
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Event Codecs ********************

  //Codec.forCaseClass does not work for id due to https://github.com/sirthias/borer/issues/23
  implicit lazy val idCodec: Codec[Id]               = CborHelpers.bimap[String, Id](Id(_), _.id)
  implicit lazy val eventNameCodec: Codec[EventName] = Codec.forCaseClass[EventName]

  implicit lazy val seCodec: Codec[SystemEvent]  = deriveCodec[SystemEvent]
  implicit lazy val oeCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
  implicit lazy val eventCodec: Codec[Event]     = deriveCodec[Event]

  // ************************ Command Codecs ********************

  implicit lazy val commandNameCodec: Codec[CommandName] = Codec.forCaseClass[CommandName]
  implicit lazy val obsIdCodec: Codec[ObsId]             = Codec.forCaseClass[ObsId]

  implicit lazy val observeCommandCodec: Codec[Observe]          = deriveCodec[Observe]
  implicit lazy val setupCommandCodec: Codec[Setup]              = deriveCodec[Setup]
  implicit lazy val waitCommandCodec: Codec[Wait]                = deriveCodec[Wait]
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand] = deriveCodec[SequenceCommand]
  implicit lazy val controlCommandCodec: Codec[ControlCommand]   = deriveCodec[ControlCommand]
  implicit lazy val commandCodec: Codec[Command]                 = deriveCodec[Command]

  // ************************ CommandResponse Codecs ********************

  implicit lazy val resultCodec: Codec[Result]                                      = deriveCodec[Result]
  implicit lazy val validateCommandResponseCodec: Codec[ValidateCommandResponse]    = deriveCodec[ValidateCommandResponse]
  implicit lazy val validateResponseCodec: Codec[ValidateResponse]                  = deriveCodec[ValidateResponse]
  implicit lazy val oneWayCommandResponseCodec: Codec[OnewayResponse]               = deriveCodec[OnewayResponse]
  implicit lazy val submitCommandResponseCodec: Codec[SubmitResponse]               = deriveCodec[SubmitResponse]
  implicit lazy val queryCommandResponseCodec: Codec[QueryResponse]                 = deriveCodec[QueryResponse]
  implicit lazy val matchingCommandResponseCodec: Codec[MatchingResponse]           = deriveCodec[MatchingResponse]
  implicit lazy val acceptedCodec: Codec[Accepted]                                  = deriveCodec[Accepted]
  implicit lazy val startedCodec: Codec[Started]                                    = deriveCodec[Started]
  implicit lazy val completedWithResultCodec: Codec[CompletedWithResult]            = deriveCodec[CompletedWithResult]
  implicit lazy val completedCodec: Codec[Completed]                                = deriveCodec[Completed]
  implicit lazy val invalidCodec: Codec[Invalid]                                    = deriveCodec[Invalid]
  implicit lazy val errorCodec: Codec[Error]                                        = deriveCodec[Error]
  implicit lazy val cancelledCodec: Codec[Cancelled]                                = deriveCodec[Cancelled]
  implicit lazy val lockedCodec: Codec[Locked]                                      = deriveCodec[Locked]
  implicit lazy val commandNotAvailableCodec: Codec[CommandNotAvailable]            = deriveCodec[CommandNotAvailable]
  implicit lazy val commandResponseRemoteMsgCodec: Codec[CommandResponse.RemoteMsg] = deriveCodec[CommandResponse.RemoteMsg]

  // ************************ CommandIssue Codecs ********************

  implicit lazy val missingKeyIssueCodec: Codec[MissingKeyIssue]                 = deriveCodec[MissingKeyIssue]
  implicit lazy val wrongPrefixIssueCodec: Codec[WrongPrefixIssue]               = deriveCodec[WrongPrefixIssue]
  implicit lazy val wrongParameterTypeIssueCodec: Codec[WrongParameterTypeIssue] = deriveCodec[WrongParameterTypeIssue]
  implicit lazy val wrongUnitsIssueCodec: Codec[WrongUnitsIssue]                 = deriveCodec[WrongUnitsIssue]
  implicit lazy val wrongNumberOfParametersIssueCodec: Codec[WrongNumberOfParametersIssue] =
    deriveCodec[WrongNumberOfParametersIssue]
  implicit lazy val assemblyBusyIssueCodec: Codec[AssemblyBusyIssue]               = deriveCodec[AssemblyBusyIssue]
  implicit lazy val unresolvedLocationsIssueCodec: Codec[UnresolvedLocationsIssue] = deriveCodec[UnresolvedLocationsIssue]
  implicit lazy val parameterValueOutOfRangeIssueCodec: Codec[ParameterValueOutOfRangeIssue] =
    deriveCodec[ParameterValueOutOfRangeIssue]
  implicit lazy val wrongInternalStateIssueCodec: Codec[WrongInternalStateIssue] = deriveCodec[WrongInternalStateIssue]
  implicit lazy val unsupportedCommandInStateIssueCodec: Codec[UnsupportedCommandInStateIssue] =
    deriveCodec[UnsupportedCommandInStateIssue]
  implicit lazy val unsupportedCommandIssueCodec: Codec[UnsupportedCommandIssue] = deriveCodec[UnsupportedCommandIssue]
  implicit lazy val requiredServiceUnavailableIssueCodec: Codec[RequiredServiceUnavailableIssue] =
    deriveCodec[RequiredServiceUnavailableIssue]
  implicit lazy val requiredHCDUnavailableIssueCodec: Codec[RequiredHCDUnavailableIssue] =
    deriveCodec[RequiredHCDUnavailableIssue]
  implicit lazy val requiredAssemblyUnavailableIssueCodec: Codec[RequiredAssemblyUnavailableIssue] =
    deriveCodec[RequiredAssemblyUnavailableIssue]
  implicit lazy val requiredSequencerUnavailableIssueCodec: Codec[RequiredSequencerUnavailableIssue] =
    deriveCodec[RequiredSequencerUnavailableIssue]
  implicit lazy val otherIssueCodec: Codec[OtherIssue]     = deriveCodec[OtherIssue]
  implicit lazy val commandIssueCodec: Codec[CommandIssue] = deriveCodec[CommandIssue]

  // ************************ StateVariable Codecs ********************

  implicit lazy val stateNameCodec: Codec[StateName]         = deriveCodec[StateName]
  implicit lazy val demandStateCodec: Codec[DemandState]     = deriveCodec[DemandState]
  implicit lazy val currentStateCodec: Codec[CurrentState]   = deriveCodec[CurrentState]
  implicit lazy val stateVariableCodec: Codec[StateVariable] = deriveCodec[StateVariable]

  // ************************ Subsystem Codecs ********************
  implicit lazy val subSystemCodec: Codec[Subsystem] = CborHelpers.enumCodec[Subsystem]
}

case class Timestamp(seconds: Long, nanos: Long) {
  def toInstant: Instant = Instant.ofEpochSecond(seconds, nanos)

}

object Timestamp {
  def fromInstant(instant: Instant): Timestamp = Timestamp(instant.getEpochSecond, instant.getNano)
}
