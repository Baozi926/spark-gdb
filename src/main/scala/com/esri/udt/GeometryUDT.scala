package com.esri.udt

import com.esri.core.geometry.Geometry
import org.apache.spark.sql.types.SQLUserDefinedType

/**
  */
@SQLUserDefinedType(udt = classOf[ShapeEsri])
case class GeometryUDT(val geometry: Geometry)