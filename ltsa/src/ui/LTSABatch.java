package ui;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import dispatcher.TransitionSystemDispatcher;

import lts.CompactState;
import lts.CompositeState;
import lts.LTSCompiler;
import lts.LTSError;
import lts.LTSException;
import lts.LTSInput;
import lts.LTSInputString;
import lts.LTSManager;
import lts.LTSOutput;
import lts.LabelSet;
import lts.Options;
import lts.SymbolTable;
import lts.Options.CompositionStrategy;

public class LTSABatch 
implements LTSManager, LTSInput, LTSOutput, LTSError {
	
	CompositeState current = null;
	Set<String> compositeNames = new HashSet<String>();
	String currentDirectory = System.getProperty("user.dir");
	Hashtable<String,LabelSet> labelSetConstants = null;
//	SETCompositionalBatch compositionBatch = null;
	String model = "";
	int fPos = -1;
	String fSrc = "\n";
	
	public LTSABatch (String fileTxt, int modulesCount, boolean memo, boolean ref, String heur, boolean proj) {
		//SymbolTable.init();
//		compositionBatch = 
//			new SETCompositionalBatch(this,this,this,this,true, 
//								      memo, ref, heur,proj,modulesCount);	
		model = fileTxt;
	}
	
	public void out ( String str ) {
		System.out.print(str);
	}
	
	public void outln ( String str ) {
		System.out.println(str);
	}
	
	public void clearOutput () {
		//not needed
	}
	
	public char nextChar () {
		fPos = fPos + 1;
		if (fPos < fSrc.length ()) {
			return fSrc.charAt (fPos);
		} else {
			//fPos = fPos - 1;
			return '\u0000';
		}
	}
	
	public char backChar () {
		fPos = fPos - 1;
		if (fPos < 0) {
			fPos = 0;
			return '\u0000';
		}
		else
			return fSrc.charAt (fPos);		
	}
	
	public int getMarker () {
		return fPos;
	}
	
	public void resetMarker () {
		fPos = -1;
	}
	

	private void safety() {
		safety(true, false);
	}
	private void safety(boolean checkDeadlock, boolean multiCe) {
		compile();
		if (current != null) {
			//no sirve asi!
			current.analyse(checkDeadlock, this);
		}
	}
	
	private void compile () {
		if (!parse()) return;
		current = docompile();
	}
	
	public void displayError(LTSException x) {
		outln("ERROR - "+x.getMessage());
	}
	
	private CompositeState docompile() {
		fPos = -1;
        fSrc = model;
		CompositeState cs=null;
		LTSCompiler comp=new LTSCompiler(this,this,currentDirectory);
		try {
			comp.compile();
			cs = comp.continueCompilation("ALL");
		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}
	
	private Hashtable doparse () {
		Hashtable cs = new Hashtable();
		Hashtable ps = new Hashtable();
		doparse(cs,ps);
		return cs;
	}
	
	private void doparse(Hashtable cs, Hashtable ps) {
		fPos = -1;
        fSrc = model;
		LTSCompiler comp = new LTSCompiler(this,this,currentDirectory);
		try {
			comp.parse(cs,ps);
		} catch (LTSException x) {
			displayError(x);
			cs=null;
		}
	}
	
	public void compileIfChange () {
		//not needed
	}
	
	public boolean parse() {
		// >>> AMES: Enhanced Modularity
		Hashtable cs = new Hashtable();
		Hashtable ps = new Hashtable();
		doparse(cs,ps);
		// <<< AMES
		
		if (cs==null) return false;
		if (cs.size()==0) {
			compositeNames.add("DEFAULT");
		} else  {
			Enumeration e = cs.keys();
			java.util.List forSort = new ArrayList();
			while( e.hasMoreElements() ) {
				forSort.add( e.nextElement() );
			}
			Collections.sort( forSort );
			for( Iterator i = forSort.iterator() ; i.hasNext() ; ) {
				compositeNames.add((String)i.next());
			}
		}
		current = null;
		
		return true;
	}
	
	public CompositeState compile(String name) {
		fPos = -1;
        fSrc = model;
		CompositeState cs=null;
		LTSCompiler comp = new LTSCompiler(this,this,currentDirectory);
		try {
			comp.compile();
			cs = comp.continueCompilation(name);
		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}
	
	/**
	 * Returns the current labeled transition system.
	 */
	public CompositeState getCurrentTarget() {
		return current;
	}
	
	public Set<String> getCompositeNames() {	
		return compositeNames;
	}	
	
	/**
	 * Returns the set of actions which correspond to the label set definition
	 * with the given name.
	 */
	public Set<String> getLabelSet(String name) {
		if (labelSetConstants == null)
			return null;
		
		Set<String> s = new HashSet<String>();
		LabelSet ls = labelSetConstants.get(name);
		
		if (ls == null)
			return null;
		
		for ( String a : (Vector<String>) ls.getActions(null) )
			s.add(a);
		
		return s;
	}
	
	public void performAction (final Runnable r, final boolean showOutputPane) {
		//not needed
	}
	
	public String getTargetChoice() {
		//not needed
		return "";
	}
	
	public void newMachines(java.util.List<CompactState> machines) {
		//not needed
	}
	
	private static void showUsage() {
		String usage= "LTSABatch usage:\n\t" +
		  "LTSABatch <LTS_FILE> [--composeStrategy <STRATEGY>] [--maxStateGen <MAX_STATES>] [--format <EXPORTFORMAT>] [--output <OUTPUTFILE>] [--monolithic] [--recurse <NUMBER_OF_MODULES> | [--memoize [PROJECT]] [--refine <HEURISTIC>] <NUMBER_OF_MODULES>]\n\n" +
		  "Available options:\n\n" +
		  "\t(--composeStrategy | -s <STRATEGY>): specify the composition strategy. Possible values are DFS (default), BFS, RANDOM.\n" +
		  "\t(--maxStateGen | -m <MAX_STATES>): specify the limit of state generation. Zero sets no limit (default).\n" +
		  "\t(--format | -f) <EXPORTFORMAT>: Specify the output format: aut for Aldebaran (default).\n" +
		  "\t(--output | -o) <OUTPUTFILE>: Specify the output filename (default: no output file)\n" +
		  "\t--monolithic | -m:\n" +
		  "\t(--recurse | -r) <NUMBER_OF_MODULES>: Should NOT be used with --memoize.\n" +
		  "\t(--memoize | -z) [PROJECT] <NUMBER_OF_MODULES>: do additional memoization on top of learning. Should NOT be used with --recurse.\n" +
		  "\t(--refine | -n) [HEURISTIC]: available heuristics: fwd, bwd, alldiff, bwdPluspropAlpha, prevAlpha" +
		  " (for recursive only and it does bwd without initializing by property alphabet).\n\n";
		System.out.print(usage);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileTxt = "";
		LTSABatch batch = null;
		String ltsFilename= null;
		Integer moduleCount= null;

		CmdLineParser cmdParser= new CmdLineParser();
		Option monolithic= cmdParser.addBooleanOption('m', "monolithic");
		Option recurse= cmdParser.addIntegerOption('r', "recurse");
		Option memoize= cmdParser.addStringOption('z', "memoize");
		Option refine= cmdParser.addStringOption('n', "refine");
		Option output= cmdParser.addStringOption('o', "output");
		Option format= cmdParser.addStringOption('f', "format");
		Option strategy= cmdParser.addStringOption('s', "composeStrategy");
		Option maxGen= cmdParser.addIntegerOption('m', "maxStateGen");

		try {
			cmdParser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.out.println("Invalid option: " + e.getMessage() + "\n");
			showUsage();
			System.exit(0);
		}

		//parse arguments
		Boolean monolithicValue= (Boolean) cmdParser.getOptionValue(monolithic, Boolean.FALSE);
		Integer recurseValue= (Integer) cmdParser.getOptionValue(recurse);
		String memoizeValue= (String) cmdParser.getOptionValue(memoize);
		String refineValue= (String) cmdParser.getOptionValue(refine);
		String outputValue= (String) cmdParser.getOptionValue(output);
		String formatValue= (String) cmdParser.getOptionValue(format);
		String strategyValue= (String) cmdParser.getOptionValue(strategy, "DFS");
		Integer maxGenValue= (Integer) cmdParser.getOptionValue(maxGen, new Integer(0));
		String[] unnamedOpts= cmdParser.getRemainingArgs();
		
		if (unnamedOpts.length == 0 || unnamedOpts.length > 2) {
			showUsage();
			System.exit(0);
		} else {
			for (String opt: unnamedOpts) {
				// one should be a number (ONLY if recurse is not specified), the other a filename
				try {
					moduleCount= Integer.parseInt(opt); // unsafe if filename can be interpreted as a number
				} catch (NumberFormatException e) {
				}
				
				if (ltsFilename == null)
					ltsFilename= opt;
			}
		}

		// analyse all options, sift through illegal combos
		if ( (recurseValue != null && memoize != null) ||
			 ltsFilename == null ||
			 (outputValue == null && moduleCount == null && recurseValue == null && memoizeValue == null && refineValue == null)
		   )
		{
			System.out.println("Invalid parameter combination\n");
			showUsage();
			System.exit(0);
		}
		
		try {
			BufferedReader file = new BufferedReader(new FileReader(ltsFilename));
			String thisLine;
			StringBuffer buff = new StringBuffer();
			while ((thisLine = file.readLine()) != null) {
				buff.append(thisLine+"\n");
			}
			file.close();
			fileTxt=buff.toString();
		} catch (Exception e) {
			System.err.print("Error reading LTS file " + ltsFilename + ": " + e);
		}

		if (outputValue == null) { // old style stuff. TODO: see what happens here and uniformise all
			if (!monolithicValue && recurseValue == null && memoizeValue == null &&	refineValue == null) {
				// only fileName
				batch = new LTSABatch(fileTxt, 2, false, false,"none",false);
			} else if (monolithicValue && recurseValue == null && memoizeValue == null && refineValue == null) {
				batch = new LTSABatch(fileTxt, 2, false, false,"none",false);
				batch.safety();
				System.exit(0);
			} else if (!monolithicValue && recurseValue != null && memoizeValue == null && refineValue == null) {
				// <file> recurse <number of modules>
				if (moduleCount == null) {
					System.out.println("Specified options require module count\n");
					showUsage();
					System.exit(0);
				}
				batch = new LTSABatch(fileTxt, moduleCount, false, false,"none",false);
			} else if (!monolithicValue && recurseValue == null && memoizeValue == null && refineValue !=null) {
				// <file> <number of modules> refine <heuristic> ("refine" is "true")
				if (moduleCount == null) {
					System.out.println("Specified options require module count\n");
					showUsage();
					System.exit(0);
				}
				batch = new LTSABatch(fileTxt, moduleCount, false, true, refineValue, false);
			} else if (!monolithicValue  && recurseValue == null && memoizeValue != null && refineValue != null) {
				if (moduleCount == null) {
					System.out.println("Specified options require module count\n");
					showUsage();
					System.exit(0);
				}
				batch = new LTSABatch(fileTxt, moduleCount, true, true, refineValue, false);
			} else {
				System.out.println("Invalid options or combination specified\n");
				showUsage();
				System.exit(0);
			}
		} else {
			// new style options. TODO monolithic,recurse,memoize,refine will not be heeded for now
			Options.setMaxStatesGeneration(maxGenValue);
			if (strategyValue.equals("DFS"))
				Options.setCompositionStrategyClass(CompositionStrategy.DFS_STRATEGY);
			else if (strategyValue.equals("BFS"))
				Options.setCompositionStrategyClass(CompositionStrategy.BFS_STRATEGY);
			else if (strategyValue.equals("RANDOM"))
				Options.setCompositionStrategyClass(CompositionStrategy.RANDOM_STRATEGY);
			else {
				System.out.println("Invalid composition strategy specified: " + strategyValue + "\n");
				showUsage();
				System.exit(0);
			}

			String outFilePath= outputValue; 
			CompositeState compositeState= null;
			try {
				compositeState= compileCompositeState(fileTxt, "QUEUESSYSTEM");
			} catch (Exception e) {
				System.err.print("Error compiling MTS: " + e);
			}

			try {
				File outFile= new File(outFilePath); 
				FileOutputStream foStream= new FileOutputStream(outFile);
				PrintStream outStream= new PrintStream(foStream); 
				compositeState.composition.printAUT(outStream);
				outStream.close(); foStream.close();
			} catch (Exception e) {
				System.err.print("Error while exporting to file " + outFilePath);
				System.exit(0);
			}
		}
		
		// parse model
		// batch.parse();
		// do compositional learning 
		// batch.compositionBatch.SET();
		
	}
	
	
	private static CompositeState compileCompositeState(String inputString, String modelName) throws IOException {
		return compileComposite(modelName, new LTSInputString(inputString));
	}

	public static CompositeState compileComposite(String modelName, LTSInput input)
			throws IOException {
		LTSOutput output = new StandardOutput(); 
		String currentDirectory = (new File(".")).getCanonicalPath();
		LTSCompiler compiler = new LTSCompiler( input , output , currentDirectory );
		//lts.SymbolTable.init();
		compiler.compile();
		CompositeState c = compiler.continueCompilation(modelName);
		TransitionSystemDispatcher.applyComposition(c, output);
		return c;
	}
}
