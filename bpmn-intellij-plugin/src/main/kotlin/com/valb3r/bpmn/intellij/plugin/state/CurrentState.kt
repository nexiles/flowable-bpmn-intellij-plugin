package com.valb3r.bpmn.intellij.plugin.state

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.render.EdgeElementState
import java.util.concurrent.atomic.AtomicReference

private val currentStateProvider = AtomicReference<CurrentStateProvider>()

fun currentStateProvider(): CurrentStateProvider {
    return currentStateProvider.updateAndGet {
        if (null == it) {
            return@updateAndGet CurrentStateProvider()
        }

        return@updateAndGet it
    }
}

data class CurrentState(
        val shapes: List<ShapeElement>,
        val edges: List<EdgeWithIdentifiableWaypoints>,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByBpmnId: Map<BpmnElementId, WithBpmnId>,
        val elemPropertiesByStaticElementId: Map<BpmnElementId, Map<PropertyType, Property>>
)

// Global singleton
class CurrentStateProvider {
    private var fileState = CurrentState(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())
    private var currentState = CurrentState(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())

    fun resetStateTo(processObject: BpmnProcessObjectView) {
        fileState = CurrentState(
                processObject.diagram.firstOrNull()?.bpmnPlane?.bpmnShape ?: emptyList(),
                processObject.diagram.firstOrNull()?.bpmnPlane?.bpmnEdge?.map { EdgeElementState(it) } ?: emptyList(),
                processObject.elementByDiagramId,
                processObject.elementByStaticId,
                processObject.elemPropertiesByElementId
        )
        currentState = fileState
        updateEventsRegistry().reset()
    }

    fun currentState(): CurrentState {
        return handleUpdates(currentState)
    }

    private fun handleUpdates(state: CurrentState): CurrentState {
        var updatedShapes = state.shapes.toMutableList()
        var updatedEdges = state.edges.toMutableList()
        val updatedElementByDiagramId = state.elementByDiagramId.toMutableMap()
        val updatedElementByStaticId = state.elementByBpmnId.toMutableMap()
        val updatedElemPropertiesByStaticElementId = state.elemPropertiesByStaticElementId.toMutableMap()

        val updates = updateEventsRegistry().updateEventList()

        updates.map { it.event }.forEach { event ->
            when (event) {
                is LocationUpdateWithId -> {
                    updatedShapes = updatedShapes.map { shape -> if (shape.id == event.diagramElementId) updateWaypointLocation(shape, event) else shape }.toMutableList()
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.parentElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is NewWaypoints -> {
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.edgeElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is BpmnElementRemoved -> {
                    handleRemoved(event.elementId, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId, updatedElemPropertiesByStaticElementId)
                }
                is DiagramElementRemoved -> {
                    handleDiagramRemoved(event.elementId, updatedShapes, updatedEdges, updatedElementByDiagramId)
                }
                is BpmnShapeObjectAdded -> {
                    updatedShapes.add(event.shape)
                    updatedElementByDiagramId[event.shape.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props
                }
                is BpmnEdgeObjectAdded -> {
                    updatedEdges.add(event.edge)
                    updatedElementByDiagramId[event.edge.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props
                }
                is PropertyUpdateWithId -> {
                    if (null == event.newIdValue) {
                        updateProperty(event, updatedElemPropertiesByStaticElementId)
                    } else {
                        updateId(event.bpmnElementId, event.newIdValue!!, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId, updatedElemPropertiesByStaticElementId)
                    }
                }
            }
        }

        return CurrentState(
                updatedShapes,
                updatedEdges,
                updatedElementByDiagramId,
                updatedElementByStaticId,
                updatedElemPropertiesByStaticElementId
        )
    }

    private fun updateProperty(event: PropertyUpdateWithId, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, Map<PropertyType, Property>>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId]?.toMutableMap() ?: mutableMapOf()
        updated[event.property] = Property(event.newValue)
        updatedElemPropertiesByStaticElementId[event.bpmnElementId] = updated
    }

    private fun handleDiagramRemoved(diagramId: DiagramElementId, updatedShapes: MutableList<ShapeElement>, updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>, updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>) {
        val shape = updatedShapes.find { it.id == diagramId }
        val edge = updatedEdges.find { it.id == diagramId }
        shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
        edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
        updatedElementByDiagramId.remove(diagramId)
    }

    private fun handleRemoved(
            elementId: BpmnElementId,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithBpmnId>,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, Map<PropertyType, Property>>
    ) {
        val shape = updatedShapes.find { it.bpmnElement == elementId }
        val edge = updatedEdges.find { it.bpmnElement == elementId }
        shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
        edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
        updatedElementByStaticId.remove(elementId)
        updatedElemPropertiesByStaticElementId.remove(elementId)
    }

    private fun updateId(
            elementId: BpmnElementId,
            newElementId: BpmnElementId,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithBpmnId>,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, Map<PropertyType, Property>>
    ) {
        val shape = updatedShapes.find { it.bpmnElement == elementId }
        val edge = updatedEdges.find { it.bpmnElement == elementId }
        shape?.let {
            updatedShapes.remove(it)
            updatedElementByDiagramId[it.id] = newElementId
            updatedShapes.add(it.copy(bpmnElement = newElementId))
        }
        edge?.let {
            updatedEdges.remove(it)
            updatedElementByDiagramId[it.id] = newElementId
            updatedEdges.add(it.updateBpmnElemId(newElementId))
        }
        val elemByBpmnIdUpdated = updatedElementByStaticId.remove(elementId)
        elemByBpmnIdUpdated?.let { updatedElementByStaticId[newElementId] = it.updateBpmnElemId(newElementId) }
        val elemPropUpdated = updatedElemPropertiesByStaticElementId.remove(elementId)?.toMutableMap() ?: mutableMapOf()
        elemPropUpdated[PropertyType.ID] = Property(newElementId.id)
        updatedElemPropertiesByStaticElementId[newElementId] = elemPropUpdated
    }

    private fun updateWaypointLocation(elem: ShapeElement, update: LocationUpdateWithId): ShapeElement {
        return elem.copyAndTranslate(update.dx, update.dy)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: LocationUpdateWithId): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, elem.waypoint.filter { it.physical }.map { if (it.id == update.diagramElementId ) it.copyAndTranslate(update.dx, update.dy) else it }, elem.epoch)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: NewWaypoints): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, update.waypoints, update.epoch)
    }
}