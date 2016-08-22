package ltsa.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ltsa.lts.automata.automaton.event.LTSEvent;
import ltsa.lts.automata.lts.Alphabet;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.gui.EventClient;
import ltsa.lts.gui.EventManager;
import ltsa.lts.output.LTSOutput;

public class AlphabetWindow extends JSplitPane implements LTSOutput,
		EventClient {

	public static boolean fontFlag = false;

	JTextArea output;
	JList list;
	JScrollPane left;
	JScrollPane right;
	EventManager eman;
	int Nmach;
	int selectedMachine = 0;
	Alphabet current = null;
	int expandLevel = 0;
	LabelledTransitionSystem[] sm; // an array of machines
	Font f1 = new Font("Monospaced", Font.PLAIN, 12);
	Font f2 = new Font("Monospaced", Font.BOLD, 20);
	Font f3 = new Font("SansSerif", Font.PLAIN, 12);
	Font f4 = new Font("SansSerif", Font.BOLD, 18);
	AlphabetWindow thisWindow;
	private final static int MAXPRINT = 400;

	public AlphabetWindow(CompositeState cs, EventManager eman) {
		super();
		this.eman = eman;
		thisWindow = this;
		// scrollable output pane
		output = new JTextArea(23, 50);
		output.setEditable(false);
		right = new JScrollPane(output,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		output.setBackground(Color.white);
		output.setBorder(new EmptyBorder(0, 5, 0, 0));
		// scrollable list pane
		list = new JList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new PrintAction());
		left = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel fortools = new JPanel(new BorderLayout());
		fortools.add("Center", right);
		// tool bar
		JToolBar tools = new JToolBar();
		tools.setOrientation(JToolBar.VERTICAL);
		fortools.add("West", tools);
		tools.add(createTool("icon/expanded.gif", "Expand Most",
				new ExpandMostAction()));
		tools.add(createTool("icon/expand.gif", "Expand",
				new ExpandMoreAction()));
		tools.add(createTool("icon/collapse.gif", "Collapse",
				new ExpandLessAction()));
		tools.add(createTool("icon/collapsed.gif", "Most Concise",
				new ExpandLeastAction()));
		if (eman != null)
			eman.addClient(this);
		new_machines(cs);
		setLeftComponent(left);
		setRightComponent(fortools);
		setDividerLocation(150);
		setBigFont(fontFlag);
		validate();
	}

	// ------------------------------------------------------------------------

	class ExpandMoreAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (current == null)
				return;
			if (expandLevel < current.maxLevel)
				++expandLevel;
			clearOutput();
			current.print(thisWindow, expandLevel);
		}
	}

	class ExpandLessAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (current == null)
				return;
			if (expandLevel > 0)
				--expandLevel;
			clearOutput();
			current.print(thisWindow, expandLevel);
		}
	}

	class ExpandMostAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (current == null)
				return;
			expandLevel = current.maxLevel;
			clearOutput();
			current.print(thisWindow, expandLevel);
		}
	}

	class ExpandLeastAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (current == null)
				return;
			expandLevel = 0;
			clearOutput();
			current.print(thisWindow, expandLevel);
		}
	}

	class PrintAction implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			int machine = list.getSelectedIndex();
			if (machine < 0 || machine >= Nmach)
				return;
			selectedMachine = machine;
			clearOutput();
			current = new Alphabet(sm[machine]);
			if (expandLevel > current.maxLevel)
				expandLevel = current.maxLevel;
			current.print(thisWindow, expandLevel);
		}
	}

	/*---------LTS event broadcast action-----------------------------*/
	@Override
	public void ltsAction(LTSEvent e) {
		switch (e.kind) {
		case LTSEvent.NEWSTATE:
			break;
		case LTSEvent.INVALID:
			new_machines((CompositeState) e.info);
			break;
		case LTSEvent.KILL:
			// this.dispose();
			break;
		default:
			;
		}
	}

	// ------------------------------------------------------------------------
	@Override
	public void out(String str) {
		output.append(str);
	}

	@Override
	public void outln(String str) {
		output.append(str + "\n");
	}

	@Override
	public void clearOutput() {
		output.setText("");
	}

	
	private void new_machines(CompositeState cs) {
		int hasC = (cs != null && cs.getComposition() != null) ? 1 : 0;
		if (cs != null && cs.getMachines() != null && cs.getMachines().size() > 0) { // get
																			// set
																			// of
																			// machines
			sm = new LabelledTransitionSystem[cs.getMachines().size() + hasC];
			Enumeration<LabelledTransitionSystem> e = cs.getMachines().elements();
			for (int i = 0; e.hasMoreElements(); i++)
				sm[i] = (LabelledTransitionSystem) e.nextElement();
			Nmach = sm.length;
			if (hasC == 1)
				sm[Nmach - 1] = cs.getComposition();
		} else
			Nmach = 0;
		DefaultListModel lm = new DefaultListModel();
		for (int i = 0; i < Nmach; i++) {
			if (hasC == 1 && i == (Nmach - 1))
				lm.addElement("||" + sm[i].getName());
			else
				lm.addElement(sm[i].getName());
		}
		list.setModel(lm);
		if (selectedMachine >= Nmach)
			selectedMachine = 0;
		current = null;
		clearOutput();
	}

	// ------------------------------------------------------------------------

	protected JButton createTool(String icon, String tip, ActionListener act) {
		JButton b = new JButton(
				new ImageIcon(this.getClass().getResource(icon))) {
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

	// --------------------------------------------------------------------
	public void setBigFont(boolean b) {
		fontFlag = b;
		if (fontFlag) {
			output.setFont(f2);
			list.setFont(f4);
		} else {
			output.setFont(f1);
			list.setFont(f3);
		}
	}

	// --------------------------------------------------------------------
	public void removeClient() {
		if (eman != null)
			eman.removeClient(this);
	}

	public void copy() {
		output.copy();
	}

	// ------------------------------------------------------------------------

	public void saveFile() {

		FileDialog fd = new FileDialog((Frame) getTopLevelAncestor(),
				"Save text in:", FileDialog.SAVE);
		if (Nmach > 0) {
			String fname = sm[selectedMachine].getName();
			int colon = fname.indexOf(':', 0);
			if (colon > 0)
				fname = fname.substring(0, colon);
			fd.setFile(fname + ".txt");
		}
		fd.show();
		String file = fd.getFile();
		if (file != null)
			try {
				int i = file.indexOf('.', 0);
				file = file.substring(0, i) + "." + "txt";
				FileOutputStream fout = new FileOutputStream(fd.getDirectory()
						+ file);
				// now convert the FileOutputStream into a PrintStream
				PrintStream myOutput = new PrintStream(fout);
				String text = output.getText();
				myOutput.print(text);
				myOutput.close();
				fout.close();
				// outln("Saved in: "+ fd.getDirectory()+file);
			} catch (IOException e) {
				outln("Error saving file: " + e);
			}
	}

}