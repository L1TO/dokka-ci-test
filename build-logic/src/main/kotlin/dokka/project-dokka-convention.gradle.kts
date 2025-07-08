package dokka

import com.android.build.gradle.BaseExtension
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

val suggestGlobalDokkaIncludesProvider = tasks.register<GlobalSuggestDokkaIncludePatternsTask>("suggestGlobalDokkaIncludes") {
    project.subprojects.forEach { subproject ->
        subproject.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val kotlinExtension = subproject.extensions.findByType(KotlinProjectExtension::class.java)
            kotlinExtension?.sourceSets?.matching {
                !it.name.contains("test", ignoreCase = true) &&
                        (it.name.endsWith("Main", ignoreCase = true) ||
                                it.name.startsWith("common", ignoreCase = true) ||
                                it.name == "main")
            }?.forEach { kotlinSourceSet ->
                allSourceDirectories.from(kotlinSourceSet.kotlin.srcDirs.filter(
                    existingAndDirectoryFilter
                ))
            }
        }

        subproject.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kmpExtension = subproject.extensions.findByType(KotlinMultiplatformExtension::class.java)
            kmpExtension?.sourceSets?.matching { kotlinSourceSet ->
                !kotlinSourceSet.name.contains("test", ignoreCase = true) &&
                        !kotlinSourceSet.name.contains("benchmark", ignoreCase = true)
            }?.forEach { kotlinSourceSet ->
                allSourceDirectories.from(kotlinSourceSet.kotlin.srcDirs.filter(
                    existingAndDirectoryFilter
                ))
            }
        }

        val handleAndroidPlugin: (AppliedPlugin) -> Unit = { _ ->
            val androidExtension = subproject.extensions.findByType(BaseExtension::class.java)
            androidExtension?.sourceSets?.matching { it.name == "main" }
                ?.forEach { androidSourceSet ->
                    allSourceDirectories.from(androidSourceSet.java.srcDirs.filter(
                        existingAndDirectoryFilter
                    ))
                    val kotlinSpecificDir = subproject.file("src/${androidSourceSet.name}/kotlin")
                    if (existingAndDirectoryFilter(kotlinSpecificDir)) {
                        allSourceDirectories.from(kotlinSpecificDir)
                    }
                }
        }
        subproject.pluginManager.withPlugin("com.android.library", handleAndroidPlugin)
        subproject.pluginManager.withPlugin("com.android.application", handleAndroidPlugin)
    }

    significantKdocElements.set(sharedGlobalSignificantKdocElements)
    outputFile.set(project.layout.projectDirectory.file("documentation/global_all_patterns.list"))
}

val globalIncludePatternsProvider: Provider<List<String>> = suggestGlobalDokkaIncludesProvider.flatMap { task ->
    task.outputFile.map { file ->
        if (file.asFile.exists()) file.asFile.readLines().filter { it.isNotBlank() } else emptyList()
    }
}

project.subprojects {
    plugins.withId("org.jetbrains.dokka") {
        tasks.withType<AbstractDokkaLeafTask>().configureEach {
            dependsOn(suggestGlobalDokkaIncludesProvider)

            this@subprojects.rootProject.findProject(":shared")?.let { sharedModule ->
                DOKKA_SHARED_MODULE_DEPENDENCY_TASK_NAMES.forEach { taskName ->
                    sharedModule.tasks.findByName(taskName)?.let { dependencyTask ->
                        dependsOn(dependencyTask)
                    }
                }
            }

            dokkaSourceSets.configureEach {
                val dokkaSourceSetName = this.name
                val customDisplayName = generateCustomDokkaDisplayName(
                    dokkaSourceSetName = dokkaSourceSetName,
                    projectName = this@subprojects.name
                )

                configureDokkaSourceSet(
                    sourceSetBuilder = this,
                    gradleProject = this@subprojects,
                    kotlinSourceSetName = dokkaSourceSetName,
                    customDisplayName = customDisplayName,
                    includePatternsProvider = globalIncludePatternsProvider
                )
            }
        }
    }
}

tasks.named<DokkaMultiModuleTask>("dokkaHtmlMultiModule") {
    outputDirectory.set(project.layout.projectDirectory.dir("documentation/dokka-documentation"))
    dependsOn(suggestGlobalDokkaIncludesProvider)
    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {

        footerMessage = "Сlonelab"
        suppressInheritedMembers = true
    }
}