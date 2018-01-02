name := "gddp"
libraryDependencies ++= Seq(
  "org.locationtech.geotrellis" %% "geotrellis-raster"    % "1.2.0-RC2",
  "org.locationtech.geotrellis" %% "geotrellis-spark"     % "1.2.0-RC2",
  "org.apache.hadoop"            % "hadoop-client"        % Version.hadoop % "provided",
  "org.apache.spark"            %% "spark-core"           % Version.spark  % "provided"
)

fork in Test := false
parallelExecution in Test := false
