package io.github.mysoundpro.registry

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.OutputStreamWriter

class SourceRegistryProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        SourceRegistryProcessor(environment.codeGenerator, environment.logger)
}

private class SourceRegistryProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        val symbols = resolver.getSymbolsWithAnnotation(METADATA_ANNOTATION).toList()
        if (symbols.isEmpty()) return emptyList()
        val deferred = symbols.filterNot { it.validate() }
        if (deferred.isNotEmpty()) return deferred

        val declarations = symbols.filterIsInstance<KSClassDeclaration>()
        val metadata = declarations.map(::metadataOf)
        try {
            MetadataValidator.validate(metadata)
        } catch (failure: IllegalArgumentException) {
            logger.error(failure.message ?: "invalid source metadata")
            throw failure
        }

        writeRegistry(metadata, declarations.mapNotNull { it.containingFile })
        generated = true
        return emptyList()
    }

    private fun metadataOf(declaration: KSClassDeclaration): SourceMetadata {
        require(declaration.getAllSuperTypes().any { it.declaration.qualifiedName?.asString() == AUDIO_SOURCE }) {
            "${declaration.qualifiedName?.asString()} must implement AudioSource"
        }
        require(declaration.classKind == ClassKind.OBJECT) {
            "${declaration.qualifiedName?.asString()} must be declared as a Kotlin object"
        }
        val annotation = declaration.annotations.single {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == METADATA_ANNOTATION
        }
        val arguments = annotation.arguments.associate { it.name?.asString().orEmpty() to it.value.toString() }
        return SourceMetadata(
            qualifiedName = requireNotNull(declaration.qualifiedName?.asString()),
            id = arguments.getValue("id"),
            name = arguments.getValue("name"),
            host = arguments.getValue("host"),
            isObject = true,
        )
    }

    private fun writeRegistry(metadata: List<SourceMetadata>, sourceFiles: List<com.google.devtools.ksp.symbol.KSFile>) {
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *sourceFiles.toTypedArray()),
            packageName = GENERATED_PACKAGE,
            fileName = "GeneratedSourceRegistry",
        ).use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.appendLine("package $GENERATED_PACKAGE")
                writer.appendLine()
                writer.appendLine("import io.github.mysoundpro.api.AudioSource")
                writer.appendLine()
                writer.appendLine("object GeneratedSourceRegistry {")
                writer.appendLine("    @JvmStatic")
                if (metadata.isEmpty()) {
                    writer.appendLine("    fun all(): List<AudioSource> = emptyList()")
                } else {
                    writer.appendLine("    fun all(): List<AudioSource> = listOf(")
                    metadata.sortedBy { it.id }.forEach { writer.appendLine("        ${it.qualifiedName},") }
                    writer.appendLine("    )")
                }
                writer.appendLine("}")
            }
        }
    }

    private companion object {
        const val METADATA_ANNOTATION = "io.github.mysoundpro.api.AudioSourceMetadata"
        const val AUDIO_SOURCE = "io.github.mysoundpro.api.AudioSource"
        const val GENERATED_PACKAGE = "io.github.mysoundpro.generated"
    }
}

data class SourceMetadata(
    val qualifiedName: String,
    val id: String,
    val name: String,
    val host: String,
    val isObject: Boolean,
)

object MetadataValidator {
    @JvmStatic
    fun validate(metadata: List<SourceMetadata>) {
        val duplicateIds = metadata.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "duplicate source id: ${duplicateIds.sorted().joinToString()}" }

        val duplicateHosts = metadata
            .groupBy { it.host.trimEnd('/').lowercase() }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateHosts.isEmpty()) { "duplicate source host: ${duplicateHosts.sorted().joinToString()}" }
    }
}
