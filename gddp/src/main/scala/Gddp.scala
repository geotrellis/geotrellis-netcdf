package com.example.gddp

import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import ucar.nc2._


object Gddp {

  val logger = Logger.getLogger(Gddp.getClass)

  /**
    * Main
    */
  def main(args: Array[String]) : Unit = {
    val uri =
      if (args.size > 0) args(0)
      else "s3://nasanex/NEX-GDDP/BCSD/rcp85/day/atmos/tasmin/r1i1p1/v1.0/tasmin_day_BCSD_rcp85_r1i1p1_inmcm4_2099.nc"
    val tiles =
      if (args.size > 1) args(1).toInt
      else 7

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

    val rdd = sc.parallelize(Range(0, tiles))
      .mapPartitions({ itr =>
        val ncfile = NetcdfFile.open(uri)
        val vs = ncfile.getVariables()
        val tasmin = vs.get(3)
        val attribs = tasmin.getAttributes()
        val nodata = attribs.get(0).getValues().getFloat(0)

        itr.map({ n =>
          val ucarVariable = tasmin.slice(0, n)
          val ucarType = ucarVariable.getDataType()
          val Array(x, y) = ucarVariable.getShape()
          val array = ucarVariable.read().get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
          FloatUserDefinedNoDataArrayTile(array, x, y, FloatUserDefinedNoDataCellType(nodata))
        })
      })

    rdd.foreach({ tasmin => println(tasmin) })

    sparkContext.stop
  }
}
