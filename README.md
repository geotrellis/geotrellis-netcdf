# Introduction #

This repository contains an example project that demonstrates how to read NetCDF data into a Spark/Scala program using [NetCDF Java](https://github.com/Unidata/thredds/) and manipulate the data using [GeoTrellis](https://github.com/locationtech/geotrellis).
The ability to easily and efficiently read NetCDF data into a GeoTrellis program opens the possibility for those who are familiar with GeoTrellis and its related and surrounding tools to branch into climate research, and also makes it possible for climate researchers to take advantage of the many benefits that GeoTrellis can provide.
Because GeoTrellis is a raster-oriented library, the approach that is demonstrated in this repository is to use the NetCDF library to load and query datasets and present the results as Java arrays which can be readily turned into GeoTrellis tiles.
Once the data have been transformed into GeoTrellis tiles, they can be masked, summarized, and/or manipulated like any other GeoTrellis raster data.
The results of that are shown below.

# Compiling And Running #

## Dependencies ##

This code relies on two main dependencies to do most of the work: NetCDF Java and GeoTrellis.

### NetCDF Java ###

Any [recent snapshot of 5.0.0 branch of NetCDF Java code](https://github.com/Unidata/thredds/tree/5.0.0) should be sufficient to compile and run this code,
but if you would like to be able to read data directly from S3 and/or HDFS, you must compile and locally-publish a [partiular feature branch](https://github.com/Unidata/thredds/tree/feature/s3+hdfs) contributed by the present author.
To compile and locally-publish the feature branch, try something like the following:

```bash
git clone 'git@github.com:Unidata/thredds.git'
cd thredds/
git fetch origin 'feature/s3+hdfs:feature/s3+hdfs'
./gradlew assemble
./gradlew publishToMavenLocal
```

If you do not want or need the S3 and/or HDFS capability, pulling this dependency from the maintainer's [Maven repository](http://artifacts.unidata.ucar.edu/) should work.

### GeoTrellis ###

This code requires a [1.2.0-SNAPSHOT](https://github.com/locationtech/geotrellis) or later version of GeoTrellis.
That is due to the fact that recently-added tile transformation functions are used in this code which are not present in earlier version of GeoTrellis.
To compile and locally-publish GeoTrellis, try something like the following:

```bash
git clone 'git@github.com:locationtech/geotrellis.git'
cd geotrellis/
./scripts/publish-local.sh
```
## Compile ##

With the dependencies in place, compiling the code in this repository is straightforward:

```bash
sbt "project gddp" assembly
```

## Run ##

To run the code from the root directory of the project, something like this should work:

```bash
$SPARK_HOME/bin/spark-submit --master 'local[*]' gddp/target/scala-2.11/gddp-assembly-0.22.7.jar /tmp/gddp.nc /tmp/political_boundary.json '32.856388889,90.4075'
```

where the first argument (after the name of the jar file) is the name of a GDDP NetCDF file, the second argument is the name of a file that contains a polygon in GeoJSON format, and the third argument is a latitude-longitude pair.

# Structure Of This Code #

# Example #

# Future Work #

