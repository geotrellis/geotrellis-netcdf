import sbtassembly.PathList

lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0.22.7",
  scalaVersion := "2.11.11",
  test in assembly := {},
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-language:experimental.macros",
    "-feature"),
  assemblyMergeStrategy in assembly := {
    case "log4j.properties" => MergeStrategy.first
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) =>
      xs match {
        case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
        case ("services" :: _ :: Nil) =>
          MergeStrategy.concat
        case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
          MergeStrategy.concat
        case (name :: Nil) => {
          if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))
            MergeStrategy.discard
          else
            MergeStrategy.first
        }
        case _ => MergeStrategy.first
      }
    case _ => MergeStrategy.first
  },
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  resolvers  += Resolver.mavenLocal,
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.92",
    "edu.ucar" % "cdm" % "5.0.0-SNAPSHOT",
    "org.apache.hadoop" % "hadoop-client" % "2.7.3" exclude("javax.servlet", "servlet-api")
  ),
  initialCommands in console +=
"""
import ucar.nc2._
import ucar.unidata.io.s3._
"""
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)

lazy val gddp = (project in file("gddp"))
  .dependsOn(root)
  .settings(commonSettings: _*)

lazy val loca = (project in file("loca"))
  .dependsOn(root)
  .settings(commonSettings: _*)
