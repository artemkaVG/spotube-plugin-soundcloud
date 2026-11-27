/*
 * Copyright (C) 2026 artemkaVG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.spotube.PluginAbility
import org.spotube.PluginCapability
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.zipline.gradle.plugin)
    alias(libs.plugins.spotubeGradle)
}

spotubePlugin {
    name = "SoundCloud"
    version = "0.1.0"
    apiVersion = "0.1.0"
    description = "SoundCloud catalog for Spotube without an account: charts, search, artists, albums and playlists"
    author = "artemkaVG"
    capabilities = listOf(PluginCapability.NETWORK_REQUESTS)
    abilities = listOf(PluginAbility.METADATA)
    cookieDomains = emptyList()
    license = "AGPL-3.0-or-later"
    contact = "https://github.com/artemkaVG"
    repository = "https://github.com/artemkaVG/spotube-plugin-soundcloud"
    bugs = "https://github.com/artemkaVG/spotube-plugin-soundcloud/issues"
}

kotlin {
    applyDefaultHierarchyTemplate()

    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":soundcloud_client"))
            api(libs.zipline.core)
            implementation(libs.spotube.plugin.interfaces)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.semver)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

zipline {
    mainFunction.set("io.github.artemkavg.spotube_plugin_soundcloud.main")
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().yarnLockAutoReplaceProperty.set(true)
}
