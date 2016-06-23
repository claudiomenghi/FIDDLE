package ltsa.lts;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.operations.compiler.LTSCompiler;

/* -----------------------------------------------------------------------*/

public abstract class Declaration {
	public static final int TAU = 0;
	public static final int TAU_MAYBE = 1;
	public static final int ERROR = -1;
	public static final int STOP = 0;
	public static final int SUCCESS = 1;

	public void explicitStates(StateMachine m) {
	};

	public void crunch(StateMachine m) {
	}; // makes sure aliases refer to the same state

	public void transition(StateMachine m) {
	};
}







