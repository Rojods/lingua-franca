package org.lflang.generator.ts

import org.lflang.getWidth
import org.lflang.isMultiport
import org.lflang.lf.Input
import org.lflang.lf.Output
import org.lflang.lf.Port
import org.lflang.lf.Type
import java.util.*

/**
 * Generate input and output ports for TypeScript target.
 */
class TSPortGenerator (
    // TODO(hokeun): Remove dependency on TSGenerator.
    private val tsGenerator: TSGenerator,
    private val inputs: List<Input>,
    private val outputs: List<Output>
) {
    private fun Type.getTargetType(): String = tsGenerator.getTargetTypeW(this)

    /**
     * Return a TS type for the specified port.
     * If the type has not been specified, return
     * "Present" which is the base type for ports.
     * @param port The port
     * @return The TS type.
     */
    private fun getPortType(port: Port): String {
        if (port.type != null) {
            return port.type.getTargetType()
        } else {
            return "Present"
        }
    }

    fun generateClassProperties(): String {
        val portClassProperties = LinkedList<String>()
        for (input in inputs) {
            if (input.isMultiport) {
                portClassProperties.add("${input.name}: Array<__InPort<${getPortType(input)}>>;")
            } else {
                portClassProperties.add("${input.name}: __InPort<${getPortType(input)}>;")
            }
        }
        for (output in outputs) {
            if (output.isMultiport) {
                portClassProperties.add("${output.name}: Array<__OutPort<${getPortType(output)}>>;")
                portClassProperties.add("__rw__${output.name}: Array<ReadWrite<${getPortType(output)}>>;")
            } else {
                portClassProperties.add("${output.name}: __OutPort<${getPortType(output)}>;")
            }
        }
        return portClassProperties.joinToString("\n")
    }

    fun generateInstantiations(): String {
        val porInstantiations = LinkedList<String>()
        for (input in inputs) {
            if (input.isMultiport) {
                porInstantiations.add("this.${input.name} = [];")
                porInstantiations.add("""
                    |for (let i = 0; i< ${input.widthSpec.getWidth()}; i++) {
                    |    this.${input.name}.push(new __InPort<${getPortType(input)}>(this));
                    |}""".trimMargin())
            } else {
                porInstantiations.add("this.${input.name} = new __InPort<${getPortType(input)}>(this);")
            }
        }
        for (output in outputs) {
            if (output.isMultiport) {
                porInstantiations.add("this.${output.name} = [];")
                porInstantiations.add("""
                    |for (let i = 0; i < ${output.widthSpec.getWidth()}; i++) {
                    |    this.${output.name}.push(new __OutPort<${getPortType(output)}>(this));
                    |}""".trimMargin())
                porInstantiations.add("this.__rw__${output.name} = [];")
                porInstantiations.add("""
                    |this.${output.name}.forEach(element => {
                    |    this.__rw__${output.name}.push(this.writable(element))
                    |});""".trimMargin())
            } else {
                porInstantiations.add("this.${output.name} = new __OutPort<${getPortType(output)}>(this);")
            }
        }
        return porInstantiations.joinToString("\n")
    }
}