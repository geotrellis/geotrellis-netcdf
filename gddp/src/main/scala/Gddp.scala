package com.example.gddp

import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.io._
import geotrellis.raster.render.ColorRamps
import geotrellis.raster.summary.polygonal.{MinDoubleSummary, MeanSummary}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.vector._
import geotrellis.vector.io._

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import ucar.nc2._

import java.io._

object Gddp {

  val logger = Logger.getLogger(Gddp.getClass)

  /**
    * Dump bytes to disk [1]
    *
    * 1. https://stackoverflow.com/questions/29978264/how-to-write-the-contents-of-a-scala-stream-to-a-file
    */
  def dump(data: Array[Byte], file: File ) = {
    val target = new BufferedOutputStream( new FileOutputStream(file) );
    try data.foreach( target.write(_) ) finally target.close;
  }

  /**
    * Main
    */
  def main(args: Array[String]) : Unit = {
    val netcdfUri =
      if (args.size > 0) args(0)
      // else "s3://nasanex/NEX-GDDP/BCSD/rcp85/day/atmos/tasmin/r1i1p1/v1.0/tasmin_day_BCSD_rcp85_r1i1p1_inmcm4_2099.nc"
      else "/tmp/tasmin_day_BCSD_rcp85_r1i1p1_inmcm4_2099.nc"
    val geojsonUri =
      if (args.size > 1) args(1)
      else "./geojson/CA.geo.json"
    val latLng =
      if (args.size > 2) args(2)
      else "33.897,-118.225"
    val Array(lat, lon) = latLng.split(",").map(_.toDouble)

    // Get first tile and NODATA value
    val ncfile = NetcdfFile.open(netcdfUri)
    val vs = ncfile.getVariables()
    val ucarType = vs.get(1).getDataType()
    val latArray = vs.get(1).read().get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
    val lonArray = vs.get(2).read().get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
    val attribs = vs.get(3).getAttributes()
    val nodata = attribs.get(0).getValues().getFloat(0)
    val wholeTile = {
      val tileData = vs.get(3).slice(0, 0)
      val Array(y, x) = tileData.getShape()
      val array = tileData.read().get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
      FloatUserDefinedNoDataArrayTile(array, x, y, FloatUserDefinedNoDataCellType(nodata)).rotate180.flipVertical
    }

    // Save first tile to disk as a PNG
    val histogram = StreamingHistogram.fromTile(wholeTile)
    val breaks = histogram.quantileBreaks(1<<15)
    val ramp = ColorRamps.BlueToRed.toColorMap(breaks)
    val png = wholeTile.renderPng(ramp).bytes
    dump(png, new File("/tmp/gddp.png"))

    // Get polygon
    val polygon =
      scala.io.Source.fromFile(geojsonUri, "UTF-8")
        .getLines
        .mkString
        .extractGeometries[Polygon]
        .head
    val polygonExtent = polygon.envelope

    // Get extent, slice positions, and tile size (in pixels) for the
    // area around the query polygon
    val xSliceStart = math.floor(4 * (polygonExtent.xmin - (-360 + 1/8.0))).toInt // start at 1/8 degrees and proceed in increments of 1/4 degrees
    val xSliceStop = math.ceil(4 * (polygonExtent.xmax - (-360 + 1/8.0))).toInt
    val ySliceStart = math.floor(4 * (polygonExtent.ymin - (-90 + 1/8.0))).toInt // start at (-90 + 1/8) degrees and proceed in increments of 1/4 degrees
    val ySliceStop = math.ceil(4 * (polygonExtent.ymax - (-90 + 1/8.0))).toInt
    val extent = Extent( // Probably only works in intersection of Northern and Western hemispheres
      lonArray(xSliceStart) + (-360 + 1/8.0),
      latArray(ySliceStart),
      lonArray(xSliceStop+1) + (-360 + 1/8.0),
      latArray(ySliceStop+1))
    val x = xSliceStop - xSliceStart + 1
    val y = ySliceStop - ySliceStart + 1

    // Get slice positions for the query point
    val xSlice = math.round(4 * (lon - (-360 + 1/8.0))).toInt
    val ySlice = math.round(4 * (lat - (-90 + 1/8.0))).toInt

    // Establish Spark Context
    val sparkConf = (new SparkConf())
      .setAppName("GDDP")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "geotrellis.spark.io.kryo.KryoRegistrator")
      .set("spark.kryo.unsafe", "true")
      .set("spark.rdd.compress", "true")
      .set("spark.ui.enabled", "false")
    val sparkContext = new SparkContext(sparkConf)
    implicit val sc = sparkContext

    // Get RDD of tiles for entire year
    val rdd = sc.parallelize(Range(0, 365))
      .mapPartitions({ itr =>
        val ncfile = NetcdfFile.open(netcdfUri)
        val tasmin = ncfile.getVariables().get(3)

        itr.map({ t =>
          val array = tasmin
            .read(s"$t,$ySliceStart:$ySliceStop,$xSliceStart:$xSliceStop")
            .get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
          FloatUserDefinedNoDataArrayTile(array, x, y, FloatUserDefinedNoDataCellType(nodata)).rotate180.flipVertical
        })
      })

    // Save first tile to disk as a PNG
    dump(rdd.first().renderPng(ramp).bytes, new File("/tmp/gddp1.png"))
    dump(rdd.first().mask(extent, polygon).renderPng(ramp).bytes, new File("/tmp/gddp2.png"))

    // Compute means, mins for the given query polygon
    val californias = rdd.map({ tile => tile.mask(extent, polygon) })
    val means = californias.map({ tile => MeanSummary.handleFullTile(tile).mean }).collect().toList
    val mins = californias.map({ tile => MinDoubleSummary.handleFullTile(tile) }).collect().toList

    // Get values for the given query point
    val values = sc.parallelize(Range(0, 365))
      .mapPartitions({ itr =>
        val ncfile = NetcdfFile.open(netcdfUri)
        val tasmin = ncfile.getVariables().get(3)

        itr.map({ t =>
          tasmin
            .read(s"$t,$ySlice,$xSlice")
            .getFloat(0)
        })
      })
      .collect()
      .toList

    sparkContext.stop

    println(s"MEANS: $means")
    println(s"MINS: $mins")
    println(s"VALUES: $values")
  }
}
