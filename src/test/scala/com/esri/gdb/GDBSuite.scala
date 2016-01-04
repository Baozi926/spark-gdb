package com.esri.gdb

import com.esri.udt.GeometryUDT
import com.vividsolutions.jts.geom.Point
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  */
class GDBSuite extends FunSuite with BeforeAndAfterAll {
  private var sc: SparkContext = _
  private var sqlContext: SQLContext = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    sc = new SparkContext("local[2]", "GDBSuite")
    sqlContext = new SQLContext(sc)
  }

  override protected def afterAll(): Unit = {
    try {
      sqlContext.sparkContext.stop()
    } finally {
      super.afterAll()
    }
  }

  private val gdbPath = "src/test/resources/Test.gdb"

  /*
    test("Context") {
      sc.gdbFile(gdbPath, "Points", numPartitions = 1).map(row => {
        row.getAs[Geometry](row.fieldIndex("Shape")).buffer(10)
      }).foreach(println)
    }
  */

  test("JTS Points") {
    doPoints(sqlContext.gdbFile(gdbPath, "Points", "jts", 2))
  }

  test("WKT Points") {
    doPoints(sqlContext.gdbFile(gdbPath, "Points", "wkt", 2))
  }

  test("WKB Points") {
    doPoints(sqlContext.gdbFile(gdbPath, "Points", "wkb", 2))
  }

  def doPoints(dataFrame: DataFrame): Unit = {
    val xyTolerance = dataFrame.schema("Shape").metadata.getDouble("xyTolerance")

    val results = dataFrame.select("Shape", "X", "Y", "RID", "OBJECTID")
    results.collect.foreach(row => {
      val point = row.getAs[GeometryUDT](0).geometry.asInstanceOf[Point]
      val x = row.getDouble(1)
      val y = row.getDouble(2)
      val rid = row.getInt(3)
      val oid = row.getInt(4)
      assert((point.getX - x).abs <= xyTolerance)
      assert((point.getY - y).abs <= xyTolerance)
      assert(rid === oid)
    })
  }

  test("JTS Lines") {
    doLines(sqlContext.gdbFile(gdbPath, "Lines", "jts", 2))
  }

  test("WKB Lines") {
    doLines(sqlContext.gdbFile(gdbPath, "Lines", "wkb", 2))
  }

  test("WKT Lines") {
    doLines(sqlContext.gdbFile(gdbPath, "Lines", "wkt", 2))
  }

  def doLines(dataFrame: DataFrame): Unit = {
    val xyTolerance = dataFrame.schema("Shape").metadata.getDouble("xyTolerance")

    val results = dataFrame.select("Shape",
      "X1", "Y1",
      "X2", "Y2",
      "X3", "Y3",
      "RID",
      "OBJECTID")
    results.collect.foreach(row => {
      val lineString = row.getAs[GeometryUDT](0).geometry
      val x1 = row.getDouble(1)
      val y1 = row.getDouble(2)
      val x2 = row.getDouble(3)
      val y2 = row.getDouble(4)
      val x3 = row.getDouble(5)
      val y3 = row.getDouble(6)
      val rid = row.getInt(7)
      val oid = row.getInt(8)

      val coords = lineString.getCoordinates

      assert((coords(0).x - x1).abs <= xyTolerance)
      assert((coords(0).y - y1).abs <= xyTolerance)

      assert((coords(1).x - x2).abs <= xyTolerance)
      assert((coords(1).y - y2).abs <= xyTolerance)

      assert((coords(2).x - x3).abs <= xyTolerance)
      assert((coords(2).y - y3).abs <= xyTolerance)

      assert(rid === oid)
    })
  }

  test("JTS Polygons") {
    doPolygons(sqlContext.gdbFile(gdbPath, "Polygons", "jts", 2))
  }

  test("WKT Polygons") {
    doPolygons(sqlContext.gdbFile(gdbPath, "Polygons", "wkt", 2))
  }

  test("WKB Polygons") {
    doPolygons(sqlContext.gdbFile(gdbPath, "Polygons", "wkb", 2))
  }

  def doPolygons(dataFrame: DataFrame): Unit = {
    val xyTolerance = dataFrame.schema("Shape").metadata.getDouble("xyTolerance")

    val results = dataFrame.select("Shape",
      "X1", "Y1",
      "X2", "Y2",
      "RID",
      "OBJECTID")
    results.collect.foreach(row => {
      val polygon = row.getAs[GeometryUDT](0).geometry
      val x1 = row.getDouble(1)
      val y1 = row.getDouble(2)
      val x2 = row.getDouble(3)
      val y2 = row.getDouble(4)
      val rid = row.getInt(5)
      val oid = row.getInt(6)

      val envp = polygon.getEnvelopeInternal

      assert((envp.getMinX - x1).abs <= xyTolerance)
      assert((envp.getMinY - y1).abs <= xyTolerance)

      assert((envp.getMaxX - x2).abs <= xyTolerance)
      assert((envp.getMaxY - y2).abs <= xyTolerance)

      assert(rid === oid)
    })
  }

  test("DDL test") {
    sqlContext.sql(
      s"""
         |CREATE TEMPORARY TABLE points
         |USING com.esri.gdb
         |OPTIONS (path "$gdbPath", name "Points", numPartitions "1")
      """.stripMargin.replaceAll("\n", " "))

    assert(sqlContext.sql("SELECT * FROM points").collect().length === 20)
  }

  test("Field names, aliases and values") {
    val dataframe = sqlContext.gdbFile(gdbPath, "Types", "jts", 2)
    val schema = dataframe.schema

    val fieldShape = schema("Shape")
    assert(fieldShape.name === "Shape")
    assert(fieldShape.metadata.getString("alias") === "Shape")

    val fieldAText = schema("A_TEXT")
    assert(fieldAText.name === "A_TEXT")
    assert(fieldAText.metadata.getString("alias") === "A Text")
    assert(fieldAText.metadata.getLong("maxLength") === 32)

    val fieldAFloat = schema("A_FLOAT")
    assert(fieldAFloat.name === "A_FLOAT")
    assert(fieldAFloat.metadata.getString("alias") === "A Float")

    val fieldADouble = schema("A_DOUBLE")
    assert(fieldADouble.name === "A_DOUBLE")
    assert(fieldADouble.metadata.getString("alias") === "A Double")

    val fieldAShort = schema("A_SHORT")
    assert(fieldAShort.name === "A_SHORT")
    assert(fieldAShort.metadata.getString("alias") === "A Short")

    val fieldALong = schema("A_LONG")
    assert(fieldALong.name === "A_LONG")
    assert(fieldALong.metadata.getString("alias") === "A Long")

    val fieldADate = schema("A_DATE")
    assert(fieldADate.name === "A_DATE")
    assert(fieldADate.metadata.getString("alias") === "A Date")

    val fieldAGuid = schema("A_GUID")
    assert(fieldAGuid.name === "A_GUID")
    assert(fieldAGuid.metadata.getString("alias") === "A GUID")

    val row = dataframe
      .select("Shape", "A_TEXT", "A_FLOAT", "A_DOUBLE", "A_SHORT", "A_LONG", "A_DATE", "A_GUID")
      .collect()
      .head

    val point = row.getAs[GeometryUDT](0).geometry.asInstanceOf[Point]
    assert((point.getX - 33.8869).abs < 0.00001)
    assert((point.getY - 35.5131).abs < 0.00001)

    assert(row.getString(1) === "Beirut")

    assert((row.getFloat(2) - 33.8869).abs < 0.00001)
    assert((row.getDouble(3) - 35.5131).abs < 0.00001)

    assert(row.getShort(4) === 33)
    assert(row.getInt(5) === 35)

    val timestamp = row.getTimestamp(6)
    val datetime = new DateTime(timestamp.getTime, DateTimeZone.UTC)
    // 2016, 01, 01, 07, 24, 32
    assert(datetime.getYear === 2016)
    assert(datetime.getMonthOfYear === 1)
    assert(datetime.getDayOfMonth === 1)
    assert(datetime.getHourOfDay === 7)
    assert(datetime.getMinuteOfHour === 24)
    assert(datetime.getSecondOfMinute === 32)

    assert(row.getString(7) === "{2AA7D58D-2BF4-4943-83A8-457B70DB1871}")
  }

}
