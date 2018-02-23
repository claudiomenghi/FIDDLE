package ltsa.lts.gui;

import ltsa.lts.automata.automaton.event.LTSEvent;

public interface EventClient {
    public void ltsAction(LTSEvent e);
}