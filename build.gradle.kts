group = "page.eiim.cubestats"
version = "v0"

plugins {
	// Apply the application plugin to add support for building a CLI application in Java.
	application
	java
	eclipse
}

repositories {
	// Use Maven Central for resolving dependencies.
	mavenCentral()
}

dependencies {
	implementation("com.google.code.gson:gson:2.13.1")
	implementation("org.mariadb.jdbc:mariadb-java-client:3.5.5")
	implementation("org.eclipse.jetty:jetty-server:12.1.1")
	implementation("org.eclipse.jetty.compression:jetty-compression-server:12.1.1")
	implementation("org.eclipse.jetty.http2:jetty-http2-server:12.1.1")
	implementation("org.eclipse.jetty:jetty-alpn-server:12.1.1")
	implementation("org.eclipse.jetty:jetty-alpn-java-server:12.1.1")
	implementation("org.eclipse.jetty.http3:jetty-http3-server:12.1.1")
	implementation("org.eclipse.jetty.quic:jetty-quic-quiche-server:12.1.1")
	implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
	implementation("org.apache.logging.log4j:log4j-core:2.25.1")
	implementation("org.apache.logging.log4j:log4j-api:2.25.1")
}

// Apply a specific Java toolchain to ease working on different environments.
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

    // Optionally include dependencies in the JAR (fat jar)
    // if you don't use a plugin like Shadow
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
