import WebKeys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart}
import NativePackagerKeys._

import ReleaseTransformations._

// basics
name := "dogleg-web"
organization in ThisBuild := "org.cranst0n.dogleg"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
scalaVersion := "2.11.7"

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

// sbt-native-packager
maintainer in Linux := "Ian McIntosh <cranston.ian@gmail.com>"
packageSummary in Linux := "Backend web server for Dogleg."
packageDescription := "Dogleg is a golf data management system to record course and round data with GPS tracking and more."
serverLoading in Debian := SystemV

// Temporary fix for installed config always being overwritten on package install
// See: https://github.com/sbt/sbt-native-packager/issues/378
mappings in Universal := {
  (mappings in Universal).value map {
    case (file,name) if name startsWith "conf//" => (file, "conf/" + name.substring(6))
    case (file,name) => (file,name)
  }
}

// sbt-release
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  releaseTask(packageBin in Debian),      // : ReleaseStep, creates Debian package
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

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

// Dependencies
resolvers ++= Seq(
  Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns),
  "google-sedis-fix" at "http://pk11-scratch.googlecode.com/svn/trunk"
)

libraryDependencies ++= Seq(
  cache, filters, jdbc, ws,

  // server-side
  "org.scaldi"                %% "scaldi"              % "0.5.6",
  "org.scaldi"                %% "scaldi-play-23"      % "0.5.6",
  "com.github.tototoshi"      %% "play-flyway"         % "1.2.1",
  "com.typesafe.play"         %% "play-slick"          % "0.8.1",
  "org.postgresql"            %  "postgresql"          % "9.4-1201-jdbc41",
  "com.github.tminglei"       %% "slick-pg"            % "0.8.5",
  "com.edulify"               %% "play-hikaricp"       % "2.0.4",
  "com.vividsolutions"        %  "jts"                 % "1.13",
  "com.lambdaworks"           %  "scrypt"              % "1.4.0",
  "com.github.nscala-time"    %% "nscala-time"         % "2.0.0",
  "com.sksamuel.scrimage"     %% "scrimage-core"       % "1.4.2",
  "com.sksamuel.scrimage"     %% "scrimage-canvas"     % "1.4.2",
  "com.typesafe.play.plugins" %% "play-plugins-redis"  % "2.3.1",
  "com.typesafe.play"         %% "play-mailer"         % "2.4.1",

  // Using this to support JSON format macro for case classes with > 22 params
  "org.cvogt"                 %% "play-json-extensions" % "0.2",

  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.1.18",
  "org.webjars" % "angularjs" % "1.4.2" exclude("org.webjars", "jquery"),
  "org.webjars" % "cryptojs"  % "3.1.2",
  "org.webjars" % "lodash"    % "3.9.0",
  "org.webjars" % "momentjs"  % "2.10.3",
  "org.webjars" % "angular-material" % "0.8.3"
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
