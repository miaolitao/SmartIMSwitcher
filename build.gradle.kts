import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.example.smartim"

// 版本管理：读取 version.properties 文件，支持自动递增
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
}
val major = versionProps.getProperty("major", "1").toInt()
val minor = versionProps.getProperty("minor", "0").toInt()
val patch = versionProps.getProperty("patch", "0").toInt()

// 判断是否为正式发布 (通过 -Prelease 参数)
val isRelease = project.hasProperty("release")
version = if (isRelease) "$major.$minor.$patch" else "$major.$minor.$patch-SNAPSHOT"

// 正式发布时自动递增 patch 版本
tasks.register("incrementVersion") {
    doLast {
        val newPatch = patch + 1
        versionProps.setProperty("major", major.toString())
        versionProps.setProperty("minor", minor.toString())
        versionProps.setProperty("patch", newPatch.toString())
        versionProps.store(versionPropsFile.outputStream(), "Auto-incremented version")
        println("Version incremented to $major.$minor.$newPatch")
    }
}

// 发布时先递增版本
tasks.named("buildPlugin") {
    if (isRelease) {
        finalizedBy("incrementVersion")
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
    
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

intellijPlatform {
    pluginConfiguration {
        name = "Smart IM Switcher"
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "999.*" // 允许在所有未来版本上安装
        }
    }

    publishing {
        // 根据需要配置发布信息
    }

    verifyPlugin {
        // 根据需要配置验证逻辑
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}
