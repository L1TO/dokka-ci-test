package dokka

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.util.Locale

val sharedGlobalSignificantKdocElements = listOf(
    "@param", "@return", "@property", "@throws", "@constructor", "@receiver", "@author"
)

val existingAndDirectoryFilter: (File) -> Boolean = { it.exists() && it.isDirectory }

val DOKKA_SHARED_MODULE_DEPENDENCY_TASK_NAMES = listOf(
    "transformIosMainDependenciesMetadata",
    "transformAppleMainDependenciesMetadata",
    "transformIosMainCInteropDependenciesMetadata",
    "transformNativeMainCInteropDependenciesMetadata",
    "transformAppleMainCInteropDependenciesMetadata",
    "transformCommonMainDependenciesMetadata",
    "commonize"
)

fun findKotlinSourceSet(project: Project, sourceSetName: String): KotlinSourceSet? {
    val kotlinProjectExtension = project.extensions.findByType(KotlinProjectExtension::class.java)
    val kmpExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
    return kotlinProjectExtension?.sourceSets?.findByName(sourceSetName)
        ?: kmpExtension?.sourceSets?.findByName(sourceSetName)
}

fun configureDokkaSourceSet(
    sourceSetBuilder: GradleDokkaSourceSetBuilder,
    gradleProject: Project,
    kotlinSourceSetName: String,
    customDisplayName: String,
    includePatternsProvider: Provider<List<String>>
) {
    sourceSetBuilder.apply {
        displayName.set(customDisplayName)

        val ktsSourceSet = findKotlinSourceSet(gradleProject, kotlinSourceSetName) ?: return@apply
        sourceRoots.from(ktsSourceSet.kotlin.srcDirs)

        val filesToDocument = includePatternsProvider.get().toSet()
        if (filesToDocument.isEmpty()) {
            return@apply
        }

        val filesToSuppress = ktsSourceSet.kotlin.srcDirs.flatMap { srcDir ->
            if (!srcDir.isDirectory) {
                return@flatMap emptyList<File>()
            }
            gradleProject.fileTree(srcDir).files.filter { file ->
                val relativePath = file.relativeTo(srcDir).path.replace(File.separator, "/")
                relativePath !in filesToDocument
            }
        }
        suppressedFiles.setFrom(filesToSuppress)
        skipEmptyPackages.set(true)
        reportUndocumented.set(false)
    }
}

fun generateCustomDokkaDisplayName(
    dokkaSourceSetName: String,
    projectName: String
): String {
    val displaySuffix = dokkaSourceSetName
        .replace("Main", "", ignoreCase = true)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    return if (displaySuffix.lowercase(Locale.ROOT) == projectName.lowercase(Locale.ROOT) ||
        displaySuffix.isEmpty() ||
        displaySuffix.equals(projectName, ignoreCase = true)
    ) {
        projectName
    } else {
        "$projectName${if (displaySuffix.isNotEmpty()) "/$displaySuffix" else ""}"
    }
}