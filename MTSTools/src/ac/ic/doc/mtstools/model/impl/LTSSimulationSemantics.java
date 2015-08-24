package ac.ic.doc.mtstools.model.impl;

import java.util.HashSet;
import java.util.Set;

import ac.ic.doc.mtstools.model.ImplementationNotion;
import ac.ic.doc.mtstools.model.Refinement;

public class LTSSimulationSemantics extends BaseSemanticsByRelation implements Refinement, ImplementationNotion
{
  private static final HashSet<String> taus = new HashSet<String>();
  static
  {
    taus.add("tau");
  }
  
  public LTSSimulationSemantics()
  {
    this(taus);
  }
  
  public LTSSimulationSemantics(Set<String> silentActions)
  {
    super(new FixedPointRelationConstructor(
        new WeakBackwardSimulation<String>(silentActions)),
        silentActions);
  }
}
