package dokka

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GlobalSuggestDokkaIncludePatternsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allSourceDirectories: ConfigurableFileCollection

    @get:Input
    abstract val significantKdocElements: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    companion object {
        private val KDOC_REGEX = Regex("/\\*\\*(.*?)\\*/", RegexOption.DOT_MATCHES_ALL)
    }

    @TaskAction
    fun execute() {
        val patterns = mutableSetOf<String>()
        val significantElements = significantKdocElements.get()

        allSourceDirectories.files.forEach { sourceDirFile ->
            if (!sourceDirFile.isDirectory) {
                return@forEach
            }

            sourceDirFile.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { ktFile ->
                    val fileContent = ktFile.readText()
                    var foundDetailedKdocInFile = false

                    KDOC_REGEX.findAll(fileContent).forEach { matchResult ->
                        val kdocBlockContent = matchResult.groupValues[1]
                        if (significantElements.any { element -> kdocBlockContent.contains(element) }) {
                            foundDetailedKdocInFile = true
                        }
                    }

                    if (foundDetailedKdocInFile) {
                        val relativePath = ktFile.relativeTo(sourceDirFile).path.replace(File.separatorChar, '/')
                        patterns.add(relativePath)
                    }
                }
        }
        outputFile.get().asFile.parentFile?.mkdirs()
        outputFile.get().asFile.writeText(patterns.joinToString("\n"))
    }
}