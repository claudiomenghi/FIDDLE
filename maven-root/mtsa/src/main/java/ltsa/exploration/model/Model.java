package ltsa.exploration.model;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

public class Model
{
    private LabelledTransitionSystem[] components;
    private Integer[] currentStates;

    //region Constructor
    public Model(LabelledTransitionSystem[] components)
    {
        this.components = components;
        this.currentStates = new Integer[components.length];
        for (int i = 0; i < components.length; i++)
            this.currentStates[i] = 0;
    }
    //endregion

    //region Public methods
    public LTSTransitionList execute(int componentNumber, String nextViewAction)
    {
        if (nextViewAction.equals("WAIT"))
            return LTSTransitionList.copy(this.components[componentNumber].getStates()[this.currentStates[componentNumber]]);

        String nextAction = nextViewAction;

        int event = -1;
        for (int i = 0; i < this.components[componentNumber].getAlphabet().length; i++)
            if (nextAction.equals(this.components[componentNumber].getAlphabet()[i]))
                event = i;

        if (event > -1)
        {
            int[] events = this.components[componentNumber].getStates()[this.currentStates[componentNumber]].getEvents();
            for (int anEvent : events)
            {
                if (event == anEvent)
                {
                    this.currentStates[componentNumber] = this.components[componentNumber].getStates()[this.currentStates[componentNumber]].getNext(event);
                    return LTSTransitionList.copy(this.components[componentNumber].getStates()[this.currentStates[componentNumber]]);
                }
            }
        }

        nextAction = nextAction + "?";
        for (int i = 0; i < this.components[componentNumber].getAlphabet().length; i++)
            if (nextAction.equals(this.components[componentNumber].getAlphabet()[i]))
                event = i;

        if (event > -1)
        {
            int[] events = this.components[componentNumber].getStates()[this.currentStates[componentNumber]].getEvents();
            for (int anEvent : events)
            {
                if (event == anEvent)
                {
                    this.currentStates[componentNumber] = this.components[componentNumber].getStates()[this.currentStates[componentNumber]].getNext(event);
                    return LTSTransitionList.copy(this.components[componentNumber].getStates()[this.currentStates[componentNumber]]);
                }
            }
        }

        throw new UnsupportedOperationException("Invalid action");
    }
    public void reset()
    {
        this.currentStates[0] = 0;
    }
    //endregion
}
