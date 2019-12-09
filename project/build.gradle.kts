import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler

plugins {
    id("kotlin-multiplatform") version "1.3.61"
    id("org.hidetake.ssh") version "2.10.1"
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    linuxArm32Hfp("chifoumi") {
        binaries {
            executable {
                entryPoint("chifoumi.robot.main")

                // libpigpio.so are compiled on raspberry pi and copied here
                linkerOpts("-Lsrc/lib/pigpio", "-lpigpio")

                // libPCA9685.so are compiled on raspberry pi and copied here
                linkerOpts("-Lsrc/lib/pca9685", "-lpca9685")

                linkerOpts("-Lsrc/lib/tensorflow", "-ltensorflow")
            }
        }

        compilations.getByName("main") {
            val pigpio by cinterops.creating {
                includeDirs("src/include/pigpio")
            }
            val pca9685 by cinterops.creating {
                includeDirs("src/include/pca9685")
            }
            val tensorflow by cinterops.creating {
                includeDirs("src/include/tensorflow")
            }
        }
    }
}

val pi = remotes.create("pi") {
    // your raspberry pi IP address
    host = "192.168.1.98"
    user = "pi"
    identity = file("${System.getProperty("user.home")}/.ssh/raspberry_pi")
}

tasks.register("deployOnPi") {
    description = "Copy the executable to the Raspberry Pi"
    doLast {
        ssh.runSessions {
            session(pi) {
                val sourceFile = "$buildDir/bin/chifoumi/releaseExecutable/${project.name}.kexe"
                val destinationDir = "/home/pi/qian/playground"
                put(sourceFile, destinationDir)
                execute("chmod u+x $destinationDir/${project.name}.kexe")
            }
        }
    }
}

// https://github.com/int128/gradle-ssh-plugin/issues/317#issuecomment-547665723
fun Service.runSessions(action: RunHandler.() -> Unit) =
        run(delegateClosureOf(action))

fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
        session(*remotes, delegateClosureOf(action))

fun SessionHandler.put(from: Any, into: Any) =
        put(hashMapOf("from" to from, "into" to into))
