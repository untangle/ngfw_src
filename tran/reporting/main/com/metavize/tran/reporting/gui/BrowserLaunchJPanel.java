
package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.net.URL;

public class BrowserLaunchJPanel extends JPanel implements ActionListener {

    private JButton launchJButton;
    private JLabel launchJLabel;
    private GridBagConstraints launchJButtonConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private GridBagConstraints launchJLabelConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

    public BrowserLaunchJPanel(){
	this.setLayout(new GridBagLayout());

	launchJButton = new JButton("Launch Web Browser");
	launchJButton.setFocusPainted(false);
	launchJButton.setFont(new java.awt.Font("Arial", 0, 11));
	launchJButton.setPreferredSize(new Dimension(225, 25));
	launchJButton.setMaximumSize(new Dimension(225, 25));
	launchJButton.setSize(new Dimension(225, 25));
	launchJButton.addActionListener(this);
	this.add(launchJButton, launchJButtonConstraints);

	try{
	    URL newURL = new URL( Util.getServerCodeBase(), "../reports");
	    launchJLabel = new JLabel();
	    launchJLabel.setFont(new java.awt.Font("Arial", 0, 11));
	    launchJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	    launchJLabel.setHorizontalTextPosition(SwingConstants.CENTER);
	    launchJLabel.setText("<html><center><br>To view reports from another browser, go to:<br>" + newURL.toString() + "</center></html>");
	    this.add(launchJLabel, launchJLabelConstraints);
	}
	catch(Exception f){
	    Util.handleExceptionNoRestart("Error:", f);
	}
	



							    
    }

    public void actionPerformed(ActionEvent e){
	 
	try{
	    URL newURL = new URL( Util.getServerCodeBase(), "../reports");
	    System.err.println("Showing: " + newURL.toString() );
	    ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
	}
	catch(Exception f){
	    f.printStackTrace();
	}
	
    }

}
