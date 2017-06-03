package com.example.loca

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


object Loca {

  val logger = Logger.getLogger(Loca.getClass)

  /**
    * Main
    */
  def main(args: Array[String]) : Unit = {
    val uri =
      if (args.size > 0) args(0)
      else "s3://nasanex/LOCA/CCSM4/16th/rcp85/r6i1p1/tasmax/tasmax_day_CCSM4_rcp85_r6i1p1_21000101-21001231.LOCA_2016-04-02.16th.nc"
    val tiles =
      if (args.size > 1) args(1).toInt
      else 7

    // Establish Spark Context
    val sparkConf = (new SparkConf())
      .setAppName("LOCA")
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
        val tasmax = vs.get(1)
        val attribs = tasmax.getAttributes()
        val nodata = attribs.get(4).getValues().getFloat(0)

        itr.map({ n =>
          val ucarVariable = tasmax.slice(0, n)
          val ucarType = ucarVariable.getDataType()
          val Array(y, x) = ucarVariable.getShape()
          val array = ucarVariable.read().get1DJavaArray(ucarType).asInstanceOf[Array[Float]]
          FloatUserDefinedNoDataArrayTile(array, x, y, FloatUserDefinedNoDataCellType(nodata))
        })
      })

    rdd.foreach({ tasmax => println(tasmax) })

    sparkContext.stop
  }
}
