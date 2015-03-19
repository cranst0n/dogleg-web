import WebKeys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart}

import NativePackagerKeys._

import sbtrelease._
import ReleaseStateTransformations._

// basics
name := "dogleg-web"
organization in ThisBuild := "org.dogleg"
version := "0.1-M1"

// sbt-buildinfo
buildInfoSettings
sourceGenerators in Compile <+= buildInfo
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "build"
buildInfoKeys ++= Seq[BuildInfoKey](
  BuildInfoKey.action("buildTime") {
    System.currentTimeMillis
  } // re-computed each time at compile
)

// sbt-release
releaseSettings
ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

// sbt-native-packager
maintainer in Linux := "Ian McIntosh <cranston.ian@gmail.com>"
packageSummary in Linux := "Backend web server for Dogleg."
packageDescription := "Dogleg is a golf data management system to record course and round data with GPS tracking and more."
serverLoading in Debian := SystemV

scalaVersion := "2.11.6"

// Scala Compiler Options
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation",         // warning and location for usages of deprecated APIs
  "-feature",             // warning and location for usages of features that should be imported explicitly
  "-unchecked",           // additional warnings where generated code depends on assumptions
  "-Xlint",               // recommended additional warnings
  "-Ywarn-adapted-args",  // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

addCommandAlias("deb", "debian:packageBin")

// Dependencies
resolvers ++= Seq(
  Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns),
  "google-sedis-fix" at "http://pk11-scratch.googlecode.com/svn/trunk"
)

libraryDependencies ++= Seq(
  cache, filters, jdbc, ws,

  // server-side
  "org.scaldi"                %% "scaldi"              % "0.5.4",
  "org.scaldi"                %% "scaldi-play"         % "0.5.3",
  "com.github.tototoshi"      %% "play-flyway"         % "1.2.1",
  "com.typesafe.play"         %% "play-slick"          % "0.8.1",
  "org.postgresql"            %  "postgresql"          % "9.4-1201-jdbc41",
  "com.github.tminglei"       %% "slick-pg"            % "0.8.4",
  "com.edulify"               %% "play-hikaricp"       % "2.0.1",
  "com.vividsolutions"        %  "jts"                 % "1.13",
  "com.lambdaworks"           %  "scrypt"              % "1.4.0",
  "com.github.nscala-time"    %% "nscala-time"         % "1.8.0",
  "com.sksamuel.scrimage"     %% "scrimage-core"       % "1.4.2",
  "com.sksamuel.scrimage"     %% "scrimage-canvas"     % "1.4.2",
  "com.typesafe.play.plugins" %% "play-plugins-redis"  % "2.3.1",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",

  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.1.16",
  "org.webjars" % "angularjs" % "1.3.14" exclude("org.webjars", "jquery"),
  "org.webjars" % "cryptojs"  % "3.1.2",
  "org.webjars" % "lodash"    % "3.3.1",
  "org.webjars" % "momentjs"  % "2.9.0"
)

// sbt-rjs (sbt-web)
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
pipelineStages := Seq(digest, gzip) // TODO: rjs stage breaks angular injection somewhere
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))
RjsKeys.mainModule := "main"
RjsKeys.generateSourceMaps := false

// scoverage
ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "<empty>;controllers.javascript.*;controllers.ref.*;views.html.*"

// All work and no play...
emojiLogs
