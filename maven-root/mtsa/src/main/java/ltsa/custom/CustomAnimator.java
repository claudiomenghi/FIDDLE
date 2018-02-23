package ltsa.custom;

import java.awt.Frame;
import java.io.File;

import ltsa.lts.animator.Animator;
import ltsa.lts.csp.Relation;

public abstract class CustomAnimator extends Frame {

    public abstract void init(Animator a, File xml, 
    	                        Relation actionMap, Relation controlMap,
    	                        boolean replay);

    public abstract void stop();

    public void dispose() {
        stop();
        super.dispose();
    }

}