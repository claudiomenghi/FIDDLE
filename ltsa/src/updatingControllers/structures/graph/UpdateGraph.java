package updatingControllers.structures.graph;

import ac.ic.doc.mtstools.model.GraphWithInitialNode;

import javax.swing.*;
import java.util.Collection;

/**
 * Created by Victor Wjugow on 10/06/15.
 */
public class UpdateGraph {

    private UpdateNode initialState;
    private UpdateNode[] vertices;
    private UpdateTransition[] edges;

    public void addEdge(UpdateTransition transition, UpdateNode fromNode, UpdateNode toNode) {
    }

    public boolean containsVertex(UpdateNode fromNode) {
        return false;
    }

    public UpdateNode getInitialState() {
        return initialState;
    }

    public UpdateNode[] getVertices() {
        return vertices;
    }

    public UpdateTransition[] getEdges() {
        return edges;
    }

    public DefaultListModel getIncidentVertices(UpdateTransition updateTransition) {
        return null;
    }

    public UpdateTransition findEdge(UpdateNode currentNode, UpdateNode nextNode) {
        return null;
    }

    public Collection getSuccessors(UpdateNode currentNode) {
        return null;
    }

    public void setInitialState(UpdateNode initialState) {
        this.initialState = initialState;
    }
}
