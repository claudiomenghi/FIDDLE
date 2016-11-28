package ltsa.lts.parser;

import static ltsa.lts.util.MTSUtils.getMaybeAction;
import static ltsa.lts.util.MTSUtils.getOpositeActionLabel;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import ltsa.control.ControlStackDefinition;
import ltsa.control.ControlTierDefinition;
import ltsa.control.ControllerDefinition;
import ltsa.control.ControllerGoalDefinition;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.exploration.ExplorerDefinition;
import ltsa.lts.Diagnostics;
import ltsa.lts.automata.automaton.StateMachine;
import ltsa.lts.automata.lts.state.AutCompactState;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.probabilistic.ProbabilisticTransition;
import ltsa.lts.chart.BasicChartDefinition;
import ltsa.lts.chart.ConditionDefinition;
import ltsa.lts.chart.ConditionLocation;
import ltsa.lts.chart.DuplicatedTriggeredScenarioDefinitionException;
import ltsa.lts.chart.ExistentialTriggeredScenarioDefinition;
import ltsa.lts.chart.Interaction;
import ltsa.lts.chart.Location;
import ltsa.lts.chart.TriggeredScenarioDefinition;
import ltsa.lts.chart.UniversalTriggeredScenarioDefinition;
import ltsa.lts.chart.util.TriggeredScenarioTransformationException;
import ltsa.lts.csp.BoxStateDefn;
import ltsa.lts.csp.ChoiceElement;
import ltsa.lts.csp.CompositeBody;
import ltsa.lts.csp.CompositionExpression;
import ltsa.lts.csp.MenuDefinition;
import ltsa.lts.csp.ProbabilisticChoiceElement;
import ltsa.lts.csp.ProcessRef;
import ltsa.lts.csp.ProcessSpec;
import ltsa.lts.csp.ProgressDefinition;
import ltsa.lts.csp.Range;
import ltsa.lts.csp.RelabelDefn;
import ltsa.lts.csp.SeqProcessRef;
import ltsa.lts.csp.StateDefn;
import ltsa.lts.csp.StateExpr;
import ltsa.lts.distribution.DistributionDefinition;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.ltl.visitors.FormulaTransformerVisitor;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.actions.ActionExpr;
import ltsa.lts.parser.actions.ActionLabels;
import ltsa.lts.parser.actions.ActionName;
import ltsa.lts.parser.actions.ActionRange;
import ltsa.lts.parser.actions.ActionSet;
import ltsa.lts.parser.actions.ActionSetExpr;
import ltsa.lts.parser.actions.ActionVarRange;
import ltsa.lts.parser.actions.ActionVarSet;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.lts.parser.ltsinput.LTSInput;
import ltsa.lts.util.LTSUtils;
import ltsa.updatingControllers.UpdatingControllersDefinition;
import ltsa.updatingControllers.structures.UpdateGraphDefinition;
import ltsa.updatingControllers.synthesis.UpdateGraphGenerator;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.PredicateUtils;
import org.apache.commons.logging.LogFactory;
import org.jfree.util.Log;

import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import MTSSynthesis.ar.dc.uba.model.condition.FluentImpl;
import MTSSynthesis.ar.dc.uba.model.condition.Formula;
import MTSSynthesis.controller.game.util.GeneralConstants;
import MTSTools.ac.ic.doc.commons.relations.Pair;

import com.google.common.base.Preconditions;

public class LTSCompiler {

	/** Logger available to subclasses */
	protected final org.apache.commons.logging.Log logger = LogFactory
			.getLog(getClass());

	private Lex lex;
	private LTSOutput output;
	private String currentDirectory;
	private Symbol current;

	public static PreconditionDefinitionManager preconditionDefinitionManager;
	public static PostconditionDefinitionManager postconditionDefinitionManager;

	public static Hashtable<String, ProcessSpec> processes;
	public static Hashtable<String, ProcessSpec> replacements;

	public static HashMap<String, String> mapBoxReplacementName;

	public static Hashtable<String, LabelledTransitionSystem> compiled;
	public static Hashtable<String, CompositionExpression> composites;
	public static Hashtable<String, ExplorerDefinition> explorers;
	private static Hashtable<String, CompositionExpression> allComposites;

	public static Map<String, String> mapsEachPostConditionToTheCorrespondingBox;
	public static Map<String, String> mapsEachPreconditionToTheCorrespondingBox;
	public static Map<String, String> mapsEachPreconditionToTheCorrespondingProcess;
	private int compositionType = -1;

	public static String boxOfInterest;

	public LTSCompiler(LTSInput input, LTSOutput output, String currentDirectory) {
		Preconditions.checkNotNull(input, "The LTSInput cannot be null");
		Preconditions.checkNotNull(output, "The LTSOutput cannot be null");
		Preconditions.checkNotNull(currentDirectory,
				"The current directory cannot be null");

		this.lex = new Lex(input);
		this.output = output;
		this.currentDirectory = currentDirectory;
		Diagnostics.init(output);
		SeqProcessRef.output = output;
		StateMachine.output = output;
		Expression.constants = new Hashtable<>();
		Range.ranges = new Hashtable<>();
		LabelSet.constants = new Hashtable<>();
		ProgressDefinition.definitions = new Hashtable<>();
		MenuDefinition.definitions = new Hashtable<>();
		mapBoxReplacementName = new HashMap<>();
		processes = new Hashtable<>();
		replacements = new Hashtable<>();
		Def.init();
		PredicateDefinition.init();
		AssertDefinition.init();
		TriggeredScenarioDefinition.init();
		ControllerDefinition.init();

		preconditionDefinitionManager = new PreconditionDefinitionManager();
		postconditionDefinitionManager = new PostconditionDefinitionManager();
		ControllerGoalDefinition.init();
		ControlStackDefinition.initDefinitionList();
		DistributionDefinition.init();
		mapsEachPostConditionToTheCorrespondingBox = new HashMap<>();
		mapsEachPreconditionToTheCorrespondingBox = new HashMap<>();
		mapsEachPreconditionToTheCorrespondingProcess = new HashMap<>();
	}

	public void parse(Hashtable<String, CompositionExpression> composites,
			Hashtable<String, ProcessSpec> processes,
			Hashtable<String, ExplorerDefinition> explorations) {
		doparse(composites, processes, null);
	}

	private void doparse(Hashtable<String, CompositionExpression> composites,
			Hashtable<String, ProcessSpec> processes,
			Hashtable<String, LabelledTransitionSystem> compiled) {
		ProbabilisticTransition
				.setLastProbBundle(ProbabilisticTransition.NO_BUNDLE);
		nextSymbol();
		try {
			while (current.kind != Symbol.EOFSYM) {
				if (current.kind == Symbol.REPLACEMENT) {
					nextSymbol();
					ProcessSpec p = compileReplacement();
		
					nextSymbol();
					currentIs(Symbol.AT, "sub-controller interface expected");
					nextSymbol();
					p.alphaAdditions=this.labelSet();
					currentIs(Symbol.DOT, "sub-controller interface expected");
					//nextSymbol();
					
					if (processes.put(p.getName(), p) != null) {
						Diagnostics.fatal(
								"duplicate process definition: " + p.getName(),
								p.getName());
					}
					else{
						replacements.put(p.getName(), p);

					}
				} else if (current.kind == Symbol.LTLPRECONDITION) {
					nextSymbol();
					assertPrecondition();
				} else if (current.kind == Symbol.LTLPOSTCONDITION) {
					nextSymbol();
					assertPostcondition();
				} else if (current.kind == Symbol.CONSTANT) {
					nextSymbol();
					constantDefinition(Expression.constants);
				} else if (current.kind == Symbol.RANGE) {
					nextSymbol();
					rangeDefinition();
				} else if (current.kind == Symbol.SET) {
					nextSymbol();
					setDefinition();
				} else if (current.kind == Symbol.PROGRESS) {
					nextSymbol();
					progressDefinition();
				} else if (current.kind == Symbol.MENU) {
					nextSymbol();
					menuDefinition();
				} else if (current.kind == Symbol.ANIMATION) {
					nextSymbol();
					animationDefinition();
				} else if (current.kind == Symbol.ASSERT) {
					nextSymbol();
					assertDefinition(false, false);
				} else if (current.kind == Symbol.CONSTRAINT) {
					nextSymbol();
					assertDefinition(true, false);
				} else if (current.kind == Symbol.LTLPROPERTY) {
					nextSymbol();
					assertDefinition(true, true);
				} else if (current.kind == Symbol.PREDICATE) {
					nextSymbol();
					predicateDefinition();
				} else if (current.kind == Symbol.DEF) {
					nextSymbol();
					defDefinition();
				} else if (current.kind == Symbol.GOAL) {
					nextSymbol();

					currentIs(Symbol.UPPERIDENT, "goal identifier expected");

					this.validateUniqueProcessName(current);
					ControllerGoalDefinition goal = new ControllerGoalDefinition(
							current);
					this.goalDefinition(goal);

				} else if (current.kind == Symbol.EXPLORATION) {
					nextSymbol();

					currentIs(Symbol.UPPERIDENT,
							"exploration identifier expected");

					this.validateUniqueProcessName(current);
					ExplorerDefinition explorerDefinition = new ExplorerDefinition(
							current);
					nextSymbol();

					this.explorerDefinition(explorerDefinition);

					output.outln("Explorer: " + explorerDefinition.getName());

				} else if (current.kind == Symbol.UPDATING_CONTROLLER) {
					nextSymbol();

					currentIs(Symbol.UPPERIDENT,
							"updating controller identifier expected");

					UpdatingControllersDefinition cuDefinition = new UpdatingControllersDefinition(
							current);

					this.updateControllerDefinition(cuDefinition);

					if (composites.put(cuDefinition.getName().getValue(),
							cuDefinition) != null) {
						Diagnostics.fatal("duplicate composite definition: "
								+ cuDefinition.getName(),
								cuDefinition.getName());
					} else {
						if (allComposites != null) {
							allComposites.put(
									cuDefinition.getName().getValue(),
									cuDefinition);
						}
					}

				} else if (current.kind == Symbol.GRAPH_UPDATE) {
					expectIdentifier("Graph Update");
					UpdateGraphDefinition graphDefinition = new UpdateGraphDefinition(
							current.getValue());
					expectBecomes();
					expectLeftCurly();
					graphDefinition.setInitialProblem(parseInitialState());
					graphDefinition.setTransitions(parseTransitions());
					expectRightCurly();
					UpdateGraphGenerator.addGraphDefinition(graphDefinition);
				} else if (current.kind == Symbol.CONTROL_STACK) {

					ControlStackDefinition def = this.controlStackDefinition();
					ControlStackDefinition.addDefinition(def);

					CompositionExpression c = new CompositionExpression(
							postconditionDefinitionManager);
					c.name = def.getName();
					c.setComposites(composites);
					c.processes = processes;
					c.compiledProcesses = compiled;
					c.controlStackEnvironments = new Vector<Symbol>();
					for (ControlTierDefinition tier : def) {
						c.controlStackEnvironments.add(tier.getEnvModel());
					}
					c.output = output;
					c.makeControlStack = true;
					if (allComposites != null) {
						allComposites.put(c.name.toString(), c);
					}
					if (composites.put(c.name.toString(), c) != null) {
						Diagnostics.fatal("duplicate composite definition: "
								+ c.name, c.name);
					}

				} else if (current.kind == Symbol.IMPORT) {
					nextSymbol();
					ProcessSpec p = importDefinition();
					if (processes.put(p.getName().toString(), p) != null) {
						Diagnostics.fatal(
								"duplicate process definition: " + p.getName(),
								p.getName());
					}
				} else if (current.kind == Symbol.ETRIGGEREDSCENARIO) {
					nextSymbol();
					// Check the syntax
					currentIs(Symbol.UPPERIDENT, "chart identifier expected");

					this.validateUniqueProcessName(current);

					// create the existential triggeredScenario with the given
					// identifier
					TriggeredScenarioDefinition eTSDefinition = new ExistentialTriggeredScenarioDefinition(
							current);

					nextSymbol();
					this.triggeredScenarioDefinition(eTSDefinition);
				} else if (current.kind == Symbol.UTRIGGEREDSCENARIO) {
					nextSymbol();
					// Check the syntax
					currentIs(Symbol.UPPERIDENT, "chart identifier expected");

					this.validateUniqueProcessName(current);

					// create the universal triggered Scenario with the given
					// identifier
					TriggeredScenarioDefinition uTSDefinition = new UniversalTriggeredScenarioDefinition(
							current);

					nextSymbol();
					this.triggeredScenarioDefinition(uTSDefinition);
				} else if (current.kind == Symbol.DISTRIBUTION) {
					this.distributionDefinition();
				} else if (current.kind == Symbol.DETERMINISTIC
						|| current.kind == Symbol.MINIMAL
						|| current.kind == Symbol.PROPERTY
						|| current.kind == Symbol.COMPOSE
						|| current.kind == Symbol.OPTIMISTIC
						|| current.kind == Symbol.PESSIMISTIC
						|| LTSUtils.isCompositionExpression(current)
						|| current.kind == Symbol.CLOUSURE
						|| current.kind == Symbol.ABSTRACT
						|| current.kind == Symbol.CONTROLLER
						|| current.kind == Symbol.CHECK_COMPATIBILITY
						|| current.kind == Symbol.COMPONENT
						|| current.kind == Symbol.PROBABILISTIC
						|| current.kind == Symbol.MDP
						|| current.kind == Symbol.STARENV
						|| current.kind == Symbol.PLANT
						|| current.kind == Symbol.CONTROLLED_DET
						|| current.kind == Symbol.SYNC_CONTROLLER) {
					// TODO: refactor needed. Some of the operations can be
					// combined, however
					// the parser does not allow some valid combinations. Also
					// the order of the operations
					// is not kept when the operations are applied

					boolean makeDet = false;
					boolean makeMin = false;
					boolean makeProp = false;
					boolean makeComp = false;
					boolean makeOptimistic = false;
					boolean makePessimistic = false;
					boolean makeClousure = false;
					boolean makeAbstract = false;
					boolean makeController = false;
					boolean makeSyncController = false;
					boolean checkCompatible = false;
					boolean makeComponent = false;
					boolean probabilistic = false;
					boolean isMDP = false;
					boolean isEnactment = false;
					boolean makeStarEnv = false;
					boolean makePlant = false;
					boolean makeControlledDet = false;
					Symbol controlledActions = null;

					if (current.kind == Symbol.CLOUSURE) {
						makeClousure = true;
						nextSymbol();
					}
					if (current.kind == Symbol.ABSTRACT) {
						makeAbstract = true;
						nextSymbol();
					}
					if (current.kind == Symbol.DETERMINISTIC) {
						makeDet = true;
						nextSymbol();
					}
					if (current.kind == Symbol.MINIMAL) {
						makeMin = true;
						nextSymbol();
					}
					if (current.kind == Symbol.COMPOSE) {
						makeComp = true;
						nextSymbol();
					}
					if (current.kind == Symbol.PROPERTY) {
						makeProp = true;
						nextSymbol();
					}
					if (current.kind == Symbol.OPTIMISTIC) {
						makeOptimistic = true;
						nextSymbol();
					}
					if (current.kind == Symbol.PESSIMISTIC) {
						makePessimistic = true;
						nextSymbol();
					}
					if (current.kind == Symbol.COMPONENT) {
						makeComponent = true;
						nextSymbol();
					}
					if (current.kind == Symbol.CONTROLLER) {
						makeController = true;
						nextSymbol();
					}
					if (current.kind == Symbol.SYNC_CONTROLLER) {
						makeSyncController = true;
						nextSymbol();
					}
					if (current.kind == Symbol.STARENV) {
						makeStarEnv = true;
						nextSymbol();
					}
					if (current.kind == Symbol.PLANT) {
						makePlant = true;
						nextSymbol();
					}
					if (current.kind == Symbol.CHECK_COMPATIBILITY) {
						checkCompatible = true;
						nextSymbol();
					}
					if (current.kind == Symbol.CONTROLLED_DET) {
						makeControlledDet = true;
						nextSymbol();
					}
					if (current.kind == Symbol.PROBABILISTIC) {
						probabilistic = true;
						nextSymbol();
					}
					if (current.kind == Symbol.MDP) {
						isMDP = true;
						nextSymbol();
					}
					if (current.kind == Symbol.ENACTMENT) {
						isEnactment = true;
						nextSymbol();
						if (current.kind == Symbol.LCURLY) {
							nextSymbol();
							controlledActions = current;
							nextSymbol();
							// }
							nextSymbol();
						}
					}

					if (current.kind != Symbol.OR
							&& current.kind != Symbol.PLUS_CA
							&& current.kind != Symbol.PLUS_CR
							&& current.kind != Symbol.MERGE) {
						ProcessSpec p = stateDefns();
						if (processes.put(p.getName(), p) != null) {
							Diagnostics.fatal("duplicate process definition: "
									+ p.getName(), p.getName());
						}
						p.isProperty = makeProp;
						p.isMinimal = makeMin;
						p.isDeterministic = makeDet;
						p.isOptimistic = makeOptimistic;
						p.isPessimistic = makePessimistic;
						p.isClousure = makeClousure;
						p.isAbstract = makeAbstract;
						p.isProbabilistic = probabilistic;
						p.isMDP = isMDP;
						p.isStarEnv = makeStarEnv;

						if (makeController || checkCompatible || makePlant
								|| makeControlledDet || makeSyncController) {
							Diagnostics
									.fatal("The operation requires a composite model.");
						}

						if (makeComponent) {
							Diagnostics
									.fatal("A component can only be created from a composite model.");
						}

						if (probabilistic
								&& (makeProp || makeMin || makeDet
										|| makeOptimistic || makePessimistic
										|| makeClousure || makeAbstract)) {
							Diagnostics
									.fatal("Probabilistic automata cannot be combined with other options.");
						}

						if (probabilistic != isMDP) { // x to account for future
														// probabilistic
														// variations
							Diagnostics
									.fatal("Probabilistic automata must be one of: mdp.");
						}
					} else if (LTSUtils.isCompositionExpression(current)) {
						CompositionExpression c = composition();
						c.setComposites(composites);
						c.processes = processes;
						c.compiledProcesses = compiled;
						c.output = output;
						c.makeDeterministic = makeDet;
						c.makeProperty = makeProp;
						c.makeMinimal = makeMin;
						c.makeCompose = makeComp;
						c.makeOptimistic = makeOptimistic;
						c.makePessimistic = makePessimistic;
						c.makeClousure = makeClousure;
						c.makeAbstract = makeAbstract;
						c.makeMDP = isMDP;
						c.makeEnactment = isEnactment;
						c.enactmentControlled = controlledActions;
						c.makeController = makeController;
						c.makeSyncController = makeSyncController;
						c.checkCompatible = checkCompatible;
						c.isStarEnv = makeStarEnv;
						c.isPlant = makePlant;
						c.isControlledDet = makeControlledDet;
						c.setMakeComponent(makeComponent);
						c.compositionType = compositionType;
						compositionType = -1;
						if (allComposites != null) {
							allComposites.put(c.name.toString(), c);
						}
						if (composites.put(c.name.toString(), c) != null) {
							Diagnostics
									.fatal("duplicate composite definition: "
											+ c.name, c.name);
						}
					}
				} else {
					ProcessSpec p = stateDefns();
					if (processes.put(p.getName(), p) != null) {
						Diagnostics.fatal(
								"duplicate process definition: " + p.getName(),
								p.getName());
					}
				}

				nextSymbol();
			}
		} catch (DuplicatedTriggeredScenarioDefinitionException e) {
			Diagnostics.fatal("duplicate Chart definition: " + e.getName());
		}
	}

	public static ProcessSpec getSpec(String processName) {
		Preconditions.checkNotNull(processName,
				"The process name cannot be null");
		return processes.get(processName);
	}

	private Symbol nextSymbol() {
		return (current = lex.nextSymbol());
	}

	private void pushSymbol() {
		lex.pushSymbol();
	}

	public Hashtable<String, CompositionExpression> getComposites() {
		return composites;
	}

	public Hashtable<String, ExplorerDefinition> getExplorers() {
		return explorers;
	}

	public Hashtable<String, ProcessSpec> getProcesses() {
		return processes;
	}

	public static CompositionExpression getComposite(String name) {
		return name != null ? allComposites.get(name) : null;
	}

	public static Hashtable<String, LabelledTransitionSystem> getCompiled() {
		return compiled;
	}

	public static Hashtable<String, CompositionExpression> getAllComposites() {
		return allComposites;
	}

	private void error(String errorMsg) {
		Diagnostics.fatal(errorMsg, current);
	}

	private void currentIs(int kind, String errorMsg) {
		if (current.kind != kind) {
			error(errorMsg);
		}
	}

	private void currentIs(Collection<Integer> possibleKind, String errorMsg) {

		if (!CollectionUtils.exists(possibleKind,
				PredicateUtils.equalPredicate(new Integer(current.kind)))) {
			error(errorMsg);
		}
	}

	/**
	 * Compiles the process specified by <i>name</i>.
	 * 
	 * @return
	 */
	public void compile() {
		processes = new Hashtable<>(); // processes
		composites = new Hashtable<>(); // composites
		explorers = new Hashtable<>();
		compiled = new Hashtable<>(); // compiled
		allComposites = new Hashtable<>(); // All composites
		preconditionDefinitionManager.reset();
		mapBoxReplacementName = new HashMap<>();
		postconditionDefinitionManager.reset();
		doparse(composites, processes, compiled);
	}

	public CompositeState checkPrecondition(String name, LTSOutput ltsOutput) {

		ProgressDefinition.compile();
		MenuDefinition.compile();
		PredicateDefinition.compileAll();
		AssertDefinition.compileAll(output);
		CompositionExpression ce = composites.get(name);
		if (ce != null) {

			// return ce.compose(null, );
			// Is a composition expression.
			// compileProcesses(processes, compiled, ltsOutput);
			// return noCompositionExpression(compiled);

		}
		return null;
	}

	public CompositeState continueCompilation(String name, LTSOutput ltsOutput) {

		ProgressDefinition.compile();
		MenuDefinition.compile();
		PredicateDefinition.compileAll();
		AssertDefinition.compileAll(output);
		CompositionExpression ce = composites.get(name);
		if (ce == null && composites.size() > 0) {
			if (explorers.containsKey(name)) {
				ExplorerDefinition explorerDefinition = explorers.get(name);
				Enumeration<CompositionExpression> e = composites.elements();
				CompositionExpression fce = e.nextElement();

				ce = new CompositionExpression(postconditionDefinitionManager);
				ce.name = new Symbol(123, name);
				ce.processes = fce.processes;
				ce.setComposites(fce.getComposites());
				ce.output = fce.output;
				ce.priorityIsLow = true;
				ce.compositionType = 45;
				ce.makeController = true;
				ce.goal = explorerDefinition.getGoal();
				ce.compiledProcesses = new Hashtable<String, LabelledTransitionSystem>(
						0);

				ce.body = new CompositeBody();
				ce.body.procRefs = new Vector<CompositeBody>(explorerDefinition
						.getView().size() + 1);

				for (int i = 0; i < explorerDefinition.getView().size(); i++) {
					CompositeBody aCompositeBody = new CompositeBody();
					aCompositeBody.singleton = new ProcessRef(
							postconditionDefinitionManager, false, true);
					aCompositeBody.singleton.name = explorerDefinition
							.getView().get(i);
					ce.body.procRefs.add(aCompositeBody);
				}

				for (int i = 0; i < explorerDefinition.getModel().size(); i++) {
					CompositeBody aCompositeBody = new CompositeBody();
					aCompositeBody.singleton = new ProcessRef(
							postconditionDefinitionManager, false, true);
					aCompositeBody.singleton.name = explorerDefinition
							.getModel().get(i);
					ce.body.procRefs.add(aCompositeBody);
				}
			} else {
				Enumeration<CompositionExpression> e = composites.elements();
				ce = e.nextElement();
			}
		}
		if (ce != null) {

			return ce.compose(null);
			// Is a composition expression.
			// compileProcesses(processes, compiled, ltsOutput);
			// return noCompositionExpression(compiled);

		} else {

			if (explorers.containsKey(name)) {
				ExplorerDefinition explorerDefinition = explorers.get(name);

				ce = new CompositionExpression(postconditionDefinitionManager);
				ce.name = new Symbol(123, name);
				ce.processes = processes;
				ce.output = output;
				ce.priorityIsLow = true;
				ce.compositionType = 45;
				ce.makeController = true;
				ce.goal = explorerDefinition.getGoal();
				ce.compiledProcesses = new Hashtable<String, LabelledTransitionSystem>(
						0);
				ce.body = new CompositeBody();
				ce.body.procRefs = new Vector<CompositeBody>(explorerDefinition
						.getView().size() + 1);

				for (int i = 0; i < explorerDefinition.getView().size(); i++) {
					CompositeBody aCompositeBody = new CompositeBody();
					aCompositeBody.singleton = new ProcessRef(
							postconditionDefinitionManager, false, true);
					aCompositeBody.singleton.name = explorerDefinition
							.getView().get(i);
					ce.body.procRefs.add(aCompositeBody);
				}

				for (int i = 0; i < explorerDefinition.getModel().size(); i++) {
					CompositeBody aCompositeBody = new CompositeBody();
					aCompositeBody.singleton = new ProcessRef(
							postconditionDefinitionManager, false, true);
					aCompositeBody.singleton.name = explorerDefinition
							.getModel().get(i);
					ce.body.procRefs.add(aCompositeBody);
				}

				return ce.compose(null);
			}

			// There is no composite expression.
			try {
				// All scenarios are synthesised
				this.addAllToCompiled(TriggeredScenarioDefinition
						.synthesiseAll(output));
			} catch (TriggeredScenarioTransformationException e) {
				throw new RuntimeException(e);
			}

			// All Distributions are compiled
			// try to distribute
			Set<DistributionDefinition> allDistributionDefinitions = DistributionDefinition
					.getAllDistributionDefinitions();
			for (DistributionDefinition aDistributionDefinition : allDistributionDefinitions) {
				Symbol systemModelId = aDistributionDefinition.getSystemModel();
				// check if the system model has been compiled
				LabelledTransitionSystem systemModel = compiled
						.get(systemModelId.getValue());
				if (systemModel == null) {
					// it needs to be compiled
					systemModel = this
							.compileSingleProcess((ProcessSpec) processes
									.get(systemModelId.getValue()));
				}
				Collection<LabelledTransitionSystem> distributedComponents = new HashSet<>();
				boolean isDistributionSuccessful = TransitionSystemDispatcher
						.tryDistribution(systemModel, aDistributionDefinition,
								output, distributedComponents);

				// Add the distributed components as compiled
				// add to compiled process
				for (LabelledTransitionSystem component : distributedComponents) {
					compiled.put(component.getName(), component);
				}

				if (!isDistributionSuccessful) {
					Diagnostics.fatal("Model " + systemModelId.getValue()
							+ " could not be distributed.", systemModelId);
				}
			}

			// All processes are compiled.
			compileProcesses(processes, compiled, ltsOutput);
			return noCompositionExpression(compiled);
		}
	}

	public static void makeFluents(Symbol symbol, Set<Fluent> involvedFluents) {
		AssertDefinition def = AssertDefinition
				.getDefinition(symbol.toString());
		if (def != null && !symbol.toString().equals(GeneralConstants.FALSE)
				&& !symbol.toString().equals(GeneralConstants.TRUE)) {
			adaptFormulaAndCreateFluents(def.getFormula(true), involvedFluents);
		} else {
			PredicateDefinition fdef = PredicateDefinition.get(symbol
					.toString());
			if (fdef != null) {
				adaptFormulaAndCreateFluents(new FormulaFactory().make(symbol),
						involvedFluents);
			} else if (symbol.toString().equals(GeneralConstants.FALSE)) {
				involvedFluents
						.add(new FluentImpl(
								symbol.toString(),
								new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(),
								new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(),
								false));
			} else if (symbol.toString().equals(GeneralConstants.TRUE)) {
				involvedFluents
						.add(new FluentImpl(
								symbol.toString(),
								new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(),
								new HashSet<MTSSynthesis.ar.dc.uba.model.language.Symbol>(),
								true));
			} else {
				Diagnostics.fatal("Fluent/assertion not defined [" + symbol
						+ "].");
			}
		}

	}

	/**
	 * Returns a CompactState representation of a process given its name or null
	 * if no such process is present
	 * 
	 * @param processName
	 *            the name of the process to be returned as a compactState
	 * @return a CompactState representation of an existing process give its
	 *         name
	 */
	public LabelledTransitionSystem getProcessCompactStateByName(
			String processName) {
		if (!processes.containsKey(processName)) {
			return null;
		}
		ProcessSpec processSpec = processes.get(processName);
		LabelledTransitionSystem compiled;
		if (!processSpec.imported()) {
			StateMachine one = new StateMachine(processSpec);
			compiled = one.makeCompactState();
			output.outln("Compiled: " + compiled.getName());

		} else {
			compiled = new AutCompactState(processSpec.getSymbol(),
					processSpec.importFile);
			output.outln("Imported: " + compiled.getName());
		}
		return compiled;
	}

	private static Formula adaptFormulaAndCreateFluents(
			ltsa.lts.ltl.formula.Formula formula, Set<Fluent> involvedFluents) {
		// create a visitor for the formula
		FormulaTransformerVisitor formulaTransformerVisitor = new FormulaTransformerVisitor();
		formula.accept(formulaTransformerVisitor);

		// After visiting the formula, the visitor has the transformed formula
		// and the involved fluents
		involvedFluents.addAll(formulaTransformerVisitor.getInvolvedFluents());
		return formulaTransformerVisitor.getTransformedFormula();
	}

	/**
	 * Add all the CompactState in compiledToBeAdded to the compiled table with
	 * the name of the CompactState as key.
	 * 
	 * @param compiledToBeAdded
	 */
	private void addAllToCompiled(
			Collection<LabelledTransitionSystem> compiledToBeAdded) {
		for (LabelledTransitionSystem compactState : compiledToBeAdded) {
			compiled.put(compactState.getName(), compactState);
		}
	}

	/**
	 * put the compiled definitions in Hashtable compiled
	 * 
	 * @param processSpecificationMap
	 * @param compiled
	 */
	private void compileProcesses(
			Hashtable<String, ProcessSpec> processSpecificationMap,
			Hashtable<String, LabelledTransitionSystem> compiled,
			LTSOutput ltsOutput) {

		for (ProcessSpec processSpec : processSpecificationMap.values()) {

			LabelledTransitionSystem compiledProcess = this
					.compileSingleProcess(processSpec);

			Vector<LabelledTransitionSystem> machines = new Vector<>();

			machines.add(compiledProcess);
			compiled.put(compiledProcess.getName(), compiledProcess);
		}

		AssertDefinition.compileConstraints(output, compiled);
	}

	private LabelledTransitionSystem compileSingleProcess(
			ProcessSpec processSpec) {
		LabelledTransitionSystem compiledProcess;
		if (!processSpec.imported()) {
			StateMachine one = new StateMachine(processSpec);
			compiledProcess = one.makeCompactState();
			output.outln("Compiled: " + compiledProcess.getName());

		} else {
			compiledProcess = new AutCompactState(processSpec.getSymbol(),
					processSpec.importFile);
			output.outln("Imported: " + compiledProcess.getName());
		}
		return compiledProcess;
	}

	private ProcessSpec compileReplacement() {
		currentIs(Symbol.UPPERIDENT,
				"You have to specify the name of the process the replacement refers to.");
		nextSymbol();

		currentIs(Symbol.UPPERIDENT,
				"You have to specify the name of the box the replacement refers to.");
		Symbol box = current;

		nextSymbol();
		ProcessSpec replacementSpec = stateDefns();
		
		
		if (mapBoxReplacementName.containsKey(box.getValue())) {
			Diagnostics.fatal("duplicate replacement for the box: " + box);
		} else {
			mapBoxReplacementName
					.put(box.getValue(), replacementSpec.getName());
		}
		if (processes.containsKey(replacementSpec.getName())) {
			Diagnostics.fatal("duplicate replacement definition: "
					+ replacementSpec.getName(), replacementSpec.getName());
		}
		replacementSpec.setReplacement(true);

		return replacementSpec;
	}

	private CompositeState noCompositionExpression(
			Hashtable<String, LabelledTransitionSystem> compiledProcesses) {
		Vector<LabelledTransitionSystem> processesVector = new Vector<>(16);
		processesVector.addAll(compiledProcesses.values());
		return new CompositeState(processesVector);
	}

	private CompositionExpression composition() {
		currentIs(Symbol.OR, "|| expected");
		nextSymbol();
		CompositionExpression c = new CompositionExpression(
				postconditionDefinitionManager);
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		c.name = current;
		nextSymbol();
		paramDefns(c.initConstants, c.parameters);
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();
		c.body = compositebody();
		c.priorityActions = priorityDefn(c);

		this.priorizeMaybeActions(c.priorityActions);
		if (current.kind == Symbol.BACKSLASH || current.kind == Symbol.AT) {
			c.exposeNotHide = (current.kind == Symbol.AT);
			nextSymbol();
			c.alphaHidden = labelSet();
		}

		// Controller Synthesis
		if (Symbol.SINE == current.kind) {
			parseControllerGoal(c);
		}

		if (Symbol.BITWISE_OR == current.kind) {
			nextSymbol();
			this.parseComponentAlphabet(c);
		}
		currentIs(Symbol.DOT, "dot expected");
		return c;
	}

	private void parseComponentAlphabet(CompositionExpression c) {
		c.setComponentAlphabet(this.labelSet());
	}

	private CompositeBody compositebody() {
		CompositeBody b = new CompositeBody();
		if (current.kind == Symbol.IF) {
			nextSymbol();
			b.boolexpr = new Stack<Symbol>();
			expression(b.boolexpr);
			currentIs(Symbol.THEN, "keyword then expected");
			nextSymbol();
			b.thenpart = compositebody();
			if (current.kind == Symbol.ELSE) {
				nextSymbol();
				b.elsepart = compositebody();
			}
		} else if (current.kind == Symbol.FORALL) {
			nextSymbol();
			b.range = forallRanges();
			b.thenpart = compositebody();
		} else {
			// get accessors if any
			if (isLabel()) {
				ActionLabels el = labelElement();
				if (current.kind == Symbol.COLON_COLON) {
					b.accessSet = el;
					nextSymbol();
					if (isLabel()) {
						b.setPrefix(labelElement());
						currentIs(Symbol.COLON, " : expected");
						nextSymbol();
					}
				} else if (current.kind == Symbol.COLON) {
					b.setPrefix(el);
					nextSymbol();
				} else
					error(" : or :: expected");
			}
			if (current.kind == Symbol.LROUND) {
				b.procRefs = processRefs();
				b.relabelDefns = relabelDefns();
			} else {
				b.singleton = processRef();
				b.relabelDefns = relabelDefns();
			}
		}
		return b;
	}

	private ActionLabels forallRanges() {
		currentIs(Symbol.LSQUARE, "range expected");
		ActionLabels head = range();
		ActionLabels next = head;
		while (current.kind == Symbol.LSQUARE) {
			ActionLabels t = range();
			next.addFollower(t);
			next = t;
		}
		return head;
	}

	private Vector<CompositeBody> processRefs() {
		Vector<CompositeBody> procRefs = new Vector<>();
		currentIs(Symbol.LROUND, "( expected");
		nextSymbol();
		if (current.kind != Symbol.RROUND) {
			procRefs.addElement(compositebody());
			while (LTSUtils.isCompositionExpression(current)) {
				nextSymbol();
				procRefs.addElement(compositebody());
			}
			currentIs(Symbol.RROUND, ") expected");
		}
		nextSymbol();
		return procRefs;
	}

	private Vector<RelabelDefn> relabelDefns() {
		if (current.kind != Symbol.DIVIDE)
			return null;
		nextSymbol();
		return relabelSet();
	}

	private LabelSet priorityDefn(CompositionExpression c) {
		if (current.kind != Symbol.SHIFT_RIGHT
				&& current.kind != Symbol.SHIFT_LEFT)
			return null;
		if (current.kind == Symbol.SHIFT_LEFT)
			c.priorityIsLow = false;
		nextSymbol();
		return labelSet();
	}

	private Vector<RelabelDefn> relabelSet() {
		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();
		Vector<RelabelDefn> v = new Vector<>();
		relabelBoth(v, relabelDefn());
		while (current.kind == Symbol.COMMA) {
			nextSymbol();
			relabelBoth(v, relabelDefn());
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
		return v;
	}

	private void relabelBoth(Vector<RelabelDefn> v, RelabelDefn relabelDefn) {
		v.addElement(relabelDefn);
		this.relabelMTS(v, relabelDefn);
	}

	private RelabelDefn relabelDefn() {
		RelabelDefn r = new RelabelDefn();
		if (current.kind == Symbol.FORALL) {
			nextSymbol();
			r.range = forallRanges();
			r.defns = relabelSet();
		} else {
			r.newlabel = labelElement();
			currentIs(Symbol.DIVIDE, "/ expected");
			nextSymbol();
			r.oldlabel = labelElement();
		}
		return r;
	}

	private ProcessRef processRef() {
		ProcessRef p = new ProcessRef(postconditionDefinitionManager);
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		p.name = current;
		nextSymbol();
		p.actualParams = actualParameters();
		if (current.kind != Symbol.RROUND) {
			compositionType = current.kind;
		}
		return p;
	}

	private Vector<Stack<Symbol>> actualParameters() {
		if (current.kind != Symbol.LROUND)
			return null;
		Vector<Stack<Symbol>> v = new Vector<>();
		nextSymbol();
		Stack<Symbol> stk = new Stack<Symbol>();
		expression(stk);
		v.addElement(stk);
		while (current.kind == Symbol.COMMA) {
			nextSymbol();
			stk = new Stack<Symbol>();
			expression(stk);
			v.addElement(stk);
		}
		currentIs(Symbol.RROUND, ") - expected");
		nextSymbol();
		return v;
	}

	/**
	 * parses a process specification
	 * 
	 * @return a process specification
	 */
	private ProcessSpec stateDefns() {
		ProcessSpec p = new ProcessSpec();
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		Symbol temp = current;
		nextSymbol();
		// parses the parameters of the process
		paramDefns(p.init_constants, p.parameters);
		pushSymbol();
		current = temp;
		p.stateDefns.addElement(stateDefn());
		// parses the states of the system
		while (current.kind == Symbol.COMMA) {
			nextSymbol();
			p.stateDefns.addElement(stateDefn());
		}
		if (current.kind == Symbol.PLUS) {
			nextSymbol();
			p.alphaAdditions = labelSet();
		}
		for (StateDefn state : p.stateDefns) {
			if (state instanceof BoxStateDefn) {
				p.alphaAdditions.labels.add(((BoxStateDefn) state)
						.getInterface());

			}
		}
		p.alphaRelabel = relabelDefns();
		if (current.kind == Symbol.BACKSLASH || current.kind == Symbol.AT) {
			p.exposeNotHide = (current.kind == Symbol.AT);
			nextSymbol();
			p.alphaHidden = labelSet();
		}

		if (Symbol.SINE == current.kind) {
			parseControllerGoal(p);
		}

		p.getName();
		currentIs(Symbol.DOT, "dot expected");
		return p;
	}

	private void parseControllerGoal(ProcessSpec p) {
		expectLeftCurly();
		nextSymbol();
		currentIs(Symbol.UPPERIDENT, "goal identifier expected");
		p.goal = current;
		nextSymbol();
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
	}

	// TODO reutilizar codigo,los metodos son iguales. Pueden hacer una interfaz
	// hasGoal que tenga el metodo setGoal()
	private void parseControllerGoal(CompositionExpression c) {
		expectLeftCurly();
		nextSymbol();
		currentIs(Symbol.UPPERIDENT, "goal identifier expected");
		c.goal = current;
		nextSymbol();
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
	}

	private boolean isLabelSet() {
		if (current.kind == Symbol.LCURLY)
			return true;
		if (current.kind != Symbol.UPPERIDENT)
			return false;
		return LabelSet.constants.containsKey(current.toString());
	}

	private boolean isLabel() {
		return (isLabelSet() || current.kind == Symbol.IDENTIFIER || current.kind == Symbol.LSQUARE);
	}

	private ProcessSpec importDefinition() {
		currentIs(Symbol.UPPERIDENT, "imported process identifier expected");
		ProcessSpec p = new ProcessSpec(current);
		expectBecomes();
		nextSymbol();
		currentIs(Symbol.STRING_VALUE, " - imported file name expected");
		p.importFile = new File(currentDirectory, current.toString());
		return p;
	}

	private void animationDefinition() {
		currentIs(Symbol.UPPERIDENT, "animation identifier expected");
		MenuDefinition m = new MenuDefinition();
		m.name = current;
		expectBecomes();
		nextSymbol();
		currentIs(Symbol.STRING_VALUE, " - XML file name expected");
		m.params = current;
		nextSymbol();
		if (current.kind == Symbol.TARGET) {
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, " - target composition name expected");
			m.target = current;
			nextSymbol();
		}
		if (current.kind == Symbol.COMPOSE) {
			expectLeftCurly();
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "animation name expected");
			Symbol name = current;
			nextSymbol();
			m.addAnimationPart(name, relabelDefns());
			while (LTSUtils.isOrSymbol(current)) {
				nextSymbol();
				currentIs(Symbol.UPPERIDENT, "animation name expected");
				name = current;
				nextSymbol();
				m.addAnimationPart(name, relabelDefns());
			}
			currentIs(Symbol.RCURLY, "} expected");
			nextSymbol();
		}
		if (current.kind == Symbol.ACTIONS) {
			nextSymbol();
			m.actionMapDefn = relabelSet();
		}
		if (current.kind == Symbol.CONTROLS) {
			nextSymbol();
			m.controlMapDefn = relabelSet();
		}
		pushSymbol();
		if (MenuDefinition.definitions.put(m.name.toString(), m) != null) {
			Diagnostics.fatal("duplicate menu/animation definition: " + m.name,
					m.name);
		}
	}

	private void menuDefinition() {
		currentIs(Symbol.UPPERIDENT, "menu identifier expected");
		MenuDefinition m = new MenuDefinition();
		m.name = current;
		expectBecomes();
		nextSymbol();
		m.actions = labelElement();
		pushSymbol();
		if (MenuDefinition.definitions.put(m.name.toString(), m) != null) {
			Diagnostics.fatal("duplicate menu/animation definition: " + m.name,
					m.name);
		}
	}

	private void progressDefinition() {
		currentIs(Symbol.UPPERIDENT, "progress test identifier expected");
		ProgressDefinition p = new ProgressDefinition();
		p.name = current;
		nextSymbol();
		if (current.kind == Symbol.LSQUARE)
			p.range = forallRanges();
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();
		if (current.kind == Symbol.IF) {
			nextSymbol();
			p.pactions = labelElement();
			currentIs(Symbol.THEN, "then expected");
			nextSymbol();
			p.cactions = labelElement();
		} else {
			p.pactions = labelElement();
		}
		if (ProgressDefinition.definitions.put(p.name.toString(), p) != null) {
			Diagnostics.fatal("duplicate progress test: " + p.name, p.name);
		}
		pushSymbol();
	}

	private void setDefinition() {
		currentIs(Symbol.UPPERIDENT, "set identifier expected");
		Symbol temp = current;
		expectBecomes();
		nextSymbol();
		new LabelSet(temp, setValue());
		pushSymbol();
	}

	private LabelSet labelSet() {
		if (current.kind == Symbol.LCURLY) {
			return new LabelSet(setValue());
		} else {
			if (current.kind == Symbol.UPPERIDENT) {
				LabelSet ls = LabelSet.constants.get(current.toString());
				if (ls == null) {
					error("set definition not found for: " + current);
				}
				nextSymbol();
				return ls;
			} else {
				error("{ or set identifier expected");
				return null;
			}
		}
	}

	private Vector<ActionLabels> setValue() {
		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();
		Vector<ActionLabels> v = new Vector<>();
		v.addElement(labelElement());
		while (current.kind == Symbol.COMMA) {
			nextSymbol();
			v.addElement(labelElement());
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
		return v;
	}

	private ActionLabels labelElement() {
		if (current.kind != Symbol.IDENTIFIER && !isLabelSet()
				&& current.kind != Symbol.LSQUARE) {
			error("identifier, label set or range expected");
		}

		ActionLabels e = null;
		if (current.kind == Symbol.IDENTIFIER) {
			String toString = current.toString();
			if ("tau".equals(toString) || "tau?".equals(toString))
				error("'tau' cannot be used as an action label");
			e = new ActionName(current);
			nextSymbol();
		} else if (isLabelSet()) {
			LabelSet left = labelSet();
			if (current.kind == Symbol.BACKSLASH) {
				nextSymbol();
				LabelSet right = labelSet();
				e = new ActionSetExpr(left, right);
			} else {
				e = new ActionSet(left);
			}
		} else if (current.kind == Symbol.LSQUARE) {
			e = range();
		}

		if (current.kind == Symbol.DOT || current.kind == Symbol.LSQUARE) {
			if (current.kind == Symbol.DOT)
				nextSymbol();
			if (e != null)
				e.addFollower(labelElement());
		}

		return e;
	}

	private void constantDefinition(Hashtable<String, Value> p) {
		currentIs(Symbol.UPPERIDENT, "constant, upper case identifier expected");
		Symbol name = current;
		expectBecomes();
		nextSymbol();
		Stack<Symbol> tmp = new Stack<>();
		simpleExpression(tmp);
		pushSymbol();
		if (p.put(name.toString(), Expression.getValue(tmp, null, null)) != null) {
			Diagnostics.fatal("duplicate constant definition: " + name, name);
		}
	}

	/**
	 * parses the parameter of the process
	 * 
	 * @param p
	 * @param parameters
	 */
	private void paramDefns(Hashtable<String, Value> p,
			Vector<String> parameters) {
		if (current.kind == Symbol.LROUND) {
			nextSymbol();
			parameterDefinition(p, parameters);
			while (current.kind == Symbol.COMMA) {
				nextSymbol();
				parameterDefinition(p, parameters);
			}
			currentIs(Symbol.RROUND, ") expected");
			nextSymbol();
		}
	}

	private void parameterDefinition(Hashtable<String, Value> p,
			Vector<String> parameters) {
		currentIs(Symbol.UPPERIDENT,
				"parameter, upper case identifier expected");
		Symbol name = current;
		expectBecomes();
		nextSymbol();
		Stack<Symbol> tmp = new Stack<>();
		expression(tmp);
		pushSymbol();
		if (p.put(name.toString(), Expression.getValue(tmp, null, null)) != null) {
			Diagnostics.fatal("duplicate parameter definition: " + name, name);
		}
		if (parameters != null) {
			parameters.addElement(name.toString());
			nextSymbol();
		}
	}

	/**
	 * parses a state of the state machine
	 * 
	 * @return
	 */
	private StateDefn stateDefn() {
		StateDefn s;
		if (current.kind == Symbol.FINAL) {
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "process identifier expected");
			s = new StateDefn(current);
			parseState(s);
			s.setFinal(true);
		} else {
			if (current.kind == Symbol.BOX) {
				nextSymbol();
				currentIs(Symbol.UPPERIDENT, "process identifier expected");
				BoxStateDefn box = new BoxStateDefn(current);
				parseState(box);
				box.setInterface(this.parseBoxInterface());
				s = box;

			} else {
				currentIs(Symbol.UPPERIDENT, "process identifier expected");
				s = new StateDefn(current);
				parseState(s);

			}
		}

		return s;
	}

	private void parseState(StateDefn s) {
		nextSymbol();
		if (current.kind == Symbol.DOT) {
			return;
		}
		if (current.kind == Symbol.AT) {
			s.accept = true;
			nextSymbol();
		}
		if (current.kind == Symbol.DOT || current.kind == Symbol.LSQUARE) {
			if (current.kind == Symbol.DOT) {
				nextSymbol();
			}
			s.range = labelElement();
		}
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();
		s.setStateExpr(stateExpr());
	}

	private ActionLabels parseBoxInterface() {
		currentIs(Symbol.LSQUARE, "[ expected");
		nextSymbol();
		ActionLabels ts = labelElement();
		currentIs(Symbol.RSQUARE, "] expected");
		nextSymbol();
		return ts;
	}

	private Stack<Symbol> getEvaluatedExpression() {
		Stack<Symbol> tmp = new Stack<>();
		simpleExpression(tmp);
		BigDecimal v = Expression.evaluate(tmp, null, null);
		tmp = new Stack<>();
		if (LTSUtils.isInteger(v)) {
			tmp.push(new Symbol(Symbol.INT_VALUE, v));
		} else {
			tmp.push(new Symbol(Symbol.DOUBLE_VALUE, v));
		}

		return tmp;
	}

	private void defDefinition() {
		currentIs(Symbol.UPPERIDENT, "def name, upper case identifier expected");
		Symbol nameSymbol = current;
		Def d = new Def(nameSymbol.getValue());
		nextSymbol();
		currentIs(Symbol.LROUND, "( expected");
		nextSymbol();
		while (current.kind != Symbol.RROUND) {
			currentIs(Symbol.IDENTIFIER, "identifier expected for def argument");
			d.addParameter(current);
			nextSymbol();
			if (current.kind == Symbol.COMMA)
				nextSymbol();
			else
				currentIs(Symbol.RROUND, ") expected");
		}
		expectBecomes();
		nextSymbol();
		expression(d.getExpressionStack());
		if (Def.put(d))
			Diagnostics.fatal("duplicate def definition: " + nameSymbol,
					nameSymbol);
		pushSymbol();
	}

	private void rangeDefinition() {
		currentIs(Symbol.UPPERIDENT,
				"range name, upper case identifier expected");
		Symbol name = current;
		expectBecomes();
		nextSymbol();
		Range r = new Range();
		r.low = getEvaluatedExpression();
		currentIs(Symbol.DOT_DOT, "..  expected");
		nextSymbol();
		r.high = getEvaluatedExpression();
		if (Range.ranges.put(name.toString(), r) != null) {
			Diagnostics.fatal("duplicate range definition: " + name, name);
		}
		pushSymbol();
	}

	private ActionLabels range() { // this is a mess.. needs to be rewritten
		if (current.kind == Symbol.LSQUARE) {
			nextSymbol();
			ActionLabels r;
			Stack<Symbol> low = null;
			Stack<Symbol> high = null;
			if (current.kind != Symbol.IDENTIFIER) {
				if (isLabelSet()) {
					r = new ActionSet(labelSet());
				} else if (current.kind == Symbol.UPPERIDENT
						&& Range.ranges.containsKey(current.toString())) {
					r = new ActionRange(Range.ranges.get(current.toString()));
					nextSymbol();
				} else {
					low = new Stack<Symbol>();
					expression(low);
					r = new ActionExpr(low);
				}
				if (current.kind == Symbol.DOT_DOT) {
					nextSymbol();
					high = new Stack<>();
					expression(high);
					r = new ActionRange(low, high);
				}
			} else {
				Symbol varname = current;
				nextSymbol();
				if (current.kind == Symbol.COLON) {
					nextSymbol();
					if (isLabelSet()) {
						r = new ActionVarSet(varname, labelSet());
					} else if (current.kind == Symbol.UPPERIDENT
							&& Range.ranges.containsKey(current.toString())) {
						r = new ActionVarRange(varname,
								(Range) Range.ranges.get(current.toString()));
						nextSymbol();
					} else {
						low = new Stack<>();
						expression(low);
						currentIs(Symbol.DOT_DOT, "..  expected");
						nextSymbol();
						high = new Stack<>();
						expression(high);
						r = new ActionVarRange(varname, low, high);
					}
				} else {
					pushSymbol();
					current = varname;
					low = new Stack<>();
					expression(low);
					if (current.kind == Symbol.DOT_DOT) {
						nextSymbol();
						high = new Stack<>();
						expression(high);
						r = new ActionRange(low, high);
					} else
						r = new ActionExpr(low);
				}
			}
			currentIs(Symbol.RSQUARE, "] expected");
			nextSymbol();
			return r;
		} else
			return null;
	}

	/**
	 * parses an expression associated with a state
	 * 
	 * @return the expression associated with a state
	 */
	private StateExpr stateExpr() {
		StateExpr s = new StateExpr();
		if (current.kind == Symbol.UPPERIDENT) {
			stateRef(s);
		} else {
			if (current.kind == Symbol.IF) {
				nextSymbol();
				s.boolexpr = new Stack<>();
				expression(s.boolexpr);
				currentIs(Symbol.THEN, "keyword then expected");
				nextSymbol();
				s.thenpart = stateExpr();
				if (current.kind == Symbol.ELSE) {
					nextSymbol();
					s.elsepart = stateExpr();
				} else {
					Symbol stop = new Symbol(Symbol.UPPERIDENT, "STOP");
					StateExpr se = new StateExpr();
					se.name = stop;
					s.elsepart = se;
				}
			} else {
				if (current.kind == Symbol.LROUND) {
					nextSymbol();
					if (current.kind == Symbol.FOREACH) {
						nextSymbol();
						s.actions = labelElement();
					}
					choiceExpr(s);
					currentIs(Symbol.RROUND, ") expected");
					nextSymbol();
				} else {
					error(" (, if or process identifier expected");
				}
			}
		}
		return s;
	}

	private void stateRef(StateExpr s) {
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		s.name = current;
		nextSymbol();
		while (current.kind == Symbol.SEMICOLON
				|| current.kind == Symbol.LROUND) {
			s.addSeqProcessRef(new SeqProcessRef(s.name, actualParameters()));
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "process identifier expected");
			s.name = current;
			nextSymbol();
		}
		if (current.kind == Symbol.LSQUARE) {
			s.expr = new Vector<Stack<Symbol>>();
			while (current.kind == Symbol.LSQUARE) {
				nextSymbol();
				Stack<Symbol> x = new Stack<>();
				expression(x);
				s.expr.addElement(x);
				currentIs(Symbol.RSQUARE, "] expected");
				nextSymbol();
			}
		}
	}

	private void choiceExpr(StateExpr s) {
		s.choices = new Vector<ChoiceElement>();
		s.choices.addElement(choiceElement());
		while (current.kind == Symbol.BITWISE_OR) {
			nextSymbol();
			s.choices.addElement(choiceElement());
		}
	}

	private ChoiceElement choiceElement() {
		boolean isProbabilistic = false;
		ChoiceElement first = new ChoiceElement();
		if (current.kind == Symbol.WHEN) {
			nextSymbol();
			first.setGuard(new Stack<Symbol>());
			expression(first.getGuard());
		}
		first.action = labelElement();
		currentIs(Symbol.ARROW, "-> expected");
		ChoiceElement next = first;
		ChoiceElement last = first;
		nextSymbol();
		// TODO EPAVESE for now I only restrict to ONE transition per
		// transformation. This really asks for a yacc approach
		// we expect targets of the form {<float constant> : UPPERIDENT (+
		// <float constant> : UPPERIDENT)* }
		// TODO EPAVESE this is not ok, isProbabilistic should be globally
		// defined as a machine, not a transition
		if (current.kind == Symbol.LCURLY) {
			isProbabilistic = true;
			nextSymbol();
		}

		if (isProbabilistic
				&& (current.kind == Symbol.DOUBLE_VALUE || current.kind == Symbol.UPPERIDENT)) {
			ProbabilisticChoiceElement newFirst = new ProbabilisticChoiceElement(
					first);
			int bundle = ProbabilisticTransition.getNextProbBundle();

			BigDecimal totalProbs = BigDecimal.ZERO;
			StateExpr stateExpression = new StateExpr();
			stateExpression.choices = new Vector<ChoiceElement>();
			while (current.kind == Symbol.DOUBLE_VALUE
					|| current.kind == Symbol.UPPERIDENT) {
				BigDecimal nextProb;
				if (current.kind == Symbol.DOUBLE_VALUE)
					nextProb = current.doubleValue();
				else {
					if (!Expression.constants.containsKey(current.toString())) {
						error("Identifier " + current.toString()
								+ " is undefined");
					}
					Value val = (Value) Expression.constants.get(current
							.toString());
					nextProb = val.doubleValue();
				}
				totalProbs = totalProbs.add(nextProb);
				nextSymbol();
				currentIs(Symbol.COLON, "':' expected");
				nextSymbol();
				currentIs(Symbol.UPPERIDENT, "process identifier expected");

				// TODO get the process identifier, build the
				// (Probabilistic)ChoiceElement
				stateExpression = stateExpr();
				newFirst.addProbabilisticChoice(nextProb, bundle,
						stateExpression);

				if (current.kind != Symbol.PLUS
						&& current.kind != Symbol.RCURLY)
					error("'+', '}' expected");
				if (current.kind == Symbol.PLUS) {
					nextSymbol();
					if (current.kind != Symbol.DOUBLE_VALUE
							&& current.kind != Symbol.UPPERIDENT)
						error("Float constant expected");
				}
			}
			if (totalProbs.compareTo(BigDecimal.ONE) != 0)
				error("Probabilities should add up to 1 -- "
						+ totalProbs.toString());

			currentIs(Symbol.RCURLY, "} expected");
			nextSymbol();
			return newFirst;
		} else {
			while (current.kind == Symbol.IDENTIFIER
					|| current.kind == Symbol.LSQUARE || isLabelSet()) {
				StateExpr ex = new StateExpr();
				next = new ChoiceElement();
				next.action = labelElement();
				ex.choices = new Vector<ChoiceElement>();
				ex.choices.addElement(next);
				last.stateExpr = ex;
				last = next;
				currentIs(Symbol.ARROW, "-> expected");
				nextSymbol();
			}
			next.stateExpr = stateExpr();
			return first;
		}
	}

	private Symbol event() {
		currentIs(Symbol.IDENTIFIER, "event identifier expected");
		Symbol e = current;
		nextSymbol();
		return e;
	}

	// LABELCONSTANT -------------------------------

	private ActionLabels labelConstant() {
		nextSymbol();
		ActionLabels el = labelElement();
		if (el != null) {
			return el;
		} else
			error("label definition expected");
		return null;
	}

	// set selection @(set , expr)
	private void setSelect(Stack<Symbol> expr) {
		Symbol op = current;
		nextSymbol();
		currentIs(Symbol.LROUND, "( expected to start set index selection");
		Symbol temp = current; // preserve marker
		temp.setAny(labelConstant());
		temp.kind = Symbol.LABELCONST;
		expr.push(temp);
		currentIs(Symbol.COMMA, ", expected before set index expression");
		nextSymbol();
		expression(expr);
		currentIs(Symbol.RROUND, ") expected to end set index selection");
		nextSymbol();
		expr.push(op);
	}

	// UNARY ---------------------------------
	private void unary(Stack<Symbol> expr) { // +, -, identifier,
		Symbol unary_operator;
		switch (current.kind) {
		case Symbol.PLUS:
			unary_operator = current;
			unary_operator.kind = Symbol.UNARY_PLUS;
			nextSymbol();
			break;
		case Symbol.MINUS:
			unary_operator = current;
			unary_operator.kind = Symbol.UNARY_MINUS;
			nextSymbol();
			break;
		case Symbol.PLING:
		case Symbol.SINE:
			unary_operator = current;
			nextSymbol();
			break;

		default:
			unary_operator = null;
		}
		switch (current.kind) {
		case Symbol.UPPERIDENT:
			Def d = Def.get(current);
			if (d != null) {
				nextSymbol();
				currentIs(Symbol.LROUND, "( expected to assign def arguments");
				nextSymbol();
				List<Stack<Symbol>> arguments = new ArrayList<Stack<Symbol>>();
				while (current.kind != Symbol.RROUND) {
					Stack<Symbol> arg = new Stack<Symbol>();
					arguments.add(arg);
					expression(arg);
					if (arguments.size() < d.getParameterCount()) {
						currentIs(Symbol.COMMA,
								"',' expected delimiting def arguments");
						nextSymbol();
					} else {
						currentIs(Symbol.RROUND,
								") expected to end def arguments");
					}
				}
				nextSymbol();
				d.pushExpressionStack(arguments, expr);
				break;
			}
		case Symbol.IDENTIFIER:
		case Symbol.INT_VALUE:
		case Symbol.DOUBLE_VALUE:
			expr.push(current);
			nextSymbol();
			break;
		case Symbol.LROUND:
			nextSymbol();
			expression(expr);
			currentIs(Symbol.RROUND, ") expected to end expression");
			nextSymbol();
			break;
		case Symbol.HASH:
			unary_operator = new Symbol(current);
		case Symbol.QUOTE: // this is a labelConstant
			Symbol temp = current; // preserve marker
			temp.setAny(labelConstant());
			temp.kind = Symbol.LABELCONST;
			expr.push(temp);
			break;
		case Symbol.AT:
			setSelect(expr);
			break;
		default:
			error("syntax error in expression");
		}

		if (unary_operator != null)
			expr.push(unary_operator);
	}

	// POWERS / ROOTS
	private void exponential(Stack<Symbol> expr) { // **
		unary(expr);
		while (current.kind == Symbol.POWER) {
			Symbol op = current;
			nextSymbol();
			unary(expr);
			expr.push(op);
		}
	}

	// MULTIPLICATIVE
	private void multiplicative(Stack<Symbol> expr) { // *, /, %
		exponential(expr);
		while (current.kind == Symbol.STAR || current.kind == Symbol.DIVIDE
				|| current.kind == Symbol.BACKSLASH
				|| current.kind == Symbol.MODULUS) {
			Symbol op = current;
			nextSymbol();
			exponential(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// ADDITIVE

	private void additive(Stack<Symbol> expr) { // +, -
		multiplicative(expr);
		while (current.kind == Symbol.PLUS || current.kind == Symbol.MINUS) {
			Symbol op = current;
			nextSymbol();
			multiplicative(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// SHIFT

	private void shift(Stack<Symbol> expr) { // <<, >>
		additive(expr);
		while (current.kind == Symbol.SHIFT_LEFT
				|| current.kind == Symbol.SHIFT_RIGHT) {
			Symbol op = current;
			nextSymbol();
			additive(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// RELATIONAL

	private void relational(Stack<Symbol> expr) { // <, <=, >, >=
		shift(expr);
		while (current.kind == Symbol.LESS_THAN
				|| current.kind == Symbol.LESS_THAN_EQUAL
				|| current.kind == Symbol.GREATER_THAN
				|| current.kind == Symbol.GREATER_THAN_EQUAL) {
			Symbol op = current;
			nextSymbol();
			shift(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// EQUALITY

	private void equality(Stack<Symbol> expr) { // ==, !=
		relational(expr);
		while (current.kind == Symbol.EQUALS
				|| current.kind == Symbol.NOT_EQUAL) {
			Symbol op = current;
			nextSymbol();
			relational(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// AND

	private void and(Stack<Symbol> expr) { // &
		equality(expr);
		while (current.kind == Symbol.BITWISE_AND) {
			Symbol op = current;
			nextSymbol();
			equality(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// EXCLUSIVE_OR

	private void exclusiveOr(Stack<Symbol> expr) { // ^
		and(expr);
		while (current.kind == Symbol.CIRCUMFLEX) {
			Symbol op = current;
			nextSymbol();
			and(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// INCLUSIVE_OR

	private void inclusive_or(Stack<Symbol> expr) { // |
		exclusiveOr(expr);
		while (current.kind == Symbol.BITWISE_OR) {
			Symbol op = current;
			nextSymbol();
			exclusiveOr(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// LOGICAL_AND

	private void logical_and(Stack<Symbol> expr) { // &&
		inclusive_or(expr);
		while (current.kind == Symbol.AND) {
			Symbol op = current;
			nextSymbol();
			inclusive_or(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// LOGICAL_OR

	private void logical_or(Stack<Symbol> expr) { // ||
		logical_and(expr);
		while (current.kind == Symbol.OR) {
			Symbol op = current;
			nextSymbol();
			logical_and(expr);
			expr.push(op);
		}
	}

	// _______________________________________________________________________________________
	// TERNARY_CONDITIOAL

	private void ternary_conditional(Stack<Symbol> expr) { // _ ? _ : _
		logical_or(expr);
		if (current.kind == Symbol.QUESTION) {
			Symbol op1 = current;
			nextSymbol();
			ternary_conditional(expr);

			currentIs(Symbol.COLON, "':' expected");
			nextSymbol();
			ternary_conditional(expr);

			expr.push(op1);
		}
	}

	// _______________________________________________________________________________________
	// EXPRESSION

	private void expression(Stack<Symbol> expr) {
		ternary_conditional(expr);
	}

	// this is used to avoid a syntax problem
	// when a parallel composition
	// follows a range or constant definition e.g.
	// const N = 3
	// ||S = (P || Q)
	private void simpleExpression(Stack<Symbol> expr) {
		additive(expr);
	}

	// _______________________________________________________________________________________
	// LINEAR TEMPORAL LOGIC ASSERTIONS

	private void assertDefinition(boolean isConstraint, boolean isProperty) {
		Hashtable<String, Value> initparams = new Hashtable<>();
		Vector<String> params = new Vector<>();
		LabelSet ls = null;

		currentIs(Symbol.UPPERIDENT, "LTL property identifier expected");
		Symbol name = current;
		nextSymbol();
		paramDefns(initparams, params);
		currentIs(Symbol.BECOMES, "= expected");
		next_symbol_mod();

		FormulaSyntax formula = ltlUnary();

		if (current.kind == Symbol.PLUS) {
			nextSymbol();
			ls = labelSet();
		}
		pushSymbol();
		this.validateUniqueProcessName(name);

		AssertDefinition.put(name, formula, ls, initparams, params,
				isConstraint, isProperty);

		// Negation of the formula
		if (!(isConstraint && isProperty)) {
			Symbol notName = new Symbol(name);
			notName.setString(AssertDefinition.NOT_DEF + notName.getValue());
			Symbol s = new Symbol(Symbol.PLING);
			FormulaSyntax notF = FormulaSyntax.make(null, s, formula);

			this.validateUniqueProcessName(notName);
			AssertDefinition.put(notName, notF, ls, initparams, params,
					isConstraint, isProperty);
		}
	}

	// _______________________________________________________________________________________
	// LINEAR TEMPORAL LOGIC ASSERTIONS
	private void assertPrecondition() {
		Hashtable<String, Value> initparams = new Hashtable<>();
		Vector<String> params = new Vector<>();
		LabelSet ls = null;
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		Symbol process = current;
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "black box state identifier expected");
		Symbol box = current;
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "LTL property identifier expected");
		Symbol name = current;
		// LTLAdditionalSymbolTable.preconditionBoxMap.put(name, box);
		nextSymbol();
		mapsEachPreconditionToTheCorrespondingBox.put(name.getValue(),
				box.getValue());
		mapsEachPreconditionToTheCorrespondingProcess.put(name.getValue(),
				process.getValue());
		paramDefns(initparams, params);
		currentIs(Symbol.BECOMES, "= expected");
		next_symbol_mod();

		FormulaSyntax formula = ltlUnary();

		if (current.kind == Symbol.PLUS) {
			nextSymbol();
			ls = labelSet();
		}
		pushSymbol();
		this.validateUniqueProcessName(name);
		preconditionDefinitionManager.put(name, formula, ls, initparams,
				params, process.getValue(), box.getValue());

		Symbol notName = new Symbol(name);
		notName.setString(AssertDefinition.NOT_DEF + notName.getValue());
		Symbol s = new Symbol(Symbol.PLING);
		FormulaSyntax notF = FormulaSyntax.make(null, s, formula);

		this.validateUniqueProcessName(notName);
		preconditionDefinitionManager.put(notName, notF, ls, initparams,
				params, process.getValue(), box.getValue());
	}

	private void assertPostcondition() {
		Hashtable<String, Value> initparams = new Hashtable<>();
		Vector<String> params = new Vector<>();
		LabelSet ls = null;
		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		Symbol process = current;
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "black box state identifier expected");
		Symbol box = current;
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "LTL property identifier expected");
		Symbol name = current;
		nextSymbol();
		paramDefns(initparams, params);
		currentIs(Symbol.BECOMES, "= expected");
		next_symbol_mod();

		FormulaSyntax formula = ltlUnary();

		if (current.kind == Symbol.PLUS) {
			nextSymbol();
			ls = labelSet();
		}
		pushSymbol();
		this.validateUniqueProcessName(name);
		postconditionDefinitionManager.put(name, formula, ls, initparams,
				params, box.toString(), process.toString());

		Symbol notName = new Symbol(name);
		notName.setString(AssertDefinition.NOT_DEF + notName.getValue());

		this.validateUniqueProcessName(notName);

	}

	/**
	 * Validates that there is no process or composite process with name
	 * designated by <code>processName</code> If so it reports the fatal error
	 * 
	 */
	private void validateUniqueProcessName(Symbol processName) {
		if (processes != null
				&& processes.get(processName.toString()) != null
				|| composites != null
				&& composites.get(processName.toString()) != null
				|| (AssertDefinition.getDefinition(processName.toString()) != null)
				|| (TriggeredScenarioDefinition.contains(processName))
				|| DistributionDefinition.contains(processName)) {

			Diagnostics.fatal(
					"name already defined  " + processName.toString(),
					processName);
		}
	}

	// do not want X and U to be keywords outside of LTL expressions
	private Symbol modify(Symbol s) {
		if (s.kind != Symbol.UPPERIDENT)
			return s;
		if (s.toString().equals("X")) {
			Symbol nx = new Symbol(s);
			nx.kind = Symbol.NEXTTIME;
			return nx;
		}
		if (s.toString().equals("U")) {
			Symbol ut = new Symbol(s);
			ut.kind = Symbol.UNTIL;
			return ut;
		}
		if (s.toString().equals("W")) {
			Symbol wut = new Symbol(s);
			wut.kind = Symbol.WEAKUNTIL;
			return wut;
		}
		return s;
	}

	private void next_symbol_mod() {
		nextSymbol();
		current = modify(current);
	}

	// _______________________________________________________________________________________
	// LINEAR TEMPORAL LOGIC EXPRESSION

	private FormulaSyntax ltlUnary() { // !,<>,[]
		Symbol op = current;
		switch (current.kind) {
		case Symbol.PLING:
		case Symbol.NEXTTIME:
		case Symbol.EVENTUALLY:
		case Symbol.ALWAYS:
			next_symbol_mod();
			return FormulaSyntax.make(null, op, ltlUnary());
		case Symbol.UPPERIDENT:
			next_symbol_mod();
			if (current.kind == Symbol.LSQUARE) {
				ActionLabels range = forallRanges();
				current = modify(current);
				return FormulaSyntax.make(op, range);
			} else if (current.kind == Symbol.LROUND) {
				Vector<Stack<Symbol>> actparams = actualParameters();
				return FormulaSyntax.make(op, actparams);
			} else {
				return FormulaSyntax.make(op);
			}
		case Symbol.LROUND:
			next_symbol_mod();
			FormulaSyntax right = ltlOr();
			currentIs(Symbol.RROUND, ") expected to end LTL expression");
			next_symbol_mod();
			return right;
		case Symbol.IDENTIFIER:
		case Symbol.LSQUARE:
		case Symbol.LCURLY:
			ActionLabels ts = labelElement();
			pushSymbol();
			next_symbol_mod();
			return FormulaSyntax.make(ts);
		case Symbol.EXISTS:
			next_symbol_mod();
			ActionLabels ff = forallRanges();
			pushSymbol();
			next_symbol_mod();
			return FormulaSyntax.make(new Symbol(Symbol.OR), ff, ltlUnary());
		case Symbol.FORALL:
			next_symbol_mod();
			ff = forallRanges();
			pushSymbol();
			next_symbol_mod();
			return FormulaSyntax.make(new Symbol(Symbol.AND), ff, ltlUnary());
		default:
			Diagnostics.fatal("syntax error in LTL expression", current);
		}
		return null;
	}

	// _______________________________________________________________________________________
	// LTL_AND

	private FormulaSyntax ltlAnd() { // &
		FormulaSyntax left = ltlUnary();
		while (current.kind == Symbol.AND) {
			Symbol op = current;
			next_symbol_mod();
			FormulaSyntax right = ltlUnary();
			left = FormulaSyntax.make(left, op, right);
		}
		return left;
	}

	// _______________________________________________________________________________________
	// LTL_OR

	private FormulaSyntax ltlOr() { // |
		FormulaSyntax left = ltlBinary();
		while (LTSUtils.isOrSymbol(current)) {
			Symbol op = current;
			next_symbol_mod();
			FormulaSyntax right = ltlBinary();
			left = FormulaSyntax.make(left, op, right);
		}
		return left;
	}

	// _______________________________________________________________________________________
	// LTLBINARY

	private FormulaSyntax ltlBinary() { // until, ->
		FormulaSyntax left = ltlAnd();
		if (current.kind == Symbol.UNTIL || current.kind == Symbol.WEAKUNTIL
				|| current.kind == Symbol.ARROW
				|| current.kind == Symbol.EQUIVALENT) {
			Symbol op = current;
			next_symbol_mod();
			FormulaSyntax right = ltlAnd();
			left = FormulaSyntax.make(left, op, right);
		}
		return left;
	}

	//
	// ___________________________________________________________________________________
	// STATE PREDICATE DEFINITIONS

	private void predicateDefinition() {
		currentIs(Symbol.UPPERIDENT, "predicate identifier expected");
		Symbol name = current;
		ActionLabels range = null;
		nextSymbol();
		if (current.kind == Symbol.LSQUARE) {
			range = forallRanges();
		}
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();
		currentIs(Symbol.LESS_THAN, "< expected");
		nextSymbol();
		ActionLabels initialFluentActions = labelElement();
		currentIs(Symbol.COMMA, ", expected");
		nextSymbol();
		ActionLabels finalFluentActions = labelElement();
		currentIs(Symbol.GREATER_THAN, "> expected");
		nextSymbol();
		if (current.kind == Symbol.INIT) {
			nextSymbol();
			Stack<Symbol> tmp = new Stack<>();
			simpleExpression(tmp);
			pushSymbol();
			PredicateDefinition.put(name, range, initialFluentActions,
					finalFluentActions, tmp);
		} else {
			pushSymbol();
			PredicateDefinition.put(name, range, initialFluentActions,
					finalFluentActions, null);
		}
	}

	// *******************************************************************************************************/
	// MTS operations
	// *******************************************************************************************************/

	private ActionLabels getMaybeActionLabels(ActionLabels actionLabel) {
		ActionLabels result = null;
		if (actionLabel instanceof ActionName) {
			ActionName actionName = (ActionName) actionLabel;
			Symbol symbol = actionName.name;
			result = new ActionName(new Symbol(symbol,
					getMaybeAction(symbol.getValue())));
		} else if (actionLabel instanceof ActionSet) {
			ActionSet actionSet = (ActionSet) actionLabel;
			Vector<ActionLabels> maybeSetLabels = new Vector<>();
			for (ActionLabels setLabel : actionSet.getLabelSet().labels)
				maybeSetLabels.add(getMaybeActionLabels(setLabel));
			result = new ActionSet(new LabelSet(maybeSetLabels));
		}
		return result;
	}

	/**
	 * This method extends the set of labels to be relabeled with the maybe or
	 * the required labels.
	 */
	private void relabelMTS(Vector<RelabelDefn> relabels,
			RelabelDefn relabelDefn) {
		RelabelDefn relabelMaybe = new RelabelDefn();
		relabelMaybe.oldlabel = getMaybeActionLabels(relabelDefn.oldlabel);
		relabelMaybe.newlabel = getMaybeActionLabels(relabelDefn.newlabel);
		if (relabelMaybe.oldlabel != null && relabelMaybe.newlabel != null) {
			relabels.add(relabelMaybe);
		} else {
			// FIXME: MTSs add a '?' to action names, but currently expression
			// actions are not supported
			// A possibility is to extend the computeName method in order to
			// take into account MTSs
			// String message =
			// "Relabeling with maybe actions can only be made over labels and sets.";
			// Diagnostics.warning(message, message, null);
		}
	}

	private void priorizeMaybeActions(LabelSet priorityActions) {
		// Only ActionName expected.
		if (priorityActions != null) {
			this.processMTSActions(priorityActions.labels);
		}
	}

	private void processMTSActions(Vector<ActionLabels> allLabels) {
		Set<ActionName> addLabels = new HashSet<>();
		for (Iterator<ActionLabels> it = allLabels.iterator(); it.hasNext();) {
			ActionLabels action = it.next();
			if (action instanceof ActionSet) {
				this.processActionSet((ActionSet) action, addLabels);
			} else if (action instanceof ActionName) {
				processActionName((ActionName) action, addLabels);
			} else {
				throw new RuntimeException(
						"Action to hide is instance of class: "
								+ action.getClass());
			}
		}
		allLabels.clear();
		allLabels.addAll(addLabels);
	}

	private void processActionSet(ActionSet actionLabels,
			Set<ActionName> addLabels) {
		ActionSet actionSet = actionLabels;
		for (Iterator<String> it = actionSet.getActions(null, null).iterator(); it
				.hasNext();) {
			String actionLabel = it.next();
			actionLabel = getOpositeActionLabel(actionLabel);
			addLabels.add(new ActionName(new Symbol(Symbol.STRING_VALUE,
					actionLabel)));
		}
	}

	/**
	 * Computes the set of actions defined by <i>actionsName</i> and add for
	 * each of them the action with opposite maybe condition.
	 * 
	 * For example, if <i>actionsName</i> is the label "a", it returns "a?".
	 * 
	 */
	public void processActionName(ActionName actionName, Set<ActionName> toAdd) {

		String name = actionName.name.toString();
		name = getMaybeAction(name);
		ActionName tempActionName1 = new ActionName(new Symbol(actionName.name,
				name));
		tempActionName1.follower = actionName.follower;
		toAdd.add(tempActionName1);

		name = getOpositeActionLabel(name);
		ActionName tempActionName2 = new ActionName(new Symbol(actionName.name,
				name));
		tempActionName2.follower = actionName.follower;
		toAdd.add(tempActionName2);
	}

	private void goalDefinition(ControllerGoalDefinition goal) {
		this.expectBecomes();
		expectLeftCurly();
		nextSymbol();

		if (current.kind == Symbol.PERMISSIVE) {
			goal.setPermissive();
			nextSymbol();
		}

		if (current.kind == Symbol.SAFETY) {
			goal.setSafetyDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.FAULT) {
			goal.setFaultsDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.ASSUME) {
			goal.setAssumeDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.GUARANTEE) {
			goal.setGuaranteeDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.EXCEPTION_HANDLING) {
			nextSymbol();
			goal.setExceptionHandling(true);
		}

		if (current.kind == Symbol.CONTROLLER_NB) {
			nextSymbol();
			goal.setNonBlocking(true);
		}

		if (current.kind == Symbol.CONCURRENCY_FLUENTS) {
			goal.setConcurrencyDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.CONTROLLER_LAZYNESS) {
			goal.setLazyness(this.controllerSubValue());
		}

		if (current.kind == Symbol.NON_TRANSIENT) {
			nextSymbol();
			goal.setNonTransient(true);
		}

		if (current.kind == Symbol.ACTIVITY_FLUENTS) {
			goal.setActivityDefinitions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.REACHABILITY) {
			nextSymbol();
			goal.setReachability(true);
		}

		if (current.kind == Symbol.TEST_LATENCY) {
			nextSymbol();
			goal.setTestLatency(true);
			Pair<Integer, Integer> value = this.controllerSubPair();
			goal.setMaxSchedulers(value.getFirst());
			goal.setMaxControllers(value.getSecond());

		}

		this.parseControllableActionSet(goal);

		ControllerGoalDefinition.put(goal);

		currentIs(Symbol.RCURLY, "} expected");
	}

	private void explorerDefinition(ExplorerDefinition explorerDefinition) {
		currentIs(Symbol.BECOMES, "= expected after explorer identifier");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();

		currentIs(Symbol.EXPLORATION_ENVIRONMENT, "environment expected");
		nextSymbol();
		explorerDefinition.setView(this.componentsNotEmpty());
		nextSymbol();

		currentIs(Symbol.EXPLORATION_MODEL, "model expected");
		nextSymbol();
		explorerDefinition.setModel(this.componentsByCount(explorerDefinition
				.getView().size()));
		nextSymbol();

		currentIs(Symbol.EXPLORATION_GOAL, "goal expected");
		nextSymbol();
		explorerDefinition.setGoal(this.componentsByCount(1));

		if (current.kind == Symbol.COMMA) {
			nextSymbol();
			currentIs(Symbol.EXPLORATION_ENVIRONMENT_ACTIONS,
					"actions expected");
			nextSymbol();
			explorerDefinition.setEnvironmentActions(this
					.listsOfComponentsNotEmpty());
		}

		explorers.put(explorerDefinition.getName(), explorerDefinition);

		currentIs(Symbol.RCURLY, "} expected");
	}

	private Pair<Integer, Integer> controllerSubPair() {
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();

		currentIs(Symbol.INT_VALUE,
				"Expected max number of schedulers value int.");
		Integer schedulers = current.doubleValue().intValue();
		nextSymbol();

		currentIs(Symbol.COMMA, ", expected");
		nextSymbol();

		currentIs(Symbol.INT_VALUE,
				"Expected max number of controllers value int.");
		Integer controllers = current.doubleValue().intValue();
		nextSymbol();

		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		return new Pair<Integer, Integer>(schedulers, controllers);
	}

	private void updateControllerDefinition(
			UpdatingControllersDefinition ucDefinition) {

		expectBecomes();
		expectLeftCurly();
		nextSymbol();

		if (current.kind == Symbol.OLD_CONTROLLER) {
			ucDefinition.setOldController(this.controllerSubUpdateController());
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}
		if (current.kind == Symbol.OLD_ENVIRONMENT) {
			ucDefinition
					.setOldEnvironment(this.controllerSubUpdateController());
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}
		// if (current.kind == Symbol.HAT_ENVIRONMENT) {
		// ucDefinition.setHatEnvironment(this.controllerSubUpdateController());
		// currentIs(Symbol.COMMA, ", expected");
		// nextSymbol();
		// }
		if (current.kind == Symbol.NEW_ENVIRONMENT) {
			ucDefinition
					.setNewEnvironment(this.controllerSubUpdateController());
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}

		if (current.kind == Symbol.OLD_GOAL) {
			this.expectBecomes();
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "old goal identifier expected");
			ucDefinition.setOldGoal(current);
			nextSymbol();
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}
		if (current.kind == Symbol.NEW_GOAL) {
			this.expectBecomes();
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "new goal identifier expected");
			ucDefinition.setNewGoal(current);
			nextSymbol();
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}
		if (current.kind == Symbol.TRANSITION) {
			this.expectBecomes();
			nextSymbol();
			currentIs(Symbol.UPPERIDENT, "T definition expected");
			ucDefinition.addSafety(current);
			nextSymbol();
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}

		if (current.kind == Symbol.CONTROLLER_NB) {
			nextSymbol();
			ucDefinition.setNonblocking();
			currentIs(Symbol.COMMA, ", expected");
			nextSymbol();
		}
		if (current.kind == Symbol.OLD_PROPOSITIONS) {
			ucDefinition.setOldPropositions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.NEW_PROPOSITIONS) {
			ucDefinition.setNewPropositions(this.controllerSubGoal());
		}

		if (current.kind == Symbol.UPDATE_DEBUG) {
			nextSymbol();
			ucDefinition.setDebugMode();
		}
		if (current.kind == Symbol.UPDATE_CHECK_TRACE) {
			ucDefinition.setCheckTrace(this
					.controllerCheckTraceUpdateController());
		}

		currentIs(Symbol.RCURLY, "} expected");
	}

	private Symbol parseInitialState() {
		nextSymbol();
		currentIs(Symbol.GRAPH_INITIAL_STATE, "initialState expected");
		expectBecomes();
		expectIdentifier("Initial State");
		return current;
	}

	private List<Symbol> parseTransitions() {
		nextSymbol();
		currentIs(Symbol.GRAPH_TRANSITIONS, "transitions expected");
		expectBecomes();
		expectLeftCurly();
		List<Symbol> list = new ArrayList<Symbol>();
		nextSymbol();
		while (current.kind == Symbol.UPPERIDENT) {
			list.add(current);
			nextSymbol();
			if (current.kind == Symbol.COMMA) {
				nextSymbol();
			}
		}
		currentIs(Symbol.RCURLY, "} expected");
		return list;
	}

	private Integer controllerSubValue() {
		expectBecomes();
		nextSymbol();
		currentIs(Symbol.INT_VALUE, "Expected lazyness value int.");
		Integer value = current.doubleValue().intValue();
		nextSymbol();
		return value;
	}

	private void parseControllableActionSet(ControllerGoalDefinition goal) {
		currentIs(Symbol.CONTROLLABLE, "controllable action set expected");
		nextSymbol();

		currentIs(Symbol.BECOMES, "= expected for controllable action set");
		expectLeftCurly();
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "action set identifier expected");
		goal.setControllableActionSet(current);
		nextSymbol();

		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
	}

	private List<List<Symbol>> listsOfComponentsNotEmpty() {
		List<List<Symbol>> listOfDefinitions = new ArrayList<>();

		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		while (current.kind == Symbol.LCURLY || current.kind == Symbol.COMMA) {
			List<Symbol> definitions = new ArrayList<>();

			if (current.kind == Symbol.COMMA)
				nextSymbol();

			currentIs(Symbol.LCURLY, "{ expected");
			nextSymbol();
			boolean finish = false;

			currentIs(Symbol.IDENTIFIER, "non empty set expected");
			definitions.add(current);
			nextSymbol();
			if (current.kind != Symbol.COMMA)
				finish = true;
			else
				nextSymbol();

			while (current.kind == Symbol.IDENTIFIER && !finish) {
				definitions.add(current);
				nextSymbol();
				if (current.kind != Symbol.COMMA)
					break;
				nextSymbol();
			}
			currentIs(Symbol.RCURLY, "} expected");
			nextSymbol();

			listOfDefinitions.add(definitions);
		}

		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		return listOfDefinitions;
	}

	private List<Symbol> controllerSubGoal() {
		expectBecomes();
		List<Symbol> definitions = new ArrayList<>();
		expectLeftCurly();
		nextSymbol();
		boolean finish = false;
		while (current.kind == Symbol.UPPERIDENT && !finish) {
			definitions.add(current);
			nextSymbol();
			if (current.kind != Symbol.COMMA) {
				finish = true;
				break;
			}
			nextSymbol();
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		return definitions;
	}

	private List<Symbol> componentsNotEmpty() {
		List<Symbol> definitions = new ArrayList<>();
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();
		boolean finish = false;

		currentIs(Symbol.UPPERIDENT, "non empty set expected");
		definitions.add(current);
		nextSymbol();
		if (current.kind != Symbol.COMMA)
			finish = true;
		else
			nextSymbol();

		while (current.kind == Symbol.UPPERIDENT && !finish) {
			definitions.add(current);
			nextSymbol();
			if (current.kind != Symbol.COMMA) {
				finish = true;
				break;
			}
			nextSymbol();
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		return definitions;
	}

	private List<Symbol> componentsByCount(int count) {
		List<Symbol> definitions = new ArrayList<>();
		currentIs(Symbol.BECOMES, "= expected");
		nextSymbol();

		currentIs(Symbol.LCURLY, "{ expected");
		nextSymbol();
		boolean finish = false;

		currentIs(Symbol.UPPERIDENT, "non empty set expected");
		definitions.add(current);
		nextSymbol();
		if (current.kind != Symbol.COMMA)
			finish = true;
		else
			nextSymbol();

		while (current.kind == Symbol.UPPERIDENT && !finish) {
			definitions.add(current);
			nextSymbol();
			if (current.kind != Symbol.COMMA) {
				finish = true;
				break;
			}
			nextSymbol();
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		if (definitions.size() != count)
			error("View and Model haven't the same number of components");

		return definitions;
	}

	private ArrayList<Symbol> controllerSubUpdateController() {
		this.expectBecomes();
		nextSymbol();
		ArrayList<Symbol> definitions = new ArrayList<Symbol>();
		currentIs(Symbol.UPPERIDENT, "Upperident expected");
		definitions.add(current);
		nextSymbol();

		return definitions;

	}

	private ArrayList<Symbol> controllerCheckTraceUpdateController() {
		expectBecomes();
		ArrayList<Symbol> definitions = new ArrayList<Symbol>();
		expectLeftCurly();
		nextSymbol();
		boolean finish = false;
		while (current.kind == Symbol.IDENTIFIER && !finish) {
			definitions.add(current);
			nextSymbol();
			if (current.kind == Symbol.RCURLY) {
				finish = true;
				break;
			} else if (current.kind == Symbol.DOT) {
				while (current.kind != Symbol.COMMA
						&& current.kind != Symbol.RCURLY) {
					Symbol lastDef = definitions.get(definitions.size() - 1);
					String parametricValue = lastDef.toString() + ".";
					nextSymbol();
					parametricValue = parametricValue + current.toString();
					definitions.remove(definitions.size() - 1);
					definitions.add(new Symbol(Symbol.IDENTIFIER,
							parametricValue));
					nextSymbol();
				}
			}
			nextSymbol();
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();

		return definitions;
	}

	/*
	 * controlstack ||PROCNAME@{CONTSET} = { tier (ENVNAME, GOALNAME) tier
	 * (ENVNAME, GOALNAME) }
	 * 
	 * controlstack ||PROCNAME@{CONTSET}(action, action) = { tier (ENVNAME,
	 * GOALNAME) tier (ENVNAME, GOALNAME) }
	 */
	private ControlStackDefinition controlStackDefinition() {
		nextSymbol();

		currentIs(Symbol.OR, "|| expected after controlstack");
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		Symbol name = current;
		nextSymbol();

		currentIs(Symbol.AT, "@ expected");
		expectLeftCurly();
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "controlled action set expected");
		Symbol contSet = current;
		nextSymbol();

		currentIs(Symbol.RCURLY, "} expected");
		expectBecomes();
		expectLeftCurly();
		ControlStackDefinition def = new ControlStackDefinition(name, contSet);

		nextSymbol();

		while (current.kind == Symbol.CONTROL_TIER) {
			def.addTier(controlTierDefinition());
		}

		currentIs(Symbol.RCURLY, "} expected");
		// nextSymbol(); //it's swallowed elsewhere

		return def;
	}

	private ControlTierDefinition controlTierDefinition() {
		nextSymbol();

		currentIs(Symbol.LROUND, "( expected");
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "process identifier expected");
		Symbol env = current;
		nextSymbol();

		currentIs(Symbol.COMMA, ", expected");
		nextSymbol();

		currentIs(Symbol.UPPERIDENT, "goal identifier expected");
		Symbol goal = current;
		nextSymbol();

		ControlTierDefinition def = new ControlTierDefinition(env, goal);

		if (current.kind == Symbol.COMMA) {
			expectLeftCurly();
			nextSymbol();

			List<String> initialTrace = new ArrayList<>();
			while (current.kind == Symbol.IDENTIFIER) {
				// only flat dotted IDs
				String label = current.toString();
				nextSymbol();
				while (current.kind == Symbol.DOT) {
					nextSymbol();

					currentIs(Symbol.IDENTIFIER, "action label expected");
					label += "." + current.toString();
					nextSymbol();
				}
				initialTrace.add(label);

				// if a comma, consume it
				if (current.kind == Symbol.COMMA)
					nextSymbol();
			}

			currentIs(Symbol.RCURLY, "} expected");
			nextSymbol();

			def.setInitialTrace(initialTrace);
		}

		currentIs(Symbol.RROUND, ") expected");
		nextSymbol();

		return def;
	}

	/**
	 * 
	 */
	private void distributionDefinition() {
		// parse the components name
		List<Symbol> componentsName = new LinkedList<>();

		List<Integer> expected = new LinkedList<>();
		expected.add(Symbol.BECOMES);
		expected.add(Symbol.COMMA);

		do {
			nextSymbol();
			// Check the syntax and that the process name for the component is
			// unique
			currentIs(Symbol.UPPERIDENT, "component identifier expected");
			this.validateUniqueProcessName(current);

			componentsName.add(current);
			nextSymbol();

			// next symbol should be = or ,
			currentIs(expected, "= or , expected");

		} while (current.kind != Symbol.BECOMES);
		currentIs(Symbol.BECOMES, "= expected");

		expectLeftCurly();
		// parse body
		nextSymbol();

		// parse components alphabet
		List<Symbol> alphabets = this.parseDistributedAlphabets();

		if (alphabets.size() != componentsName.size()) {
			error("There should be one and only one alphabet for each component. Components: "
					+ componentsName + " . Alphabets: " + alphabets);
		}

		// parse system model name
		currentIs(Symbol.SYSTEM_MODEL, "systemModel expected");

		expectBecomes();

		nextSymbol();
		currentIs(Symbol.UPPERIDENT, "component identifier expected");
		Symbol systemModel = current;

		// parse output file name
		String outputFileName;
		nextSymbol();

		if (current.equals(Symbol.OUTPUT_FILE_NAME)) {
			expectBecomes();
			nextSymbol();
			currentIs(Symbol.STRING_VALUE, "String with file name expected");
			outputFileName = current.toString();
			if (outputFileName == null) {
				error("Problem parsing output file name");
			} else {
				try {
					File testFile = new File(outputFileName);
					testFile.createNewFile();
					testFile.canWrite();
				} catch (Exception e) {
					error("Problem handling file with name " + outputFileName
							+ ". " + e.getMessage());
				}
			}
			nextSymbol();

		} else {
			outputFileName = null;
		}

		currentIs(Symbol.RCURLY, "} expected");

		DistributionDefinition distributionDefinition;
		// create the distribution
		if (outputFileName != null) {
			distributionDefinition = new DistributionDefinition(systemModel,
					componentsName, alphabets, outputFileName);
		} else {
			distributionDefinition = new DistributionDefinition(systemModel,
					componentsName, alphabets);
		}
		DistributionDefinition.put(distributionDefinition);
	}

	private List<Symbol> parseDistributedAlphabets() {
		currentIs(Symbol.DISTRIBUTED_ALPHABETS, "distributedAlphabets expected");
		expectBecomes();

		expectLeftCurly();
		List<Integer> expected = new LinkedList<>();
		expected.add(Symbol.RCURLY);
		expected.add(Symbol.COMMA);

		List<Symbol> result = new ArrayList<>();
		do {
			nextSymbol();
			// Check the syntax and that the process name for the component is
			// unique
			currentIs(Symbol.UPPERIDENT, "set identifier expected");

			result.add(current);
			nextSymbol();

			// next symbol should be } or ,
			currentIs(expected, "} or , expected");

		} while (current.kind != Symbol.RCURLY);

		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
		return result;
	}

	/**
	 * Parses a Triggered Scenario
	 */
	private void triggeredScenarioDefinition(
			TriggeredScenarioDefinition triggeredScenarioDefinition)
			throws DuplicatedTriggeredScenarioDefinitionException {

		currentIs(Symbol.BECOMES, "= expected after chart identifier");
		expectLeftCurly();
		nextSymbol();

		// Instances must come next
		triggeredScenarioDefinition.setInstances(this.chartInstancesValues());

		// Conditions (if any) comes next
		while (current.kind == Symbol.CONDITION) {
			nextSymbol();
			triggeredScenarioDefinition.addConditionDefinition(this
					.conditionDefinition());
		}

		// Prechart must come next
		currentIs(Symbol.PRECHART, "prechart expected");
		triggeredScenarioDefinition.setPrechart(this.basicChartDefinition(true,
				triggeredScenarioDefinition));

		// Mainchart must come next
		nextSymbol();
		currentIs(Symbol.MAINCHART, "mainchart expected");
		triggeredScenarioDefinition.setMainchart(this.basicChartDefinition(
				false, triggeredScenarioDefinition));

		triggeredScenarioDefinition.setRestricted(this
				.restrictsDefinition(triggeredScenarioDefinition));

		expectRightCurly();
		nextSymbol();

		TriggeredScenarioDefinition.put(triggeredScenarioDefinition);

		pushSymbol();
	}

	private ConditionDefinition conditionDefinition() {
		currentIs(Symbol.UPPERIDENT, "Identifier expected");
		Symbol name = current;

		expectBecomes();

		next_symbol_mod();
		FormulaSyntax formula = ltlUnary();

		if (!formula.isPropositionalLogic()) {
			error("Condition must be a Fluent Propositional Logic formula");
			return null;
		}
		ConditionDefinition conditionDefinition = new ConditionDefinition(
				name.getValue(), formula);

		return conditionDefinition;
	}

	private Set<Interaction> restrictsDefinition(
			TriggeredScenarioDefinition tsDefinition) {
		Set<Interaction> result = new HashSet<Interaction>();

		if (current.kind == Symbol.RESTRICTS) {
			expectLeftCurly();
			nextSymbol();

			while (current.kind != Symbol.RCURLY) {
				try {
					result.add((Interaction) locationValue(false, tsDefinition));
				} catch (ClassCastException e) {
					error("Restrictions can only be Interactions");
					return null;
				}
			}
		}
		return result;
	}

	private BasicChartDefinition basicChartDefinition(
			boolean isPrechartDefinition,
			TriggeredScenarioDefinition tsDefinition) {
		expectLeftCurly();
		nextSymbol();
		BasicChartDefinition chartDefinition = new BasicChartDefinition();
		chartDefinition.addLocation(locationValue(isPrechartDefinition,
				tsDefinition));

		while (current.kind != Symbol.RCURLY) {
			chartDefinition.addLocation(locationValue(isPrechartDefinition,
					tsDefinition));
		}
		return chartDefinition;
	}

	private Location locationValue(boolean isPrechartDefinition,
			TriggeredScenarioDefinition tsDefinition) {
		// A location can be a Condition or an Interaction
		// Interactions are of the form: UPPERIDENT -> IDENT -> UPPERIDENT
		// Conditions are like: UPPERIDENT [UPPERIDENT UPPERIDENT...]. The first
		// ident is the name of the condition and then
		// comes the set of instances that the condition synchronises.
		Symbol previous = this.identifier();

		if (current.kind == Symbol.ARROW) {
			// Location is an Interaction
			nextSymbol();

			// previous symbol is the interaction's source
			String source = previous.getValue();
			String message = this.event().getValue();

			currentIs(Symbol.ARROW, "-> expected");
			nextSymbol();

			String target = this.identifier().getValue();

			return new Interaction(source, message, target);
		} else if (current.kind == Symbol.LSQUARE) {
			// Conditions can only be placed in the Prechart
			if (!isPrechartDefinition) {
				error("Conditions can only be placed in the Prechart");
				return null;
			} else {
				// Location is a Condition
				nextSymbol();

				// previous symbol is the condition's identifier
				if (!tsDefinition.hasCondition(previous.getValue())) {
					// Condition must be defined previously in the
					// TriggeredScenario.
					error("Condition not defined: " + previous.getValue());
					return null;
				} else {
					String conditionName = previous.getValue();

					// get the instances synchronising with this condition
					Set<String> instances = new HashSet<String>();

					// at least there must be an instance
					instances.add(this.identifier().getValue());
					while (current.kind != Symbol.RSQUARE) {
						instances.add(this.identifier().getValue());
					}
					nextSymbol();

					return new ConditionLocation(conditionName, instances);
				}
			}
		} else {
			error("-> or [ expected");
			return null;
		}
	}

	/**
	 * Checks that next symbol is an identifier and gets it.
	 */
	private Symbol identifier() {
		currentIs(Symbol.UPPERIDENT, "identifier expected");
		Symbol identifier = current;
		nextSymbol();
		return identifier;
	}

	private Set<String> chartInstancesValues() {
		nextSymbol();
		currentIs(Symbol.INSTANCES, "instances expected");
		expectLeftCurly();
		nextSymbol();
		Set<String> instances = new HashSet<String>();
		while (current.kind == Symbol.UPPERIDENT) {
			instances.add(current.toString());
			nextSymbol();
		}
		currentIs(Symbol.RCURLY, "} expected");
		nextSymbol();
		return instances;
	}

	public static Symbol saveControllableSet(Set<String> controllableSet,
			String name) {
		Symbol updateControllableSetSymbol = new Symbol(Symbol.SET,
				"controller_update_" + name + "_controllable_set");
		Vector<ActionLabels> vector = new Vector<>();
		for (String action : controllableSet) {
			ActionName actionName = new ActionName(new Symbol(
					Symbol.IDENTIFIER, action));
			vector.add(actionName);
		}
		new LabelSet(updateControllableSetSymbol, vector);
		return updateControllableSetSymbol;
	}

	private void expectBecomes() {
		nextSymbol();
		currentIs(Symbol.BECOMES, "= expected");
	}

	private void expectIdentifier(String errorMsg) {
		nextSymbol();
		currentIs(Symbol.UPPERIDENT, errorMsg + " identifier expected");
	}

	private void expectLeftCurly() {
		nextSymbol();
		currentIs(Symbol.LCURLY, "{ expected");
	}

	private void expectRightCurly() {
		nextSymbol();
		currentIs(Symbol.RCURLY, "} expected");
	}

	public PreconditionDefinitionManager getPreconditionDefinitionManager() {
		return preconditionDefinitionManager;
	}

	public PostconditionDefinitionManager getPostconditionDefinitionManager() {
		return postconditionDefinitionManager;
	}
}