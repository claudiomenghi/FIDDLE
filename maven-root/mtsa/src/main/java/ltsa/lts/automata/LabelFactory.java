package ltsa.lts.automata;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ltsa.lts.Diagnostics;
import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.ltl.PredicateDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.Proposition;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.parser.Symbol;

public class LabelFactory {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());
	
	private SortedSet<Proposition> allprops;
	private FormulaFactory formulaFactory;
	Vector<String> alphaX;
	String name;
	HashMap<String, BitSet> tr; // transition sets
	BitSet[] ps; // proposition sets
	BitSet[] nps; // not proposition sets
	/**
	 * contains a process for each proposition
	 */
	public Vector<LabelledTransitionSystem> propProcs;

	private List<PredicateDefinition> fluents = new ArrayList<>();

	SortedSet<String> allActions; // application alphabet of all propositions

	public LabelFactory(String name, FormulaFactory formulaFactory,
			Vector<String> alphaExtension) {
		
		this.allprops = formulaFactory.getPropositions();
		
		logger.debug("PROPOSITIONS: "+this.allprops);
		this.formulaFactory = formulaFactory;
		this.name = name;
		alphaX = alphaExtension;
		tr = new HashMap<>();
		initPropSets();
		compileProps();
	}

	public HashMap<String, BitSet> getTransLabels() {
		return tr;
	}

	public Vector<String> getPrefix() {
		Vector<String> v = new Vector<>();
		Formula f = allprops.first();
		v.add("_" + f);
		return v;
	}

	public String makeLabel(SortedSet<Proposition> props) {
		StringBuilder sb = new StringBuilder();
		Iterator<Proposition> ii = allprops.iterator();
		boolean isMore = false;
		BitSet labels = new BitSet();
		int m = 0;
		while (ii.hasNext()) {
			Formula f = ii.next();
			if (props.contains(f)) {
				if (isMore) {
					sb.append("&");
					labels.and(ps[m]);
				} else {
					labels.or(ps[m]);
					isMore = true;
				}
				sb.append(f);
			} else if (props.contains(formulaFactory.makeNot(f))) {
				if (isMore) {
					sb.append("&");
					labels.and(nps[m]);
				} else {
					labels.or(nps[m]);
					isMore = true;
				}
				sb.append("!" + f);
			}
			++m;
		}
		String s = sb.toString();
		tr.put(s, labels);
		return s;
	}

	public String[] makeAlphabet() {
		return makeAlphabet(null, null, null);
	}

	private String[] makeAlphabet(PredicateDefinition p, BitSet tt, BitSet ft) {
		int extra = 0;
		if (p == null) {
			extra = 1; // accept label
		} else {
			extra = p.getTrueActions().size() + p.getFalseActions().size();
		}
		int len = (1 << allprops.size()) + 1 + extra; // labels + tau + extra
		String alpha[] = new String[len];
		for (int i = 0; i < len - extra; i++) {
			StringBuffer sb = new StringBuffer();
			Iterator<Proposition> ii = allprops.iterator();
			boolean isMore = false;
			int m = 0;
			while (ii.hasNext()) {
				Formula f = ii.next();
				if (isMore)
					sb.append(".");
				isMore = true;
				sb.append("_" + f + "." + (i >> m) % 2);
				++m;
			}
			alpha[i + 1] = sb.toString();
		}
		alpha[0] = "tau";
		if (p == null) {
			//alpha[len - 1] = "@" + name;
			alpha[len - 1] = "@" + "any";
		} else {
			int pos = len - extra;
			Iterator<String> ii = p.getFalseActions().iterator();
			while (ii.hasNext()) {
				alpha[pos] = ii.next();
				ft.set(pos);
				++pos;
			}
			ii = p.getTrueActions().iterator();
			while (ii.hasNext()) {
				alpha[pos] = ii.next();
				tt.set(pos);
				++pos;
			}
		}
		return alpha;
	}

	void initPropSets() {
		int len = allprops.size();
		ps = new BitSet[len];
		nps = new BitSet[len];
		BitSet trueSet = new BitSet(1 << len);
		for (int m = 0; m < len; ++m) {
			ps[m] = new BitSet(1 << len);
			nps[m] = new BitSet(1 << len);
		}
		for (int i = 0; i < (1 << len); ++i) {
			trueSet.set(i);
			for (int m = 0; m < len; ++m)
				if (((i >> m) % 2) == 1) {
					ps[m].set(i);
				} else {
					nps[m].set(i);
				}
		}
		tr.put("true", trueSet);
	}

	public PredicateDefinition[] getFluents() {
		if (this.fluents.size() == 0) {
			return null;
		}
		PredicateDefinition[] pds = new PredicateDefinition[fluents.size()];
		for (int i = 0; i < pds.length; ++i) {
			pds[i] = this.fluents.get(i);
		}
		return pds;
	}

	protected void compileProps() {
		this.propProcs = new Vector<>();
		this.allActions = new TreeSet<>();
		// Pass 1 PredicateDefinition processes
		Iterator<Proposition> propositionIterator = allprops.iterator();

		int m = 0;
		while (propositionIterator.hasNext()) {
			Proposition proposition = propositionIterator.next();

			PredicateDefinition predicate = PredicateDefinition.get(proposition
					.toString());

			if (predicate != null) {
				fluents.add(predicate);
				predicate.getTrueActions();

				allActions.addAll(predicate.getTrueActions());
				allActions.addAll(predicate.getFalseActions());
				propProcs.add(makePropProcess(predicate, m));
			} else {
				if (formulaFactory.getActionPredicates() != null
						&& formulaFactory.getActionPredicates().containsKey(
								proposition.toString())) {
					// only add to alphabet this pass
					Vector<String> vl = formulaFactory.getActionPredicates()
							.get(proposition.toString());
					allActions.addAll(vl);
				} else {
					Diagnostics.fatal("Proposition " + proposition
							+ " not found", proposition.getSymbol());
				}
			}
			++m;
		}
		if (alphaX != null) {
			allActions.addAll(alphaX);
		}
		// Pass 2 Action Predicate processes
		propositionIterator = allprops.iterator();
		m = 0;
		while (propositionIterator.hasNext()) {
			Proposition f = propositionIterator.next();
			PredicateDefinition p = PredicateDefinition.get(f.toString());
			if (p != null) {
				// do nothing this pass
			} else if (formulaFactory.getActionPredicates() != null
					&& formulaFactory.getActionPredicates().containsKey(
							f.toString())) {
				Vector<String> trueActions = formulaFactory
						.getActionPredicates().get(f.toString());
				Vector<String> falseActions = new Vector<>();
				falseActions.addAll(allActions);
				falseActions.removeAll(trueActions);
				p = new PredicateDefinition(new Symbol(Symbol.UPPERIDENT,
						f.toString()), trueActions, falseActions);
				LabelledTransitionSystem cs = makePropProcess(p, m);
				propProcs.add(cs);
			} else
				Diagnostics.fatal("Proposition " + f + " not found",
						f.getSymbol());
			++m;
		}
		// make sync process
		propProcs.add(makeSyncProcess());
	}

	LabelledTransitionSystem makePropProcess(PredicateDefinition p, int notPropositionIndex) {
		LabelledTransitionSystem cs = new LabelledTransitionSystem(p.getSymbol().toString(), 2);
		cs.setStates(new LTSTransitionList[cs.getMaxStates()]);
		BitSet trueTrans = new BitSet();
		BitSet falseTrans = new BitSet();
		cs.setAlphabet(makeAlphabet(p, trueTrans, falseTrans));
		int falseS = p.getInitial() ? 1 : 0;
		int trueS = p.getInitial() ? 0 : 1;
		for (int i = 0; i < trueTrans.size(); ++i)
			if (trueTrans.get(i))
				cs.getStates()[falseS] = EventStateUtils.add(
						cs.getStates()[falseS], new LTSTransitionList(i, trueS));
		for (int i = 0; i < falseTrans.size(); ++i)
			if (falseTrans.get(i))
				cs.getStates()[trueS] = EventStateUtils.add(
						cs.getStates()[trueS], new LTSTransitionList(i, falseS));
		for (int i = 0; i < falseTrans.size(); ++i)
			if (falseTrans.get(i))
				cs.getStates()[falseS] = EventStateUtils.add(
						cs.getStates()[falseS], new LTSTransitionList(i, falseS));
		for (int i = 0; i < trueTrans.size(); ++i)
			if (trueTrans.get(i))
				cs.getStates()[trueS] = EventStateUtils.add(
						cs.getStates()[trueS], new LTSTransitionList(i, trueS));
		for (int i = 0; i < nps[notPropositionIndex].size(); ++i)
			if (nps[notPropositionIndex].get(i))
				cs.getStates()[falseS] = EventStateUtils.add(
						cs.getStates()[falseS], new LTSTransitionList(i + 1, falseS));
		for (int i = 0; i < ps[notPropositionIndex].size(); ++i)
			if (ps[notPropositionIndex].get(i))
				cs.getStates()[trueS] = EventStateUtils.add(
						cs.getStates()[trueS], new LTSTransitionList(i + 1, trueS));
		return cs;
	}

	LabelledTransitionSystem makeSyncProcess() {
		LabelledTransitionSystem cs = new LabelledTransitionSystem("SYNC", 2);
		cs.setStates(new LTSTransitionList[cs.getMaxStates()]);
		String[] propA = makeAlphabet();
		String[] appA = new String[allActions.size()];
		int ind = 0;
		for (Iterator<String> ii = allActions.iterator(); ii.hasNext(); appA[ind++] = (String) ii
				.next())
			;
		String[] newAlphabet = new String[propA.length - 1 + appA.length];
		newAlphabet[0] = "tau";
		for (int i = 1; i < (propA.length - 1); ++i) {
			newAlphabet[i] = propA[i];
			cs.getStates()[1] = EventStateUtils.add(cs.getStates()[1],
					new LTSTransitionList(i, 0));
		}
		for (int i = 0; i < appA.length; ++i) {
			newAlphabet[i + propA.length - 1] = appA[i];
			cs.getStates()[0] = EventStateUtils.add(cs.getStates()[0],
					new LTSTransitionList(i + propA.length - 1, 1));
		}
		cs.setAlphabet(newAlphabet);
		return cs;
	}

	/**
	 * returns a process for each proposition
	 * 
	 * @return a process for each proposition
	 */
	public Vector<LabelledTransitionSystem> getPropProcs() {
		return propProcs;
	}

}