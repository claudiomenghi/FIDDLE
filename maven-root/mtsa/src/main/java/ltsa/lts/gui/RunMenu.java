package ltsa.lts.gui;

import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.csp.Relation;

public class RunMenu {

    public String name;
    public Vector alphabet;   // vector of strings
    public String params;
    public Relation actions;
    public Relation controls;

    public static Hashtable menus; //  vector of all menus

    public static void init(){
        menus = new Hashtable();
    }

    public RunMenu(String name, String params, Relation actions, Relation controls) {
        this.name = name;
        this.params = params;
        this.actions = actions;
        this.controls = controls;
    }

    public RunMenu(String name, Vector actions) {
        this.name = name;
        this.alphabet = actions;
    }
    
    public static void add(RunMenu r) {
       menus.put(r.name,r);
    }

    public boolean isCustom() { return params!=null;}

}