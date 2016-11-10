name := "DirTreeEncryptor"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies += "org.bouncycastle" % "bcpg-jdk15on" % "1.55"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0"
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19"

scalacOptions += "-unchecked"
scalacOptions += "-deprecation"
scalacOptions += "-feature"