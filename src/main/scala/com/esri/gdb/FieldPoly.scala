package com.esri.gdb

import java.nio.ByteBuffer
import com.esri.core.geometry.MultiPath
import org.apache.spark.sql.types.{DataType, Metadata}

abstract class FieldPoly(name: String,
                         dataType: DataType,
                         nullValueAllowed: Boolean,
                         xorig: Double,
                         yorig: Double,
                         xyscale: Double,
                         metadata: Metadata
                        )
  extends FieldGeom(name, dataType, nullValueAllowed, xorig, yorig, xyscale, metadata) {

  protected var dx = 0L
  protected var dy = 0L

/*
  @deprecated
  def getCoordinates(byteBuffer: ByteBuffer, numCoordinates: Int) = {
    val coordinates = new Array[Coordinate](numCoordinates)
    0 until numCoordinates foreach (n => {
      dx += byteBuffer.getVarInt
      dy += byteBuffer.getVarInt
      val x = dx / xyscale + xorig
      val y = dy / xyscale + yorig
      coordinates(n) = new Coordinate(x, y)
    })
    coordinates
  }
*/

  def addPath(byteBuffer: ByteBuffer, numCoordinates: Int, path: MultiPath) = {
    0 until numCoordinates foreach (n => {
      dx += byteBuffer.getVarInt
      dy += byteBuffer.getVarInt
      val x = dx / xyscale + xorig
      val y = dy / xyscale + yorig
      n match {
        case 0 => path.startPath(x, y)
        case _ => path.lineTo(x, y)
      }
    })
    path
  }
}