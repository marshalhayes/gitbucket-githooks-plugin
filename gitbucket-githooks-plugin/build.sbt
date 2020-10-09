name := "gitbucket-githooks-plugin"
organization := "io.github.gitbucket"
version := "0.1.1"
scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
    "io.github.gitbucket" %% "gitbucket"         % "4.33.0" % "provided",
    "javax.servlet"        % "javax.servlet-api" % "3.1.0"  % "provided"
)

resolvers ++= Seq(
    Classpaths.typesafeReleases,
    Resolver.jcenterRepo,
    "sonatype-snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/"
)
