package ui.update;

import ui.update.events.EndedEvent;
import ui.update.events.NodeClickedEvent;
import ui.update.events.StartedEvent;

/**
 * Created by Victor Wjugow on 18/06/15.
 */
public interface UpdateGraphEventListener {

	void handleUpdateGraphEvent(StartedEvent event);

	void handleUpdateGraphEvent(NodeClickedEvent event);

	void handleUpdateGraphEvent(EndedEvent event);
}