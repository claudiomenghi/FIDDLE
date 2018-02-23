package ltsa.ui;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ltsa.lts.automata.automaton.event.LTSEvent;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.gui.EventClient;
import ltsa.lts.gui.EventManager;
import ltsa.lts.gui.PrintTransitions;
import ltsa.lts.output.LTSOutput;

public class PrintWindow extends JSplitPane implements LTSOutput, EventClient {

	public static boolean fontFlag = false;

	JTextArea output;
	JList list;
	JScrollPane left, right;
	EventManager eman;
	int Nmach;
	int selectedMachine = 0;
	LabelledTransitionSystem[] sm; // an array of machines
	Font f1 = new Font("Monospaced", Font.PLAIN, 12);
	Font f2 = new Font("Monospaced", Font.BOLD, 20);
	Font f3 = new Font("SansSerif", Font.PLAIN, 12);
	Font f4 = new Font("SansSerif", Font.BOLD, 18);
	PrintWindow thisWindow;
	private final static int MAXPRINT = Integer.MAX_VALUE;

	public PrintWindow(CompositeState cs, EventManager eman) {
		super();
		this.eman = eman;
		thisWindow = this;
		// scrollable output pane
		output = new JTextArea(23, 50);
		output.setEditable(false);
		right = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		output.setBackground(Color.white);
		output.setBorder(new EmptyBorder(0, 5, 0, 0));
		// scrollable list pane
		list = new JList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new PrintAction());
		left = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		if (eman != null)
			eman.addClient(this);
		new_machines(cs);
		setLeftComponent(left);
		setRightComponent(right);
		setDividerLocation(200);
		setBigFont(fontFlag);
		validate();
	}

	// ------------------------------------------------------------------------

	class PrintAction implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			int machine = list.getSelectedIndex();
			if (machine < 0 || machine >= Nmach)
				return;
			selectedMachine = machine;
			clearOutput();
			PrintTransitions p = new PrintTransitions(sm[selectedMachine]);
			p.print(thisWindow, MAXPRINT);
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

	public void out(String str) {
		output.append(str);
	}

	public void outln(String str) {
		output.append(str + "\n");
	}

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
			for (int i = 0; e.hasMoreElements(); i++){
				sm[i] = e.nextElement();
			}
			Nmach = sm.length;
			if (hasC == 1){
				sm[Nmach - 1] = cs.getComposition();
			}
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
		if (selectedMachine >= Nmach){
			selectedMachine = 0;
		}
		clearOutput();
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
	public void saveFile(String currentDirectory, String extension) {
		String message;
		if (extension.equals(".txt"))
			message = "Save text in:";
		else
			message = "Save as Aldebaran format (.aut) in:";

		FileDialog fd = new FileDialog((Frame) getTopLevelAncestor(), message, FileDialog.SAVE);
		if (Nmach > 0) {
			String fname = sm[selectedMachine].getName();
			int colon = fname.indexOf(':', 0);
			if (colon > 0)
				fname = fname.substring(0, colon);
			fd.setFile(fname + extension);
			fd.setDirectory(currentDirectory);
		}
		fd.setVisible(true);

		String sn = fd.getFile();
		if (sn != null)
			try {
				int i = sn.indexOf('.', 0);
				sn = sn.substring(0, i) + extension;
				File file = new File(fd.getDirectory(), sn);
				FileOutputStream fout = new FileOutputStream(file);

				// now convert the FileOutputStream into a PrintStream
				PrintStream myOutput = new PrintStream(fout);
				if (extension.equals(".txt")) {
					String text = output.getText();
					myOutput.print(text);
				} else {
					if (Nmach > 0)
						sm[selectedMachine].printAUT(myOutput);
				}
				myOutput.close();
				fout.close();
				// outln("Saved in: "+ fd.getDirectory()+file);
			} catch (IOException e) {
				outln("Error saving file: " + e);
			}
	}

}