import java.awt.GridLayout
import javax.swing.*

plugins {
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "0.12.0"
  `maven-publish`
  jacoco
  id("pl.droidsonroids.jacoco.testkit") version "1.0.7"
  id("org.sonarqube") version "3.0"
}

group = "edu.umich.med.michr"
version = "0.1-SNAPSHOT"
description = "A Gradle plugin for running the H2 database."

repositories {
  jcenter()
}

dependencies {
  implementation("com.h2database:h2:1.4.200")
  testImplementation(gradleTestKit())
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
  withJavadocJar()
  withSourcesJar()
}

tasks.compileJava {
  options.compilerArgs = listOf("-Xlint:all")
}

tasks.compileTestJava {
  sourceCompatibility = "11"
  targetCompatibility = "11"
  options.compilerArgs = listOf("-Xlint:all")
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    csv.isEnabled = false
    html.isEnabled = true
    xml.isEnabled = true
  }
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = "0.9".toBigDecimal()
      }
    }
  }
}

tasks.sonarqube {
  dependsOn(tasks.jacocoTestReport)
}

tasks.check {
  dependsOn(tasks.jacocoTestCoverageVerification)
}

gradle.taskGraph.whenReady {
  if (hasTask(":sonarqube")) {
    val sonarTokenEnvironment = System.getenv("SONAR_TOKEN")
    val sonarTokenProperty = System.getProperty("sonar.login")
    val sonarToken: String

    // Check for a Sonarqube token first in System environment, then Gradle system property, then prompt user
    if (sonarTokenEnvironment != null) {
      sonarToken = sonarTokenEnvironment
    } else if (sonarTokenProperty != null) {
      sonarToken = sonarTokenProperty
    } else {
      // We don't have a Sonarqube token stored, so we are prompting the user

      // Create the labels and text fields.
      val sonarTokenLabel = JLabel("Sonar Token: ", JLabel.RIGHT)
      val sonarTokenField: JTextField = JPasswordField("", 25)

      // Create the panels and configure the layout
      val sonarqubePanel = JPanel(false)
      sonarqubePanel.layout = BoxLayout(sonarqubePanel, BoxLayout.X_AXIS)
      val labelPanel = JPanel(false)
      labelPanel.layout = GridLayout(0, 1)
      labelPanel.add(sonarTokenLabel)
      val fieldPanel = JPanel(false)
      fieldPanel.layout = GridLayout(0, 1)
      fieldPanel.add(sonarTokenField)
      sonarqubePanel.add(labelPanel)
      sonarqubePanel.add(fieldPanel)

      // Enter Sonarqube token or quit
      if (JOptionPane.showOptionDialog(null, sonarqubePanel,
              "Sonarqube login",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE,
              null, arrayOf("Okay", "Cancel"),
              "Okay") != 0) {
        throw StopExecutionException("User canceled sonarqube")
      }

      sonarToken = sonarTokenField.text

      if (sonarToken.isBlank()) {
        throw StopExecutionException("You must provide a valid Sonarqube token!")
      }
    }

    project.sonarqube.properties {
      property("sonar.login", sonarToken)
    }
  }
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.organization", "umich-michr")
    property("sonar.projectKey", "umich-michr_h2-gradle-plugin")
    property("sonar.projectName", "Gradle H2 plugin")
  }
}

// Configure the java-gradle-plugin. Note that the ID must match it's Gradle Plugin Portal id.
gradlePlugin {
  plugins {
    create("h2Plugin") {
      id = "edu.umich.med.michr.h2-plugin"
      displayName = "Gradle H2 plugin"
      description = project.description
      implementationClass = "edu.umich.med.michr.gradle.H2Plugin"
    }
  }
}

// Configuration for publishing to the Gradle plugins portal
pluginBundle {
  website = "https://github.com/umich-michr/h2-gradle-plugin"
  vcsUrl = "https://github.com/umich-michr/h2-gradle-plugin.git"
  description = project.description
  tags = listOf("gradle", "h2", "database")
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      pom {
        name.set("Gradle H2 plugin")
        description.set(project.description)
        url.set("https://github.com/umich-michr/h2-gradle-plugin")
        packaging = "jar"
        licenses {
          license {
            name.set("The MIT License")
            url.set("http://www.opensource.org/licenses/mit-license.php")
          }
        }
        developers {
          developer {
            id.set("raymojos")
            name.set("Joshua Raymond")
            email.set("raymojos@med.umich.edu")
          }
        }
        scm {
          connection.set("scm:git:https://github.com/umich-michr/h2-gradle-plugin.git")
          developerConnection.set("scm:git:https://github.com/umich-michr/h2-gradle-plugin.git")
          url.set("https://github.com/umich-michr/h2-gradle-plugin")
        }
      }
    }
  }
}