group = "page.eiim.cubestats"
version = "v0"

plugins {
	application
	java
	eclipse
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("com.google.code.gson:gson:2.13.2")
	implementation("org.mariadb.jdbc:mariadb-java-client:3.5.8")
	implementation("org.eclipse.jetty:jetty-server:12.1.8")
	implementation("org.eclipse.jetty.compression:jetty-compression-server:12.1.8")
	implementation("org.eclipse.jetty.http2:jetty-http2-server:12.1.8")
	implementation("org.eclipse.jetty:jetty-alpn-server:12.1.8")
	implementation("org.eclipse.jetty:jetty-alpn-java-server:12.1.8")
	implementation("org.eclipse.jetty.http3:jetty-http3-server:12.1.8")
	implementation("org.eclipse.jetty.quic:jetty-quic-quiche-server:12.1.8")
	implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.4")
	implementation("org.apache.logging.log4j:log4j-core:2.25.4")
	implementation("org.apache.logging.log4j:log4j-api:2.25.4")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

tasks.jar {
    archiveBaseName.set("CubeStats")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "page.eiim.cubestats.Main"
        )
    }

    // Include dependencies in the JAR (fat jar)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
	
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
	mainClass = "page.eiim.cubestats.Main"
}
