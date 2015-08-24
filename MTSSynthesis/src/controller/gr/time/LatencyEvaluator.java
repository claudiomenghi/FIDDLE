package controller.gr.time;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ac.ic.doc.mtstools.model.MTS;
import ac.ic.doc.mtstools.model.MTS.TransitionType;
import ac.ic.doc.mtstools.model.impl.LTSAdapter;
import ac.ic.doc.mtstools.model.impl.MTSAdapter;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Z3Exception;

public abstract class LatencyEvaluator<P,A,S>{

	private void addStat(Map<Result, Integer> stats, Result res) {
		if(stats.keySet().contains(res)){
			stats.put(res, stats.get(res)+1);
		}else{
			stats.put(res, 1);
		}
	}
	
	public void evaluateLatency(
			Set<A> controllableActions,
			MTS<P, A> heuristic,
			MTS<P, A> maximalControllerUsingGR1,
			Set<P> heuristicFinalStates, 
			Set<P> perfectFinalStates, 
			Translator<S, P> translator, 
			Integer maxControllers,
			ActivityDefinitions<A> activityDefinitions) {
		//Start Z3 Definitions
		String END_TO_END_NAME_1 = "f1";
		String END_TO_END_NAME_2 = "f2";
		
		Map<Result,Integer> stats = new HashMap<Result,Integer>();
		HeuristicSolutionsSchedulerIterator<P,A> heuristicIterator  = new HeuristicSolutionsSchedulerIterator<P,A>(heuristic, controllableActions ,heuristicFinalStates);
		System.out.println("#Heuristic Solutions:" + heuristicIterator.getSize());
		boolean doo = true;
		
		HashMap<GenericChooser<S,A,P>,BoolExpr> heursticGamas = new HashMap<GenericChooser<S,A,P>,BoolExpr>();
		GamaComparator<A, P> comparator = new GamaComparator<A, P>(activityDefinitions);

		ControllerGenerator<P, A> controllerGenerator = new ControllerGenerator<P, A>(new LTSAdapter<P,A>(maximalControllerUsingGR1, TransitionType.REQUIRED), controllableActions,perfectFinalStates);
		
		 Translator<P,P> trivialTranslator = new TrivialTranslator<P>();
		 ControllerChooser<P, A> controllerChooser = controllerGenerator.getNew();
		 int i =0;
		 while(controllerChooser != null && i < maxControllers){
			 controllerChooser.applyTo(new LTSAdapter<P,A>(maximalControllerUsingGR1, TransitionType.REQUIRED), trivialTranslator);
			 controllerChooser = controllerGenerator.getNew();
			 i++;
		 }
		
		while(heuristicIterator.hasNext() && doo){
			Chooser<P,A> heuristicScheduler  =  heuristicIterator.next();
			MTS<P, A> result2 = heuristicScheduler.applyTo(heuristic);
			try{
				preCalculateGamaForHeuristic(heuristicFinalStates, controllableActions, END_TO_END_NAME_2, heursticGamas, getSchedulers(), result2, comparator, translator);
				System.out.println("#Controllers:" + controllerGenerator.generated.size());
				int j = 0;
				for(GenericChooser<P, A, P> controllerScheduler: controllerGenerator.generated){
					Map<Result, Integer> ctr_stats = new HashMap<Result,Integer>();
					Result ctr_res = null;
					for (GenericChooser<S,A,P> scheduler : getSchedulers()) {
						MTS<P, A> result1 = new MTSAdapter<P,A>(controllerScheduler.applyTo(new LTSAdapter<P,A>(maximalControllerUsingGR1, TransitionType.REQUIRED), trivialTranslator));
						MTS<P, A> result_1_schedulled = new MTSAdapter<P, A>(scheduler.applyTo(new LTSAdapter<P,A>(result1, TransitionType.REQUIRED),translator));
						BoolExpr gama1 = comparator.calculateGama(result_1_schedulled , END_TO_END_NAME_1, result1, controllableActions, perfectFinalStates);
						Result res = comparator.compareControllers(gama1, heursticGamas.get(scheduler), END_TO_END_NAME_1, END_TO_END_NAME_2);
						addStat(ctr_stats,res);
						if(ctr_stats.keySet().contains(Result.UNCOMPARABLES)||
						  (ctr_stats.keySet().contains(Result.WORSE) && ctr_stats.keySet().contains(Result.BETTER))){
							ctr_res = Result.UNCOMPARABLES;
							addStat(stats, Result.UNCOMPARABLES);
							System.out.println("RESULT_1");
							System.out.println(result_1_schedulled.toString());
							MTS<P, A> heuristic_schedulled = new MTSAdapter<P, A>(scheduler.applyTo(new LTSAdapter<P,A>(result2, TransitionType.REQUIRED),translator));
							System.out.println("RESULT_2");
							System.out.println(heuristic_schedulled.toString());
							break;
						}
					}
					if(ctr_res == null){
						if(!ctr_stats.containsKey(Result.UNCOMPARABLES) && !ctr_stats.containsKey(Result.WORSE)){
							if(ctr_stats.containsKey(Result.BETTER)){
								ctr_res = Result.BETTER;
							}else{
								ctr_res = Result.EQUALLYGOOD;
							}
						}else if(ctr_stats.keySet().contains(Result.UNCOMPARABLES)||
								(ctr_stats.keySet().contains(Result.WORSE) && ctr_stats.keySet().contains(Result.BETTER))){
							ctr_res = Result.UNCOMPARABLES;
						}else{
							ctr_res = Result.WORSE;
						}
						addStat(stats, ctr_res);
					}
					System.out.println("Controller: " + j + " " + ctr_res);
					j++;
				}
				System.out.println(stats.toString());
				doo = false;
			}catch(Exception e){
				e.printStackTrace();
				addStat(stats, Result.EXCEPTION);
				System.out.println(stats.toString());
			}
		}
	}
	
	protected abstract Set<GenericChooser<S, A, P>> getSchedulers();
	
	private void preCalculateGamaForHeuristic(Set<P> finalStates,
			Set<A> controllableActions, String END_TO_END_NAME_2,
			HashMap<GenericChooser<S,A,P>, BoolExpr> heursticGamas,
			Set<GenericChooser<S,A,P>> schedulers,
			MTS<P, A> result2,
			GamaComparator<A, P> comparator, 
			Translator<S,P> translator) throws Z3Exception {
		for (GenericChooser<S,A,P> scheduler : schedulers) {
			MTS<P, A> result_2_schedulled = new MTSAdapter<P,A>(scheduler.applyTo(new LTSAdapter<P,A>(result2, TransitionType.REQUIRED),translator));
			BoolExpr gama2 = comparator.calculateGama(result_2_schedulled , END_TO_END_NAME_2, result2, controllableActions, finalStates);
			if(gama2 == null){
				throw new RuntimeException("Error generating Gama of the scheduled heuristic solution");
			}
			heursticGamas.put(scheduler, gama2);
		}
	}
}