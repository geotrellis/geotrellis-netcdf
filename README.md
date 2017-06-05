# Introduction #

This repository contains an example project that demonstrates how to read NetCDF data into a Spark/Scala program using [NetCDF Java](https://github.com/Unidata/thredds/) and manipulate the data using [GeoTrellis](https://github.com/locationtech/geotrellis).
The ability to easily and efficiently read NetCDF data into a GeoTrellis program opens the possiblity for those who are familiar with GeoTrellis and its related and surrounding tools to branch into climate research, and also makes it possible for climate researchers to take advantage of the many benefits that GeoTrellis can provide.
Because GeoTrellis is a raster-oriented library, the approach that is demonstrated in this repository is to use the NetCDF library to load and query datasets and present the results as Java arrays which can be readily turned into GeoTrellis tiles.
Once the data have been transformed into GeoTrellis tiles, they can be masked, summarized, and/or manipulated like any other GeoTrellis raster data.
The results of that are shown below.

# Dependencies #

# This Code #

# Example #

# Future Work #

