package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class ExclusiveGateway(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        @JacksonXmlProperty(isAttribute = true, localName = "default") val defaultElement: String?,
        val documentation: String?
): BpmnMappable<BpmnExclusiveGateway> {

    override fun toElement(): BpmnExclusiveGateway {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper
    interface Mapping {
        fun convertToDto(input: ExclusiveGateway) : BpmnExclusiveGateway
    }
}