package MTSSynthesis.controller.gr.time;

import java.util.Set;

import MTSSynthesis.controller.gr.time.model.ChoiceType;
import MTSSynthesis.controller.gr.time.utils.TimeUtils;

public class SchedulerUtils<A> {
	
	public SchedulerUtils(){}
	
	public ChoiceType getChoiceType(A label, Set<A> cActions) {
		if(cActions.contains(label)){
			return ChoiceType.CONTROLLABLE;
		}else{
			if(TimeUtils.isEnding(label.toString())){
				return ChoiceType.ENDS;
			}else{
				return ChoiceType.UNCONTROLLABLE;
			}
		}
	}
}
