package ltsa.lts.automata.lts.state;

import static ltsa.lts.util.MTSUtils.computeHiddenAlphabet;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.automata.automaton.Automata;
import ltsa.lts.checkers.Analyser;
import ltsa.lts.checkers.CounterExample;
import ltsa.lts.checkers.ProgressCheck;
import ltsa.lts.csp.Relation;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;

/**
 * a composite state contains a vector of state machines that must be performed
 * in parallel.
 *
 */
public class CompositeState {
	/** Logger available to subclasses */
	protected final org.apache.commons.logging.Log logger = LogFactory.getLog(getClass());

	public static boolean reduceFlag = true;

	private FluentTrace tracer;

	/**
	 * The name of the composite state
	 */
	private String name;

	private LabelledTransitionSystem env;

	/**
	 * set of CompactState from which this
	 */
	private Vector<LabelledTransitionSystem> machines;

	/**
	 * the result of a composition;
	 */
	private LabelledTransitionSystem composition;

	/**
	 * set of actions concealed in composed version
	 */
	private Vector<String> hidden;

	public boolean exposeNotHide = false; // expose rather than conceal
	public boolean priorityIsLow = true;
	public boolean makeDeterministic = false; // construct equivalent DFA if
												// NDFA
	public boolean makeOptimistic = false;
	public boolean makeAbstract = false;
	public boolean makeClousure = false;

	private LabelledTransitionSystem saved = null;

	public boolean makePessimistic = false;
	public boolean makeMinimal = false;
	public boolean makeCompose = false; // force composition if true
	public boolean isProperty = false;
	public boolean makeController = false;
	public boolean makeSyncController = false;
	public boolean checkCompatible = false;
	public boolean isStarEnv = false;
	public boolean isPlant = false;
	public boolean isControlledDet = false;
	public boolean makeMDP = false;
	public boolean makeEnactment = false;
	public boolean makeControlStack = false;

	public boolean satisfied = true;

	public Hashtable<String, Object> controlStackEnvironments;
	public int controlStackSpecificTier = -1;
	public List<String> enactmentControlled;

	private int compositionType = -1;
	/**
	 * set of actions given priority
	 */
	public Vector<String> priorityLabels;
	public LabelledTransitionSystem alphaStop; // stop process with alphabet of
												// the
	// composition
	protected Vector<String> errorTrace = null;

	/**
	 * If the isComponent flag is true, then this ProcessSpec represents a
	 * composition of several component processes. The component process can be
	 * built with the componentAlphabet.
	 */
	private boolean makeComponent = false;

	/**
	 * this alphabet is one of the component. It must be a subset of the process
	 * alphabet.
	 */
	private Vector<String> componentAlphabet;

	/**
	 * creates a new composite state
	 * 
	 * @param name
	 *            the name of the machine
	 * @throws NullPointerException
	 *             if the name of the machine is null
	 */
	public CompositeState(String name) {
		Preconditions.checkNotNull(name, "The name of the machine cannot be null");

		this.name = name;
		this.machines = new Vector<>();
	}

	public CompositeState(Vector<LabelledTransitionSystem> machine) {
		this("DEFAULT", machine);
	}

	public CompositeState(String name, Vector<LabelledTransitionSystem> machine) {
		this(name);
		Preconditions.checkNotNull(machine, "The set of machines cannot be null");

		this.machines = machine;
		initAlphaStop();
	}

	public void setName(String name) {
		this.name = name;
	}

	public LabelledTransitionSystem getEnv() {
		return env;
	}

	public void setEnv(LabelledTransitionSystem env) {
		this.env = env;
	}

	public boolean isMakeComponent() {
		return makeComponent;
	}

	public void setMakeComponent(boolean makeComponent) {
		this.makeComponent = makeComponent;
	}

	public Vector<String> getComponentAlphabet() {
		return componentAlphabet;
	}

	public void setComponentAlphabet(Vector<String> componentAlphabet) {
		this.componentAlphabet = componentAlphabet;
	}

	public Set<String> getAlphabetEvents() {
		Set<String> alphabet = new HashSet<>();
		this.machines.forEach(m -> alphabet.addAll(m.getAlphabetEvents()));
		return alphabet;
	}

	public Vector<String> getErrorTrace() {
		return this.errorTrace;
	}

	public void setErrorTrace(List<String> ll) {
		if (ll != null) {
			errorTrace = new Vector<>();
			errorTrace.addAll(ll);
		}
	}

	public void compose(LTSOutput output) {
		Preconditions.checkNotNull(output, "The output cannot be null");
		this.compose(output, false);
	}

	public void compose(LTSOutput output, boolean ignoreAsterisk) {
		if (machines != null && !machines.isEmpty()) {

			Analyser analyzer = new Analyser(this, output, null, ignoreAsterisk);
			this.composition = analyzer.composeNoHide();
			this.applyLTSOperations(output);
		}
	}

	public void addMachine(LabelledTransitionSystem machine) {
		this.machines.addElement(machine);
	}

	public void applyLTSOperations(LTSOutput output) {
		if (!makeDeterministic && !makeMinimal) {
			this.applyHiding();
		}
	}

	public void applyOperations(LTSOutput output) {
		if (this.makeComponent) {
			applyHiding();
			TransitionSystemDispatcher.makeComponentModel(this, output);
		}
		if (this.makeDeterministic) {
			applyHiding();
			TransitionSystemDispatcher.determinise(this, output);
		}
		if (this.makeMinimal) {
			applyHiding();
			TransitionSystemDispatcher.minimise(this, output);
		}
		if (this.makeOptimistic) {
			TransitionSystemDispatcher.makeOptimisticModel(this, output);
			applyHiding();
		}
		if (this.makePessimistic) {
			TransitionSystemDispatcher.makePessimisticModel(this, output);
			applyHiding();
		}
		if (this.makeClousure) {
			TransitionSystemDispatcher.makeClosureModel(this, output);
			applyHiding();
		}
		if (this.makeAbstract) {
			TransitionSystemDispatcher.makeAbstractModel(this, output);
			applyHiding();
			if (this.isProperty) {
				TransitionSystemDispatcher.makeProperty(this, output);
				applyHiding();
			}
		}

	}

	public void applyOperationsNoText(LTSOutput output) {
		if (this.makeComponent) {
			applyHiding();
			TransitionSystemDispatcher.makeComponentModel(this, output);
		}
		if (makeDeterministic) {
			applyHiding();
			TransitionSystemDispatcher.determinise(this, output);
		}
		if (makeMinimal) {
			applyHiding();
			TransitionSystemDispatcher.minimise(this, output);
		}
		if (makeOptimistic) {
			TransitionSystemDispatcher.makeOptimisticModel(this, output);
			applyHiding();
		}
		if (makePessimistic) {
			TransitionSystemDispatcher.makePessimisticModel(this, output);
			applyHiding();
		}
		if (makeClousure) {
			TransitionSystemDispatcher.makeClosureModel(this, output);
			applyHiding();
		}
		if (makeAbstract) {
			TransitionSystemDispatcher.makeAbstractModel(this, output);
			applyHiding();
		}

	}

	private void applyHiding() {
		if (composition == null)
			return;
		if (hidden != null) {
			computeHiddenAlphabet(hidden);
			if (!exposeNotHide) {
				composition.conceal(hidden);
			} else {
				composition.expose(hidden);
			}
		}
	}

	public void analyse(boolean checkDeadlocks, LTSOutput output) {
		if (saved != null) {
			machines.remove(saved);
			saved = null;
		}
		if (composition != null) {
			CounterExample ce = new CounterExample(this);
			ce.print(output, checkDeadlocks);
			errorTrace = ce.getErrorTrace();
		} else {
			Analyser a = new Analyser(this, output, null);
			a.analyse(checkDeadlocks);
			this.setErrorTrace(a.getErrorTrace());
		}
	}

	public void checkProgress(LTSOutput output) {
		ProgressCheck cc;
		if (saved != null) {
			machines.remove(saved);
			saved = null;
		}
		if (composition != null) {
			cc = new ProgressCheck(composition, output);
			cc.doProgressCheck();
		} else {
			Automata a = new Analyser(this, output, null);
			cc = new ProgressCheck(a, output);
			cc.doProgressCheck();
		}
		errorTrace = cc.getErrorTrace();
	}

	/**
	 * checks whether the current model satisfies the LTL property contained in
	 * the composite state
	 * 
	 * @param output
	 * @param cs
	 *            the composite state encoding the LTL property
	 * @throws NullPointerException
	 *             if the composite state is null
	 * @return true if the property is satisfied, false otherwise
	 */
	public boolean checkLTL(LTSOutput output, CompositeState cs) {

		Preconditions.checkNotNull(output, "The output cannot be null");
		Preconditions.checkNotNull(cs, "The composite state cannot be null");
		Preconditions.checkNotNull(cs.tracer, "The tracer of the property cannot be null");

		LabelledTransitionSystem ltlProperty = cs.composition;
		ltlProperty.setName(cs.getName());

		if (name.equals("DEFAULT") && machines.isEmpty()) {
			// debug feature for producing consistent machines
			machines = cs.machines;
			composition = cs.composition;
		} else {
			if (saved != null) {
				machines.remove(saved);
			}
			Vector<String> saveHidden = hidden;
			boolean saveExposeNotHide = exposeNotHide;
			hidden = ltlProperty.getAlphabetV();
			exposeNotHide = true;
			machines.add(saved = ltlProperty);
			Analyser analyzer = new Analyser(this, output, null);
			
			// If the property has an error state it is necessary to do 
			// a progress check to verify whether the property is satisfied
			if (!ltlProperty.hasERROR()) {

				logger.debug("Composition has no error state");
				// do full liveness check
				ProgressCheck cc = new ProgressCheck(analyzer, output, cs.tracer);
				boolean satisfied = cc.doLTLCheck();
				if (!satisfied) {
					this.satisfied=false;
					logger.debug("Progress checker returns an error trace");
					errorTrace = cc.getErrorTrace();
					setErrorTrace(errorTrace);
				}
				else{
					logger.debug("The progress checker does not return an error trace");
					this.satisfied=true;
				}
			} else {

				// do safety check
				logger.debug("The property  contains an error state");
				boolean satisfied=analyzer.analyse(cs.tracer, false, false);
				if(satisfied){
					this.satisfied=false;
					logger.debug("Analyzer returned an error trace");
					errorTrace = new Vector<String>(analyzer.getErrorTrace());
					setErrorTrace(analyzer.getErrorTrace());
				}
				else{
					logger.debug("The property is satisified");
					this.satisfied=true;
				}
		 }
			hidden = saveHidden;
			exposeNotHide = saveExposeNotHide;
		}
		return this.satisfied;
	}

	public void minimise(LTSOutput output) {
		if (composition != null) {
			// change (a ->(tau->P|tau->Q)) to (a->P | a->Q) and (a->tau->P) to
			// a->P
			if (reduceFlag) {
				composition.removeNonDetTau();
			}
			Minimiser e = new Minimiser(composition, output);
			composition = e.minimise();
		}
	}

	public void determinise(LTSOutput output) {
		if (composition != null) {
			Minimiser d = new Minimiser(composition, output);
			composition = d.trace_minimise();
			if (isProperty) {
				composition.makeProperty();
			}
		}
	}

	public LabelledTransitionSystem create(LTSOutput output) {
		TransitionSystemDispatcher.applyComposition(this, output);
		return composition;
	}

	public boolean compositionNotRequired() {
		return (hidden == null && priorityLabels == null && !makeDeterministic && !makeMinimal && !makeCompose
				&& !makeController && !makeSyncController);
	}

	/*
	 * prefix all constituent machines
	 */
	public void prefixLabels(String prefix) {
		name = prefix + ":" + name;
		alphaStop.prefixLabels(prefix);
		for (LabelledTransitionSystem mm : machines) {
			mm.prefixLabels(prefix);
		}
	}

	/*
	 * add prefix set to all constitutent machines
	 */
	public void addAccess(Vector<String> pset) {
		int n = pset.size();
		if (n == 0)
			return;
		String s = "{";
		int i = 0;
		for (String prefix : pset) {
			s = s + prefix;
			i++;
			if (i < n)
				s = s + ",";
		}
		// new name
		name = s + "}::" + name;
		alphaStop.addAccess(pset);
		for (LabelledTransitionSystem mm : machines) {
			mm.addAccess(pset);
		}
	}

	/*
	 * relabel all constituent machines checks to see if it is safe to leave
	 * uncomposed if a relabeling causes synchronization, then the composition
	 * is formed before relabelling is applied
	 */
	public LabelledTransitionSystem relabel(Relation oldtonew, LTSOutput output) {
		alphaStop.relabel(oldtonew);
		if (alphaStop.relabelDuplicates() && machines.size() > 1) {
			// we have to do the composition, before relabelling
			TransitionSystemDispatcher.applyComposition(this, output);
			composition.relabel(oldtonew);
			return composition;
		} else {
			for (LabelledTransitionSystem mm : machines) {
				mm.relabel(oldtonew);
			}
		}
		return null;
	}

	/**
	 * initialise the alphaStop process
	 */
	protected void initAlphaStop() {
		alphaStop = new LabelledTransitionSystem(name, 1);
		alphaStop.setStates(new LTSTransitionList[alphaStop.getMaxStates()]); // statespace
		// for STOP
		// process
		alphaStop.getStates()[0] = null;
		// now define alphabet as union of constituents
		Hashtable<String, String> alpha = new Hashtable<String, String>();
		for (LabelledTransitionSystem m : machines) {
			for (int i = 1; i < m.getAlphabet().length; ++i)
				alpha.put(m.getAlphabet()[i], m.getAlphabet()[i]);
		}
		String[] alphabet = new String[alpha.size() + 1];
		alphabet[0] = "tau";
		int j = 1;
		for (String s : alpha.keySet()) {
			alphabet[j] = s;
			++j;
		}
		alphaStop.setAlphabet(alphabet);
	}

	public void setFluentTracer(FluentTrace ft) {
		tracer = ft;
	}

	public FluentTrace getFluentTracer() {
		return tracer;
	}

	public String getName() {
		return name;
	}

	public LabelledTransitionSystem getComposition() {
		return composition;
	}

	public void setComposition(LabelledTransitionSystem compactSate) {
		Preconditions.checkNotNull(compactSate, "The composition cannot be null");
		this.composition = compactSate;
	}

	public void setReduction(boolean b) {
		reduceFlag = b;
	}

	public void setMachines(Vector<LabelledTransitionSystem> machines) {
		this.machines = machines;
	}

	public void setCompositionType(int compositionType) {
		this.compositionType = compositionType;
	}

	public int getCompositionType() {
		return compositionType;
	}

	@Override
	public CompositeState clone() {

		Vector<LabelledTransitionSystem> newMachines = new Vector<LabelledTransitionSystem>();
		for (LabelledTransitionSystem currentMachine : machines) {
			newMachines.add(currentMachine.clone());
		}

		CompositeState c = new CompositeState(getName(), newMachines);
		c.setCompositionType(getCompositionType());
		c.makeAbstract = makeAbstract;
		c.makeClousure = makeClousure;
		c.makeCompose = makeCompose;
		c.makeDeterministic = makeDeterministic;
		c.makeMinimal = makeMinimal;
		c.makeControlStack = makeControlStack;
		c.makeOptimistic = makeOptimistic;
		c.makePessimistic = makePessimistic;
		c.makeController = makeController;
		c.setMakeComponent(isMakeComponent());
		c.setComponentAlphabet(getComponentAlphabet());
		c.controlStackEnvironments = controlStackEnvironments;
		c.controlStackSpecificTier = controlStackSpecificTier;
		return c;
	}

	public Vector<LabelledTransitionSystem> getMachines() {
		return this.machines;
	}

	public Vector<String> getHidden() {
		return hidden;
	}

	public void addHiddenAction(String action) {
		this.hidden.add(action);
	}

	public void setHidden(Vector<String> hidden) {
		this.hidden = hidden;
	}
}