/* AMES: Enhanced Modularity */
package ltsa.ui;

import java.util.List;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;

public interface LTSManager {
	// TODO this is not being used, either use for something apart from just
	// implementing it, or delete it.
	void performAction(Runnable r, boolean showOutputPane);

	String getTargetChoice();

	CompositeState compile(String name);

	void newMachines(List<LabelledTransitionSystem> machines);
	// Set<String> getLabelSet(String name);
}
