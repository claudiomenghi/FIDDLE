package ltsa.lts.csp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.checkers.ProgressTest;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.ActionLabels;


/* -----------------------------------------------------------------------*/

public class ProgressDefinition {
	public Symbol name;
	public ActionLabels pactions;
	public ActionLabels cactions; //if P then C
	public ActionLabels range;    //range of tests

    public static Hashtable<String, ProgressDefinition> definitions;

    public static void compile(){
        ProgressTest.init();
        Enumeration<ProgressDefinition> e = definitions.elements();
        while (e.hasMoreElements()){
            ProgressDefinition p = e.nextElement();
            p.makeProgressTest();
        }
    }

    public void makeProgressTest(){
        Vector<String> pa=null;
        Vector<String> ca=null;
        String na = name.toString();
        if (range==null) {
            pa = pactions.getActions(null,null);
            if (cactions!=null) ca = cactions.getActions(null,null);
            new ProgressTest(na,pa,ca);
        } else {
            Hashtable<String, Value> locals = new Hashtable<>();
            range.initContext(locals,null);
            while(range.hasMoreNames()) {
                String s = range.nextName();
                pa = pactions.getActions(locals,null);
                if (cactions!=null) ca = cactions.getActions(locals,null);
                new ProgressTest(na+"."+s,pa,ca);
            }
            range.clearContext();
        }
    }
}

