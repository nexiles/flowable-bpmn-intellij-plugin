package com.valb3r.bpmn.intellij.plugin.bpmn.api.info

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.BOOLEAN
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.STRING

enum class PropertyType(
        val id: String,
        val caption: String,
        val valueType: PropertyValueType,
        val path: String = id,
        val cascades: Boolean = false,
        val updatedBy: PropertyType? = null
) {
    ID("id", "ID", STRING, "id.id", true),
    NAME("name", "Name", STRING),
    DOCUMENTATION("documentation", "Documentation", STRING),
    ASYNC("async", "Asynchronous", BOOLEAN),
    CALLED_ELEM("calledElement", "Called element", PropertyValueType.EXPRESSION),
    CALLED_ELEM_TYPE("calledElementType", "Called element type", STRING),
    INHERIT_VARS("inheritVariables", "Inherit parent variables", BOOLEAN),
    FALLBACK_TO_DEF_TENANT("fallbackToDefaultTenant", "Fallback to default tenant", BOOLEAN),
    EXCLUSIVE("exclusive", "Exclusive", BOOLEAN),
    EXPRESSION("expression", "Expression", PropertyValueType.EXPRESSION),
    DELEGATE_EXPRESSION("delegateExpression", "Delegate expression", PropertyValueType.EXPRESSION),
    CLASS("clazz", "Class", PropertyValueType.CLASS),
    SKIP_EXPRESSION("skipExpression", "Skip expression", PropertyValueType.EXPRESSION),
    IS_TRIGGERABLE("triggerable", "Is activity triggerable?", BOOLEAN),
    SOURCE_REF("sourceRef","Source reference", STRING, "sourceRef", false, ID),
    TARGET_REF("targetRef", "Target reference", STRING, "targetRef", false, ID),
    CONDITION_EXPR_VALUE("conditionExpression.text", "Condition expression", PropertyValueType.EXPRESSION, "conditionExpression.text"),
    CONDITION_EXPR_TYPE("conditionExpression.type", "Condition expression type", STRING, "conditionExpression.type"),
    DEFAULT_FLOW("defaultElement", "Default flow element", STRING)
}