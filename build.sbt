import sbtassembly.MergeStrategy

excludeLintKeys in Global ++= Set(idePackagePrefix)


inThisBuild {
  List(
    organization := "org.aulune",
    scalaVersion := "3.3.6",
    semanticdbEnabled := true,
    version := "0.1.0-SNAPSHOT",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Wnonunit-statement",
      "-Werror",
      "-Xmax-inlines:64",
    ),
    assembly / assemblyMergeStrategy := mergeStrategy,
  )
}


def mergeStrategy: String => MergeStrategy = {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList(
         "META-INF",
         "maven",
         "org.webjars",
         "swagger-ui",
         "pom.properties") => MergeStrategy.singleOrError
  case PathList("META-INF", "resources", "webjars", "swagger-ui", _*) =>
    MergeStrategy.singleOrError
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "module-info.class"      => MergeStrategy.discard
  case x                        => MergeStrategy.defaultMergeStrategy(x)
}


lazy val app = (project in file("."))
  .aggregate(commons, auth, permissions, aggregator)
  .dependsOn(auth, permissions, aggregator)
  .settings(
    name := "app",
    idePackagePrefix := Some("org.aulune"),
    assembly / mainClass := Some("org.aulune.App"),
    libraryDependencies ++= http4sDeps ++ tapirDeps ++ doobieDeps ++ Seq(
      "ch.qos.logback"         % "logback-classic" % logbackVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,
      "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
      "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
    ),
  )


lazy val commons = (project in file("commons")).settings(
  name := "commons",
  idePackagePrefix := Some("org.aulune.commons"),
  libraryDependencies ++= testDeps ++ tapirDeps ++ circeDeps ++ doobieDeps ++ Seq(
    "co.fs2"       %% "fs2-core"                        % fs2Version,
    "com.dimafeng" %% "testcontainers-scala-minio"      % testcontainersVersion,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersVersion,
    "com.dimafeng" %% "testcontainers-scala-scalatest"  % testcontainersVersion,
    "org.postgresql" % "postgresql" % postgresqlVersion,
    "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorsVersion,
    "org.scalamock" %% "scalamock" % scalamockVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion,
    "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
    "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
    "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectTestingVersion,
    minioDep,
  ),
)


lazy val auth = (project in file("auth"))
  .dependsOn(commons)
  .settings(
    name := "auth",
    idePackagePrefix := Some("org.aulune.auth"),
    libraryDependencies ++= testDeps ++ http4sDeps ++ tapirDeps ++ circeDeps ++ doobieDeps ++ Seq(
      "com.github.jwt-scala" %% "jwt-circe"  % jwtVersion,
      "de.mkammerer"          % "argon2-jvm" % argon2Version,
      "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
      "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
      "org.typelevel" %% "log4cats-core"   % log4catsVersion,
      "com.nimbusds"   % "nimbus-jose-jwt" % nimbusJoseJwt,
      http4sClient,
    ),
  )


lazy val permissions = (project in file("permissions"))
  .dependsOn(commons)
  .settings(
    name := "permissions",
    idePackagePrefix := Some("org.aulune.permissions"),
    libraryDependencies ++= testDeps ++ doobieDeps ++ Seq(
      "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
      "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
      "org.typelevel" %% "log4cats-core" % log4catsVersion,
    ),
  )


lazy val aggregator = (project in file("aggregator"))
  .dependsOn(commons)
  .settings(
    name := "aggregator",
    idePackagePrefix := Some("org.aulune.aggregator"),
    libraryDependencies ++= tapirDeps ++ circeDeps ++ doobieDeps ++ Seq(
      "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc (),
      "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
      "org.typelevel" %% "log4cats-core" % log4catsVersion,
      "co.fs2"        %% "fs2-core"      % fs2Version,
      minioDep,
    ) ++ testDeps :+ (http4sClient % Test),
  )


val argon2Version = "2.12"
val catsEffectTestingVersion = "1.6.0"
val catsEffectVersion = "3.6.3"
val catsVersion = "2.13.0"
val circeGenericExtras = "0.14.5-RC1"
val circeVersion = "0.14.14"
val doobieVersion = "1.0.0-RC9"
val fs2Version = "3.12.2"
val http4sVersion = "0.23.30"
val jwtVersion = "11.0.2"
val log4catsVersion = "2.7.1"
val logbackVersion = "1.5.18"
val minioVersion = "8.5.17"
val nimbusJoseJwt = "10.4.2"
val parserCombinatorsVersion = "2.4.0"
val postgresqlVersion = "42.7.7"
val pureconfigVersion = "0.17.9"
val scalamockVersion = "7.4.1"
val scalatestVersion = "3.2.19"
val sqliteVersion = "3.50.3.0"
val tapirVersion = "1.11.40"
val testcontainersVersion = "0.43.0"

resolvers += Resolver.sonatypeCentralSnapshots

val http4sClient = "org.http4s" %% "http4s-ember-client" % http4sVersion


val http4sDeps = Seq(
  "org.http4s" %% "http4s-ember-server",
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-circe",
).map(_ % http4sVersion)


val circeDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion) ++ Seq(
  "io.circe" %% "circe-generic-extras" % circeGenericExtras,
)


val tapirDeps = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle",
).map(_ % tapirVersion)


val doobieDeps = Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari",
  "org.tpolecat" %% "doobie-postgres",
  "org.tpolecat" %% "doobie-postgres-circe",
).map(_ % doobieVersion)


val minioDep = "io.minio" % "minio" % minioVersion


val testDeps = Seq(
  "ch.qos.logback" % "logback-classic"                 % logbackVersion,
  "com.dimafeng"  %% "testcontainers-scala-postgresql" % testcontainersVersion,
  "com.dimafeng"  %% "testcontainers-scala-scalatest"  % testcontainersVersion,
  "org.postgresql" % "postgresql"                      % postgresqlVersion,
  "org.scalamock" %% "scalamock"                       % scalamockVersion,
  "org.scalatest" %% "scalatest"                       % scalatestVersion,
  "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectTestingVersion,
  "org.typelevel" %% "log4cats-slf4j"                % log4catsVersion,
).map(_ % Test)
