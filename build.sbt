import WebKeys._

name := "dogleg-web"

organization in ThisBuild := "org.dogleg"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.5"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Dependencies
libraryDependencies ++= Seq(
  cache, filters, jdbc, ws,

  // server-side
  "org.scaldi"                %% "scaldi"              % "0.5.3",
  "org.scaldi"                %% "scaldi-play"         % "0.5.3",
  "com.github.tototoshi"      %% "play-flyway"         % "1.2.1",
  "com.typesafe.play"         %% "play-slick"          % "0.8.1",
  "org.postgresql"            %  "postgresql"          % "9.4-1200-jdbc41",
  "com.github.tminglei"       %% "slick-pg"            % "0.8.1",
  "com.vividsolutions"        %  "jts"                 % "1.13",
  "com.lambdaworks"           %  "scrypt"              % "1.4.0",
  "com.github.nscala-time"    %% "nscala-time"         % "1.8.0",
  "com.sksamuel.scrimage"     %% "scrimage-core"       % "1.4.2",
  "com.sksamuel.scrimage"     %% "scrimage-canvas"     % "1.4.2",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",

  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.1.16",
  "org.webjars" % "angularjs" % "1.3.13" exclude("org.webjars", "jquery"),
  "org.webjars" % "cryptojs"  % "3.1.2",
  "org.webjars" % "lodash"    % "3.1.0",
  "org.webjars" % "momentjs"  % "2.9.0"
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

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

pipelineStages := Seq(digest, gzip) // TODO: rjs stage breaks angular injection somewhere

// RequireJS with sbt-rjs (https://github.com/sbt/sbt-rjs#sbt-rjs)
// ~~~
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))

RjsKeys.mainModule := "main"

RjsKeys.generateSourceMaps := false

ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "<empty>;controllers.javascript.*;controllers.ref.*;views.html.*"

// All work and no play...
emojiLogs
