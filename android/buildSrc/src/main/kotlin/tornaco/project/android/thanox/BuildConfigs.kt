package tornaco.project.android.thanox

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.net.InetAddress
import java.util.*

private val props = Properties()

object Configs {
    const val compileSdkVersion = 30
    const val minSdkVersion = 23
    const val targetSdkVersion = 30
    const val buildToolsVersion = "30.0.3"
    const val testRunner = "androidx.test.runner.AndroidJUnitRunner"

    var thanoxVersionCode: Int? = 0
    var thanoxVersionName: String? = null

    var thanoxAppId: String? = null
    var thanoxBuildHostName: String? = null
    var thanoxBuildFlavor: String? = null
    var thanoxBuildVariant: String? = null
    var thanoxBuildIsDebug: Boolean? = false
    var thanoxBuildIsRow: Boolean? = false

    val thanoxAppIdPrefix: String get() = "github.tornaco.android.thanos"
    val thanoxBuildFP: String get() = "Thanox@tornaco:${UUID.randomUUID().toString()}"
    val thanoxShortcutAppIdPrefix: String get() = "github.tornaco.android.thanos.shortcut"


    val Project.resPrefix: String get() = "${this.name}_"

    val Project.outDir: File get() = rootProject.file("out")

    operator fun get(key: String): String? {
        val v = props[key] as? String ?: return null
        return if (v.isBlank()) null else v
    }
}

enum class Flavors {
    ROW,
    PRC
}

enum class Variant {
    DEBUG,
    RELEASE
}

object MagiskModConfigs {
    /*
       This name will be used in the name of the so file ("lib${moduleLibraryName}.so").
       If this module need to support Riru pre-v24 (API < 24), moduleLibraryName must start with "riru_".
    */
    const val moduleLibraryName = "riru_thanox"

    /* Minimal supported Riru API version, used in the version check of riru.sh */
    const val moduleMinRiruApiVersion = 25

    /* The version name of minimal supported Riru, used in the version check of riru.sh */
    const val moduleMinRiruVersionName = "v22.0"

    /* Maximum supported Riru API version, used in the version check of riru.sh */
    const val moduleRiruApiVersion = 25

    /*
       Magisk module ID
       Since Magisk use it to distinguish different modules, you should never change it.
       Note, the older version of the template uses '-' instead of '_', if your are upgrading from
       the older version, please pay attention.
    */
    const val magiskModuleId = "riru_thanox"

    const val moduleName = "Thanox-Core"
    const val moduleAuthor = "Tornaco"
    const val moduleDescription =
        "Provide android framework and app hooks for thanox, " +
                "requires Riru $moduleMinRiruVersionName or above(需要安装riru). " +
                "Only support Android11(只支持Android11)"
    val moduleVersion = Configs.thanoxVersionName
    val moduleVersionCode = Configs.thanoxVersionCode
}

@ExperimentalStdlibApi
class ThanoxProjectBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) = project.applyPlugin()

    private fun Project.applyPlugin() {
        props.clear()
        rootProject.file("gradle.properties").inputStream().use { props.load(it) }
        rootProject.file("local.properties").inputStream().use { props.load(it) }

        updateBuildConfigFields()
    }

    private fun Project.updateBuildConfigFields() {
        Configs.thanoxBuildHostName = InetAddress.getLocalHost().hostName
        Configs.thanoxBuildFlavor = getBuildFlavor().name.lowercase()
        Configs.thanoxBuildVariant = getBuildVariant().name.lowercase()
        Configs.thanoxBuildIsDebug = Configs.thanoxBuildVariant == "debug"
        val isRow = Configs.thanoxBuildFlavor == "row"
        Configs.thanoxBuildIsRow = isRow
        Configs.thanoxAppId =
            if (isRow) "$${Configs.thanoxAppIdPrefix}.pro" else Configs.thanoxAppIdPrefix

        log("thanoxBuildHostName: ${Configs.thanoxBuildHostName}")
        log("thanoxBuildFlavor: ${Configs.thanoxBuildFlavor}")
        log("thanoxBuildVariant: ${Configs.thanoxBuildVariant}")
        log("thanoxBuildIsDebug: ${Configs.thanoxBuildIsDebug}")
        log("thanoxAppId: ${Configs.thanoxAppId}")
    }

    private fun Project.getBuildFlavor(): Flavors {
        val tskReqStr = gradle.startParameter.taskRequests.map {
            it.args
        }.toString()
        log("tskReqStr: $tskReqStr")
        val isRow = tskReqStr.contains(Flavors.ROW.name, ignoreCase = true)
        return if (isRow) {
            Flavors.ROW
        } else {
            Flavors.PRC
        }
    }

    private fun Project.getBuildVariant(): Variant {
        val tskReqStr = gradle.startParameter.taskRequests.map {
            it.args
        }.toString()
        log("tskReqStr: $tskReqStr")
        val isDebug = tskReqStr.contains(Variant.DEBUG.name, ignoreCase = true)
        return if (isDebug) {
            Variant.DEBUG
        } else {
            Variant.RELEASE
        }
    }
}