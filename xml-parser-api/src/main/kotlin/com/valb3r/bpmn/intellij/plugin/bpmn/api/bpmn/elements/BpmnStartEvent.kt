package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class BpmnStartEvent(
        override val id: String,
        val name: String?,
        val documentation: String?
) : WithId