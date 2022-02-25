/* Generator for Cpp target. */

/*************
 * Copyright (c) 2019-2021, TU Dresden.

 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ***************/

package org.lflang.generator.cpp

import org.eclipse.emf.ecore.resource.Resource
import org.lflang.ErrorReporter
import org.lflang.Target
import org.lflang.generator.LFGeneratorContext.Mode
import org.lflang.TargetProperty
import org.lflang.TimeUnit
import org.lflang.TimeValue
import org.lflang.generator.CodeMap
import org.lflang.generator.GeneratorBase
import org.lflang.generator.GeneratorResult
import org.lflang.generator.IntegratedBuilder
import org.lflang.generator.JavaGeneratorUtils
import org.lflang.generator.LFGeneratorContext
import org.lflang.generator.TargetTypes
import org.lflang.generator.canGenerate
import org.lflang.isGeneric
import org.lflang.lf.Action
import org.lflang.lf.VarRef
import org.lflang.scoping.LFGlobalScopeProvider
import org.lflang.toDefinition
import org.lflang.toUnixString
import org.lflang.util.LFCommand
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("unused")
class CppGenerator(
    private val cppFileConfig: CppFileConfig,
    errorReporter: ErrorReporter,
    private val scopeProvider: LFGlobalScopeProvider
) :
    GeneratorBase(cppFileConfig, errorReporter) {

    companion object {
        /** Path to the Cpp lib directory (relative to class path)  */
        const val libDir = "/lib/cpp"
    }

    /** Convert a log level to a severity number understood by the reactor-cpp runtime. */
    private val TargetProperty.LogLevel.severity
        get() = when (this) {
            TargetProperty.LogLevel.ERROR -> 1
            TargetProperty.LogLevel.WARN  -> 2
            TargetProperty.LogLevel.INFO  -> 3
            TargetProperty.LogLevel.LOG   -> 4
            TargetProperty.LogLevel.DEBUG -> 4
        }

    override fun doGenerate(resource: Resource, context: LFGeneratorContext) {
        super.doGenerate(resource, context)

        if (!canGenerate(errorsOccurred(), mainDef, errorReporter, context)) return

        val codeMaps = generateFiles()

        if (targetConfig.noCompile || errorsOccurred()) {
            println("Exiting before invoking target compiler.")
            context.finish(GeneratorResult.GENERATED_NO_EXECUTABLE.apply(codeMaps))
        } else if (context.mode == Mode.LSP_MEDIUM) {
            context.reportProgress(
                "Code generation complete. Validating generated code...", IntegratedBuilder.GENERATED_PERCENT_PROGRESS
            )
            if (runCmake(context).first == 0) {
                CppValidator(cppFileConfig, errorReporter, codeMaps).doValidate(context)
                context.finish(GeneratorResult.GENERATED_NO_EXECUTABLE.apply(codeMaps))
            } else {
                context.unsuccessfulFinish()
            }
        } else {
            context.reportProgress(
                "Code generation complete. Compiling...", IntegratedBuilder.GENERATED_PERCENT_PROGRESS
            )
            doCompile(context, codeMaps)
        }
    }

    private fun fetchReactorCpp() {
        val version = targetConfig.runtimeVersion
        val libPath = fileConfig.srcGenBasePath.resolve("reactor-cpp-$version")
        // abort if the directory already exists
        if (Files.isDirectory(libPath)) {
            return
        }
        // clone the reactor-cpp repo and fetch the specified version
        Files.createDirectories(libPath)
        commandFactory.createCommand(
            "git",
            listOf("clone", "-n", "https://github.com/lf-lang/reactor-cpp.git", "reactor-cpp-$version"),
            fileConfig.srcGenBasePath
        ).run()
        commandFactory.createCommand("git", listOf("checkout", version), libPath).run()
    }

    private fun generateFiles(): Map<Path, CodeMap> {
        val srcGenPath = fileConfig.srcGenPath

        val mainReactor = mainDef.reactorClass.toDefinition()

        // copy static library files over to the src-gen directory
        val genIncludeDir = srcGenPath.resolve("__include__")
        fileConfig.copyFileFromClassPath("$libDir/lfutil.hh", genIncludeDir.resolve("lfutil.hh"), true)
        fileConfig.copyFileFromClassPath("$libDir/time_parser.hh", genIncludeDir.resolve("time_parser.hh"), true)
        fileConfig.copyFileFromClassPath(
            "$libDir/3rd-party/cxxopts.hpp",
            genIncludeDir.resolve("CLI").resolve("cxxopts.hpp"),
            true
        )

        // build reactor-cpp if needed
        if (targetConfig.externalRuntimePath == null) {
            if (targetConfig.runtimeVersion != null) {
                fetchReactorCpp()
            } else {
                fileConfig.copyDirectoryFromClassPath(
                    "$libDir/reactor-cpp",
                    fileConfig.srcGenBasePath.resolve("reactor-cpp-lfbuiltin"),
                    true
                )
            }
        }

        // keep a list of all source files we generate
        val cppSources = mutableListOf<Path>()
        val codeMaps = HashMap<Path, CodeMap>()

        // generate the main source file (containing main())
        val mainFile = Paths.get("main.cc")
        val mainCodeMap = CodeMap.fromGeneratedCode(CppMainGenerator(mainReactor, targetConfig, cppFileConfig).generateCode())
        cppSources.add(mainFile)
        codeMaps[srcGenPath.resolve(mainFile)] = mainCodeMap
        JavaGeneratorUtils.writeToFile(mainCodeMap.generatedCode, srcGenPath.resolve(mainFile), true)

        // generate header and source files for all reactors
        for (r in reactors) {
            val generator = CppReactorGenerator(r, cppFileConfig, errorReporter)
            val headerFile = cppFileConfig.getReactorHeaderPath(r)
            val sourceFile = if (r.isGeneric) cppFileConfig.getReactorHeaderImplPath(r) else cppFileConfig.getReactorSourcePath(r)
            val reactorCodeMap = CodeMap.fromGeneratedCode(generator.generateSource())
            if (!r.isGeneric)
                cppSources.add(sourceFile)
            codeMaps[srcGenPath.resolve(sourceFile)] = reactorCodeMap
            val headerCodeMap = CodeMap.fromGeneratedCode(generator.generateHeader())
            codeMaps[srcGenPath.resolve(headerFile)] = headerCodeMap

            JavaGeneratorUtils.writeToFile(headerCodeMap.generatedCode, srcGenPath.resolve(headerFile), true)
            JavaGeneratorUtils.writeToFile(reactorCodeMap.generatedCode, srcGenPath.resolve(sourceFile), true)
        }

        // generate file level preambles for all resources
        for (r in resources) {
            val generator = CppPreambleGenerator(r.eResource, cppFileConfig, scopeProvider)
            val sourceFile = cppFileConfig.getPreambleSourcePath(r.eResource)
            val headerFile = cppFileConfig.getPreambleHeaderPath(r.eResource)
            val preambleCodeMap = CodeMap.fromGeneratedCode(generator.generateSource())
            cppSources.add(sourceFile)
            codeMaps[srcGenPath.resolve(sourceFile)] = preambleCodeMap
            val headerCodeMap = CodeMap.fromGeneratedCode(generator.generateHeader())
            codeMaps[srcGenPath.resolve(headerFile)] = headerCodeMap

            JavaGeneratorUtils.writeToFile(headerCodeMap.generatedCode, srcGenPath.resolve(headerFile), true)
            JavaGeneratorUtils.writeToFile(preambleCodeMap.generatedCode, srcGenPath.resolve(sourceFile), true)
        }

        // generate the cmake scripts
        val cmakeGenerator = CppCmakeGenerator(targetConfig, cppFileConfig)
        val srcGenRoot = fileConfig.srcGenBasePath
        val pkgName = fileConfig.srcGenPkgPath.fileName.toString()
        JavaGeneratorUtils.writeToFile(cmakeGenerator.generateRootCmake(pkgName), srcGenRoot.resolve("CMakeLists.txt"), true)
        JavaGeneratorUtils.writeToFile(cmakeGenerator.generateCmake(cppSources), srcGenPath.resolve("CMakeLists.txt"), true)
        var subdir = srcGenPath.parent
        while (subdir != srcGenRoot) {
            JavaGeneratorUtils.writeToFile(cmakeGenerator.generateSubdirCmake(), subdir.resolve("CMakeLists.txt"), true)
            subdir = subdir.parent
        }

        return codeMaps
    }

    fun getCmakeVersion(buildPath: Path): String? {
        val cmd = commandFactory.createCommand("cmake", listOf("--version"), buildPath)
        if (cmd != null && cmd.run() == 0) {
            val regex = "\\d+(\\.\\d+)+".toRegex()
            val version = regex.find(cmd.output.toString())
            return version?.value
        }
        return null
    }

    fun doCompile(context: LFGeneratorContext) {
        doCompile(context, HashMap())
    }

    /**
     * Run CMake to generate build files.
     * @return The CMake return code and the CMake version, or
     * (1, "") if no acceptable version of CMake is installed.
     */
    private fun runCmake(context: LFGeneratorContext): Pair<Int, String> {
        val outPath = fileConfig.outPath
        val buildPath = cppFileConfig.buildPath

        // make sure the build directory exists
        Files.createDirectories(buildPath)

        // get the installed cmake version and make sure it is at least 3.5
        val version = getCmakeVersion(buildPath)
        if (version == null || version.compareVersion("3.5.0") < 0) {
            errorReporter.reportError(
                "The C++ target requires CMAKE >= 3.5.0 to compile the generated code. " +
                        "Auto-compiling can be disabled using the \"no-compile: true\" target property."
            )
            return Pair(1, "")
        }

        // run cmake
        val cmakeCommand = createCmakeCommand(buildPath, outPath)
        return Pair(cmakeCommand.run(context.cancelIndicator), version)
    }

    private fun doCompile(context: LFGeneratorContext, codeMaps: Map<Path, CodeMap>) {
        val (cmakeReturnCode, version) = runCmake(context)

        if (cmakeReturnCode == 0) {
            // If cmake succeeded, run make
            val makeCommand = createMakeCommand(cppFileConfig.buildPath, version, fileConfig.name)
            val makeReturnCode = CppValidator(cppFileConfig, errorReporter, codeMaps).run(makeCommand, context.cancelIndicator)
            var installReturnCode = 0
            if (makeReturnCode == 0) {
                val installCommand = createMakeCommand(cppFileConfig.buildPath, version, "install")
                installReturnCode = installCommand.run(context.cancelIndicator)
                if (installReturnCode == 0) {
                    println("SUCCESS (compiling generated C++ code)")
                    println("Generated source code is in ${fileConfig.srcGenPath}")
                    println("Compiled binary is in ${fileConfig.binPath}")
                }
            }
            if ((makeReturnCode != 0 || installReturnCode != 0) && !errorsOccurred()) {
                // If errors occurred but none were reported, then the following message is the best we can do.
                errorReporter.reportError("make failed with error code $makeReturnCode")
            }
        } else if (version.isNotBlank()) {
            errorReporter.reportError("cmake failed with error code $cmakeReturnCode")
        }
        if (errorReporter.errorsOccurred) {
            context.unsuccessfulFinish()
        } else {
            context.finish(
                GeneratorResult.Status.COMPILED, cppFileConfig.name, cppFileConfig, codeMaps
            )
        }
    }

    private fun String.compareVersion(other: String): Int {
        val a = this.split(".").map { it.toInt() }
        val b = other.split(".").map { it.toInt() }
        for (x in (a zip b)) {
            val res = x.first.compareTo(x.second)
            if (res != 0)
                return res
        }
        return 0
    }

    private fun createMakeCommand(buildPath: Path, version: String, target: String): LFCommand {
        val makeArgs: List<String>
        if (version.compareVersion("3.12.0") < 0) {
            errorReporter.reportWarning("CMAKE is older than version 3.12. Parallel building is not supported.")
            makeArgs =
                listOf("--build", ".", "--target", target, "--config", targetConfig.cmakeBuildType?.toString() ?: "Release")
        } else {
            val cores = Runtime.getRuntime().availableProcessors()
            makeArgs = listOf(
                "--build",
                ".",
                "--target",
                target,
                "--parallel",
                cores.toString(),
                "--config",
                targetConfig.cmakeBuildType?.toString() ?: "Release"
            )
        }

        return commandFactory.createCommand("cmake", makeArgs, buildPath)
    }

    private fun createCmakeCommand(buildPath: Path, outPath: Path): LFCommand {
        val cmd = commandFactory.createCommand(
            "cmake", listOf(
                "-DCMAKE_BUILD_TYPE=${targetConfig.cmakeBuildType}",
                "-DCMAKE_INSTALL_PREFIX=${outPath.toUnixString()}",
                "-DCMAKE_INSTALL_BINDIR=${outPath.relativize(fileConfig.binPath).toUnixString()}",
                "-DREACTOR_CPP_VALIDATE=${if (targetConfig.noRuntimeValidation) "OFF" else "ON"}",
                "-DREACTOR_CPP_TRACE=${if (targetConfig.tracing != null) "ON" else "OFF"}",
                "-DREACTOR_CPP_LOG_LEVEL=${targetConfig.logLevel.severity}",
                fileConfig.srcGenBasePath.toUnixString()
            ),
            buildPath
        )

        // prepare cmake
        if (targetConfig.compiler != null) {
            cmd.setEnvironmentVariable("CXX", targetConfig.compiler)
        }
        return cmd
    }

    /**
     * Generate code for the body of a reaction that takes an input and
     * schedules an action with the value of that input.
     * @param action the action to schedule
     * @param port the port to read from
     */
    override fun generateDelayBody(action: Action, port: VarRef): String {
        // Since we cannot easily decide whether a given type evaluates
        // to void, we leave this job to the target compiler, by calling
        // the template function below.
        return """
        // delay body for ${action.name}
        lfutil::after_delay(&${action.name}, &${port.name});
        """.trimIndent()
    }

    /**
     * Generate code for the body of a reaction that is triggered by the
     * given action and writes its value to the given port.
     * @param action the action that triggers the reaction
     * @param port the port to write to
     */
    override fun generateForwardBody(action: Action, port: VarRef): String {
        // Since we cannot easily decide whether a given type evaluates
        // to void, we leave this job to the target compiler, by calling
        // the template function below.
        return """
        // forward body for ${action.name}
        lfutil::after_forward(&${action.name}, &${port.name});
        """.trimIndent()
    }

    override fun generateDelayGeneric() = "T"

    override fun generateAfterDelaysWithVariableWidth() = false

    override fun getTarget() = Target.CPP

    override fun getTargetTypes(): TargetTypes = CppTypes
}

object CppTypes : TargetTypes {

    override fun supportsGenerics() = true

    override fun getTargetTimeType() = "reactor::Duration"
    override fun getTargetTagType() = "reactor::Tag"

    override fun getTargetFixedSizeListType(baseType: String, size: Int) = "std::array<$baseType, $size>"
    override fun getTargetVariableSizeListType(baseType: String) = "std::vector<$baseType>"

    override fun getTargetUndefinedType() = "void"

    override fun getTargetTimeExpr(timeValue: TimeValue): String =
        with(timeValue) {
            if (magnitude == 0L) "reactor::Duration::zero()"
            else magnitude.toString() + unit.cppUnit
        }

}

/** Get a C++ representation of a LF unit. */
val TimeUnit?.cppUnit
    get() = when (this) {
        TimeUnit.NANO   -> "ns"
        TimeUnit.MICRO  -> "us"
        TimeUnit.MILLI  -> "ms"
        TimeUnit.SECOND -> "s"
        TimeUnit.MINUTE -> "min"
        TimeUnit.HOUR   -> "h"
        TimeUnit.DAY    -> "d"
        TimeUnit.WEEK   -> "d*7"
        else            -> ""
    }
