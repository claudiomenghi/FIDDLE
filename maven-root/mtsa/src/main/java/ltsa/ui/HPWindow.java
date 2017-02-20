package ltsa.ui;

// This is an experimental version with progress & LTL property check

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.commons.logging.LogFactory;
import org.jfree.util.Log;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Preconditions;

import MTSTools.ac.ic.doc.mtstools.model.SemanticType;
import ltsa.custom.CustomAnimator;
import ltsa.custom.SceneAnimator;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.editor.ColoredEditorKit;
import ltsa.exploration.Explorer;
import ltsa.exploration.ExplorerDefinition;
import ltsa.jung.LTSJUNGCanvas;
import ltsa.jung.LTSJUNGCanvas.EnumLayout;
import ltsa.lts.Diagnostics;
import ltsa.lts.animator.Animator;
import ltsa.lts.automata.automaton.event.LTSEvent;
import ltsa.lts.automata.lts.LTSError;
import ltsa.lts.automata.lts.LTSException;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.checkers.Analyser;
import ltsa.lts.checkers.ProgressCheck;
import ltsa.lts.checkers.modelchecker.ModelChecker;
import ltsa.lts.checkers.realizability.RealizabilityChecker;
import ltsa.lts.checkers.substitutability.SubstitutabilityChecker;
import ltsa.lts.checkers.wellformedness.WellFormednessChecker;
import ltsa.lts.csp.CompositionExpression;
import ltsa.lts.csp.MenuDefinition;
import ltsa.lts.csp.ProcessSpec;
import ltsa.lts.csp.Relation;
import ltsa.lts.gui.EventManager;
import ltsa.lts.gui.LTSCanvas;
import ltsa.lts.gui.MaxStatesDialog;
import ltsa.lts.gui.RandomSeedDialog;
import ltsa.lts.gui.RunMenu;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.PostconditionDefinition;
import ltsa.lts.ltl.PreconditionDefinition;
import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.ltl.formula.factory.FormulaFactory;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.PostconditionDefinitionManager;
import ltsa.lts.parser.PreconditionDefinitionManager;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.lts.parser.ltsinput.LTSInput;
import ltsa.lts.util.MTSUtils;
import ltsa.lts.util.Options;


public class HPWindow extends JFrame implements LTSManager, LTSInput, LTSOutput, LTSError, Runnable {

	/** Logger available to subclasses */
	protected final org.apache.commons.logging.Log logger = LogFactory.getLog(getClass());

	private static final String FILE_TYPE = "*.lts";
	private String openFile = FILE_TYPE;
	String currentDirectory;
	private String savedText = "";
	/**
	 * 
	 */
	private static final long serialVersionUID = 8073353695841782352L;

	private static final String DEFAULT = "DEFAULT";

	// ------------------------------------------------------------------------

	private static final int DOSAFETYNODEADLOCK = 2;

	private static final int DOEXECUTE = 3;
	private static final int DOCOMPILE = 5;
	private static final int DOCOMPOSITION = 6;
	private static final int DOMINIMISECOMPOSITION = 7;
	private static final int DOMODELCHECK = 9;
	private static final int DOPARSE = 10;

	// Dipi
	private static final int DOPLUSCR = 11;
	private static final int DOPLUSCA = 12;
	private static final int DODETERMINISE = 13;
	private static final int DOREFINEMENT = 14;
	private static final int DOCONSISTENCY = 16;
	// Dipi

	static final int DOARRANGEDANIMATOR = 19;
	static final int DOEXPLORATION = 21;
	static final int DOEXPLORATIONSTEPOVER = 25;
	static final int DOEXPLORATIONRESUME = 23;
	static final int DOEXPLORATIONMANUAL = 24;

	private static final int DORUNENACTORS = 17;
	private static final int DOENACTORSOPTIONS = 18;

	private static final int DOSAFERYMULTICE = 20;
	private static final int DOPRECONDITION = 26;

	private static final int DOREALIZABILITY = 27;

	private int theAction = 0;
	private Thread executer;

	LTSCompiler comp;
	JTextArea output;
	JEditorPane input;
	JEditorPane manual;
	AlphabetWindow alphabet;
	PrintWindow prints;
	LTSDrawWindow draws;
	LTSLayoutWindow layouts;
	JTabbedPane textIO;
	JToolBar tools;
	JTextField stepscount;

	FindDialog findDialog;

	JComboBox<String> environmentTargetChoice;
	JComboBox<String> controllerTargetChoice;
	EventManager eman = new EventManager();
	Frame animator = null;
	CompositeState current = null;
	Explorer explorer = null;
	Hashtable<String, ExplorerDefinition> explorerDefinitions = null;
	String run_menu = DEFAULT;
	String asserted = null;

	// >>> AMES: Enhanced Modularity
	Hashtable<String, LabelSet> labelSetConstants = null;
	// <<< AMES

	// Listener for the edits on the current document.
	protected UndoableEditListener undoHandler = new UndoHandler();
	// UndoManager that we add edits to.
	protected UndoManager undo = new UndoManager();

	JMenu file;
	JMenu edit;
	JMenu check;
	JMenu build;
	JMenu window;
	JMenu option;
	JMenu menuEnactment;
	JMenu menuEnactmentEnactors;
	JMenuItem fileNew;
	JMenuItem fileOpen;
	JMenuItem fileSave;
	JMenuItem fileSaveAs;
	JMenuItem fileExport;
	JMenuItem fileExit;
	JMenuItem editCut;
	JMenuItem editCopy;
	JMenuItem editPaste;
	JMenuItem editUndo;
	JMenuItem editRedo;
	JMenuItem buildParse;
	JMenuItem buildCompile;
	JMenuItem buildCompose;
	JMenuItem buildMinimise;
	JMenuItem mtsRefinement;
	JMenuItem mtsConsistency;

	JMenuItem controllerSynthesis;
	JMenuItem menuEnactmentRun;
	JMenuItem menu_enactment_options;
	JMenuItem layout_options;

	// >>> AMES: Deadlock Insensitive Analysis
	JMenuItem check_safe_no_deadlock;
	// <<< AMES

	JMenuItem check_safe_multi_ce;

	JMenuItem edit_find;

	JMenu file_example, checkLiveness, compositionStrategy;
	JMenu checkRealizability;

	JMenu wellFormednessChecker;
	JMenu checkPostconditions;
	JMenuItem[] runItems;
	JMenuItem[] assertItems;
	JMenuItem[] realizabilityItems;
	JMenuItem[] preconditionsItems;
	JMenuItem[] postconditionsItems;

	String[] runNames;
	String[] assertNames;

	boolean[] runEnabled;
	JCheckBoxMenuItem setWarnings;
	JCheckBoxMenuItem setWarningsAreErrors;
	JCheckBoxMenuItem setFair;
	JCheckBoxMenuItem setAlphaLTL;
	JCheckBoxMenuItem setSynchLTL;
	JCheckBoxMenuItem setPartialOrder;
	JCheckBoxMenuItem setObsEquiv;
	JCheckBoxMenuItem setReduction;
	JCheckBoxMenuItem setBigFont;
	JCheckBoxMenuItem setDisplayName;
	JCheckBoxMenuItem setNewLabelFormat;
	JCheckBoxMenuItem setAutoRun;
	JCheckBoxMenuItem setMultipleLTS;
	JCheckBoxMenuItem helpManual;
	JCheckBoxMenuItem windowAlpha;
	JCheckBoxMenuItem windowPrint;
	JCheckBoxMenuItem windowDraw;
	JCheckBoxMenuItem windowLayout;
	JRadioButtonMenuItem strategyDFS;
	JRadioButtonMenuItem strategyBFS;
	JRadioButtonMenuItem strategyRandom;
	ButtonGroup strategyGroup;
	JMenuItem maxStateGeneration;
	JMenuItem randomSeed;

	// tool bar buttons - that need to be enabled and disabled
	JButton // stopTool,
	parseTool, progressTool, cutTool, pasteTool, newFileTool, openFileTool, saveFileTool, compileTool, composeTool,
			minimizeTool, undoTool, redoTool;
	// used to implement muCSPInput
	int fPos = -1;
	String fSrc = "\n";
	public static final Font FIXED = new Font("Monospaced", Font.PLAIN, 12);
	public static final Font BIG = new Font("Monospaced", Font.BOLD, 20);

	private AppletButton isApplet = null;

	private ApplicationContext applicationContext = null;

	private String lastCompiled = "";

	public HPWindow(AppletButton isap) {

		isApplet = isap;
		// SymbolTable.init();
		getContentPane().setLayout(new BorderLayout());

		textIO = new JTabbedPane();

		// edit window for specification source
		// input = new JTextArea("",24,80);
		input = new JEditorPane();

		input.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.getKeyCode() == KeyEvent.VK_Z) && e.isControlDown()) {
					try {
						undo.undo();
					} catch (CannotUndoException ignored) {

					}
					updateDoState();
				}

				if ((e.getKeyCode() == KeyEvent.VK_Y) && e.isControlDown()) {
					try {
						undo.redo();
					} catch (CannotUndoException ignored) {

					}
					updateDoState();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}
		});

		input.setEditorKit(new ColoredEditorKit());

		input.setFont(FIXED);
		// Dipi, only for viva
		// input.setFont(BIG);
		input.setBackground(Color.white);
		input.getDocument().addUndoableEditListener(undoHandler);
		undo.setLimit(10); // set maximum undo edits
		// input.setLineWrap(true);
		// input.setWrapStyleWord(true);
		input.setBorder(new EmptyBorder(0, 5, 0, 0));
		JScrollPane inp = new JScrollPane(input, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		textIO.addTab("Edit", inp);
		// results window
		output = new JTextArea("", 30, 100);
		output.setEditable(false);
		// output.setFont(FIXED);
		// Dipi, only for viva
		output.setFont(BIG);
		output.setBackground(Color.white);
		output.setLineWrap(true);
		output.setWrapStyleWord(true);
		output.setBorder(new EmptyBorder(0, 5, 0, 0));
		JScrollPane outp = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textIO.addTab("Output", outp);
		textIO.addChangeListener(new TabChange());
		textIO.setRequestFocusEnabled(false);
		getContentPane().add("Center", textIO);
		// Build the menu bar.
		JMenuBar mb = new JMenuBar();
		setJMenuBar(mb);
		// file menu
		file = new JMenu("File");
		mb.add(file);
		fileNew = new JMenuItem("New");
		fileNew.addActionListener(new NewFileAction());
		file.add(fileNew);
		fileOpen = new JMenuItem("Open...");
		fileOpen.addActionListener(new OpenFileAction());
		file.add(fileOpen);
		fileSave = new JMenuItem("Save");
		fileSave.addActionListener(new SaveFileAction());
		file.add(fileSave);
		fileSaveAs = new JMenuItem("Save as...");
		fileSaveAs.addActionListener(new SaveAsFileAction());
		file.add(fileSaveAs);
		fileExport = new JMenuItem("Export...");
		fileExport.addActionListener(new ExportFileAction());
		file.add(fileExport);
		file_example = new JMenu("Examples");
		new Examples(file_example, this).getExamples();
		file.add(file_example);
		fileExit = new JMenuItem("Quit");
		fileExit.addActionListener(new ExitFileAction());
		file.add(fileExit);
		// edit menu
		edit = new JMenu("Edit");
		mb.add(edit);
		editCut = new JMenuItem("Cut");
		editCut.addActionListener(new EditCutAction());
		edit.add(editCut);
		editCopy = new JMenuItem("Copy");
		editCopy.addActionListener(new EditCopyAction());
		edit.add(editCopy);
		editPaste = new JMenuItem("Paste");
		editPaste.addActionListener(new EditPasteAction());
		edit.add(editPaste);
		edit.addSeparator();
		editUndo = new JMenuItem("Undo");
		editUndo.addActionListener(new UndoAction());
		edit.add(editUndo);
		editRedo = new JMenuItem("Redo");
		editRedo.addActionListener(new RedoAction());
		edit.add(editRedo);

		edit_find = new JMenuItem("Find");
		edit_find.addActionListener(new EditFindAction());
		edit.add(edit_find);
		
		// check menu
		check = new JMenu("Check");
		mb.add(check);

		checkRealizability = new JMenu("Realizability checker");
		if (hasLTL2BuchiJar()) {
			check.add(checkRealizability);
		}

		checkLiveness = new JMenu("Model checker");
		if (hasLTL2BuchiJar()) {
			check.add(checkLiveness);
		}
		wellFormednessChecker = new JMenu("Well-formedness checker");
		check.add(wellFormednessChecker);

		checkPostconditions = new JMenu("Substitutability checker");
		check.add(checkPostconditions);

		// build menu
		build = new JMenu("Build");
		mb.add(build);
		buildParse = new JMenuItem("Parse");
		buildParse.addActionListener(new DoAction(DOPARSE));
		build.add(buildParse);
		buildCompile = new JMenuItem("Compile");
		buildCompile.addActionListener(new DoAction(DOCOMPILE));
		build.add(buildCompile);
		buildCompose = new JMenuItem("Compose");
		buildCompose.addActionListener(new DoAction(DOCOMPOSITION));
		build.add(buildCompose);
		buildMinimise = new JMenuItem("Minimise");
		buildMinimise.addActionListener(new DoAction(DOMINIMISECOMPOSITION));
		build.add(buildMinimise);

		// window menu
		window = new JMenu("Window");
		mb.add(window);
		windowAlpha = new JCheckBoxMenuItem("Alphabet");
		windowAlpha.setSelected(false);
		windowAlpha.addActionListener(new WinAlphabetAction());
		window.add(windowAlpha);
		windowPrint = new JCheckBoxMenuItem("Transitions");
		windowPrint.setSelected(false);
		windowPrint.addActionListener(new WinPrintAction());
		window.add(windowPrint);
		windowDraw = new JCheckBoxMenuItem("Draw");
		windowDraw.setSelected(true);
		windowDraw.addActionListener(new WinDrawAction());
		window.add(windowDraw);
		// layout
		windowLayout = new JCheckBoxMenuItem("Layout");
		windowLayout.setSelected(true);
		windowLayout.addActionListener(new WinLayoutAction());
		window.add(windowLayout);
		// help menu
		// option menu
		OptionAction opt = new OptionAction();
		option = new JMenu("Options");
		mb.add(option);
		setWarnings = new JCheckBoxMenuItem("Display warning messages");
		setWarnings.addActionListener(opt);
		option.add(setWarnings);
		setWarnings.setSelected(true);
		setWarningsAreErrors = new JCheckBoxMenuItem("Treat warnings as errors");
		setWarningsAreErrors.addActionListener(opt);
		option.add(setWarningsAreErrors);
		setWarningsAreErrors.setSelected(false);
		setFair = new JCheckBoxMenuItem("Fair Choice for LTL check");
		setFair.addActionListener(opt);
		option.add(setFair);
		setFair.setSelected(false);

		ProgressCheck.strongFairFlag = setFair.isSelected();

		setAlphaLTL = new JCheckBoxMenuItem("Alphabet sensitive LTL");
		setAlphaLTL.addActionListener(opt);
		// option.add(setAlphaLTL);
		setAlphaLTL.setSelected(false);
		setSynchLTL = new JCheckBoxMenuItem("Timed LTL");
		setSynchLTL.addActionListener(opt);
		// option.add(setSynchLTL);
		setSynchLTL.setSelected(false);
		setPartialOrder = new JCheckBoxMenuItem("Partial Order Reduction");
		setPartialOrder.addActionListener(opt);
		option.add(setPartialOrder);
		setPartialOrder.setSelected(false);

		compositionStrategy = new JMenu("Composition Strategy");
		strategyGroup = new ButtonGroup();
		strategyDFS = new JRadioButtonMenuItem("Depth-first");
		strategyBFS = new JRadioButtonMenuItem("Breadth-first");
		strategyRandom = new JRadioButtonMenuItem("Random");
		strategyGroup.add(strategyDFS);
		strategyGroup.add(strategyBFS);
		strategyGroup.add(strategyRandom);
		compositionStrategy.add(strategyDFS);
		compositionStrategy.add(strategyBFS);
		compositionStrategy.add(strategyRandom);
		strategyDFS.setSelected(true);
		strategyDFS.addActionListener(opt);
		strategyBFS.addActionListener(opt);
		strategyRandom.addActionListener(opt);
		option.add(compositionStrategy);

		maxStateGeneration = new JMenuItem("Set max state limit...");
		maxStateGeneration.addActionListener(opt);
		option.add(maxStateGeneration);

		randomSeed = new JMenuItem("Set seed for randomization...");
		randomSeed.addActionListener(opt);
		option.add(randomSeed);

		setObsEquiv = new JCheckBoxMenuItem("Preserve OE for POR composition");
		setObsEquiv.addActionListener(opt);
		option.add(setObsEquiv);
		setObsEquiv.setSelected(true);
		setReduction = new JCheckBoxMenuItem("Enable Tau Reduction");
		setReduction.addActionListener(opt);
		option.add(setReduction);
		setReduction.setSelected(true);
		option.addSeparator();
		setBigFont = new JCheckBoxMenuItem("Use big font");
		setBigFont.addActionListener(opt);
		option.add(setBigFont);
		setBigFont.setSelected(false);
		setDisplayName = new JCheckBoxMenuItem("Display name when drawing LTS");
		setDisplayName.addActionListener(opt);
		option.add(setDisplayName);
		setDisplayName.setSelected(true);
		setNewLabelFormat = new JCheckBoxMenuItem("Use V2.0 label format when drawing LTS");
		setNewLabelFormat.addActionListener(opt);
		option.add(setNewLabelFormat);
		setNewLabelFormat.setSelected(true);
		setMultipleLTS = new JCheckBoxMenuItem("Multiple LTS in Draw and Layout windows");
		setMultipleLTS.addActionListener(opt);
		option.add(setMultipleLTS);
		setMultipleLTS.setSelected(false);
		option.addSeparator();
		setAutoRun = new JCheckBoxMenuItem("Auto run actions in Animator");
		setAutoRun.addActionListener(opt);
		option.add(setAutoRun);
		setAutoRun.setSelected(false);

		layout_options = new JMenuItem("Layout parameters");
		layout_options.addActionListener(new LayoutOptionListener());
		option.add(layout_options);

		menuEnactment = new JMenu("Enactment");
		mb.add(menuEnactment);
		// menu_enactment_enactors = new JMenu("Enactors");
		// menu_enactment.add(menu_enactment_enactors);
		// Loads robot enactors
		// fillEnactorsMenu(menu_enactment_enactors);

		menu_enactment_options = new JMenuItem("Options");
		menu_enactment_options.addActionListener(new DoAction(DOENACTORSOPTIONS));
		menuEnactment.add(menu_enactment_options);
		menuEnactmentRun = new JMenuItem("Run model");
		menuEnactmentRun.addActionListener(new DoAction(DORUNENACTORS));
		menuEnactment.add(menuEnactmentRun);

		// toolbar
		tools = new JToolBar();
		tools.setFloatable(false);
		tools.add(newFileTool = createTool("src/main/java/ltsa/ui/icon/new.gif", "New file", new NewFileAction()));
		tools.add(openFileTool = createTool("src/main/java/ltsa/ui/icon/open.gif", "Open file", new OpenFileAction()));
		tools.add(saveFileTool = createTool("src/main/java/ltsa/ui/icon/save.gif", "Save File", new SaveFileAction()));
		tools.addSeparator();
		tools.add(cutTool = createTool("src/main/java/ltsa/ui/icon/cut.gif", "Cut", new EditCutAction()));
		tools.add(createTool("src/main/java/ltsa/ui/icon/copy.gif", "Copy", new EditCopyAction()));
		tools.add(pasteTool = createTool("src/main/java/ltsa/ui/icon/paste.gif", "Paste", new EditPasteAction()));
		tools.add(undoTool = createTool("src/main/java/ltsa/ui/icon/undo.gif", "Undo", new UndoAction()));
		tools.add(redoTool = createTool("src/main/java/ltsa/ui/icon/redo.gif", "Redo", new RedoAction()));
		tools.addSeparator();
		tools.add(parseTool = createTool("src/main/java/ltsa/ui/icon/parse.gif", "Parse", new DoAction(DOPARSE)));
		tools.add(
				compileTool = createTool("src/main/java/ltsa/ui/icon/compile.gif", "Compile", new DoAction(DOCOMPILE)));
		tools.add(composeTool = createTool("src/main/java/ltsa/ui/icon/compose.gif", "Compose",
				new DoAction(DOCOMPOSITION)));
		tools.add(minimizeTool = createTool("src/main/java/ltsa/ui/icon/minimize.gif", "Minimize",
				new DoAction(DOMINIMISECOMPOSITION)));
		// status field used to name the composition we are wrking on
		this.environmentTargetChoice = new JComboBox<>();
		this.environmentTargetChoice.setEditable(false);
		this.environmentTargetChoice.addItem(DEFAULT);
		this.environmentTargetChoice.setToolTipText("Target Composition");
		this.environmentTargetChoice.setRequestFocusEnabled(false);
		this.environmentTargetChoice.addActionListener(new TargetAction());

		this.controllerTargetChoice = new JComboBox<>();
		this.controllerTargetChoice.setEditable(false);
		this.controllerTargetChoice.addItem(DEFAULT);
		this.controllerTargetChoice.setToolTipText("Target Composition");
		this.controllerTargetChoice.setRequestFocusEnabled(false);
		this.controllerTargetChoice.addActionListener(new TargetAction());

		tools.addSeparator();
		tools.add(new JLabel("Environment"));
		tools.add(environmentTargetChoice);
		tools.addSeparator();
		tools.add(new JLabel("Partial component"));
		tools.add(controllerTargetChoice);
		tools.addSeparator();

		getContentPane().add("North", tools);

		// >>> AMES: Text Search
		findDialog = new FindDialog(this) {
			JTextComponent currentTextComponent() {
				String title = textIO.getTitleAt(textIO.getSelectedIndex());
				if (title.equals("Edit"))
					return input;
				else if (title.equals("Output"))
					return output;
				else
					return null;
			}
		};
		// <<< AMES

		// enable menus
		menuEnable(true);
		fileSave.setEnabled(isApplet == null);
		fileSaveAs.setEnabled(isApplet == null);
		fileExport.setEnabled(isApplet == null);
		saveFileTool.setEnabled(isApplet == null);
		updateDoState();
		// switch to edit tab
		LTSCanvas.displayName = setDisplayName.isSelected();
		LTSCanvas.newLabelFormat = setNewLabelFormat.isSelected();
		LTSDrawWindow.singleMode = !setMultipleLTS.isSelected();
		newDrawWindow(windowDraw.isSelected());
		// create layout tab
		LTSLayoutWindow.singleMode = !setMultipleLTS.isSelected();
		newLayoutWindow(windowLayout.isSelected());
		// switch to edit tab
		swapto(0);
		// close window action
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new CloseWindow());
	}

	// ----------------------------------------------------------------------
	static void centre(Component c) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screen = tk.getScreenSize();
		Dimension ltsa = c.getSize();
		double x = (screen.getWidth() - ltsa.getWidth()) / 2;
		double y = (screen.getHeight() - ltsa.getHeight()) / 2;
		c.setLocation((int) x, (int) y);
	}

	// ----------------------------------------------------------------------
	void left(Component c) {
		Point ltsa = getLocationOnScreen();
		ltsa.translate(10, 100);
		c.setLocation(ltsa);
	}

	// -----------------------------------------------------------------------
	protected JButton createTool(String icon, String tip, ActionListener act) {
		URL url = null;
		try {
			url = new File(icon).toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		JButton b = new JButton(new ImageIcon(url)) {
			@Override
			public float getAlignmentY() {
				return 0.5f;
			}
		};
		b.setRequestFocusEnabled(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		b.setToolTipText(tip);
		b.addActionListener(act);
		return b;
	}

	protected JTextField createTextBox() {
		JTextField t = new JTextField("1", 1);
		t.setMaximumSize(new Dimension(300, 300));
		return t;
	}

	// ------------------------------------------------------------------------

	void menuEnable(boolean flag) {
		boolean application = (isApplet == null);
		fileNew.setEnabled(flag && tabindex == 0);
		file_example.setEnabled(flag && tabindex == 0);
		fileOpen.setEnabled(application && flag && tabindex == 0);
		fileExit.setEnabled(flag);

		buildParse.setEnabled(flag);
		buildCompile.setEnabled(flag);
		buildCompose.setEnabled(flag);
		buildMinimise.setEnabled(flag);
		parseTool.setEnabled(flag);
		compileTool.setEnabled(flag);
		composeTool.setEnabled(flag);
		minimizeTool.setEnabled(flag);
	}

	private void doAction(int action) {
		menuEnable(false);

		theAction = action;
		executer = new Thread(this);
		executer.setPriority(Thread.NORM_PRIORITY - 1);
		executer.start();
	}

	public void run() {
		try {
			switch (theAction) {
			case DOPRECONDITION:
				showOutput();
				precondition();
				break;
			case DOSAFETYNODEADLOCK:
				showOutput();
				safety(false, false);
				break;
			case DOSAFERYMULTICE:
				showOutput();
				safety(false, true);
				break;
			case DOARRANGEDANIMATOR:
				animate();
				break;
			case DOCOMPILE:
				showOutput();
				compile();
				break;
			case DOCOMPOSITION:
				showOutput();
				doComposition();
				break;
			case DOMINIMISECOMPOSITION:
				showOutput();
				minimiseComposition();
				break;
			case DOMODELCHECK:
				showOutput();
				modelCheck();
				break;
			case DOREALIZABILITY:
				showOutput();
				realizability();
				break;
			case DOPARSE:
				parse();
				break;
			case DOPLUSCR:
				doApplyPlusCROperator();
				break;
			case DOPLUSCA:
				doApplyPlusCAOperator();
				break;
			case DODETERMINISE:
				doDeterminise();
				break;
			case DOREFINEMENT:
				doRefinement();
				break;
			case DOCONSISTENCY:
				doConsistency();
				break;
			case DORUNENACTORS:
				doRunEnactors();
				break;

			}
		} catch (Throwable e) {
			showOutput();
			outln("**** Runtime Exception: " + e);
			e.printStackTrace();
			current = null;
			explorer = null;
			explorerDefinitions = null;
		}
		menuEnable(true);
		// check_stop.setEnabled(false);
		// stopTool.setEnabled(false);
	}

	// ------------------------------------------------------------------------

	class CloseWindow extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			quitAll();
		}

		@Override
		public void windowActivated(WindowEvent e) {
			if (animator != null)
				animator.toFront();
		}
	}

	// ------------------------------------------------------------------------
	private void invalidateState() {
		current = null;
		explorer = null;
		explorerDefinitions = null;
		environmentTargetChoice.removeAllItems();
		environmentTargetChoice.addItem(DEFAULT);
		runItems = null;
		assertItems = null;
		preconditionsItems = null;
		runNames = null;
		checkLiveness.removeAll();
		this.wellFormednessChecker.removeAll();
		validate();
		eman.post(new LTSEvent(LTSEvent.INVALID, null));
		if (animator != null) {
			animator.dispose();
			animator = null;
		}
	}

	private void postState(CompositeState m) {
		if (animator != null) {
			animator.dispose();
			animator = null;
		}
		eman.post(new LTSEvent(LTSEvent.INVALID, m));
	}

	// ------------------------------------------------------------------------
	// File handling
	// -----------------------------------------------------------------------

	private void newFile() {
		if (checkSave()) {
			setTitle("FIDDLE");
			savedText = "";
			openFile = FILE_TYPE;
			input.setText("");
			swapto(0);
			output.setText("");
			invalidateState();
		}
		repaint(); // hack to solve display problem
	}

	public void newExample(String dir, String ex) {
		undo.discardAllEdits();
		input.getDocument().removeUndoableEditListener(undoHandler);
		if (checkSave()) {
			invalidateState();
			clearOutput();
			doOpenFile(dir, ex, true);
		}
		input.getDocument().addUndoableEditListener(undoHandler);
		updateDoState();
		repaint();
	}

	// ------------------------------------------------------------------------
	private void openAFile() {
		if (checkSave()) {
			invalidateState();
			clearOutput();
			FileDialog fd = new FileDialog(this, "Select source file:");
			if (currentDirectory != null)
				fd.setDirectory(currentDirectory);
			fd.setFile(FILE_TYPE);
			fd.setVisible(true);
			doOpenFile(currentDirectory = fd.getDirectory(), fd.getFile(), false);
		}
		repaint(); // hack to solve display problem
	}

	private void doOpenFile(String dir, String f, boolean resource) {
		if (f != null)
			try {
				openFile = f;
				setTitle("FIDDLE " + openFile);
				InputStream fin;
				if (!resource)
					fin = new FileInputStream(dir + openFile);
				else
					fin = this.getClass().getResourceAsStream(dir + openFile);
				// now turn the FileInputStream into a DataInputStream
				try {
					BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
					try {
						String thisLine;
						StringBuffer buff = new StringBuffer();
						while ((thisLine = myInput.readLine()) != null) {
							buff.append(thisLine + "\n");
						}
						savedText = buff.toString();
						input.setText(savedText);
						parse();
					} catch (Exception e) {
						outln("Error reading file: " + e);
					}
				} // end try
				catch (Exception e) {
					outln("Error creating InputStream: " + e);
				}
			} // end try
			catch (Exception e) {
				outln("Error creating FileInputStream: " + e);
			}

		// >>> AMES: Arbitrary Fixes
		input.setCaretPosition(0);
		// <<< AMES
	}

	// ------------------------------------------------------------------------

	private void saveAsFile() {
		FileDialog fd = new FileDialog(this, "Save file in:", FileDialog.SAVE);
		if (currentDirectory != null)
			fd.setDirectory(currentDirectory);
		fd.setFile(openFile);
		fd.setVisible(true);
		String tmp = fd.getFile();
		if (tmp != null) { // if not cancelled
			currentDirectory = fd.getDirectory();
			openFile = tmp;
			setTitle("FIDDLE - " + openFile);
			saveFile();
		}
	}

	private void saveFile() {
		if (openFile != null && openFile.equals("*.lts"))
			saveAsFile();
		else if (openFile != null)
			try {
				int i = openFile.indexOf('.', 0);
				if (i > 0)
					openFile = openFile.substring(0, i) + "." + "lts";
				else
					openFile = openFile + ".lts";
				String tempname = (currentDirectory == null) ? openFile : currentDirectory + openFile;
				FileOutputStream fout = new FileOutputStream(tempname);
				// now convert the FileOutputStream into a PrintStream
				PrintStream myOutput = new PrintStream(fout);
				savedText = input.getText();
				myOutput.print(savedText);
				myOutput.close();
				fout.close();
				outln("Saved in: " + tempname);
			} catch (IOException e) {
				outln("Error saving file: " + e);
			}
	}

	// -------------------------------------------------------------------------

	private void exportFile() {
		String message = "Export as Aldebaran format (.aut) to:";
		FileDialog fd = new FileDialog(this, message, FileDialog.SAVE);
		if (current == null || current.getComposition() == null) {
			JOptionPane.showMessageDialog(this, "No target composition to export");
			return;
		}
		String fname = current.getComposition().getName();
		fd.setFile(fname + ".aut");
		fd.setDirectory(currentDirectory);
		fd.setVisible(true);
		String sn;
		if ((sn = fd.getFile()) != null)
			try {
				int i = sn.indexOf('.', 0);
				sn = sn.substring(0, i) + ".aut";
				File file = new File(fd.getDirectory(), sn);
				FileOutputStream fout = new FileOutputStream(file);
				// now convert the FileOutputStream into a PrintStream
				PrintStream myOutput = new PrintStream(fout);
				current.getComposition().printAUT(myOutput);
				myOutput.close();
				fout.close();
				outln("Exported to: " + fd.getDirectory() + file);
			} catch (IOException e) {
				outln("Error exporting file: " + e);
			}
	}

	// ------------------------------------------------------------------------
	// return false if operation cancelled otherwise true
	private boolean checkSave() {
		if (isApplet != null)
			return true;
		if (!savedText.equals(input.getText())) {
			int result = JOptionPane.showConfirmDialog(this, "Do you want to save the contents of " + openFile);
			if (result == JOptionPane.YES_OPTION) {
				saveFile();
				return true;
			} else if (result == JOptionPane.NO_OPTION)
				return true;
			else if (result == JOptionPane.CANCEL_OPTION)
				return false;
		}
		return true;
	}

	// ------------------------------------------------------------------------
	private void doFont() {
		if (setBigFont.getState()) {
			input.setFont(BIG);
			output.setFont(BIG);
		} else {
			input.setFont(FIXED);
			output.setFont(FIXED);
		}
		pack();
		setVisible(true);
	}

	// ------------------------------------------------------------------------

	private void quitAll() {
		if (isApplet != null) {
			this.dispose();
			isApplet.ended();
		} else {
			if (checkSave())
				System.exit(0);
		}
	}

	// ----Event
	// Handling-----------------------------------------------------------

	class NewFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			undo.discardAllEdits();
			input.getDocument().removeUndoableEditListener(undoHandler);
			newFile();
			input.getDocument().addUndoableEditListener(undoHandler);
			updateDoState();
		}
	}

	class OpenFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			undo.discardAllEdits();
			input.getDocument().removeUndoableEditListener(undoHandler);
			openAFile();
			input.getDocument().addUndoableEditListener(undoHandler);
			updateDoState();
		}
	}

	class SaveFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit") || pp.equals("Output"))
				saveFile();
			else if (pp.equals("Alphabet"))
				alphabet.saveFile();
			else if (pp.equals("Transitions"))
				prints.saveFile(currentDirectory, ".txt");
			else if (pp.equals("Draw"))
				draws.saveFile();
			else if (pp.equals("Layout"))
				layouts.saveFile();
		}
	}

	class SaveAsFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit"))
				saveAsFile();
		}
	}

	class ExportFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit"))
				exportFile();
			else if (pp.equals("Transitions"))
				prints.saveFile(currentDirectory, ".aut");
		}
	}

	class ExitFileAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			quitAll();
		}
	}

	class DoAction implements ActionListener {
		int actionCode;

		DoAction(int a) {
			actionCode = a;
		}

		public void actionPerformed(ActionEvent e) {
			doAction(actionCode);
		}
	}

	class OptionAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == setWarnings)
				Diagnostics.warningFlag = setWarnings.isSelected();
			else if (source == setWarningsAreErrors)
				Diagnostics.warningsAreErrors = setWarningsAreErrors.isSelected();
			else if (source == setFair)
				ProgressCheck.strongFairFlag = setFair.isSelected();
			else if (source == setAlphaLTL) {
				AssertDefinition.addAsterisk = !setAlphaLTL.isSelected();
				PreconditionDefinitionManager.addAsterisk = !setAlphaLTL.isSelected();
			} else if (source == setSynchLTL)
				FormulaFactory.setNormalLTL(!setSynchLTL.isSelected());
			else if (source == setPartialOrder)
				Analyser.partialOrderReduction = setPartialOrder.isSelected();
			else if (source == setObsEquiv)
				Analyser.preserveObsEquiv = setObsEquiv.isSelected();
			else if (source == setReduction)
				CompositeState.reduceFlag = setReduction.isSelected();
			else if (source == setBigFont) {
				AnimArrangedWindow.fontFlag = setBigFont.isSelected();
				AlphabetWindow.fontFlag = setBigFont.isSelected();
				if (alphabet != null)
					alphabet.setBigFont(setBigFont.isSelected());
				PrintWindow.fontFlag = setBigFont.isSelected();
				if (prints != null)
					prints.setBigFont(setBigFont.isSelected());
				LTSDrawWindow.fontFlag = setBigFont.isSelected();
				if (draws != null)
					draws.setBigFont(setBigFont.isSelected());
				LTSCanvas.fontFlag = setBigFont.isSelected();
				doFont();
			} else if (source == setDisplayName) {
				if (draws != null)
					draws.setDrawName(setDisplayName.isSelected());
				LTSCanvas.displayName = setDisplayName.isSelected();
			} else if (source == setMultipleLTS) {
				LTSDrawWindow.singleMode = !setMultipleLTS.isSelected();
				if (draws != null)
					draws.setMode(LTSDrawWindow.singleMode);
				LTSLayoutWindow.singleMode = !setMultipleLTS.isSelected();
				if (layouts != null)
					layouts.setMode(LTSLayoutWindow.singleMode);
			} else if (source == setNewLabelFormat) {
				if (draws != null)
					draws.setNewLabelFormat(setNewLabelFormat.isSelected());
				LTSCanvas.newLabelFormat = setNewLabelFormat.isSelected();
			} else if (source == strategyBFS) {
				// TODO cleanup composition
				if (strategyBFS.isSelected())
					Options.setCompositionStrategyClass(Options.CompositionStrategy.BFS_STRATEGY);
			} else if (source == strategyDFS) {
				// TODO cleanup composition
				Options.setCompositionStrategyClass(Options.CompositionStrategy.DFS_STRATEGY);
			} else if (source == strategyRandom) {
				// TODO cleanup composition
				Options.setCompositionStrategyClass(Options.CompositionStrategy.RANDOM_STRATEGY);
			} else if (source == maxStateGeneration) {
				JFrame maxStatesDialog = new MaxStatesDialog();
				maxStatesDialog.setVisible(true);
			} else if (source == randomSeed) {
				JFrame randomSeedDialog = new RandomSeedDialog();
				randomSeedDialog.setVisible(true);
			}
		}
	}

	class LayoutOptionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			final JFrame f = new JFrame("Layout parameters");
			f.setResizable(false);
			f.setSize(300, 600);
			f.setLocationRelativeTo(null);
			Container container = f.getContentPane();
			container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
			JPanel content = new JPanel();
			content.setLayout(new GridLayout(0, 1));

			JPanel kkpanel = new JPanel();
			kkpanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.KamadaKawai.toString()));
			kkpanel.setLayout(new GridLayout(0, 2));

			kkpanel.add(new JLabel("Length factor"));
			final JSpinner KK_length_factor_spinner = new JSpinner(
					new SpinnerNumberModel(LTSJUNGCanvas.KK_length_factor, 0.1, 10.0, 0.1));
			kkpanel.add(KK_length_factor_spinner);

			// Not useful for connected graphs
			// kkpanel.add(new JLabel("Distance"));
			// final JSpinner KK_distance_spinner = new JSpinner(new
			// SpinnerNumberModel(LTSJUNGCanvas.KK_distance,0.1,10.0,0.1));
			// kkpanel.add(KK_distance_spinner);

			kkpanel.add(new JLabel("Max iterations"));
			final JSpinner kk_it_spinner = new JSpinner(
					new SpinnerNumberModel(LTSJUNGCanvas.KK_max_iterations, 1, 10000, 1));
			kkpanel.add(kk_it_spinner);

			container.add(kkpanel);

			JPanel frpanel = new JPanel();
			frpanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.FruchtermanReingold.toString()));
			frpanel.setLayout(new GridLayout(0, 2));

			frpanel.add(new JLabel("Attraction"));
			final JSpinner fr_attraction_spinner = new JSpinner(
					new SpinnerNumberModel(LTSJUNGCanvas.FR_attraction, 0.1, 10.0, 0.05));
			frpanel.add(fr_attraction_spinner);

			frpanel.add(new JLabel("Repulsion"));
			final JSpinner fr_repulsion_spinner = new JSpinner(
					new SpinnerNumberModel(LTSJUNGCanvas.FR_repulsion, 0.1, 10.0, 0.05));
			frpanel.add(fr_repulsion_spinner);

			frpanel.add(new JLabel("Max iterations"));
			final JSpinner fr_it_spinner = new JSpinner(
					new SpinnerNumberModel(LTSJUNGCanvas.FRMaxIterations, 1, 10000, 1));
			frpanel.add(fr_it_spinner);

			container.add(frpanel);
			final JButton okbutton = new JButton("Ok");
			final JButton cancelbutton = new JButton("Cancel");
			final JButton applybutton = new JButton("Apply");

			class ButtonOptionListener implements ActionListener {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (e.getSource() == applybutton) {
						updateValues();
						if (layouts != null) {
							layouts.refresh();
						}
					} else if (e.getSource() == okbutton) {
						updateValues();
						f.dispose();
					} else if (e.getSource() == cancelbutton) {
						f.dispose();
					}
				}

				private void updateValues() {
					try {
						Object o = KK_length_factor_spinner.getValue();
						if (o == null)
							return;
						LTSJUNGCanvas.KK_length_factor = Double.parseDouble(o.toString()) < 0 ? 0
								: (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
					} catch (NumberFormatException ignored) {
					}
					// try {
					// Object o = KK_distance_spinner.getValue();
					// if (o==null) return;
					// LTSJUNGCanvas.KK_distance =
					// Double.parseDouble(o.toString()) < 0 ? 0 :
					// (Double.parseDouble(o.toString()) > 10 ? 10 :
					// Double.parseDouble(o.toString()));
					// //if (layouts!=null)
					// layouts.setKK_distance(LTSLayoutWindow.KK_distance);
					// } catch(NumberFormatException nfe) {}
					try {
						Object o = kk_it_spinner.getValue();
						if (o == null)
							return;
						LTSJUNGCanvas.KK_max_iterations = Integer.parseInt(o.toString()) < 0 ? 0
								: (Integer.parseInt(o.toString()) > 10000 ? 10000 : Integer.parseInt(o.toString()));
						// if (layouts!=null)
						// layouts.setKK_max_iterations(LTSLayoutWindow.KK_max_iterations);
					} catch (NumberFormatException ignored) {
					}
					try {
						Object o = fr_attraction_spinner.getValue();
						if (o == null)
							return;
						LTSJUNGCanvas.FR_attraction = Double.parseDouble(o.toString()) < 0 ? 0
								: (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
						// if (layouts!=null)
						// layouts.setFR_attraction(LTSLayoutWindow.FR_attraction);
					} catch (NumberFormatException ignored) {
					}
					try {
						Object o = fr_repulsion_spinner.getValue();
						if (o == null)
							return;
						LTSJUNGCanvas.FR_repulsion = Double.parseDouble(o.toString()) < 0 ? 0
								: (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
						// if (layouts!=null)
						// layouts.setFR_repulsion(LTSLayoutWindow.FR_repulsion);
					} catch (NumberFormatException ignored) {
					}
					try {
						Object o = fr_it_spinner.getValue();
						if (o == null)
							return;
						LTSJUNGCanvas.FRMaxIterations = Integer.parseInt(o.toString()) < 0 ? 0
								: (Integer.parseInt(o.toString()) > 10000 ? 10000 : Integer.parseInt(o.toString()));
						// if (layouts!=null)
						// layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
					} catch (NumberFormatException ignored) {
					}
				}
			}

			JPanel buttonpanel = new JPanel();
			buttonpanel.setLayout(new GridLayout(0, 3));

			applybutton.addActionListener(new ButtonOptionListener());
			okbutton.addActionListener(new ButtonOptionListener());
			cancelbutton.addActionListener(new ButtonOptionListener());
			buttonpanel.add(applybutton);
			buttonpanel.add(okbutton);
			buttonpanel.add(cancelbutton);

			container.add(buttonpanel);

			f.pack();
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			f.setVisible(true);

		}
	}

	class WinAlphabetAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			newAlphabetWindow(windowAlpha.isSelected());
		}
	}

	class WinPrintAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			newPrintWindow(windowPrint.isSelected());
		}
	}

	class WinDrawAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			newDrawWindow(windowDraw.isSelected());
		}
	}

	class WinLayoutAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			newDrawWindow(windowLayout.isSelected());
		}
	}

	class HelpAboutAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			aboutDialog();
		}
	}

	class BlankAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			blankit();
		}
	}

	class HelpManualAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			displayManual(helpManual.isSelected());
		}
	}

	class StopAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (executer != null) {
				// executer.interrupt();
				executer.stop();
				outln("\n\t-- stop requested");
				int attempts = 0;
				while (attempts++ < 20 && executer.isAlive()) {
					try {
						outln(".");
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				outln("\n");
				executer = null;
			}
		}
	}

	class ExecuteAction implements ActionListener {
		String runtarget;

		ExecuteAction(String s) {
			runtarget = s;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			run_menu = runtarget;
			doAction(DOEXECUTE);
		}
	}

	class ModelCheckerAction implements ActionListener {
		String asserttarget;

		ModelCheckerAction(String s) {
			asserttarget = s;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			asserted = asserttarget;
			doAction(DOMODELCHECK);
		}
	}

	class RealizabilityAction implements ActionListener {
		String asserttarget;

		RealizabilityAction(String s) {
			asserttarget = s;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			asserted = asserttarget;
			doAction(DOREALIZABILITY);
		}
	}

	class SubcomponentCheckerAction implements ActionListener {
		String box;
		String process;
		String postCondition;

		SubcomponentCheckerAction(String box, String process, String postCondition) {
			this.box = box;
			this.process = process;
			this.postCondition = postCondition;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			showOutput();

			checkSubcomponent(this.postCondition, this.process, this.box);
		}
	}

	class WellFormednessCheckerAction implements ActionListener {
		String preconditiontarget;

		WellFormednessCheckerAction(String s) {
			preconditiontarget = s;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			showOutput();
			wellFormednessChecker(preconditiontarget);
		}
	}

	class EditCutAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			input.cut();
		}
	}

	class EditCopyAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit"))
				input.copy();
			else if (pp.equals("Output"))
				output.copy();
			else if (pp.equals("Manual"))
				manual.copy();
			else if (pp.equals("Alphabet"))
				alphabet.copy();
			else if (pp.equals("Transitions"))
				prints.copy();
		}
	}

	class EditPasteAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			input.paste();
		}
	}

	class TargetAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String choice = (String) environmentTargetChoice.getSelectedItem();
			if (choice == null)
				return;
			runEnabled = MenuDefinition.enabled(choice);
			if (runItems != null && runEnabled != null) {
				if (runItems.length == runEnabled.length)
					for (int i = 0; i < runItems.length; ++i)
						runItems[i].setEnabled(runEnabled[i]);
			}
		}
	}

	// --------------------------------------------------------------------
	// undo editor stuff

	class UndoHandler implements UndoableEditListener {
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
			updateDoState();
		}
	}

	class UndoAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				undo.undo();
			} catch (CannotUndoException ignored) {
			}
			updateDoState();
		}
	}

	class RedoAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				undo.redo();
			} catch (CannotUndoException ignored) {
			}
			updateDoState();
		}
	}

	// >>> AMES: Text Search
	class EditFindAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			centre(findDialog);
			findDialog.setVisible(true);
		}
	}

	// <<< AMES

	protected void updateDoState() {
		editUndo.setEnabled(undo.canUndo());
		undoTool.setEnabled(undo.canUndo());
		editRedo.setEnabled(undo.canRedo());
		redoTool.setEnabled(undo.canRedo());
	}

	// ------------------------------------------------------------------------
	private int tabindex = 0;

	private void swapto(int i) {
		if (i == tabindex)
			return;
		textIO.setBackgroundAt(i, Color.green);
		if (tabindex != i && tabindex < textIO.getTabCount())
			textIO.setBackgroundAt(tabindex, Color.lightGray);
		tabindex = i;
		setEditControls(tabindex);
		textIO.setSelectedIndex(i);
	}

	class TabChange implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			int i = textIO.getSelectedIndex();
			if (i == tabindex)
				return;
			textIO.setBackgroundAt(i, Color.green);
			textIO.setBackgroundAt(tabindex, Color.lightGray);
			tabindex = i;
			setEditControls(tabindex);
		}
	}

	private void setEditControls(int tabindex) {
		boolean app = (isApplet == null);
		String pp = textIO.getTitleAt(tabindex);
		boolean b = (tabindex == 0);
		editCut.setEnabled(b);
		cutTool.setEnabled(b);
		editPaste.setEnabled(b);
		pasteTool.setEnabled(b);
		fileNew.setEnabled(b);
		file_example.setEnabled(b);
		fileOpen.setEnabled(app && b);
		fileSaveAs.setEnabled(app && b);
		fileExport.setEnabled(app && (b || pp.equals("Transitions")));
		newFileTool.setEnabled(b);
		openFileTool.setEnabled(app && b);
		editUndo.setEnabled(b && undo.canUndo());
		undoTool.setEnabled(b && undo.canUndo());
		editRedo.setEnabled(b && undo.canRedo());
		redoTool.setEnabled(b && undo.canRedo());
		if (b)
			SwingUtilities.invokeLater(new RequestFocus());
	}

	class RequestFocus implements Runnable {
		@Override
		public void run() {
			input.requestFocusInWindow();
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void out(String str) {
		SwingUtilities.invokeLater(new OutputAppend(str));
	}

	@Override
	public void outln(String str) {
		SwingUtilities.invokeLater(new OutputAppend(str + "\n"));
	}

	@Override
	public void clearOutput() {
		SwingUtilities.invokeLater(new OutputClear());
	}

	public void showOutput() {
		SwingUtilities.invokeLater(new OutputShow());
	}

	class OutputAppend implements Runnable {
		String text;

		OutputAppend(String text) {
			this.text = text;
		}

		@Override
		public void run() {
			output.append(text);
		}
	}

	class OutputClear implements Runnable {
		@Override
		public void run() {
			output.setText("");
		}
	}

	class OutputShow implements Runnable {
		@Override
		public void run() {
			swapto(1);
		}
	}

	// ------------------------------------------------------------------------

	// Implementation of the DarwinEnvironment Interface

	public char nextChar() {
		fPos = fPos + 1;
		if (fPos < fSrc.length()) {
			return fSrc.charAt(fPos);
		} else {
			// fPos = fPos - 1;
			return '\u0000';
		}
	}

	@Override
	public char backChar() {
		fPos = fPos - 1;
		if (fPos < 0) {
			fPos = 0;
			return '\u0000';
		} else
			return fSrc.charAt(fPos);
	}

	@Override
	public int getMarker() {
		return fPos;
	}

	@Override
	public void resetMarker() {
		fPos = -1;
	}

	public void resetInput() {
		this.resetMarker();
		fSrc = input.getText();
	}

	// ------------------------------------------------------------------------

	private boolean compile() {
		clearOutput();
		current = docompile();
		if (current == null) {
			return false;
		}
		postState(current);
		return true;
	}

	/* AMES: promoted visibility from private to implement lts.LTSOutput */
	@Override
	public void displayError(LTSException x) {
		try {
			this.logger.debug(x);
			if (x.marker != null) {
				int i = ((Integer) (x.marker)).intValue();
				int line = 1;
				for (int j = 0; j < i; ++j) {
					if (fSrc.charAt(j) == '\n')
						++line;
				}
				outln("ERROR line:" + line + " - " + x.getMessage());
				input.select(i, i + 1);
			} else
				outln("ERROR - " + x.getMessage());
		} catch (Exception e) {
			outln(x.toString());
		}
	}

	private CompositeState docompile() {
		resetInput();
		CompositeState cs = null;
		comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.compile();
			this.logger.debug("PROCESSES: " + comp.getProcesses().keySet());
			if (!parse(comp.getComposites(), comp.getProcesses(), comp.getExplorers())) {
				return null;
			}

			cs = comp.continueCompilation((String) environmentTargetChoice.getSelectedItem(), this);

		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}

	private void doparse(Hashtable<String, CompositionExpression> cs, Hashtable<String, ProcessSpec> ps,
			Hashtable<String, ExplorerDefinition> ex) {
		resetInput();
		comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.parse(cs, ps, ex);

		} catch (LTSException x) {
			displayError(x);
			cs = null;
		}
	}

	private boolean compileIfChange() {
		String tmp = input.getText();
		if (current == null || !tmp.equals(lastCompiled)
				|| !current.getName().equals(environmentTargetChoice.getSelectedItem())) {
			lastCompiled = tmp;
			return compile();
		}
		return true;
	}

	// ------------------------------------------------------------------------

	private void safety(boolean checkDeadlock, boolean multiCe) {
		clearOutput();
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.checkSafety(current, this);

		}

	}


	private void realizability() {

		this.logger.debug("Running the realizability checker");
		clearOutput();
		compileIfChange();
		CompositeState ltlProperty = AssertDefinition.compile(this, asserted);
		this.logger.debug("Property of interest: " + ltlProperty.getName());
		this.logger.debug("Machines of the property of interest: " + ltlProperty.getMachines().size());

		String environmentName = (String) environmentTargetChoice.getSelectedItem();
		String controllerName = (String) controllerTargetChoice.getSelectedItem();
		this.logger.debug("Environment of interest: " + environmentName);
		this.logger.debug("Conntroller of interest: " + controllerName);

		CompositeState environment = compile(environmentName);
		CompositeState controller = compile(controllerName);

		// Silent compilation for negated formula
		CompositeState notLtlProperty = AssertDefinition.compile(new EmptyLTSOuput(),
				AssertDefinition.NOT_DEF + asserted);
		current.compose(new EmptyLTSOuput());

		if (ltlProperty != null) {
			RealizabilityChecker realizabilityChecker = new RealizabilityChecker(this, environment, controller,
					ltlProperty, notLtlProperty);
			realizabilityChecker.check();

			final Vector<LabelledTransitionSystem> machines = new Vector<>();
			environment.getMachines().forEach(machines::add);
			machines.add(controller.getMachines().get(0));

			machines.add(realizabilityChecker.getModifiedControllerStep1());
			if (realizabilityChecker.getModifiedControllerStep2() != null) {
				machines.add(realizabilityChecker.getModifiedControllerStep2());
			}
			CompositeState system = new CompositeState("System");
			environment.getMachines().forEach(system::addMachine);
			system.addMachine(realizabilityChecker.getModifiedControllerStep1());
			//system.compose(new EmptyLTSOuput());

			// adding the property to the set of machine to be showed
			machines.add(ltlProperty.getComposition());
			//realizabilityChecker.getSystem().setName("COMPOSITION_STEP_1");
			//machines.add(realizabilityChecker.getSystem().getComposition());

			if (realizabilityChecker.getModifiedControllerStep2() != null) {

				system = new CompositeState("System");
				environment.getMachines().forEach(system::addMachine);
				system.addMachine(realizabilityChecker.getModifiedControllerStep2());
//				system.compose(new EmptyLTSOuput());
//				LabelledTransitionSystem compositionStep2 = system.getComposition();
				// adding the negation of the property to the set of machine to
				// be showed
				machines.add(notLtlProperty.getComposition());
	//			compositionStep2.setName("COMPOSITION_STEP_2");

		//		machines.add(compositionStep2);

			}

			this.newMachines(machines);
		}
	}

	private void wellFormednessChecker(String preconditionName) {
		Preconditions.checkNotNull(preconditionName, "The name of the precondition cannot be null");
		clearOutput();
		compileIfChange();

		String boxOfInterest = LTSCompiler.mapsEachPreconditionToTheCorrespondingBox.get(preconditionName);

		CompositeState environment = compile((String) environmentTargetChoice.getSelectedItem());
		logger.debug("Environment name: " + environment.getName());

		CompositeState controller = compile((String) controllerTargetChoice.getSelectedItem());

		Log.info("Analysing the controller process: " + controller.getName());

		LabelledTransitionSystem controllerLTS = controller.getMachines().iterator().next();

		final Vector<LabelledTransitionSystem> machines = new Vector<>();
		machines.add(controllerLTS);
		
		controllerLTS.getBoxes().stream().filter(
				box -> LTSCompiler.postconditionDefinitionManager.hasPostCondition(controllerLTS.getName(), box))
				.forEach(box -> {
					String postConditionName = LTSCompiler.postconditionDefinitionManager
							.getPostCondition(controllerLTS.getName(), box);
					LabelledTransitionSystem post = LTSCompiler.postconditionDefinitionManager
							.toLTS(new EmptyLTSOuput(), controllerLTS.getBoxInterface(box), postConditionName);
					post.setName("POST_" + postConditionName);
					machines.add(post);
				});

		Formula preconditionFormula = LTSCompiler.preconditionDefinitionManager.getPrecondition(preconditionName);
		logger.debug("Initial precondition: "+preconditionFormula);
		WellFormednessChecker checker = new WellFormednessChecker(this, environment, controllerLTS, boxOfInterest,
				preconditionFormula, preconditionName);

		CompositeState checkedMachines =checker.check();

		machines.addAll(checkedMachines.getMachines());
		
		CompositeState system = new CompositeState("System");
		
		logger.debug("precondition name: "+preconditionName);
		checkedMachines.getMachines().stream().filter(machine -> !machine.getName().equals(preconditionName)).forEach(machine ->system.addMachine(machine));
		
		
		StringBuilder machinesToString=new StringBuilder();
		machinesToString.append("Machines: ");
		checkedMachines.getMachines().forEach(machine -> machinesToString.append(machine.getName()+"\t"));
		logger.debug(machinesToString.toString());		
		
		StringBuilder machinesInterfaceToString=new StringBuilder();
		machinesInterfaceToString.append("Machines: ");
		checkedMachines.getMachines().forEach(machine -> machinesInterfaceToString.append(machine.getName()+"["+machine.getAlphabetEvents()+"]"+machine.isProperty()+"\n"));
		logger.debug(machinesInterfaceToString.toString());

		
		//system.compose(new EmptyLTSOuput());

	//	machines.add(system.getComposition());

		this.newMachines(machines);
	}

	private void modelCheck() {
		clearOutput();
		compileIfChange();
		CompositeState ltlProperty = AssertDefinition.compile(this, asserted);

		final Vector<LabelledTransitionSystem> machines = new Vector<>();
		CompositeState environment = compile((String) environmentTargetChoice.getSelectedItem());
		logger.debug("Environment name: " + environment.getName());
		CompositeState controller = compile((String) controllerTargetChoice.getSelectedItem());
		logger.debug("Controller name: " + controller.getName());

		final LabelledTransitionSystem controllerLTS = controller.getMachines().get(0);

		machines.add(controller.getMachines().get(0));
		// adds the post-conditions to the GUI
		controllerLTS.getBoxes().stream().filter(
				box -> LTSCompiler.postconditionDefinitionManager.hasPostCondition(controllerLTS.getName(), box))
				.forEach(box -> {
					String postConditionName = LTSCompiler.postconditionDefinitionManager
							.getPostCondition(controllerLTS.getName(), box);
					LabelledTransitionSystem post = LTSCompiler.postconditionDefinitionManager
							.toLTS(new EmptyLTSOuput(), controllerLTS.getBoxInterface(box), postConditionName);
					post.setName("POST_" + postConditionName);
					machines.add(post);
				});

		ModelChecker modelChecker = new ModelChecker(this, environment, controller, ltlProperty);
		CompositeState checkedMachines = modelChecker.check();

		machines.addAll(checkedMachines.getMachines());

		CompositeState system = new CompositeState("System");
		checkedMachines.getMachines().stream().filter(machine -> !machine.getName().equals(ltlProperty.getName())).forEach(machine ->system.addMachine(machine));
		
		StringBuilder machinesToString=new StringBuilder();
		machinesToString.append("Machines: ");
		system.getMachines().forEach(machine -> machinesToString.append(machine.getName()+"\t"));
		logger.debug(machinesToString.toString());		
		
		StringBuilder machinesInterfaceToString=new StringBuilder();
		machinesInterfaceToString.append("Machines: ");
		system.getMachines().forEach(machine -> machinesInterfaceToString.append(machine.getName()+"["+machine.getAlphabetEvents()+"]"+machine.isProperty()+"\n"));
		logger.debug(machinesInterfaceToString.toString());

		
//		system.compose(new EmptyLTSOuput());

	//	machines.add(system.getComposition());

		this.newMachines(machines);

	}

	private void checkSubcomponent(String postconditionName, String process, String box) {
		Preconditions.checkNotNull(postconditionName, "The precondition cannot be null");

		clearOutput();
		logger.debug("Postcondition name: " + postconditionName);
		logger.debug("Controller name: " + process);
		logger.debug("Box name: " + box);

		String subControllerName = (String) controllerTargetChoice.getSelectedItem();

		logger.debug("Sub-controller name: " + subControllerName);
		CompositeState subcomponent = compile(subControllerName);
		CompositeState environment = compile((String) environmentTargetChoice.getSelectedItem());
		logger.debug("Environment name: " + environment.getName());

		String subcomponentLTSName = subcomponent.getMachines().get(0).getName();
		if (!LTSCompiler.subcomponents.containsKey(subcomponent.getMachines().get(0).getName())) {
			Diagnostics.fatal("The subcomponent: " + subcomponentLTSName + " associated with the process: "
					+ subcomponent.getName() + " is not a valid subcomponent");
		}

		// getting the postcondition
		PostconditionDefinitionManager postDefMan = this.comp.getPostconditionDefinitionManager();

		PostconditionDefinition postCondition = postDefMan.getpostConditions().get(postconditionName);

		// getting the precondition
		PreconditionDefinition precondition = LTSCompiler.preconditionDefinitionManager.getPrecondition(process, box);

		this.logger.info("Precondition: " + precondition.getName() + " found for the box: " + box + " of the process: "
				+ process);

		this.logger.info("Postcondition: " + postconditionName + " considered for the box: " + box + " of the process: "
				+ process);

		if (!LTSCompiler.mapBoxSubComponentName.containsKey(box)) {
			this.displayError(new LTSException("No sub-design found for the component: " + box));
		} else {
			String subcomponentProcessName = LTSCompiler.mapBoxSubComponentName.get(box);

			if (!LTSCompiler.processes.containsKey(subcomponentProcessName)) {
				this.displayError(new LTSException("No compiled process with name: " + subcomponentProcessName));
			} else {

				final Vector<LabelledTransitionSystem> machines = new Vector<>();

				LabelledTransitionSystem subComponentLTS = comp.getProcessCompactStateByName(subcomponentProcessName);
				subComponentLTS.setName(subcomponentProcessName);
				machines.addAll(environment.getMachines());
				machines.add(subComponentLTS);

				SubstitutabilityChecker ck = new SubstitutabilityChecker(this, environment, subComponentLTS,
						precondition.getFormula(true), precondition.getName(), postCondition.getFormula(false),
						postCondition.getName());
				ck.check();

				machines.add(ck.getPreconditionLTS());
				machines.add(ck.getPreconditionPlusSubcomponentLTS());
				machines.add(ck.getPostConditionLTS());
				machines.add(ck.getEnvironmentParallelPrePlusSubcomponent());
				this.newMachines(machines);
			}

		}
	}

	private void precondition() {
		clearOutput();
		compileIfChange();
	}

	// ------------------------------------------------------------------------

	private void minimiseComposition() {
		clearOutput();
		compileIfChange();
		if (compileIfChange() && current != null) {
			if (current.getComposition() == null)
				TransitionSystemDispatcher.applyComposition(current, this);
			TransitionSystemDispatcher.minimise(current, this);
			postState(current);
		}
	}

	// ------------------------------------------------------------------------

	private void doComposition() {
		clearOutput();
		compileIfChange();
		if (current != null) {
			TransitionSystemDispatcher.applyComposition(current, this);
			postState(current);
			logger.info("%GRAPH DENSITY: " + current.getComposition().getGraphDensity());
			logger.info("%COMPOSITION STATES: " + current.getComposition().getStates().length);
			logger.info("%COMPOSITION TRANSITIONS: " + current.getComposition().getTransitionNumber());
			logger.info("%COMPOSITION SIZE: " + current.getComposition().size());
			logger.info("%COMPOSITION Alphabet:" + current.getComposition().getAlphabet().length);
		}
	}

	// ------------------------------------------------------------------------
	private boolean checkReplay(Animator a) {
		if (a.hasErrorTrace()) {
			int result = JOptionPane.showConfirmDialog(this, "Do you want to replay the error trace?");
			if (result == JOptionPane.YES_OPTION) {
				return true;
			} else if (result == JOptionPane.NO_OPTION)
				return false;
			else if (result == JOptionPane.CANCEL_OPTION)
				return false;
		}
		return false;
	}

	private void animate() {
		clearOutput();
		compileIfChange();
		boolean replay = false;
		if (current != null) {

			if (current.getMachines().size() > 1 && MTSUtils.isMTSRepresentation(current)) {
				throw new UnsupportedOperationException("Animation for more than one MTS is not developed yet");
			}

			Animator anim = TransitionSystemDispatcher.generateAnimator(current, this, eman);

			RunMenu r = null;
			if (!(anim instanceof MTSAnimator)) {
				replay = checkReplay(anim);
				if (animator != null) {
					animator.dispose();
					animator = null;
				}
				if (RunMenu.menus != null)
					r = (RunMenu) RunMenu.menus.get(run_menu);
			}
			if (r != null && r.isCustom()) {
				animator = createCustom(anim, r.params, r.actions, r.controls, replay);
			} else {

				animator = new AnimArrangedWindow(anim, r, setAutoRun.getState(), replay);
			}
		}
		if (animator != null) {
			animator.pack();
			left(animator);
			animator.setVisible(true);
		}
	}

	private Frame createCustom(Animator anim, String params, Relation actions, Relation controls, boolean replay) {
		CustomAnimator window = null;
		try {
			window = new SceneAnimator();
			File f;
			if (params != null)
				f = new File(currentDirectory, params);
			else
				f = null;
			window.init(anim, f, actions, controls, replay);
			return window;
		} catch (Exception e) {
			outln("** Failed to create instance of Scene Animator" + e);
			return null;
		}
	}

	// ------------------------------------------------------------------------

	// ------------------------------------------------------------------------

	private void newDrawWindow(boolean disp) {
		if (disp && textIO.indexOfTab("Draw") < 0) {
			// create Text window
			draws = new LTSDrawWindow(current, eman);
			textIO.addTab("Draw", draws);
			swapto(textIO.indexOfTab("Draw"));
		} else if (!disp && textIO.indexOfTab("Draw") > 0) {
			swapto(0);
			textIO.removeTabAt(textIO.indexOfTab("Draw"));
			draws.removeClient();
			draws = null;
		}
	}

	// ------------------------------------------------------------------------

	private void newLayoutWindow(boolean disp) {
		if (disp && textIO.indexOfTab("Layout") < 0) {
			// create Text window
			layouts = new LTSLayoutWindow(current, eman);
			textIO.addTab("Layout", layouts);
			swapto(textIO.indexOfTab("Layout"));
		} else if (!disp && textIO.indexOfTab("Layout") > 0) {
			swapto(0);
			textIO.removeTabAt(textIO.indexOfTab("Layout"));
			layouts.removeClient();
			layouts = null;
		}
	}

	private void newPrintWindow(boolean disp) {
		if (disp && textIO.indexOfTab("Transitions") < 0) {
			// create Text window
			prints = new PrintWindow(current, eman);
			textIO.addTab("Transitions", prints);
			swapto(textIO.indexOfTab("Transitions"));
		} else if (!disp && textIO.indexOfTab("Transitions") > 0) {
			swapto(0);
			textIO.removeTabAt(textIO.indexOfTab("Transitions"));
			prints.removeClient();
			prints = null;
		}
	}

	// ------------------------------------------------------------------------

	private void newAlphabetWindow(boolean disp) {
		if (disp && textIO.indexOfTab("Alphabet") < 0) {
			// create Alphabet window
			alphabet = new AlphabetWindow(current, eman);
			textIO.addTab("Alphabet", alphabet);
			swapto(textIO.indexOfTab("Alphabet"));
		} else if (!disp && textIO.indexOfTab("Alphabet") > 0) {
			swapto(0);
			textIO.removeTabAt(textIO.indexOfTab("Alphabet"));
			alphabet.removeClient();
			alphabet = null;
		}
	}

	// ------------------------------------------------------------------------

	private void aboutDialog() {
		LTSASplash d = new LTSASplash(this);
		d.setVisible(true);
	}

	// ------------------------------------------------------------------------

	private void blankit() {
		LTSABlanker d = new LTSABlanker(this);
		d.setVisible(true);
	}

	// -------------------------------------------------------------------------

	private void doApplyPlusCROperator() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.applyPlusCROperator(current, this);
			postState(current);
		}
	}

	private void doApplyPlusCAOperator() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.applyPlusCAOperator(current, this);
			postState(current);
		}
	}

	private void doDeterminise() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.determinise(current, this);
			postState(current);
		}
	}

	private void doRefinement() {
		if (compileIfChange() && current != null) {
			RefinementOptions refinementOptions = new RefinementOptions();
			String[] models = getMachinesNames();

			final RefinementWindow refinementWindow = new RefinementWindow(this, refinementOptions, models,
					SemanticType.values(), RefinementWindow.Mode.REFINEMENT);
			refinementWindow.setLocation(this.getX() + 100, this.getY() + 100);
			refinementWindow.pack();
			refinementWindow.setVisible(true);
			if (refinementOptions.isValid()) {
				boolean areRefinement = TransitionSystemDispatcher.isRefinement(
						selectMachine(refinementOptions.getRefinesModel()),
						selectMachine(refinementOptions.getRefinedModel()), refinementOptions.getRefinementSemantic(),
						this);
				if (refinementOptions.isBidirectionalCheck()) {
					areRefinement &= TransitionSystemDispatcher.isRefinement(
							selectMachine(refinementOptions.getRefinedModel()),
							selectMachine(refinementOptions.getRefinesModel()),
							refinementOptions.getRefinementSemantic(), this);
					if (areRefinement) {
						this.outln(" ");
						this.outln("Models equivalent by simulation.");
					}
				}
			}

			postState(current);
		}
	}

	private void doConsistency() {
		if (compileIfChange() && current != null) {
			RefinementOptions refinementOptions = new RefinementOptions();
			String[] models = getMachinesNames();

			final RefinementWindow refinementWindow = new RefinementWindow(this, refinementOptions, models,
					SemanticType.values(), RefinementWindow.Mode.CONSISTENCY);
			refinementWindow.setLocation(this.getX() + 100, this.getY() + 100);
			refinementWindow.pack();
			refinementWindow.setVisible(true);
			if (refinementOptions.isValid()) {
				this.outln("Checking Consistency");
				TransitionSystemDispatcher.areConsistent(selectMachine(refinementOptions.getRefinesModel()),
						selectMachine(refinementOptions.getRefinedModel()), refinementOptions.getRefinementSemantic(),
						this);
			}

			postState(current);
		}
	}

	private void doRunEnactors() {

		if (current == null || current.getComposition() == null) {
			this.outln("There is no composite state or goal available");
			return;
		}

	}

	private String[] getMachinesNames() {
		LabelledTransitionSystem composition = current.getComposition();
		int size = current.getMachines().size();
		if (composition != null) {
			size++;
		}
		String[] result = new String[size];

		int i = 0;
		for (Iterator<LabelledTransitionSystem> it = current.getMachines().iterator(); it.hasNext(); i++) {
			LabelledTransitionSystem compactState = (LabelledTransitionSystem) it.next();
			result[i] = compactState.getName();
		}
		if (composition != null) {
			result[i] = composition.getName();
		}
		return result;
	}

	private LabelledTransitionSystem selectMachine(int machineIndex) {
		Vector<LabelledTransitionSystem> machines = current.getMachines();
		if (machineIndex < machines.size()) {
			return (LabelledTransitionSystem) machines.get(machineIndex);
		} else {
			return current.getComposition();
		}
	}

	// ------------------------------------------------------------------------

	private boolean parse() {
		return parse(null, null, null);
	}

	private boolean parse(Hashtable<String, CompositionExpression> cs, Hashtable<String, ProcessSpec> ps,
			Hashtable<String, ExplorerDefinition> ex) {

		String oldChoice = (String) environmentTargetChoice.getSelectedItem();
		String oldChoiceController = (String) controllerTargetChoice.getSelectedItem();

		if (cs == null && ps == null) {
			cs = new Hashtable<>();
			ps = new Hashtable<>();
			ex = new Hashtable<>();
			doparse(cs, ps, ex);
		}

		if (cs == null) {
			return false;
		}
		environmentTargetChoice.removeAllItems();
		controllerTargetChoice.removeAllItems();

		if (ex.size() == 0) {
			if (cs.size() == 0) {
				controllerTargetChoice.addItem(DEFAULT);
				environmentTargetChoice.addItem(DEFAULT);
			} else {
				Enumeration<String> e = cs.keys();
				List<String> forSort = new ArrayList<>();
				while (e.hasMoreElements()) {
					forSort.add(e.nextElement());
				}
				Collections.sort(forSort);
				for (String aForSort : forSort) {
					controllerTargetChoice.addItem(aForSort);
					environmentTargetChoice.addItem(aForSort);
				}
			}
		} else {
			Enumeration<String> e = ex.keys();
			List<String> forSort = new ArrayList<>();
			while (e.hasMoreElements()) {
				forSort.add(e.nextElement());
			}
			Collections.sort(forSort);
			for (String aForSort : forSort) {
				environmentTargetChoice.addItem(aForSort);
				controllerTargetChoice.addItem(aForSort);
			}
		}

		if (oldChoice != null) {
			if (!oldChoice.equals(DEFAULT)
					&& (ex.containsKey(oldChoice) || (ex.size() == 0 && cs.containsKey(oldChoice))))
				environmentTargetChoice.setSelectedItem(oldChoice);
		}
		if (oldChoiceController != null) {
			if (!oldChoiceController.equals(DEFAULT) && (ex.containsKey(oldChoiceController)
					|| (ex.size() == 0 && cs.containsKey(oldChoiceController)))) {
				controllerTargetChoice.setSelectedItem(oldChoiceController);
			}
		}
		current = null;
		explorer = null;
		explorerDefinitions = ex;

		eman.post(new LTSEvent(LTSEvent.NEWCOMPOSITES, cs.keySet()));
		eman.post(new LTSEvent(LTSEvent.NEWPROCESSES, ps.keySet()));
		eman.post(new LTSEvent(LTSEvent.NEWLABELSETS, (labelSetConstants = LabelSet.getConstants()).keySet()));

		// deals with checkRealizability menu
		this.checkRealizability.removeAll();
		assertNames = AssertDefinition.names();
		if (assertNames != null) {
			realizabilityItems = new JMenuItem[assertNames.length];
			for (int i = 0; i < assertNames.length; ++i) {
				realizabilityItems[i] = new JMenuItem(assertNames[i]);
				realizabilityItems[i].addActionListener(new RealizabilityAction(assertNames[i]));
				checkRealizability.add(realizabilityItems[i]);
			}
		}

		// deal with assert menu
		checkLiveness.removeAll();
		assertNames = AssertDefinition.names();
		if (assertNames != null) {
			assertItems = new JMenuItem[assertNames.length];
			for (int i = 0; i < assertNames.length; ++i) {
				assertItems[i] = new JMenuItem(assertNames[i]);
				assertItems[i].addActionListener(new ModelCheckerAction(assertNames[i]));
				checkLiveness.add(assertItems[i]);
			}
		}

		// deal with preconditions
		wellFormednessChecker.removeAll();
		Set<String> preconditionsNames = comp.getPreconditionDefinitionManager().names();
		SortedSet<String> orderedPreconditions = new TreeSet<>(preconditionsNames);

		if (preconditionsNames != null) {
			preconditionsItems = new JMenuItem[preconditionsNames.size()];
			int i = 0;
			for (String name : orderedPreconditions) {
				preconditionsItems[i] = new JMenuItem(name);
				preconditionsItems[i].addActionListener(new WellFormednessCheckerAction(name));
				wellFormednessChecker.add(preconditionsItems[i]);
				i++;
			}
		}

		// deal with postconditions
		checkPostconditions.removeAll();

		Map<String, Map<String, String>> map = comp.getPostconditionDefinitionManager().getMapBoxPostcondition();

		int postConditionNumber = comp.getPostconditionDefinitionManager().getNumberOfPostConditions();

		int i = 0;

		if (map != null) {
			postconditionsItems = new JMenuItem[postConditionNumber];

			for (String process : map.keySet()) {
				for (Entry<String, String> boxPre : map.get(process).entrySet()) {
					postconditionsItems[i] = new JMenuItem(boxPre.getValue());

					postconditionsItems[i].addActionListener(
							new SubcomponentCheckerAction(boxPre.getKey(), process, boxPre.getValue()));

					checkPostconditions.add(postconditionsItems[i]);
					i++;
				}
			}
		}

		return true;
	}

	// ------------------------------------------------------------------------

	private void displayManual(boolean dispman) {
		if (dispman && textIO.indexOfTab("Manual") < 0) {
			// create Manual window
			manual = new JEditorPane();
			manual.setEditable(false);
			manual.addHyperlinkListener(new Hyperactive());
			JScrollPane mm = new JScrollPane(manual, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			textIO.addTab("Manual", mm);
			swapto(textIO.indexOfTab("Manual"));
			URL man = this.getClass().getResource("doc/User-manual.html");
			try {
				manual.setPage(man);
				// outln("URL: "+man);
			} catch (java.io.IOException e) {
				outln("" + e);
			}
		} else if (!dispman && textIO.indexOfTab("Manual") > 0) {
			swapto(0);
			textIO.removeTabAt(textIO.indexOfTab("Manual"));
			manual = null;
		}
	}

	class Hyperactive implements HyperlinkListener {

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				JEditorPane pane = (JEditorPane) e.getSource();
				try {
					URL u = e.getURL();
					pane.setPage(u);
				} catch (Throwable t) {
					outln("" + e);
				}
			}
		}
	}

	// ------------------------------------------------------------------------

	public static void main(String[] args) {
		try {
			String lf = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lf);
		} catch (Exception ignored) {
		}
		HPWindow window = new HPWindow(null);
		window.setTitle("FIDDLE");
		window.pack();
		HPWindow.centre(window);
		window.setVisible(true);
		if (args.length > 0) {
			SwingUtilities.invokeLater(new ScheduleOpenFile(window, args[0]));
		} else {
			window.currentDirectory = System.getProperty("user.dir");
		}
	}

	static class ScheduleOpenFile implements Runnable {
		HPWindow window;
		String arg;

		ScheduleOpenFile(HPWindow window, String arg) {
			this.window = window;
			this.arg = arg;
		}

		@Override
		public void run() {
			window.doOpenFile("", arg, false);
		}
	}

	private boolean hasLTL2BuchiJar() {
		try {
			new gov.nasa.ltl.graph.Graph();
			return true;
		} catch (NoClassDefFoundError e) {
			return false;
		}
	}

	@Override
	public CompositeState compile(String name) {
		resetInput();
		CompositeState cs = null;
		comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.compile();
			cs = comp.continueCompilation(name, this);
		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}

	/**
	 * Returns the currently selected item from the targets selection box.
	 */
	@Override
	public String getTargetChoice() {
		return (String) environmentTargetChoice.getSelectedItem();
	}

	/**
	 * Updates the various display windows and animators with the given
	 * machines.
	 */
	@Override
	public void newMachines(java.util.List<LabelledTransitionSystem> machines) {
		CompositeState c = new CompositeState(new Vector<>(machines));
		postState(c);
		this.current = c;
	}

	/**
	 * Returns the set of actions which correspond to the label set definition
	 * with the given name.
	 */
	public Set<String> getLabelSet(String name) {
		if (labelSetConstants == null)
			return null;

		Set<String> s = new HashSet<>();
		LabelSet ls = labelSetConstants.get(name);

		if (ls == null)
			return null;

		for (String a : ls.getActions(null))
			s.add(a);

		return s;
	}

	/**
	 * Executes the given action in the main execution thread, disabling other
	 * menu actions and enabling the stop functionality while running. If
	 * showOutputPane is set, then the output console is immediately made
	 * visible.
	 * 
	 * @param r
	 *            The runnable action
	 * @param showOutputPane
	 *            Whether the output console is made visible
	 */
	@Override
	public void performAction(final Runnable r, final boolean showOutputPane) {
		// XXX: There is a race here, as there is in the method do_action.
		menuEnable(false);
		// check_stop.setEnabled(true);
		// stopTool.setEnabled(true);
		executer = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (showOutputPane)
						showOutput();
					r.run();

				} catch (Throwable e) {
					showOutput();
					outln("**** Runtime Exception: " + e);
					e.printStackTrace();
					current = null;
					explorer = null;
					explorerDefinitions = null;

				} finally {
					menuEnable(true);
					// check_stop.setEnabled(false);
					// stopTool.setEnabled(false);
				}
			}
		});
		executer.setPriority(Thread.NORM_PRIORITY - 1);
		executer.start();
	}

	/**
	 * Returns the instantiated Spring Application Context
	 * 
	 * @return the Application Context
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
