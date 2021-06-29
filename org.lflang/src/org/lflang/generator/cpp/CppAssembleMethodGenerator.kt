/*************
 * Copyright (c) 2021, TU Dresden.

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

import org.lflang.generator.PrependOperator
import org.lflang.isBank
import org.lflang.isMultiport
import org.lflang.lf.*

/**
 * A code generator for the assemble() method of a C++ reactor class
 *
 * The assemble method is called once during initialization by the reactor runtime. It is
 * responsible for declaring all triggers, dependencies and effects (antidependencies) of reactions.
 * It is also responsible for creating all connections within the reactor.
 */
class CppAssembleMethodGenerator(private val reactor: Reactor) {

    private fun declareTrigger(reaction: Reaction, trigger: TriggerRef): String {
        // check if the trigger is a multiport
        if (trigger is VarRef && trigger.variable is Port) {
            val port = trigger.variable as Port
            if (port.widthSpec != null) {
                return """
                    for (unsigned i = 0; i < ${trigger.name}.size(); i++) {
                      ${reaction.name}.declare_trigger(&${trigger.name}[i]);
                    }
                """.trimIndent()
            }
        }
        // treat as single trigger otherwise
        return "${reaction.name}.declare_trigger(&${trigger.name});"
    }

    private fun declareDependency(reaction: Reaction, dependency: VarRef): String {
        val variable = dependency.variable
        // check if the dependency is a multiport
        if (variable is Port && variable.widthSpec != null) {
            return """
                for (unsigned i = 0; i < ${dependency.name}.size(); i++) {
                  ${reaction.name}.declare_dependency(&${dependency.name}[i]);
                }
            """.trimIndent()
        }
        // treat as single dependency otherwise
        return "${reaction.name}.declare_dependency(&${dependency.name});"
    }

    private fun declareAntidependency(reaction: Reaction, antidependency: VarRef): String {
        val variable = antidependency.variable
        // check if the dependency is a multiport
        if (variable is Port && variable.widthSpec != null) {
            return """
                for (unsigned i = 0; i < ${antidependency.name}.size(); i++) {
                  ${reaction.name}.declare_antidependency(&${antidependency.name}[i]);
                }
            """.trimIndent()
        }
        // treat as single antidependency otherwise
        return if (variable is Action) "${reaction.name}.declare_schedulable_action(&${antidependency.name});"
        else "${reaction.name}.declare_antidependency(&${antidependency.name});"
    }

    private fun setDeadline(reaction: Reaction): String =
        "${reaction.name}.set_deadline(${reaction.deadline.delay.toTime(true)}, [this]() { ${reaction.name}_deadline_handler(); });"

    private fun assembleReaction(reaction: Reaction) = with(PrependOperator) {
        """
            |// ${reaction.name}
        ${" |"..reaction.triggers.joinToString(separator = "\n") { declareTrigger(reaction, it) }}
        ${" |"..reaction.sources.joinToString(separator = "\n") { declareDependency(reaction, it) }}
        ${" |"..reaction.effects.joinToString(separator = "\n") { declareAntidependency(reaction, it) }}
        ${" |"..if (reaction.deadline != null) setDeadline(reaction) else ""}
        """.trimMargin()
    }

    private val Connection.isMultiportConnection: Boolean
        get() {
            // if there are multiple ports listed on the left or right side, this is a multiport connection
            if (leftPorts.size > 1 || rightPorts.size > 1)
                return true

            // if the ports on either side are multiports, this is a multiport connection
            val leftPort = leftPorts[0].variable as Port
            val rightPort = rightPorts[0].variable as Port
            if (leftPort.isMultiport || rightPort.isMultiport)
                return true

            // if the containers on either side are banks, this is a multiport connection
            val leftContainer = leftPorts[0].container
            val rightContainer = rightPorts[0].container
            if (leftContainer?.isBank == true || rightContainer?.isBank == true)
                return true

            return false
        }

    private fun declareConnection(c: Connection, idx: Int): String {
        if (c.isMultiportConnection) {
            return declareMultiportConnection(c, idx);
        } else {
            val leftPort = c.leftPorts[0]
            val rightPort = c.rightPorts[0]

            return """
                // connection $idx
                ${leftPort.name}.bind_to(&${rightPort.name});
            """.trimIndent()
        }
    }

    private fun declareMultiportConnection(c: Connection, idx: Int): String {
        // It should be safe to assume that all ports have the same type. Thus we just pick the
        // first left port to determine the type of the entire connection
        val type = (c.leftPorts[0].variable as Port).targetType

        return with(PrependOperator) {
            """
                |// connection $idx
                |std::vector<reactor::Output<$type>*> __lf_left_ports_$idx;
            ${" |"..c.leftPorts.joinToString("\n") { addAllPortsToVector(it, "__lf_left_ports_$idx") }}
                |std::vector<reactor::Input<$type>*> __lf_right_ports_$idx;
            ${" |"..c.rightPorts.joinToString("\n") { addAllPortsToVector(it, "__lf_right_ports_$idx") }}
                |lfutil::bind_multiple_ports(__lf_left_ports_$idx, __lf_right_ports_$idx, ${c.isIsIterated});
            """.trimMargin()
        }
    }

    private fun addAllPortsToVector(varRef: VarRef, vectorName: String): String {
        val port = varRef.variable as Port
        val container = varRef.container
        return if (port.isMultiport) {
            if (container?.isBank == true) {
                // is multiport in a bank
                // FIXME: iterate over banks or ports first?
                """
                    for (auto& __lf_instance : ${container.name}) {
                      for (auto& __lf_port : __lf_instance->${port.name}) {
                        ${vectorName}.push_back(&__lf_port);
                      }
                    }
                """.trimIndent()
            } else {
                // is mulitport, but not in a bank
                """
                   for (auto& __lf_port : ${varRef.name}) {
                     ${vectorName}.push_back(&__lf_port);
                   }
                """.trimIndent()
            }
        } else {
            if (container?.isBank == true) {
                // is in a bank, but not a multiport
                """
                    for (auto& __lf_instance : ${container.name}) {
                      ${vectorName}.push_back(&__lf_instance->${port.name});
                    }
                """.trimIndent()
            } else {
                // is just a normal port
                "${vectorName}.push_back(&${varRef.name});"
            }
        }
    }

    /**
     * Generate the definition of the reactor's assemble() method
     *
     * The body of this method will declare all triggers, dependencies and antidependencies to the runtime.
     */
    fun generateDefinition() = with(PrependOperator) {
        val indexedConnections = reactor.connections.withIndex()
        """
            |${reactor.templateLine}
            |void ${reactor.templateName}::assemble() {
        ${" |  "..reactor.reactions.joinToString("\n\n") { assembleReaction(it) }}
        ${" |  "..indexedConnections.joinToString("\n", prefix = "// connections\n") { declareConnection(it.value, it.index) }}
            |}
        """.trimMargin()
    }
}