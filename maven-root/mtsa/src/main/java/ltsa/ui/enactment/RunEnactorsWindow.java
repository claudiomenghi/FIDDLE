/**
 * 
 */
package ui.enactment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import ar.uba.dc.lafhis.enactment.BaseController;
import ar.uba.dc.lafhis.enactment.TakeFirstController;

import ar.uba.dc.lafhis.enactment.gui.GraphVisualizer;

import javax.swing.JSplitPane;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



/**
 * @author Julio
 *
 */
/**
 * @author Julio
 *
 * @param <State>
 * @param <Action>
 */
public class RunEnactorsWindow<State, Action> extends JFrame  {

	
    protected JPanel mainPanel;
    protected JScrollPane ltsOutputPane;
    protected JTextArea ltsOutputTextPane;
    //protected ControllerCircularView<State, Action> ltsView;       
    protected GraphVisualizer<State, Action> ltsView;
    private JSplitPane splitPane;
    
    
    

    private JPanel panel;
    private JButton pauseButton;
    private JButton stopButton;
    
    
    
	public RunEnactorsWindow(BaseController<State, Action> controllerScheduler)
	{
		
		mainPanel				= new JPanel();
		
		panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setMinimumSize(new Dimension(10, 20));
		FlowLayout fl_panel = new FlowLayout(FlowLayout.LEADING, 5, 5);
		panel.setLayout(fl_panel);
		
		pauseButton = new JButton("Pause Simulation");
		panel.add(pauseButton);
		
		stopButton = new JButton("Stop & Close");
		panel.add(stopButton);
		
		
        
         
        JButton btZoomIn = new JButton("+");
        btZoomIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	ltsView.zoomIn();
            }
        });
        panel.add(btZoomIn);
        JButton btZoomOut = new JButton("-");
        btZoomOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	ltsView.zoomOut();
            }
        });
        panel.add(btZoomOut);
        
        
		splitPane = new JSplitPane();
		splitPane.setAlignmentY(Component.CENTER_ALIGNMENT);
		splitPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		splitPane.setResizeWeight(1.0);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		TitledBorder ltsOutBorder	= new TitledBorder("LTS trace");
		
		ltsView					= new GraphVisualizer<State, Action>();
		splitPane.setLeftComponent(ltsView);
		ltsView.setPreferredSize(new Dimension(300, 300));
		ltsOutputTextPane		= new JTextArea();
		ltsOutputTextPane.setToolTipText("LTS trace");
		ltsOutputTextPane.setBorder(ltsOutBorder);
		ltsOutputTextPane.setEditable(false);
		ltsOutputPane			= new JScrollPane(ltsOutputTextPane);
		ltsOutputPane.setMinimumSize(new Dimension(23, 150));
		splitPane.setRightComponent(ltsOutputPane);
		
				
		//Setup log output textPane
		LoggerAppender.setUiOutput(ltsOutputTextPane);
		
		ltsView.initialize(controllerScheduler.getLts(), controllerScheduler.getLts().getInitialState(), controllerScheduler);
		

		getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));
		mainPanel.add(panel, BorderLayout.NORTH);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		setTitle("MTSA Simulation");
		setSize(800, 500);
		setLocationRelativeTo(null);

		
		invalidate();
		repaint();
	    
		
		
	}



	/**
	 * @return the pauseButton
	 */
	public JButton getPauseButton() {
		return pauseButton;
	}



	/**
	 * @return the stopButton
	 */
	public JButton getStopButton() {
		return stopButton;
	}
	
	public void zoomToFit()
	{
		ltsView.zoomToFixViewPort();
	}

	
}
