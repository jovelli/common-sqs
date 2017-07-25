package com.taxis99.streams

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sqs.model.Message
import com.taxis99.streams.Consumer.getClass
import com.typesafe.scalalogging.Logger
import msgpack4z._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

object Serializer {

  protected val jsonVal = Play2Msgpack.jsValueCodec(PlayUnpackOptions.default)

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  /**
    * Returns a byte array string from a JsValue packed with MsgPack.
    * @param value the JSON value
    * @return a byte array string representation of the JsValue
    */
  def pack(value: JsValue): String = {
    val packer = MsgOutBuffer.create()
    jsonVal.pack(packer, value)
    packer.result().map(_.toInt).mkString(" ")
  }


  /**
    * Returns a JsValue from a byte array value unpacked with MsgPack.
    * @param value The byte array string
    * @return
    */
  def unpack(value: String): JsValue = {
    val bytes: Array[Byte] = value.split(" ").map(_.toByte)
    val unpacker = MsgInBuffer.apply(bytes)
    val unpacked = jsonVal.unpack(unpacker)
    unpacked.valueOr { e =>
      logger.error("Could not decode message", e)
      new ClassCastException("Could not decode message")
      Json.obj()
    }
  }

  /**
    * Returns a compressed JsValue string.
    * @return The byte array string
    */
  def encode: Flow[JsValue, String, NotUsed] = Flow[JsValue] map { value =>
    pack(value)
  }

  /**
    * Decode a compressed string back to a JsValue.
    * @return
    */
  def decode: Flow[Message, JsValue, NotUsed] = Flow[Message] map { value =>
    unpack(value.getBody)
  }
}