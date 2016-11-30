import com.github.retronym.SbtOneJar._

name := "DirTreeEncryptor"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies += "org.bouncycastle" % "bcpg-jdk15on" % "1.55"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0"
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19"
libraryDependencies += "commons-cli" % "commons-cli" % "1.3.1"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

scalacOptions += "-unchecked"
scalacOptions += "-deprecation"
scalacOptions += "-feature"

oneJarSettings