/*
 * Copyright (c) 2021, TU Dresden.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lflang.lfc

import com.google.inject.Inject
import com.google.inject.Singleton
import org.eclipse.xtext.diagnostics.Severity
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess


/**
 * Utilities to collect diagnostics and report them to the user
 * when running in standalone mode. The [IssueCollector] singleton
 * collects, sorts and de-dups all messages, which [Main] can then
 * print, using the [ReportingBackend]. The issue collector is created
 * via Guice and injected into the [StandaloneErrorReporter].
 * The error reporter is used by generators, though they only
 * use the super interface [ErrorReporter].
 *
 * When running in non-standalone mode, the [LFStandaloneModule] is
 * never bound, so the implementation of ErrorReporter that is produced
 * by guice is [DefaultErrorReporter], except within generators,
 * which build an [EclipseErrorReporter] manually instead of
 * using guice. Similarly, in IDE mode the [IssueCollector] is
 * never requested and never built.
 *
 * That ensures that Epoch doesn't use kotlin classes.
 */


/**
 * Abstraction over output streams. This is provided in case
 * we want to mock an environment for tests.
 *
 * @author Clément Fournier
 */
class Io @JvmOverloads constructor(
    val err: PrintStream = System.err,
    val out: PrintStream = System.out,
    val wd: Path = Paths.get("").toAbsolutePath()
)

/**
 * Represents an issue at a particular point in the program.
 * The issue has metadata about its location and may be formatted
 * by [ReportingBackend.printIssue].
 *
 * @author Clément Fournier
 */
data class LfIssue(
    val message: String,
    val severity: Severity,
    val line: Int?,
    val column: Int?,
    val length: Int?,
    val file: Path?
) : Comparable<LfIssue> {

    override operator fun compareTo(other: LfIssue): Int =
        issueComparator.compare(this, other)

    companion object {
        private val issueComparator =
            compareBy<LfIssue> { it.file }
                .thenComparing(compareBy { it.line })
                .thenComparing(compareBy { it.column })
                .thenComparing(compareBy { it.length })
                .thenComparing(compareBy { it.message })
                .thenComparing(compareBy { it.severity })

    }
}


/**
 * Collects issues to sort out later. This is a singleton in
 * the app, it's reset every time a generation task starts.
 *
 * @author Clément Fournier
 */
@Singleton // one instance per injector
class IssueCollector {
    private val map = mutableMapOf<Severity, MutableSet<LfIssue>>()
    /** Whether any errors occurred.*/
    val errorsOccurred: Boolean get() = map[Severity.ERROR]?.isNotEmpty() == true

    fun accept(issue: LfIssue) {
        val set = map.computeIfAbsent(issue.severity) { mutableSetOf() }
        set += issue
    }

    /** Sorted list of all errors.*/
    val errors: List<LfIssue> get() = map[Severity.ERROR].orEmpty().sorted()
    /** Sorted list of all issues.*/
    val allIssues: List<LfIssue> get() = map.values.flatten().sorted()

    fun reset() {
        map.clear()
    }
}


/**
 * Class whose responsibility is to format and print messages
 * collected from the validator, generator, or [Main]. This
 * contains a nice issue formatter that looks like what the
 * rust compiler produces.
 *
 * @author Clément Fournier
 */
class ReportingBackend constructor(
    /** Environment of the process, contains IO streams. */
    private val io: Io,
    /** An instance of the ANSI formatter to use. */
    private val colors: AnsiColors,
    /**
     * Number of lines of context to include around error
     * messages when printing a code snippet from the file
     * in which the error originated.
     */
    private val numLinesAround: Int,
) {
    /** Secondary constructor with default arguments and marked with @Inject */
    @Inject
    constructor(io: Io) : this(io, AnsiColors(true), 2)

    /** *Absolute* path to lines. */
    private val fileCache = mutableMapOf<Path, List<String>?>()
    private val header = colors.bold("lfc: ")

    private fun getLines(path: Path?): List<String>? =
        if (path == null) null
        else fileCache.computeIfAbsent(path.toAbsolutePath()) {
            try {
                Files.readAllLines(it, StandardCharsets.UTF_8)
            } catch (e: IOException) {
                null
            }
        }


    /** Print a fatal error message to [Io.err] and exit with code 1. */
    @JvmOverloads
    fun printFatalErrorAndExit(message: String, cause: Throwable? = null): Nothing {
        printFatalError(message, cause)
        exitProcess(1)
    }

    /** Print a fatal error message to [Io.err] and exit with code 1. */
    @JvmOverloads
    fun printFatalError(message: String, cause: Throwable? = null) {
        io.err.println(header + colors.redAndBold("fatal error: ") + colors.bold(message))
        cause?.printStackTrace(io.err)
    }

    /** Print an error message to [Io.err]. */
    fun printError(message: String) {
        io.err.println(header + colors.redAndBold("error: ") + message)
    }

    /** Print a warning message to [Io.err]. */
    fun printWarning(message: String) {
        io.err.println(header + colors.yellowAndBold("warning: ") + message)
    }

    /** Print an informational message to [Io.out]. */
    fun printInfo(message: String) {
        io.out.println(header + colors.bold("info: ") + message)
    }

    /**
     * Print a nicely formatted view of the region of code
     * surrounding the given [issue].
     */
    fun printIssue(issue: LfIssue) {
        val severity = issue.severity
        val filePath = issue.file?.normalize()

        val header = severity.name.toLowerCase(Locale.ROOT)

        var fullMessage: String = this.header + colors.severityColors(header, severity) + colors.bold(": " + issue.message) + "\n"
        val snippet: String? = filePath?.let { formatIssue(issue, filePath) }

        if (snippet == null) {
            val displayPath: String = filePath?.let { io.wd.relativize(it) }?.toString() ?: "(unknown file)"
            fullMessage += " --> " + displayPath + ":" + issue.line + ":" + issue.column
            fullMessage += " - " + issue.message
        } else {
            fullMessage += snippet
        }
        io.err.println(fullMessage)
        io.err.println()
    }

    private fun formatIssue(issue: LfIssue, path: Path): String? {
        val lines = getLines(path) ?: return null

        fun Int?.isInvalid() = this == null || this <= 0

        // those are nullable and need to be checked
        if (issue.line.isInvalid()
            || issue.column.isInvalid()
            || issue.length == null
        ) return null

        val fileDisplayName = io.wd.relativize(path).toString()

        return getBuilder(issue, lines, fileDisplayName).build()
    }

    private fun getBuilder(issue: LfIssue, lines: List<String>, displayPath: String): MessageTextBuilder {
        val zeroL = issue.line!! - 1
        val firstL = max(0, zeroL - numLinesAround + 1)
        val lastL = min(lines.size, zeroL + numLinesAround)
        val strings: List<String> = lines.subList(firstL, lastL)
        return MessageTextBuilder(strings, firstL, zeroL - firstL, displayPath, issue)
    }

    /** Renders a single issue. */
    inner class MessageTextBuilder(
        private val lines: List<String>,
        /** Line number of the first line of the list in the real document, one-based. */
        private val first: Int,
        /** Index in the list of the line that has the error, zero-based.  */
        private val errorIdx: Int,
        private val fileDisplayName: String,
        private val issue: LfIssue
    ) {

        init {
            assert(0 <= errorIdx && errorIdx < lines.size) { "Weird indices --- first=$first, errorIdx=$errorIdx, lines=$lines" }
        }

        fun build(): String {
            // the padding to apply to line numbers
            val pad = 2 + widthOfLargestLineNum()
            val withLineNums: MutableList<String> =
                lines.indices.mapTo(ArrayList()) { numberedLine(it, pad) }

            withLineNums.add(errorIdx + 1, makeErrorLine(pad))
            withLineNums.add(errorIdx + 2, emptyGutter(pad)) // skip a line

            // skip a line at the beginning
            // add it at the end to not move other indices
            withLineNums.add(0, emptyGutter(pad))
            withLineNums.add(0, makeHeaderLine(pad))

            return withLineNums.joinToString("\n")
        }

        private fun widthOfLargestLineNum() = (lines.size + first).toString().length

        /**
         * This formats the first line as
         *     --> src/Foo.lf:1:3
         * where the arrow is aligned on the gutter of the line numbers
         */
        private fun makeHeaderLine(pad: Int): String {
            val prefix = formatLineNum("-->".padStart(pad))

            return "$prefix $fileDisplayName:${issue.line}:${issue.column}"
        }

        private fun makeErrorLine(pad: Int): String {
            val line = lines[errorIdx]
            // tabs are replaced with spaces to align messages properly
            fun makeOffset(startIdx: Int, length: Int): Int {
                // note: this is needed because when the issue spans several lines,
                // startIdx+length may be greater than the line length.
                // todo implement real way to format multiline issues
                val endIndex = min(line.length, startIdx + length)
                val numTabs = line.substring(startIdx, endIndex).count { it == '\t' }
                return numTabs * (TAB_REPLACEMENT.length - 1)
            }

            val tabOffset = makeOffset(0, issue.column!!) // offset up to marker
            val tabSpanOffset = makeOffset(issue.column - 1, issue.length!!) // offset within marker

            val realLen = min(line.length, issue.column + issue.length)

            val caretLine = with(issue) { buildCaretLine(message.trim(), column!! + tabOffset, realLen + tabSpanOffset) }
            // gutter has its own ANSI stuff so only caretLine gets severityColors
            return emptyGutter(pad) + colors.severityColors(caretLine, issue.severity)
        }

        private fun numberedLine(idx: Int, pad: Int): String {
            val lineNum = 1 + idx + first
            val line = lines[idx].replace("\t", TAB_REPLACEMENT)
            return formatLineNum("$lineNum |".padStart(pad)) + " $line"
        }

        private fun emptyGutter(pad: Int): String = formatLineNum("|".padStart(pad))

        private fun formatLineNum(str: String) = colors.cyanAndBold(str)

        private fun buildCaretLine(message: String, column: Int, rangeLen: Int): String {
            fun StringBuilder.repeatChar(c: Char, n: Int) {
                repeat(n) { append(c) }
            }

            return buildString {
                repeatChar(' ', column)
                repeatChar('^', max(rangeLen, 1))
                append(' ').append(message)
            }
        }
    }

    companion object {
        const val TAB_REPLACEMENT = "    "
    }
}


/**
 * A strategy to add colors to messages. This uses ANSI escape
 * sequences, and can be disabled.
 *
 * @param useAnsi If true, colors will be used, otherwise all
 *                functions of this class return their argument
 *                without change
 *
 * @author Clément Fournier
 */
class AnsiColors(private val useAnsi: Boolean) {

    private fun apply(s: String, f: () -> String) =
        if (useAnsi) f() else s

    /** Return the given string in bold face. */
    fun bold(s: String): String = apply(s) { "$BOLD$s$END_BOLD" }

    /** Return the given string in red color and bold face. */
    fun redAndBold(s: String): String = apply(s) { "$RED_BOLD$s$ANSI_RESET" }

    /** Return the given string in yellow color and bold face.  */
    fun yellowAndBold(s: String): String = apply(s) { "\u001b[1;33m$s$ANSI_RESET" }

    /** Return the given string in cyan color and bold face.  */
    fun cyanAndBold(s: String): String = apply(s) { "\u001b[1;36m$s$ANSI_RESET" }


    /** Add a color determined by message severity. */
    fun severityColors(message: String, severity: Severity): String = apply(message) {
        when (severity) {
            Severity.ERROR   -> redAndBold(message)
            Severity.WARNING -> yellowAndBold(message)
            else             -> bold(message)
        }
    }


    companion object {
        /** ANSI sequence color escape sequence for red bold font. */
        private const val RED_BOLD = "\u001b[1;31m"

        /** ANSI sequence color escape sequence for resetting all attributes. */
        private const val ANSI_RESET = "\u001b[0m"

        /** ANSI sequence color escape sequence for bold font. */
        private const val BOLD = "\u001b[1m"

        /** ANSI sequence color escape sequence for ending bold font. */
        private const val END_BOLD = "\u001b[0m"
    }
}