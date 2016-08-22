package ltsa.lts.parser.actions;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;

/* -----------------------------------------------------------------------*/

public class LabelSet {
    boolean isConstant=false;
    public Vector<ActionLabels> labels;     // list of unevaluates ActionLabelss, null if this is a constant set
    Vector<String> actions;    // list of action names for an evaluated constant set

    public static Hashtable<String,LabelSet> constants; // hashtable of constant sets, <string,LabelSet>

    public LabelSet(Symbol s, Vector<ActionLabels> lbs) {
        labels = lbs;
        if(constants.put(s.toString(),this)!=null) {
            Diagnostics.fatal("duplicate set definition: "+s,s);
        }
        actions = getActions(null);  // name must be null here
        isConstant=true;
        labels = null;
    }

    public LabelSet(Vector<ActionLabels> lbs) {
        labels = lbs;
    }

    
    public String toString(){
    	StringBuilder builder=new StringBuilder();
    	for(ActionLabels label: labels){
    		builder.append(label.toString());
    	}
    	return builder.toString();
    }
    
    public Vector<String> getActions(Hashtable<String, Value> params) {
        Vector<String> actions2 = getActions(null,params);
		return actions2;
    }

    protected Vector<String> getActions(Hashtable<String, Value> locals, Hashtable<String, Value> params) {
      if (isConstant) return actions;
      if (labels ==null) return null;
      Vector<String> v = new Vector<String>();
      Hashtable<String,String> dd = new Hashtable<>(); // detect and discard duplicates
      @SuppressWarnings("unchecked")
	Hashtable<String, Value> mylocals = locals!=null?(Hashtable<String, Value>)locals.clone():null;
      Enumeration<ActionLabels> e = labels.elements();
      while (e.hasMoreElements()) {
         ActionLabels l = e.nextElement();
         l.initContext(mylocals,params);
         while(l.hasMoreNames()) {
            String s = l.nextName();
            if (!dd.containsKey(s)) {
                v.addElement(s);
                dd.put(s,s);
            }
         }
         l.clearContext();
      }
      return v;
    }

    // >>> AMES: Enhanced Modularity
    public static Hashtable<String,LabelSet> getConstants() {
    	return constants;
    }
    // <<< AMES

}
