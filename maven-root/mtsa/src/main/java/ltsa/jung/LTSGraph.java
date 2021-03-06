package ltsa.jung;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.picking.PickedState;

/**
 * Graph structure for a directed graph of type LTS
 * @author Cédric Delforge
 */
@SuppressWarnings("serial")
public class LTSGraph extends DirectedSparseMultigraph<StateVertex, TransitionEdge> {
	private final String name;
	private boolean mixed; //a graph is mixed when it holds multiple single graphs besides itself
	private Set<LTSGraph> innerGraphs;
	private Map<LTSGraph,List<Set<StateVertex>>> SCCs;
	
	public class LTSNavigator {
		private Set<StateVertex> reached;
		private Set<StateVertex> current;
		
		public LTSNavigator() {
			this(getInitials());
		}
		public LTSNavigator(final StateVertex v) {
			this(new HashSet<StateVertex>(){{add(v);}});
		}
		public LTSNavigator(Set<StateVertex> vs) {
			reached = new HashSet<>();
			current = vs;
		}
		/*
		 * Adds the destination state to the set of current states, and removes those that can reach it
		 */
		public void navigateTo(StateVertex to) {
			Set<StateVertex> froms = new HashSet<>(getPredecessors(to));
			froms.retainAll(current);
			for (StateVertex from: froms) {
				if (current.contains(from) && getSuccessors(from).contains(to)) {
					current.remove(from);
					current.add(to);
					
					reached.add(from);
				}
			}
		}
		public Set<StateVertex> getCurrent() {
			return Collections.unmodifiableSet(current);
		}
		public Set<StateVertex> getNext(StateVertex v) {
			return new HashSet<>(getSuccessors(v));
		}
		public Set<StateVertex> getPrevious(StateVertex v) {
			return new HashSet<>(getPredecessors(v));
		}
		public Set<StateVertex> getNext() {
			Set<StateVertex> r = new HashSet<>();
			for (StateVertex v: current) {
				r.addAll(getSuccessors(v));
			}
			return r;
		}
		public Set<StateVertex> getPrevious() {
			Set<StateVertex> r = new HashSet<>();
			for (StateVertex v: current) {
				r.addAll(getPredecessors(v));
			}
			return r;
		}
		public Set<TransitionEdge> getPath(StateVertex v) {
			return new HashSet<>(getOutEdges(v));
		}
		public Set<TransitionEdge> getPath() {
			Set<TransitionEdge> r = new HashSet<>();
			for (StateVertex v: current) {
				r.addAll(getOutEdges(v));
			}
			return r;		
		}
		public Set<StateVertex> getReachable() {
			Set<StateVertex> r = new HashSet<>();
			for (StateVertex v: current) {
				r.addAll(getReachable(v));
			}
			return r;
		}
		public Set<StateVertex> getReaching() {
			Set<StateVertex> r = new HashSet<>();
			for (StateVertex v: current) {
				r.addAll(getReaching(v));
			}
			return r;
		}
		public Set<StateVertex> getReachable(StateVertex v) {
			return new HashSet<>(getReachableStates(v));
		}
		public Set<StateVertex> getReaching(StateVertex v) {
			return new HashSet<>(getReachingStates(v));
		}
		public Set<StateVertex> getReached() {
			return Collections.unmodifiableSet(reached);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public LTSNavigator getNavigator() {
		return new LTSNavigator();
	}
	public LTSNavigator getNavigator(StateVertex v) {
		return new LTSNavigator(v);
	}
	public LTSNavigator getNavigator(Set<StateVertex> vs) {
		return new LTSNavigator(vs);
	}
	
	public LTSGraph() {
		this("aggregate");
	}
	public LTSGraph(String n) {
		super();
		name = n;
		mixed = true;
		innerGraphs = new HashSet<>();
		SCCs = new HashMap<>();
	}
	
	public LTSGraph(LabelledTransitionSystem lts) {
		this(lts.getName());
		mixed = false;
		
		HashMap<Integer,StateVertex> stateByNumber =
				new HashMap<>(); //Used for edges to efficiently get JUNGState objects
		ArrayList<TransitionEdge> transitions =
				new ArrayList<>();

		//Convert all states to vertices in the output graph
		for (int currentState=0; currentState < lts.getMaxStates(); currentState++) {
			final StateVertex js = new StateVertex(currentState,lts.getName());
			addVertex(js);
			stateByNumber.put(currentState, js);

			try {
				final Enumeration<LTSTransitionList> enumEvent = lts.getStates()[currentState].elements();
    			while (enumEvent.hasMoreElements()) {
    				final LTSTransitionList event = enumEvent.nextElement();
    				final String eventName = lts.getAlphabet()[event.getEvent()];
    				if (!eventName.startsWith("@")) { 
    					final TransitionEdge jt = new TransitionEdge(eventName,currentState,event.getNext());
    					transitions.add(jt);
    				}
    			} 
			} catch (NullPointerException e) {
				//FIXME this works but shouldn't throw an exception regardless
				//Happens with Maze in chap 6
			}
		}

		//temporarily add the error state
		final StateVertex jserror = new StateVertex(-1, lts.getName());
		addVertex(jserror);
		stateByNumber.put(-1, jserror);

		//Link all the temporary transitions to existing vertices and add them to the ouput graph
		final Iterator<TransitionEdge> enumTrans = transitions.iterator();
		while (enumTrans.hasNext()) {
			final TransitionEdge trans = enumTrans.next();
			final StateVertex from = stateByNumber.get(trans.getOriginState());
			final StateVertex to = stateByNumber.get(trans.getDestinationState());
			if (from != null && to != null) {
				boolean added = false;
				for (TransitionEdge jt: getOutEdges(from)) {
					if (getDest(jt) == to) {
						jt.addLabel(trans.getFirstLabel());
						added = true;
					}
				}
				if (!added)
					addEdge(trans, from, to);
			}
			else
				System.out.println(trans+String.valueOf(trans.getOriginState())+String.valueOf(trans.getDestinationState()));
		}

		//Don't display the error state if it's never reached
		if (getIncidentEdges(jserror).size() == 0) {
			removeVertex(jserror);
		}
	}

	/*
	 * Creates a new graph from a set of picked vertices and an aggregated graph holding them
	 */
	public LTSGraph (PickedState<StateVertex> ps, LTSGraph aggregate) {
		this("picked");
		
		for(StateVertex vertex : ps.getPicked()) {
			addVertex(vertex);
			Collection<TransitionEdge> incidentEdges = aggregate.getOutEdges(vertex);
			if (incidentEdges != null) {
				for(TransitionEdge edge : incidentEdges) {
					Pair<StateVertex> endpoints = aggregate.getEndpoints(edge);
					if(ps.getPicked().containsAll(endpoints)) {
						addEdge(edge, endpoints.getFirst(), endpoints.getSecond());
					}
				}
			}
		}
	}
	
	/*
	 * Gets the states of the LTS containing the state "v"
	 */
	public Set<StateVertex> getLTSFromState(StateVertex v) {
		HashSet<StateVertex> vertices = new HashSet<StateVertex>();
		LTSGraph lts = getLTSGraphFromState(v);
		
		if (lts != null) {
			for (StateVertex s: lts.getVertices()) {
				vertices.add(s);
			}
		}
		return vertices;
	}
	
	/*
	 * Gets the LTS containing the state "v"
	 */
	private LTSGraph getLTSGraphFromState(StateVertex v) {
		LTSGraph lts = null;
		if (mixed) {
			for (LTSGraph g: innerGraphs) {
				if (g.containsVertex(v)) {
					lts = g;
				}
			}
		} else {
			if (containsVertex(v)) {
				lts = this;
			}
		}
		return lts;
	}
	
	/*
	 * Gets the SCC containing the state "v"
	 */
	public Set<StateVertex> getSCCFromState(StateVertex v) {
		LTSGraph lts = getLTSGraphFromState(v);
		if (lts != null) {
			List<Set<StateVertex>> scc = SCCs.get(lts);
			
			if (scc == null) { //lazy intialization
				computeSCC(lts);
				scc = SCCs.get(lts);
			}
	
			for (Set<StateVertex> s: scc) {
				if (s.contains(v))
					return s;
			}
		}
		return new HashSet<StateVertex>();
	}
	
	/*
	 * Incrementally numbers each SCC of a graph,
	 * then returns the number of the SCC the "v" state belongs to
	 */
	public int numberSCC(StateVertex v) {
		LTSGraph lts = getLTSGraphFromState(v);
		List<Set<StateVertex>> scc = SCCs.get(lts);
		
		if (scc == null) { //lazy intialization
			computeSCC(lts);
			scc = SCCs.get(lts);
		}
		
		for (int set = 0; set < scc.size(); set++) {
			if (scc.get(set).contains(v))
				return set+1;
		}
		return 0;
	}
	
	
	private void computeSCC(LTSGraph lts) {
		StronglyConnectedComponentClusterer<StateVertex,TransitionEdge> clusterer = new StronglyConnectedComponentClusterer<StateVertex,TransitionEdge>();
		
		SCCs.put(lts,clusterer.transform(lts));
	}
	
	/*
	 * Returns the transition corresponding to the given label and origin and dest states
	 */
	public TransitionEdge getTransitionFromLabel(String label, int origin, int dest) {
		for (TransitionEdge e: getEdges()) {
			if (e.hasLabel(label) && e.getOriginState() == origin && e.getDestinationState() == dest) {
				return e;
			}
		}
		return null;
	}
	
	/*
	 * Returns state with given number
	 */
	public StateVertex getStateFromNumber(int n) {
		for (StateVertex v : getVertices()) {
			if (v.getStateName() == n) return v;
		}
		return null;
	}
	
	/*
	 * Merges this with g
	 */
	public void mergeWith(LTSGraph g) {
		innerGraphs.add(g);
		final Iterator<StateVertex> newVertices = g.getVertices().iterator();
		while (newVertices.hasNext()) {
			addVertex(newVertices.next());
		}
		
		final Iterator<TransitionEdge> newEdges = g.getEdges().iterator();
		while (newEdges.hasNext()) {
			final TransitionEdge e = newEdges.next();
			addEdge(e, g.getSource(e), g.getDest(e));
		
		}
	}
	
	/*
	 * Removes g from this
	 */
	public void separateFrom(LTSGraph g) {
		innerGraphs.remove(g);
		final Iterator<StateVertex> newVertices = g.getVertices().iterator();
		while (newVertices.hasNext()) {
			removeVertex(newVertices.next());
		}
		
		final Iterator<TransitionEdge> newEdges = g.getEdges().iterator();
		while (newEdges.hasNext()) {
			final TransitionEdge e = newEdges.next();
			removeEdge(e);
		}
	}
	
	/*
	 * Returns all the states labeled "0"
	 */
	public Set<StateVertex> getInitials() {
		Set<StateVertex> initials = new HashSet<>();
		if (mixed) {
			for (LTSGraph g: innerGraphs) {
				for (StateVertex v: g.getVertices()) {
					if (v.toString().equals("0")) {
						initials.add(v);
						break;
					}
				}
			}
			return initials;
		} else {
			for (StateVertex v: getVertices()) {
				if (v.toString().equals("0")) {
					initials.add(v);
					break;
				}					
			}
			return initials;
		}
	}
	
	/*
	 * Returns a set of all the states reachable from v
	 */
	public Set<StateVertex> getReachableStates(StateVertex v) {
		Set<StateVertex> reachables = new HashSet<>();
    	LinkedList<StateVertex> queue = new LinkedList<>();
    	queue.add(v);
    	
    	while (!queue.isEmpty()) {
    		StateVertex s = queue.removeLast();
    		
    		for (StateVertex successor: getSuccessors(s)) {
    			if (!reachables.contains(successor)) {
    				reachables.add(successor);
    				queue.add(successor);
    			}
    		}
    	}
    	
    	return reachables;
	}
	
	/*
	 * Returns a set of all the states able to reach v
	 */
	public Set<StateVertex> getReachingStates(StateVertex v) {
		Set<StateVertex> reachings = new HashSet<>();
    	LinkedList<StateVertex> queue = new LinkedList<>();
    	queue.add(v);
    	
    	while (!queue.isEmpty()) {
    		StateVertex s = queue.removeLast();
    		
    		for (StateVertex predecessor: getPredecessors(s)) {
    			if (!reachings.contains(predecessor)) {
    				reachings.add(predecessor);
    				queue.add(predecessor);
    			}
    		}
    	}
    	
    	return reachings;
	}
}
