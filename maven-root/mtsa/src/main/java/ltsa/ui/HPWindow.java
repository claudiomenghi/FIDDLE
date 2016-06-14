package ltsa.ui;

// This is an experimental version with progress & LTL property check

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
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

import ltsa.lts.Analyser;
import ltsa.lts.Animator;
import ltsa.lts.CompactState;
import ltsa.lts.CompositeState;
import ltsa.lts.CompositionExpression;
import ltsa.lts.Diagnostics;
import ltsa.lts.EmptyLTSOuput;
import ltsa.lts.EventManager;
import ltsa.lts.LTSCanvas;
import ltsa.lts.LTSCompiler;
import ltsa.lts.LTSError;
import ltsa.lts.LTSEvent;
import ltsa.lts.LTSException;
import ltsa.lts.LTSInput;
import ltsa.lts.LTSManager;
import ltsa.lts.LTSOutput;
import ltsa.lts.LabelSet;
import ltsa.lts.MaxStatesDialog;
import ltsa.lts.MenuDefinition;
import ltsa.lts.Options;
import ltsa.lts.ProgressCheck;
import ltsa.lts.RandomSeedDialog;
import ltsa.lts.Relation;
import ltsa.lts.RunMenu;
import ltsa.lts.SuperTrace;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaFactory;
import ltsa.lts.util.LTSUtils;
import ltsa.lts.util.MTSUtils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import ltsa.ui.enactment.EnactorOptionsWindows;
import ltsa.ui.update.UpdateGraphSimulation;
import ltsa.updatingControllers.structures.UpdatingControllerCompositeState;
import MTSTools.ac.ic.doc.mtstools.model.SemanticType;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import ltsa.custom.CustomAnimator;
import ltsa.custom.SceneAnimator;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.editor.ColoredEditorKit;
import ltsa.enactment.EnactmentOptions;
import ltsa.enactment.EnactorFactory;
import ltsa.enactment.MTSAEnactmentSimulation;
import ltsa.enactment.SchedulerFactory;
import ltsa.exploration.Explorer;
import ltsa.exploration.ExplorerDefinition;
import ltsa.exploration.knowledge.Knowledge;
import ltsa.exploration.model.Model;
import ltsa.exploration.strategy.*;
import ltsa.exploration.view.View;
import ltsa.exploration.view.ViewNextConfiguration;
import ltsa.exploration.view.ViewNextConfigurationRandom;
import ltsa.exploration.view.ViewNextConfigurationTrace;
import ltsa.jung.LTSJUNGCanvas;
import ltsa.jung.LTSJUNGCanvas.EnumLayout;
import ltsa.lts.*;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaFactory;
import ltsa.lts.util.MTSUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import ltsa.ui.enactment.EnactorOptionsWindows;
import ltsa.ui.LTSLayoutWindow;
import org.springframework.core.io.DefaultResourceLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class HPWindow extends JFrame implements LTSManager, LTSInput,
		LTSOutput, LTSError, Runnable {
	private static final String VERSION = " j1.2 v14-10-99, amimation support";
	private static final String DEFAULT = "DEFAULT";

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

	MTSAEnactmentSimulation<Long, String> enactmentSimulation = new MTSAEnactmentSimulation<Long, String>();
	UpdateGraphSimulation updateGraphSimulation;

	// >>> AMES: Text Search
	FindDialog findDialog;
	// <<< AMES

	JComboBox targetChoice;
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
	JMenu help;
	JMenu option;
	JMenu mts;
	JMenu menu_enactment;
	JMenu menu_enactment_enactors;
	JMenuItem file_new;
	JMenuItem file_open;
	JMenuItem file_save;
	JMenuItem file_saveAs;
	JMenuItem file_export;
	JMenuItem file_exit;
	JMenuItem edit_cut;
	JMenuItem edit_copy;
	JMenuItem edit_paste;
	JMenuItem edit_undo;
	JMenuItem edit_redo;
	JMenuItem check_safe;
	JMenuItem check_progress;
	JMenuItem check_reachable; // check_stop,
	JMenuItem check_deterministic;
	JMenuItem build_parse;
	JMenuItem build_compile;
	JMenuItem build_compose;
	JMenuItem build_minimise;
	JMenuItem help_about;
	JMenuItem supertrace_options;
	JMenuItem mtsRefinement;
	JMenuItem mtsConsistency;
	JMenuItem checkDeadlock;
	JMenuItem controllerSynthesis;
	JMenuItem menu_enactment_run;
	JMenuItem menu_enactment_options;
	JMenuItem layout_options;

	// >>> AMES: Deadlock Insensitive Analysis
	JMenuItem check_safe_no_deadlock;
	// <<< AMES

	// >>> AMES: multiple ce
	JMenuItem check_safe_multi_ce;
	// <<< AMES

	// >>> AMES: Text Search
	JMenuItem edit_find;
	// <<< AMES

	JMenu check_run, file_example, check_liveness, compositionStrategy;
	JMenuItem default_run;
	JMenuItem[] run_items, assert_items;
	String[] run_names, assert_names;
	boolean[] run_enabled;
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
	JCheckBoxMenuItem help_manual;
	JCheckBoxMenuItem window_alpha;
	JCheckBoxMenuItem window_print;
	JCheckBoxMenuItem window_draw;
	JCheckBoxMenuItem window_layout;
	JRadioButtonMenuItem strategyDFS, strategyBFS, strategyRandom;
	ButtonGroup strategyGroup;
	JMenuItem maxStateGeneration;
	JMenuItem randomSeed;

	// tool bar buttons - that need to be enabled and disabled
	JButton // stopTool,
			parseTool,
			safetyTool, progressTool, cutTool, pasteTool,
			newFileTool,
			openFileTool, saveFileTool, compileTool, composeTool,
			minimizeTool,
			undoTool, redoTool;
	// used to implement muCSPInput
	int fPos = -1;
	String fSrc = "\n";
	public static final Font FIXED = new Font("Monospaced", Font.PLAIN, 12);
	public static final Font BIG = new Font("Monospaced", Font.BOLD, 20);
	// Font title = new Font("SansSerif",Font.PLAIN,12);

	private AppletButton isApplet = null;

	private ApplicationContext applicationContext = null;
	
	private EnactmentOptions<Long, String> enactmentOptions = new EnactmentOptions<Long, String>();

	public HPWindow(AppletButton isap) {

		//Try to load Spring ltsa-context.xml file
		try {
			applicationContext = new ClassPathXmlApplicationContext("src/main/java/ltsa/ltsa-context.xml");
		} catch (Exception e)
		{
			applicationContext = new FileSystemXmlApplicationContext("src/main/java/ltsa/ltsa-context.xml");
		}
		
		isApplet = isap;
		//SymbolTable.init();
		getContentPane().setLayout(new BorderLayout());

		textIO = new JTabbedPane();

		// edit window for specification source
		// input = new JTextArea("",24,80);
		input = new JEditorPane();

        input.addKeyListener(new KeyListener()
        {

            @Override
            public void keyTyped(KeyEvent e)
            {

            }

            @Override
            public void keyPressed(KeyEvent e)
            {
                if ((e.getKeyCode() == KeyEvent.VK_Z) && e.isControlDown())
                {
                    try
                    {
                        undo.undo();
                    }
                    catch (CannotUndoException ignored)
                    {

                    }
                    updateDoState();
                }

                if ((e.getKeyCode() == KeyEvent.VK_Y) && e.isControlDown())
                {
                    try
                    {
                        undo.redo();
                    }
                    catch (CannotUndoException ignored)
                    {

                    }
                    updateDoState();
                }
            }

            @Override
            public void keyReleased(KeyEvent e)
            {

            }
        });


		input.setEditorKit(new ColoredEditorKit());

		input.setFont(FIXED);
		//Dipi, only for viva
		//input.setFont(BIG);
		input.setBackground(Color.white);
		input.getDocument().addUndoableEditListener(undoHandler);
		undo.setLimit(10); // set maximum undo edits
		// input.setLineWrap(true);
		// input.setWrapStyleWord(true);
		input.setBorder(new EmptyBorder(0, 5, 0, 0));
		JScrollPane inp = new JScrollPane(input,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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
		JScrollPane outp = new JScrollPane(output,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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
		file_new = new JMenuItem("New");
		file_new.addActionListener(new NewFileAction());
		file.add(file_new);
		file_open = new JMenuItem("Open...");
		file_open.addActionListener(new OpenFileAction());
		file.add(file_open);
		file_save = new JMenuItem("Save");
		file_save.addActionListener(new SaveFileAction());
		file.add(file_save);
		file_saveAs = new JMenuItem("Save as...");
		file_saveAs.addActionListener(new SaveAsFileAction());
		file.add(file_saveAs);
		file_export = new JMenuItem("Export...");
		file_export.addActionListener(new ExportFileAction());
		file.add(file_export);
		file_example = new JMenu("Examples");
		new Examples(file_example, this).getExamples();
		file.add(file_example);
		file_exit = new JMenuItem("Quit");
		file_exit.addActionListener(new ExitFileAction());
		file.add(file_exit);
		// edit menu
		edit = new JMenu("Edit");
		mb.add(edit);
		edit_cut = new JMenuItem("Cut");
		edit_cut.addActionListener(new EditCutAction());
		edit.add(edit_cut);
		edit_copy = new JMenuItem("Copy");
		edit_copy.addActionListener(new EditCopyAction());
		edit.add(edit_copy);
		edit_paste = new JMenuItem("Paste");
		edit_paste.addActionListener(new EditPasteAction());
		edit.add(edit_paste);
		edit.addSeparator();
		edit_undo = new JMenuItem("Undo");
		edit_undo.addActionListener(new UndoAction());
		edit.add(edit_undo);
		edit_redo = new JMenuItem("Redo");
		edit_redo.addActionListener(new RedoAction());
		edit.add(edit_redo);

		// >>> AMES: Text Search
		edit_find = new JMenuItem("Find");
		edit_find.addActionListener(new EditFindAction());
		edit.add(edit_find);
		// <<< AMES

		// check menu
		check = new JMenu("Check");
		mb.add(check);
		check_safe = new JMenuItem("Safety");
		check_safe.addActionListener(new DoAction(DO_safety));
		check.add(check_safe);

		checkDeadlock = new JMenuItem("Deadlock");
		checkDeadlock.addActionListener(new DoAction(DO_DEADLOCK));
		check.add(checkDeadlock);

		check_progress = new JMenuItem("Progress");
		check_progress.addActionListener(new DoAction(DO_progress));
		check.add(check_progress);
		check_liveness = new JMenu("LTL property");
		if (hasLTL2BuchiJar())
			check.add(check_liveness);
		check_run = new JMenu("Run");
		check.add(check_run);
		default_run = new JMenuItem(DEFAULT);
		default_run.addActionListener(new ExecuteAction(DEFAULT));
		check_run.add(default_run);
		check_reachable = new JMenuItem("Supertrace");
		check_reachable.addActionListener(new DoAction(DO_reachable));
		check.add(check_reachable);

		check_deterministic = new JMenuItem("is Deterministic");
		check_deterministic.addActionListener(new DoAction(DO_deterministic));
		check.add(check_deterministic);

		// check_stop = new JMenuItem("Stop");
		// check_stop.addActionListener(new StopAction());
		// check_stop.setEnabled(false);
		// check.add(check_stop);
		// build menu
		build = new JMenu("Build");
		mb.add(build);
		build_parse = new JMenuItem("Parse");
		build_parse.addActionListener(new DoAction(DO_parse));
		build.add(build_parse);
		build_compile = new JMenuItem("Compile");
		build_compile.addActionListener(new DoAction(DO_compile));
		build.add(build_compile);
		build_compose = new JMenuItem("Compose");
		build_compose.addActionListener(new DoAction(DO_doComposition));
		build.add(build_compose);
		build_minimise = new JMenuItem("Minimise");
		build_minimise.addActionListener(new DoAction(DO_minimiseComposition));
		build.add(build_minimise);

		// MTS Menu
		mts = new JMenu("MTS");
		mb.add(mts);

		mtsRefinement = new JMenuItem("Refinement");
		mtsRefinement.addActionListener(new DoAction(DO_REFINEMENT));
		mts.add(mtsRefinement);

		mtsConsistency = new JMenuItem("Consistency");
		mtsConsistency.addActionListener(new DoAction(DO_CONSISTENCY));
		mts.add(mtsConsistency);

		// window menu
		window = new JMenu("Window");
		mb.add(window);
		window_alpha = new JCheckBoxMenuItem("Alphabet");
		window_alpha.setSelected(false);
		window_alpha.addActionListener(new WinAlphabetAction());
		window.add(window_alpha);
		window_print = new JCheckBoxMenuItem("Transitions");
		window_print.setSelected(false);
		window_print.addActionListener(new WinPrintAction());
		window.add(window_print);
		window_draw = new JCheckBoxMenuItem("Draw");
		window_draw.setSelected(true);
		window_draw.addActionListener(new WinDrawAction());
		window.add(window_draw);
		//layout
		window_layout = new JCheckBoxMenuItem("Layout");
		window_layout.setSelected(true);
		window_layout.addActionListener(new WinLayoutAction());
		window.add(window_layout);
		// help menu
		help = new JMenu("Help");
		mb.add(help);
		help_about = new JMenuItem("About");
		help_about.addActionListener(new HelpAboutAction());
		help.add(help_about);
		help_manual = new JCheckBoxMenuItem("Manual");
		help_manual.setSelected(false);
		help_manual.addActionListener(new HelpManualAction());
		help.add(help_manual);
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
		supertrace_options = new JMenuItem("Set Supertrace parameters");
		supertrace_options.addActionListener(new SuperTraceOptionListener());
		option.add(supertrace_options);
		option.addSeparator();
		setBigFont = new JCheckBoxMenuItem("Use big font");
		setBigFont.addActionListener(opt);
		option.add(setBigFont);
		setBigFont.setSelected(false);
		setDisplayName = new JCheckBoxMenuItem("Display name when drawing LTS");
		setDisplayName.addActionListener(opt);
		option.add(setDisplayName);
		setDisplayName.setSelected(true);
		setNewLabelFormat = new JCheckBoxMenuItem(
				"Use V2.0 label format when drawing LTS");
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

		menu_enactment = new JMenu("Enactment");
		mb.add(menu_enactment);
		// menu_enactment_enactors = new JMenu("Enactors");
		// menu_enactment.add(menu_enactment_enactors);
		// Loads robot enactors
		// fillEnactorsMenu(menu_enactment_enactors);

		menu_enactment_options = new JMenuItem("Options");
		menu_enactment_options.addActionListener(new DoAction(DO_ENACTORSOPTIONS));
		menu_enactment.add(menu_enactment_options);
		menu_enactment_run = new JMenuItem("Run model");
		menu_enactment_run.addActionListener(new DoAction(DO_RUNENACTORS));
		menu_enactment.add(menu_enactment_run);
		JMenu graphUpdateMenu = new GraphUpdateMenu();
		mb.add(graphUpdateMenu);
		SchedulerFactory<Long, String> schedulerFactory = null;
		if (applicationContext != null)
		  schedulerFactory = this.applicationContext.getBean(SchedulerFactory.class);
		if (schedulerFactory != null) {
			List<String> schedulerNames = schedulerFactory.getSchedulersList();
			// Load first scheduler by default
			if (schedulerNames.size() > 0)
				this.enactmentOptions.scheduler = schedulerNames.get(0);
		}

		// >>> AMES: SET Compositional Learning, Interface Learning
		// Set<JMenu> menus = new HashSet<JMenu>();
		// menus.add(file); menus.add(edit); menus.add(check);
		// menus.add(build);menus.add(mts);
		// menus.add(window); menus.add(help); menus.add(option);
		//
		// for ( String uiName : Arrays.asList(new String[]{
		// "ames-learning-ui","ames-interface-learning-ui"})) {
		// try {
		// // Initialise so that the absense of the lstar package doesn't stop
		// // compilation or execution.
		// Object compositionUI =
		// Class.forName(System.getProperty(uiName))
		// .getConstructor(
		// LTSInput.class, LTSOutput.class, LTSError.class,
		// LTSManager.class, EventManager.class)
		// .newInstance(this, this, this, this, eman);
		//
		// Map<Component,String> menuItems =
		// (Map) compositionUI.getClass()
		// .getMethod("getMenuItems")
		// .invoke(compositionUI);
		//
		// Set<? extends Component> windows =
		// (Set) compositionUI.getClass()
		// .getMethod("getWindows")
		// .invoke(compositionUI);
		//
		// // Add the necessary menu items.
		// for ( Component c : menuItems.keySet() ) {
		// String menuName = menuItems.get(c);
		// boolean added = false;
		// for ( JMenu menu : menus ) {
		// if (menu.getText().equalsIgnoreCase(menuName)) {
		// menu.add(c);
		// added = true;
		// }
		// }
		// if (!added) {
		// JMenu menu = new JMenu(menuName);
		// menus.add(menu);
		// menu.add(c);
		// }
		// }
		//
		// // Add the necessary tabbed windows.
		// for ( final Component w : windows ) {
		// final String name = w.getName();
		// final JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		//
		// item.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// if (item.isSelected()
		// && textIO.indexOfTab(name) < 0) {
		// textIO.addTab(name,w);
		// swapto(textIO.indexOfTab(name));
		//
		// } else if ( !item.isSelected()
		// && textIO.indexOfTab(name) > 0) {
		// swapto(0);
		// textIO.removeTabAt( textIO.indexOfTab(name) );
		// }
		// }
		// });
		// window.add(item);
		// }
		// } catch (Exception e) {
		// System.out.println("Caught a " + e + " while trying to "
		// + "load/initialise the external module '" + uiName
		// +"'; skipping.");
		// }
		// }
		// <<< AMES

		// toolbar
		tools = new JToolBar();
		tools.setFloatable(false);
		tools.add(newFileTool = createTool("src/main/java/ltsa/ui/icon/new.gif", "New file",
				new NewFileAction()));
		tools.add(openFileTool = createTool("src/main/java/ltsa/ui/icon/open.gif", "Open file",
				new OpenFileAction()));
		tools.add(saveFileTool = createTool("src/main/java/ltsa/ui/icon/save.gif", "Save File",
				new SaveFileAction()));
		tools.addSeparator();
		tools.add(cutTool = createTool("src/main/java/ltsa/ui/icon/cut.gif", "Cut",
				new EditCutAction()));
		tools.add(createTool("src/main/java/ltsa/ui/icon/copy.gif", "Copy", new EditCopyAction()));
		tools.add(pasteTool = createTool("src/main/java/ltsa/ui/icon/paste.gif", "Paste",
				new EditPasteAction()));
		tools.add(undoTool = createTool("src/main/java/ltsa/ui/icon/undo.gif", "Undo",
				new UndoAction()));
		tools.add(redoTool = createTool("src/main/java/ltsa/ui/icon/redo.gif", "Redo",
				new RedoAction()));
		tools.addSeparator();
		tools.add(parseTool = createTool("src/main/java/ltsa/ui/icon/parse.gif", "Parse",
				new DoAction(DO_parse)));
		tools.add(compileTool = createTool("src/main/java/ltsa/ui/icon/compile.gif", "Compile",
				new DoAction(DO_compile)));
		tools.add(composeTool = createTool("src/main/java/ltsa/ui/icon/compose.gif", "Compose",
				new DoAction(DO_doComposition)));
		tools.add(minimizeTool = createTool("src/main/java/ltsa/ui/icon/minimize.gif", "Minimize",
				new DoAction(DO_minimiseComposition)));
		// status field used to name the composition we are wrking on
		targetChoice = new JComboBox();
		targetChoice.setEditable(false);
		targetChoice.addItem(DEFAULT);
		targetChoice.setToolTipText("Target Composition");
		targetChoice.setRequestFocusEnabled(false);
		targetChoice.addActionListener(new TargetAction());
		tools.add(targetChoice);
		tools.addSeparator();
		tools.add(safetyTool = createTool("src/main/java/ltsa/ui/icon/safety.gif", "Check safety",
				new DoAction(DO_safety)));
		tools.add(progressTool = createTool("src/main/java/ltsa/ui/icon/progress.gif",
				"Check Progress", new DoAction(DO_progress)));
		// tools.add(stopTool = createTool("src/main/java/ltsa/ui/icon/stop.gif", "Stop",
		// new StopAction()));
		// stopTool.setEnabled(false);
		tools.addSeparator();
		tools.add(createTool("src/main/java/ltsa/ui/icon/alphabet.gif", "Run Arranged Animation", new DoAction(DO_ARRANGED_ANIMATOR)));
		tools.addSeparator();
		tools.add(createTool("src/main/java/ltsa/ui/icon/exploration.gif", "Do Exploration", new DoAction(DO_EXPLORATION)));
		tools.add(createTool("src/main/java/ltsa/ui/icon/manual.png", "Manual", new DoAction(DO_EXPLORATION_MANUAL)));
		tools.add(stepscount = createTextBox());
		tools.add(createTool("src/main/java/ltsa/ui/icon/stepover.png", "Step Over", new DoAction(DO_EXPLORATION_STEPOVER)));
		tools.add(createTool("src/main/java/ltsa/ui/icon/resume.png", "Resume", new DoAction(DO_EXPLORATION_RESUME)));

		getContentPane().add("North", tools);
		tools.addSeparator();
		tools.add(createTool("src/main/java/ltsa/ui/icon/blanker.gif", "Blank Screen",
				new BlankAction()));

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
		file_save.setEnabled(isApplet == null);
		file_saveAs.setEnabled(isApplet == null);
		file_export.setEnabled(isApplet == null);
		saveFileTool.setEnabled(isApplet == null);
		updateDoState();
		// switch to edit tab
		LTSCanvas.displayName = setDisplayName.isSelected();
		LTSCanvas.newLabelFormat = setNewLabelFormat.isSelected();
		LTSDrawWindow.singleMode = !setMultipleLTS.isSelected();
		newDrawWindow(window_draw.isSelected());
		//create layout tab
		LTSLayoutWindow.singleMode = !setMultipleLTS.isSelected();
		newLayoutWindow(window_layout.isSelected());
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
			url = 	new File(icon).toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}


		JButton b = new JButton(
				new ImageIcon(url)) {
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

	protected JTextField createTextBox()
	{
		JTextField t = new JTextField("1", 1);
		t.setMaximumSize(new Dimension(300, 300));
		return t;
	}

	// ------------------------------------------------------------------------

	void menuEnable(boolean flag) {
		boolean application = (isApplet == null);
		file_new.setEnabled(flag && tabindex == 0);
		file_example.setEnabled(flag && tabindex == 0);
		file_open.setEnabled(application && flag && tabindex == 0);
		file_exit.setEnabled(flag);
		check_safe.setEnabled(flag);

		check_progress.setEnabled(flag);
		check_run.setEnabled(flag);
		check_reachable.setEnabled(flag);
		build_parse.setEnabled(flag);
		build_compile.setEnabled(flag);
		build_compose.setEnabled(flag);
		build_minimise.setEnabled(flag);
		parseTool.setEnabled(flag);
		safetyTool.setEnabled(flag);
		progressTool.setEnabled(flag);
		compileTool.setEnabled(flag);
		composeTool.setEnabled(flag);
		minimizeTool.setEnabled(flag);
	}

	// ------------------------------------------------------------------------
	private final static int DO_safety = 1;

	// >>> AMES: Deadlock Insensitive Analysis
	private final static int DO_safety_no_deadlock = 2;
	// <<< AMES

	private final static int DO_execute = 3;
	private final static int DO_reachable = 4;
	private final static int DO_compile = 5;
	private final static int DO_doComposition = 6;
	private final static int DO_minimiseComposition = 7;
	private final static int DO_progress = 8;
	private final static int DO_liveness = 9;
	private final static int DO_parse = 10;

	// Dipi
	private final static int DO_PLUS_CR = 11;
	private final static int DO_PLUS_CA = 12;
	private static final int DO_DETERMINISE = 13;
	private static final int DO_REFINEMENT = 14;
	private static final int DO_DEADLOCK = 15;
	private static final int DO_CONSISTENCY = 16;
	// Dipi

	// Naha
	static final int DO_ARRANGED_ANIMATOR = 19;
	static final int DO_EXPLORATION = 21;
	static final int DO_EXPLORATION_STEPOVER = 25;
	static final int DO_EXPLORATION_RESUME = 23;
	static final int DO_EXPLORATION_MANUAL = 24;
	static final int DO_deterministic = 999;
	// Naha

	private static final int DO_RUNENACTORS = 17;
	private static final int DO_ENACTORSOPTIONS = 18;

	// >>> AMES: multiple ce
	private final static int DO_safety_multi_ce = 20;
	// <<< AMES
	private static final int DO_GRAPH_UPDATE = 22;

	private int theAction = 0;
	private Thread executer;

	private void do_action(int action) {
		menuEnable(false);
		// check_stop.setEnabled(true);
		// stopTool.setEnabled(true);
		theAction = action;
		executer = new Thread(this);
		executer.setPriority(Thread.NORM_PRIORITY - 1);
		executer.start();
	}

	public void run() {
		try {
			switch (theAction) {
			case DO_safety:
				showOutput();
				safety();
				break;

			// >>> AMES: Deadlock Insensitive Analysis
			case DO_safety_no_deadlock:
				showOutput();
				safety(false, false);
				break;
			// <<< AMES

			// >>> AMES: multiple ce
			case DO_safety_multi_ce:
				showOutput();
				safety(false, true);
				break;
			// <<< AMES

			case DO_ARRANGED_ANIMATOR:
				animate();
				break;
			case DO_EXPLORATION:
				showOutput();
				compile();
				doComposition();
				exploration_new();
				break;
			case DO_EXPLORATION_STEPOVER:
				exploration_stepover();
				break;
			case DO_EXPLORATION_MANUAL:
				exploration_manual();
				break;
			case DO_EXPLORATION_RESUME:
				exploration_resume();
				break;
			case DO_reachable:
				showOutput();
				reachable();
				break;
			case DO_deterministic:
				showOutput();
				checkDeterministic();
				break;
			case DO_compile:
				showOutput();
				compile();
				break;
			case DO_doComposition:
				showOutput();
				doComposition();
				break;
			case DO_minimiseComposition:
				showOutput();
				minimiseComposition();
				break;
			case DO_progress:
				showOutput();
				progress();
				break;
			case DO_liveness:
				showOutput();
				liveness();
				break;
			case DO_parse:
				parse();
				break;
			case DO_PLUS_CR:
				doApplyPlusCROperator();
				break;
			case DO_PLUS_CA:
				doApplyPlusCAOperator();
				break;
			case DO_DETERMINISE:
				doDeterminise();
				break;
			case DO_REFINEMENT:
				doRefinement();
				break;
			case DO_DEADLOCK:
				doDeadlockCheck();
				break;
			case DO_CONSISTENCY:
				doConsistency();
				break;
			case DO_RUNENACTORS:
				doRunEnactors();
				break;
			case DO_ENACTORSOPTIONS:
				doEnactorOptions();
				break;
				case DO_GRAPH_UPDATE:
					doGraphUpdate();
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

	private void checkDeterministic() {

		if (current == null) {
			outln("*** Compile an LTS before checking determinism");

		}else{
			CompactState currentCS = current.getComposition();
			if (currentCS.isNonDeterministic()){
				outln("This is a non deterministic LTS");
			} else {
				outln("This is a deterministic LTS");
			}
		}
	}

	private void doGraphUpdate() {
		updateGraphSimulation = new UpdateGraphSimulation(this, currentDirectory, applicationContext);
		updateGraphSimulation.start();
	}

	// ------------------------------------------------------------------------

	class CloseWindow extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			quitAll();
		}

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
		targetChoice.removeAllItems();
		targetChoice.addItem(DEFAULT);
		check_run.removeAll();
		check_run.add(default_run);
		run_items = null;
		assert_items = null;
		run_names = null;
		check_liveness.removeAll();
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

	private final static String fileType = "*.lts";
	private String openFile = fileType;
	String currentDirectory;
	private String savedText = "";

	private void newFile() {
		if (checkSave()) {
			setTitle("MTS Analyser");
			savedText = "";
			openFile = fileType;
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
			fd.setFile(fileType);
			fd.setVisible(true);
			doOpenFile(currentDirectory = fd.getDirectory(), fd.getFile(),
					false);
		}
		repaint(); // hack to solve display problem
	}

	private void doOpenFile(String dir, String f, boolean resource) {
		if (f != null)
			try {
				openFile = f;
				setTitle("MTSA - " + openFile);
				InputStream fin;
				if (!resource)
					fin = new FileInputStream(dir + openFile);
				else
					fin = this.getClass().getResourceAsStream(dir + openFile);
				// now turn the FileInputStream into a DataInputStream
				try {
					BufferedReader myInput = new BufferedReader(
							new InputStreamReader(fin));
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
			setTitle("MTSA - " + openFile);
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
				String tempname = (currentDirectory == null) ? openFile
						: currentDirectory + openFile;
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
		if (current == null || current.composition == null) {
			JOptionPane.showMessageDialog(this,
					"No target composition to export");
			return;
		}
		String fname = current.composition.name;
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
				current.composition.printAUT(myOutput);
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
			int result = JOptionPane.showConfirmDialog(this,
					"Do you want to save the contents of " + openFile);
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
		public void actionPerformed(ActionEvent e) {
			undo.discardAllEdits();
			input.getDocument().removeUndoableEditListener(undoHandler);
			newFile();
			input.getDocument().addUndoableEditListener(undoHandler);
			updateDoState();
		}
	}

	class OpenFileAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			undo.discardAllEdits();
			input.getDocument().removeUndoableEditListener(undoHandler);
			openAFile();
			input.getDocument().addUndoableEditListener(undoHandler);
			updateDoState();
		}
	}

	class SaveFileAction implements ActionListener {
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
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit"))
				saveAsFile();
		}
	}

	class ExportFileAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String pp = textIO.getTitleAt(textIO.getSelectedIndex());
			if (pp.equals("Edit"))
				exportFile();
			else if (pp.equals("Transitions"))
				prints.saveFile(currentDirectory, ".aut");
		}
	}

	class ExitFileAction implements ActionListener {
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
			do_action(actionCode);
		}
	}

	class OptionAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == setWarnings)
				Diagnostics.warningFlag = setWarnings.isSelected();
			else if (source == setWarningsAreErrors)
				Diagnostics.warningsAreErrors = setWarningsAreErrors
						.isSelected();
			else if (source == setFair)
				ProgressCheck.strongFairFlag = setFair.isSelected();
			else if (source == setAlphaLTL)
				AssertDefinition.addAsterisk = !setAlphaLTL.isSelected();
			else if (source == setSynchLTL)
				FormulaFactory.normalLTL = !setSynchLTL.isSelected();
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
				if (layouts!=null) layouts.setMode(LTSLayoutWindow.singleMode);
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

	class SuperTraceOptionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			setSuperTraceOption();
		}
	}

	class LayoutOptionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			final JFrame f = new JFrame("Layout parameters");
			f.setResizable(false);
			f.setSize(300, 600);
			f.setLocationRelativeTo(null);
			Container container = f.getContentPane();
			container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
			JPanel content = new JPanel();
			content.setLayout(new GridLayout(0,1));

			JPanel kkpanel = new JPanel();
			kkpanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.KamadaKawai.toString()));
			kkpanel.setLayout(new GridLayout(0,2));

			kkpanel.add(new JLabel("Length factor"));
			final JSpinner KK_length_factor_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.KK_length_factor,0.1,10.0,0.1));
			kkpanel.add(KK_length_factor_spinner);

// Not useful for connected graphs
//		  kkpanel.add(new JLabel("Distance"));
//		  final JSpinner KK_distance_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.KK_distance,0.1,10.0,0.1));
//		  kkpanel.add(KK_distance_spinner);

			kkpanel.add(new JLabel("Max iterations"));
			final JSpinner kk_it_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.KK_max_iterations,1,10000,1));
			kkpanel.add(kk_it_spinner);

			container.add(kkpanel);

			JPanel frpanel = new JPanel();
			frpanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.FruchtermanReingold.toString()));
			frpanel.setLayout(new GridLayout(0,2));

			frpanel.add(new JLabel("Attraction"));
			final JSpinner fr_attraction_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.FR_attraction,0.1,10.0,0.05));
			frpanel.add(fr_attraction_spinner);

			frpanel.add(new JLabel("Repulsion"));
			final JSpinner fr_repulsion_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.FR_repulsion,0.1,10.0,0.05));
			frpanel.add(fr_repulsion_spinner);

			frpanel.add(new JLabel("Max iterations"));
			final JSpinner fr_it_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.FR_max_iterations,1,10000,1));
			frpanel.add(fr_it_spinner);

			container.add(frpanel);

// No useful parameters for TreeLikeLTS, RadialLTS
//		  JPanel treepanel = new JPanel();
//		  treepanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.TreeLikeLTS.toString()));
//		  treepanel.setLayout(new GridLayout(0,2));
//
//		  treepanel.add(new JLabel("Horizontal distance"));
//		  final JSpinner tree_distx_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.Tree_distX,0,1000,1));
//		  treepanel.add(tree_distx_spinner);
//
//		  treepanel.add(new JLabel("Vertical distance"));
//		  final JSpinner tree_disty_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.Tree_distY,0,1000,1));
//		  treepanel.add(tree_disty_spinner);
//
//		  content.add(treepanel);
//
//		  JPanel radialpanel = new JPanel();
//		  radialpanel.setBorder(BorderFactory.createTitledBorder(EnumLayout.RadialLTS.toString()));
//		  radialpanel.setLayout(new GridLayout(0,2));
//
//		  radialpanel.add(new JLabel("Horizontal distance"));
//		  final JSpinner radial_distx_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.Radial_distX,0,1000,1));
//		  radialpanel.add(radial_distx_spinner);
//
//		  radialpanel.add(new JLabel("Vertical distance"));
//		  final JSpinner radial_disty_spinner = new JSpinner(new SpinnerNumberModel(LTSJUNGCanvas.Radial_distY,0,1000,1));
//		  radialpanel.add(radial_disty_spinner);
//
//		  content.add(radialpanel);

//		  container.add(content);

			final JButton okbutton = new JButton("Ok");
			final JButton cancelbutton = new JButton("Cancel");
			final JButton applybutton = new JButton("Apply");

			class ButtonOptionListener implements ActionListener {
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
						if (o==null) return;
						LTSJUNGCanvas.KK_length_factor = Double.parseDouble(o.toString()) < 0 ? 0 : (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
						//if (layouts!=null) layouts.getCanvas().setOptions(LTSJUNGCanvas.LayoutOptions.KK_length_factor,LTSJUNGCanvas.KK_length_factor);
					} catch(NumberFormatException ignored) {}
//		    	  try {
//		  	    	  Object o = KK_distance_spinner.getValue();
//		  	    	  if (o==null) return;
//		  	    	  LTSJUNGCanvas.KK_distance = Double.parseDouble(o.toString()) < 0 ? 0 : (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
//		  	    	  //if (layouts!=null) layouts.setKK_distance(LTSLayoutWindow.KK_distance);
//		    	  } catch(NumberFormatException nfe) {}
					try {
						Object o = kk_it_spinner.getValue();
						if (o==null) return;
						LTSJUNGCanvas.KK_max_iterations = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 10000 ? 10000 : Integer.parseInt(o.toString()));
						//if (layouts!=null) layouts.setKK_max_iterations(LTSLayoutWindow.KK_max_iterations);
					} catch(NumberFormatException ignored) {}
					try {
						Object o = fr_attraction_spinner.getValue();
						if (o==null) return;
						LTSJUNGCanvas.FR_attraction = Double.parseDouble(o.toString()) < 0 ? 0 : (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
						//if (layouts!=null) layouts.setFR_attraction(LTSLayoutWindow.FR_attraction);
					} catch(NumberFormatException ignored) {}
					try {
						Object o = fr_repulsion_spinner.getValue();
						if (o==null) return;
						LTSJUNGCanvas.FR_repulsion = Double.parseDouble(o.toString()) < 0 ? 0 : (Double.parseDouble(o.toString()) > 10 ? 10 : Double.parseDouble(o.toString()));
						//if (layouts!=null) layouts.setFR_repulsion(LTSLayoutWindow.FR_repulsion);
					} catch(NumberFormatException ignored) {}
					try {
						Object o = fr_it_spinner.getValue();
						if (o==null) return;
						LTSJUNGCanvas.FR_max_iterations = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 10000 ? 10000 : Integer.parseInt(o.toString()));
						//if (layouts!=null) layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
					} catch(NumberFormatException ignored) {}
//		    	  try {
//		  	    	  Object o = tree_distx_spinner.getValue();
//		  	    	  if (o==null) return;
//		  	    	  LTSJUNGCanvas.Tree_distX = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 1000 ? 1000 : Integer.parseInt(o.toString()));
//		  	    	  //if (layouts!=null) layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
//		    	  } catch(NumberFormatException nfe) {}
//		    	  try {
//		  	    	  Object o = tree_disty_spinner.getValue();
//		  	    	  if (o==null) return;
//		  	    	  LTSJUNGCanvas.Tree_distY = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 1000 ? 1000 : Integer.parseInt(o.toString()));
//		  	    	  //if (layouts!=null) layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
//		    	  } catch(NumberFormatException nfe) {}
//		    	  try {
//		  	    	  Object o = radial_distx_spinner.getValue();
//		  	    	  if (o==null) return;
//		  	    	  LTSJUNGCanvas.Radial_distX = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 1000 ? 1000 : Integer.parseInt(o.toString()));
//		  	    	  //if (layouts!=null) layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
//		    	  } catch(NumberFormatException nfe) {}
//		    	  try {
//		  	    	  Object o = radial_disty_spinner.getValue();
//		  	    	  if (o==null) return;
//		  	    	  LTSJUNGCanvas.Radial_distY = Integer.parseInt(o.toString()) < 0 ? 0 : (Integer.parseInt(o.toString()) > 1000 ? 1000 : Integer.parseInt(o.toString()));
//		  	    	  //if (layouts!=null) layouts.setFR_max_iterations(LTSLayoutWindow.FR_max_iterations);
//		    	  } catch(NumberFormatException nfe) {}
				}
			}

			JPanel buttonpanel = new JPanel();
			buttonpanel.setLayout(new GridLayout(0,3));

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
		public void actionPerformed(ActionEvent e) {
			newAlphabetWindow(window_alpha.isSelected());
		}
	}

	class WinPrintAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			newPrintWindow(window_print.isSelected());
		}
	}

	class WinDrawAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			newDrawWindow(window_draw.isSelected());
		}
	}

	class WinLayoutAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			newDrawWindow(window_layout.isSelected());
		}
	}

	class HelpAboutAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			aboutDialog();
		}
	}

	class BlankAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			blankit();
		}
	}

	class HelpManualAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			displayManual(help_manual.isSelected());
		}
	}

	class StopAction implements ActionListener {
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
				// if (executer != null && executer.isAlive()) {
				// executer.stop();
				// menuEnable(true);
				// check_stop.setEnabled(false);
				// stopTool.setEnabled(false);
				// outln("\n\t-- process was killed");
				// executer = null;
				// } {
				// outln("\n\t-- process stopped by itself");
				// }
				executer = null;
			}
		}
	}

	class ExecuteAction implements ActionListener {
		String runtarget;

		ExecuteAction(String s) {
			runtarget = s;
		}

		public void actionPerformed(ActionEvent e) {
			run_menu = runtarget;
			do_action(DO_execute);
		}
	}

	class LivenessAction implements ActionListener {
		String asserttarget;

		LivenessAction(String s) {
			asserttarget = s;
		}

		public void actionPerformed(ActionEvent e) {
			asserted = asserttarget;
			do_action(DO_liveness);
		}
	}

	class EditCutAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			input.cut();
		}
	}

	class EditCopyAction implements ActionListener {
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
		public void actionPerformed(ActionEvent e) {
			input.paste();
		}
	}

	class TargetAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String choice = (String) targetChoice.getSelectedItem();
			if (choice == null)
				return;
			run_enabled = MenuDefinition.enabled(choice);
			if (run_items != null && run_enabled != null) {
				if (run_items.length == run_enabled.length)
					for (int i = 0; i < run_items.length; ++i)
						run_items[i].setEnabled(run_enabled[i]);
			}
		}
	}

	// --------------------------------------------------------------------
	// undo editor stuff

	class UndoHandler implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
			updateDoState();
		}
	}

	class UndoAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				undo.undo();
			} catch (CannotUndoException ignored) {
			}
			updateDoState();
		}
	}

	class RedoAction implements ActionListener {
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
		public void actionPerformed(ActionEvent e) {
			centre(findDialog);
			findDialog.setVisible(true);
		}
	}

	// <<< AMES

	protected void updateDoState() {
		edit_undo.setEnabled(undo.canUndo());
		undoTool.setEnabled(undo.canUndo());
		edit_redo.setEnabled(undo.canRedo());
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
		edit_cut.setEnabled(b);
		cutTool.setEnabled(b);
		edit_paste.setEnabled(b);
		pasteTool.setEnabled(b);
		file_new.setEnabled(b);
		file_example.setEnabled(b);
		file_open.setEnabled(app && b);
		file_saveAs.setEnabled(app && b);
		file_export.setEnabled(app && (b || pp.equals("Transitions")));
		newFileTool.setEnabled(b);
		openFileTool.setEnabled(app && b);
		edit_undo.setEnabled(b && undo.canUndo());
		undoTool.setEnabled(b && undo.canUndo());
		edit_redo.setEnabled(b && undo.canRedo());
		redoTool.setEnabled(b && undo.canRedo());
		if (b)
			SwingUtilities.invokeLater(new RequestFocus());
	}

	class RequestFocus implements Runnable {
		public void run() {
			input.requestFocusInWindow();
		}
	}

	// ------------------------------------------------------------------------

	public void out(String str) {
		SwingUtilities.invokeLater(new OutputAppend(str));
	}

	public void outln(String str) {
		SwingUtilities.invokeLater(new OutputAppend(str + "\n"));
	}

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

		public void run() {
			output.append(text);
		}
	}

	class OutputClear implements Runnable {
		public void run() {
			output.setText("");
		}
	}

	class OutputShow implements Runnable {
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

	public char backChar() {
		fPos = fPos - 1;
		if (fPos < 0) {
			fPos = 0;
			return '\u0000';
		} else
			return fSrc.charAt(fPos);
	}

	public int getMarker() {
		return fPos;
	}

	// >>> AMES: Enhanced Modularity
	@Override
	public void resetMarker() {
		fPos = -1;
	}

	// <<< AMES

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
	}

	private CompositeState docompile() {
		resetInput();
		CompositeState cs = null;
		LTSCompiler comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.compile();
			if (!parse(comp.getComposites(), comp.getProcesses(), comp.getExplorers())) {
				return null;
			}

			cs = comp.continueCompilation((String) targetChoice.getSelectedItem());

		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}

	private void doparse(Hashtable cs, Hashtable ps, Hashtable ex) {
		resetInput();
		LTSCompiler comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.parse(cs, ps, ex);

		} catch (LTSException x) {
			displayError(x);
			cs = null;
		}
	}

	// <<< AMES

	private String lastCompiled = "";

	private boolean compileIfChange() {
		String tmp = input.getText();
		if (current == null || !tmp.equals(lastCompiled)
				|| !current.getName().equals(targetChoice.getSelectedItem())) {
			lastCompiled = tmp;
			return compile();
		}

		return true;
	}

	// ------------------------------------------------------------------------

	// >>> AMES: Deadlock Insensitive Analysis, multiple ce
	private void safety() {
		safety(true, false);
	}

	private void safety(boolean checkDeadlock, boolean multiCe) {
		clearOutput();
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.checkSafety(current, this);

		}
	}

	// <<< AMES

	// ------------------------------------------------------------------------

	private void progress() {
		clearOutput();
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.checkProgress(current, this); // sihay
																		// MTS
																		// deberia
																		// dar
																		// exception
																		// , en
																		// otro
																		// caso,
																		// analiser
																		// .
																		// checkprogress
																		// o
																		// algo
																		// asi.
		}
	}

	// ------------------------------------------------------------------------

	private void liveness() {
		clearOutput();
		compileIfChange();
		CompositeState ltl_property = AssertDefinition.compile(this, asserted);
		// Silent compilation for negated formula
		CompositeState not_ltl_property = AssertDefinition.compile(
				new EmptyLTSOuput(), AssertDefinition.NOT_DEF + asserted);
		if (current != null && ltl_property != null) {
			TransitionSystemDispatcher.checkFLTL(current, ltl_property,
					not_ltl_property, this.setFair.isSelected(), this);
			postState(current);
		}
	}

	// ------------------------------------------------------------------------

	private void minimiseComposition() {
		clearOutput();
		compileIfChange();
		if (compileIfChange() && current != null) {
			if (current.composition == null)
				TransitionSystemDispatcher.applyComposition(current, this);
			TransitionSystemDispatcher.minimise(current, this);
			postState(current);
		}
	}

	// ------------------------------------------------------------------------

	private void doComposition() {
		clearOutput();
		compileIfChange();
		if (current != null)
		{
			TransitionSystemDispatcher.applyComposition(current, this);
			postState(current);
		}
	}

	// ------------------------------------------------------------------------
	private boolean checkReplay(Animator a) {
		if (a.hasErrorTrace()) {
			int result = JOptionPane.showConfirmDialog(this,
					"Do you want to replay the error trace?");
			if (result == JOptionPane.YES_OPTION) {
				return true;
			} else if (result == JOptionPane.NO_OPTION)
				return false;
			else if (result == JOptionPane.CANCEL_OPTION)
				return false;
		}
		return false;
	}

	private void exploration_new()
	{
		// Definition
		String choice = (String) targetChoice.getSelectedItem();
		if (explorerDefinitions == null || !explorerDefinitions.containsKey(choice))
		{
			this.outln(choice + " is not a valid explorer");
			return;
		}
		ExplorerDefinition explorerDefinition = explorerDefinitions.get(choice);

		// View
		CompactState[] components = new CompactState[explorerDefinition.getView().size()];
		for (int i = 0; i < explorerDefinition.getView().size(); i++)
			for (int j = 0; j < current.machines.size(); j++)
				if (explorerDefinition.getView().get(i).getName().equals(current.machines.elementAt(j).getName()))
					components[i] = current.machines.elementAt(j);

		List<List<Symbol>> environmentActions = explorerDefinition.getEnvironmentActions();
		ViewNextConfiguration[] view_configurations = new ViewNextConfiguration[current.machines.size() - 1];
		for (int i = 1; i < components.length; i++)
		{
			if (environmentActions != null && environmentActions.size() > i - 1)
            {
                String[] trace = new String[environmentActions.get(i - 1).size()];
                for (int j = 0; j < trace.length; j++)
                    trace[j] = environmentActions.get(i - 1).get(j).toString();
                view_configurations[i] = new ViewNextConfigurationTrace(trace);
            }
			else
				view_configurations[i] = new ViewNextConfigurationRandom();
		}

		View view = new View(components, view_configurations);

		// Model
		CompactState[] knowledge_configurations = new CompactState[explorerDefinition.getModel().size()];
		for (int i = 0; i < explorerDefinition.getView().size(); i++)
			for (int j = 0; j < current.machines.size(); j++)
				if (explorerDefinition.getModel().get(i).getName().equals(current.machines.elementAt(j).getName()))
					knowledge_configurations[i] = current.machines.elementAt(j);

		CompactState[] model_configurations = new CompactState[knowledge_configurations.length];
		for (int i = 0; i < model_configurations.length; i++)
			model_configurations[i] = knowledge_configurations[i].myclone();

		Model model = new Model(model_configurations);

		// Knowledge
		Knowledge knowledge = new Knowledge(knowledge_configurations);

		// Goal
		GRControllerGoal<String> goal = current.goal;
		HashSet<String> controlableActions  = new HashSet<>(0);
		for (String anAction : goal.getControllableActions())
			if (!anAction.contains("["))
				controlableActions.add(anAction);
		goal.setControllableActions(controlableActions);

		// Strategy manager
		Strategy[] strategies = new Strategy[1];
		strategies[0] = new StrategySynthesisNewAction(new StrategySynthesis(knowledge, goal.copy()), new StrategyNewAction(knowledge, goal.copy()));
		//strategies[0] = new StrategyNewAction(knowledge, goal.copy());
		StrategyManager strategyManager = new StrategyManager(strategies);

		// Explorer
		this.explorer = new Explorer(view, model, knowledge, goal, strategyManager);
		String action = "      ";
		String state = String.valueOf(knowledge.getCurrentStates()[0]);
		while (state.length() < 3)
			state = " " + state;
		this.outln(action + "  ->  " + state);

		current.composition = null;
		current.makeController = false;
		postState(current);
		draws.setCurrentState(this.explorer.getCurrentStateNumbers());
		layouts.setCurrentState(this.explorer.getCurrentStateNumbers());
	}

	private void exploration_manual()
	{
		if (this.explorer == null)
			throw new UnsupportedOperationException("Primero hay que explorar");

		String mtsControlProblemAnswer = this.explorer.getMTSControlProblemAnswer();

		if (mtsControlProblemAnswer.equals("ALL"))
		{
			this.outln("All implementations can be controlled");
			return;
		}

		if (mtsControlProblemAnswer.equals("NONE"))
		{
			this.outln("There is no controller for model for the given setting");
			return;
		}

		String[] aviableActions = this.explorer.getAviableActions();
		int choice = JOptionPane.showOptionDialog(null, "Choose the next action", "Next action", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, aviableActions, aviableActions[0]);

		if (choice == -1)
			return;

		this.explorer.explore(aviableActions[choice]);

		exploration_output();
	}

	private String exploration_stepover()
	{
		if (this.explorer == null)
			throw new UnsupportedOperationException("Primero hay que explorar");

		String mtsControlProblemAnswer = "";
		Integer steps_count = getStepsCount();
		for (int i = 0; i < steps_count; i++)
		{
			mtsControlProblemAnswer = this.explorer.getMTSControlProblemAnswer();

			if (mtsControlProblemAnswer.equals("ALL"))
			{
				this.outln("All implementations can be controlled");
				break;
			}

			if (mtsControlProblemAnswer.equals("NONE"))
			{
				this.outln("There is no controller for model for the given setting");
				break;
			}

			if (mtsControlProblemAnswer.equals("RESET"))
				explorer.reset();

			if (mtsControlProblemAnswer.equals("SOME"))
				this.explorer.explore();

			exploration_output();
		}

		return mtsControlProblemAnswer;
	}

	private void exploration_resume()
	{
		String mtsControlProblemAnswer = exploration_stepover();
		while (mtsControlProblemAnswer.equals("SOME") || mtsControlProblemAnswer.equals("RESET"))
			mtsControlProblemAnswer = exploration_stepover();
	}

	private void exploration_output()
	{
		ArrayList<String> traceStates = this.explorer.getTraceLastStates();
		ArrayList<String> traceActions = this.explorer.getTraceLastActions();

		for (int j = 0; j < traceStates.size(); j++)
		{
			String action = traceActions.get(j);

			while (action.length() < 6)
				action = action + " ";

			String state = traceStates.get(j);
			while (state.length() < 3)
				state = " " + state;

			if (Objects.equals(state.trim(), ""))
				this.outln(action);
			else
				this.outln(action + "  ->  " + state);
		}

        postState(current);
		draws.setCurrentState(this.explorer.getCurrentStateNumbers());
		layouts.setCurrentState(this.explorer.getCurrentStateNumbers());
	}

	private Integer getStepsCount()
	{
		Integer count_integer = 1;
		try
		{
			String count_string = this.stepscount.getText();
			count_integer = Integer.parseInt(count_string);
		}
		catch (NumberFormatException ignored)
		{

		}
		if (count_integer < 1)
			count_integer = 1;
		return count_integer;
	}

	private void animate() {
		clearOutput();
		compileIfChange();
		boolean replay = false;
		if (current != null) {
			if (current instanceof UpdatingControllerCompositeState && current.getComposition() == null) {
				throw new UnsupportedOperationException("Please, do parallel composition before animate");
			}
			if (current.machines.size() > 1 && MTSUtils.isMTSRepresentation(current)) {
				throw new UnsupportedOperationException("Animation for more than one MTS is not developed yet");
			}


			Animator anim = TransitionSystemDispatcher.generateAnimator(
					current, this, eman);

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

	private Frame createCustom(Animator anim, String params, Relation actions,
			Relation controls, boolean replay) {
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

	private void reachable() {
		clearOutput();
		compileIfChange();
		if (current != null && current.machines.size() > 0) {
			Analyser a = new Analyser(current, this, null);
			SuperTrace s = new SuperTrace(a, this);
			current.setErrorTrace(s.getErrorTrace());
		}
	}

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
		if (disp && textIO.indexOfTab("Layout")<0) {
			// create Text window
			layouts = new LTSLayoutWindow(current,eman);
			textIO.addTab("Layout",layouts);
			swapto(textIO.indexOfTab("Layout"));
		} else if (!disp && textIO.indexOfTab("Layout")>0) {
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

	// ------------------------------------------------------------------------

	private void setSuperTraceOption() {
		try {
			String o = (String) JOptionPane.showInputDialog(this,
					"Enter Hashtable size (Kilobytes):",
					"Supertrace parameters", JOptionPane.PLAIN_MESSAGE, null,
					null, "" + SuperTrace.getHashSize());
			if (o == null)
				return;
			SuperTrace.setHashSize(Integer.parseInt(o));
			o = (String) JOptionPane.showInputDialog(this,
					"Enter bound for search depth size:",
					"Supertrace parameters", JOptionPane.PLAIN_MESSAGE, null,
					null, "" + SuperTrace.getDepthBound());
			if (o == null)
				return;
			SuperTrace.setDepthBound(Integer.parseInt(o));
		} catch (NumberFormatException ignored) {
		}
	}

	// -------------------------------------------------------------------------

	private void doOptimist() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.makeOptimisticModel(current, this);
			postState(current);
		}
	}

	private void doPesimist() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.makePessimisticModel(current, this);
			postState(current);
		}
	}

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

	private void doDeadlockCheck() {
		if (compileIfChange() && current != null) {
			TransitionSystemDispatcher.hasCompositionDeadlockFreeImplementations(current, this);
		}
	}

	private void doRefinement() {
		if (compileIfChange() && current != null) {
			RefinementOptions refinementOptions = new RefinementOptions();
			String[] models = getMachinesNames();

			final RefinementWindow refinementWindow = new RefinementWindow(
					this, refinementOptions, models, SemanticType.values(),
					RefinementWindow.Mode.REFINEMENT);
			refinementWindow.setLocation(this.getX() + 100, this.getY() + 100);
			refinementWindow.pack();
			refinementWindow.setVisible(true);
			if (refinementOptions.isValid()) {
				boolean areRefinement = TransitionSystemDispatcher
						.isRefinement(selectMachine(refinementOptions
								.getRefinesModel()),
								selectMachine(refinementOptions
										.getRefinedModel()), refinementOptions
										.getRefinementSemantic(), this);
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

			final RefinementWindow refinementWindow = new RefinementWindow(
					this, refinementOptions, models, SemanticType.values(),
					RefinementWindow.Mode.CONSISTENCY);
			refinementWindow.setLocation(this.getX() + 100, this.getY() + 100);
			refinementWindow.pack();
			refinementWindow.setVisible(true);
			if (refinementOptions.isValid()) {
				this.outln("Checking Consistency");
				TransitionSystemDispatcher.areConsistent(
						selectMachine(refinementOptions.getRefinesModel()),
						selectMachine(refinementOptions.getRefinedModel()),
						refinementOptions.getRefinementSemantic(), this);
			}

			postState(current);
		}
	}

	private void doRunEnactors() {

		if (current == null || current.composition == null
				|| current.goal == null) {
			this.outln("There is no composite state or goal available");
			return;
		}

		if (this.enactmentSimulation != null)
			this.enactmentSimulation.runSimulation(current,
					getApplicationContext(), enactmentOptions);

	}

	private void doEnactorOptions() {
		final EnactorOptionsWindows enactorOptionsWindow = new EnactorOptionsWindows(
				enactmentOptions, this);

		if (this.enactmentOptions.scheduler != null
				&& !this.enactmentOptions.scheduler.isEmpty())
			enactorOptionsWindow.getCbSchedulers().setSelectedItem(
					enactmentOptions.scheduler);

		if (this.enactmentOptions.enactors != null) {
			List<Integer> selected = new ArrayList<Integer>();
			for (String enactor : this.enactmentOptions.enactors) {
				for (int i = 0; i < enactorOptionsWindow.getEnactorList()
						.getModel().getSize(); i++)
					if (enactor.equals(enactorOptionsWindow.getEnactorList()
							.getModel().getElementAt(i)))
						selected.add(i);
			}
			int[] selectIndices = new int[selected.size()];
			for (int i = 0; i < selected.size(); i++)
				selectIndices[i] = selected.get(i).intValue();

			enactorOptionsWindow.getEnactorList().setSelectedIndices(
					selectIndices);

		}

		enactorOptionsWindow.getOkButton().addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {

						enactmentOptions.scheduler = (String) enactorOptionsWindow
								.getCbSchedulers().getSelectedItem();
						int[] selectedIndices = enactorOptionsWindow
								.getEnactorList().getSelectedIndices();
						enactmentOptions.enactors.clear();
						for (int i = 0; i < selectedIndices.length; i++) {
							enactmentOptions.enactors.add(enactorOptionsWindow
									.getEnactorList().getModel()
									.getElementAt(selectedIndices[i])
									.toString());
						}
						enactorOptionsWindow.setVisible(false);
					}
				});
		enactorOptionsWindow.setVisible(true);

	}

	private String[] getMachinesNames() {
		CompactState composition = current.composition;
		int size = current.machines.size();
		if (composition != null) {
			size++;
		}
		String[] result = new String[size];

		int i = 0;
		for (Iterator it = current.machines.iterator(); it.hasNext(); i++) {
			CompactState compactState = (CompactState) it.next();
			result[i] = compactState.name;
		}
		if (composition != null) {
			result[i] = composition.name;
		}
		return result;
	}

	private CompactState selectMachine(int machineIndex) {
		CompactState result = null;
		Vector machines = current.machines;
		if (machineIndex < machines.size()) {
			return (CompactState) machines.get(machineIndex);
		} else {
			return current.composition;
		}
	}

	// ------------------------------------------------------------------------

	private boolean parse() {
		return parse(null, null, null);
	}

	/* AMES: promoted visibility from private to implement lts.LTSManager */
	public boolean parse(Hashtable cs, Hashtable ps, Hashtable ex) {


		String oldChoice = (String) targetChoice.getSelectedItem();

		if (cs == null && ps == null) {
			cs = new Hashtable();
			ps = new Hashtable();
			ex = new Hashtable();
			doparse(cs, ps, ex);
		}

		if (cs == null)
			return false;
		targetChoice.removeAllItems();

		if (ex.size() == 0)
		{
			if (cs.size() == 0)
			{
				targetChoice.addItem(DEFAULT);
			}
			else
			{
				Enumeration e = cs.keys();
				java.util.List forSort = new ArrayList();
				while (e.hasMoreElements()) {
					forSort.add(e.nextElement());
				}
				Collections.sort(forSort);
				for (Object aForSort : forSort) {
					targetChoice.addItem(aForSort);
				}
			}
		}
		else
		{
			Enumeration e = ex.keys();
			java.util.List forSort = new ArrayList();
			while (e.hasMoreElements()) {
				forSort.add(e.nextElement());
			}
			Collections.sort(forSort);
			for (Object aForSort : forSort) {
				targetChoice.addItem(aForSort);
			}
		}

		if (oldChoice != null)
		{
			if (!oldChoice.equals(DEFAULT) && (ex.containsKey(oldChoice) || (ex.size() == 0 && cs.containsKey(oldChoice))))
				targetChoice.setSelectedItem(oldChoice);
		}
		current = null;
		explorer = null;
		explorerDefinitions = ex;

		// >>> AMES: Enhanced Modularity
		eman.post(new LTSEvent(LTSEvent.NEWCOMPOSITES, cs.keySet()));
		eman.post(new LTSEvent(LTSEvent.NEWPROCESSES, ps.keySet()));
		eman.post(new LTSEvent(LTSEvent.NEWLABELSETS,
				(labelSetConstants = LabelSet.getConstants()).keySet()));
		// <<< AMES

		// deal with run menu
		check_run.removeAll();
		run_names = MenuDefinition.names();
		run_enabled = MenuDefinition.enabled((String) targetChoice
				.getSelectedItem());
		check_run.add(default_run);
		if (run_names != null) {
			run_items = new JMenuItem[run_names.length];
			for (int i = 0; i < run_names.length; ++i) {
				run_items[i] = new JMenuItem(run_names[i]);
				run_items[i].setEnabled(run_enabled[i]);
				run_items[i].addActionListener(new ExecuteAction(run_names[i]));
				check_run.add(run_items[i]);
			}
		}
		// deal with assert menu
		check_liveness.removeAll();
		assert_names = AssertDefinition.names();
		if (assert_names != null) {
			assert_items = new JMenuItem[assert_names.length];
			for (int i = 0; i < assert_names.length; ++i) {
				assert_items[i] = new JMenuItem(assert_names[i]);
				assert_items[i].addActionListener(new LivenessAction(
						assert_names[i]));
				check_liveness.add(assert_items[i]);
			}
		}
		// validate(); //seems to cause deadlock in GUI
		return true;
	}

	// ------------------------------------------------------------------------

	private void displayManual(boolean dispman) {
		if (dispman && textIO.indexOfTab("Manual") < 0) {
			// create Manual window
			manual = new JEditorPane();
			manual.setEditable(false);
			manual.addHyperlinkListener(new Hyperactive());
			JScrollPane mm = new JScrollPane(manual,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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

		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				JEditorPane pane = (JEditorPane) e.getSource();
				try {
					URL u = e.getURL();
					// outln("URL: "+u);
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
		window.setTitle("MTS Analyser");
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

	// >>> AMES: Enhanced Modularity
	@Override
	public CompositeState compile(String name) {
		resetInput();
		CompositeState cs = null;
		LTSCompiler comp = new LTSCompiler(this, this, currentDirectory);
		try {
			comp.compile();
			cs = comp.continueCompilation(name);
		} catch (LTSException x) {
			displayError(x);
		}
		return cs;
	}

	/**
	 * Returns the currently selected item from the targets selection box.
	 */
	public String getTargetChoice() {
		return (String) targetChoice.getSelectedItem();
	}

	/**
	 * Updates the various display windows and animators with the given
	 * machines.
	 */
	public void newMachines(java.util.List<CompactState> machines) {
		CompositeState c = new CompositeState(
				new Vector<>(machines));
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
	public void performAction(final Runnable r, final boolean showOutputPane) {
		// XXX: There is a race here, as there is in the method do_action.
		menuEnable(false);
		// check_stop.setEnabled(true);
		// stopTool.setEnabled(true);
		executer = new Thread(new Runnable() {
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

	// <<< AMES

	private void fillEnactorsMenu(JMenuItem parent_menu) {
		EnactorFactory<Long, String> enactorFactory = applicationContext
				.getBean(EnactorFactory.class);
		for (String enactorName : enactorFactory.getEnactorNames()) {
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(enactorName);
			menuItem.setSelected(false);
			parent_menu.add(menuItem);

		}

	}

	/**
	 * Returns the instantiated Spring Application Context
	 * 
	 * @return the Application Context
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	//TODO move this class to a file. refactor needed to organize the menu related code.
	class GraphUpdateMenu extends JMenu {
		JMenuItem startMenu = new JMenuItem("Begin Simulation");
		public GraphUpdateMenu() {
			super("Update");
			startMenu.addActionListener(new DoAction(DO_GRAPH_UPDATE));
			this.add(startMenu);
		}
	}

}
