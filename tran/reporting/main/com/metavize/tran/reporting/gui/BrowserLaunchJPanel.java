
package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;

import javax.jnlp.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.net.URL;

public class BrowserLaunchJPanel extends JPanel implements ActionListener {

    private JButton launchJButton;
    private GridBagConstraints launchJButtonConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

    public BrowserLaunchJPanel(){
	this.setLayout(new GridBagLayout());
	launchJButton = new JButton("Launch Web Browser");
	launchJButton.setFont(new java.awt.Font("Arial", 0, 11));
	launchJButton.setPreferredSize(new Dimension(225, 25));
	launchJButton.setMaximumSize(new Dimension(225, 25));
	launchJButton.setSize(new Dimension(225, 25));
	launchJButton.addActionListener(this);
	this.add(launchJButton, launchJButtonConstraints);
    }

    public void actionPerformed(ActionEvent e){
	
	try{
	    URL newURL = new URL("http://" + Util.getServerName() + "/reports");
	    System.err.println("Showing: " + newURL.toString() );
	    BasicService bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
	    bs.showDocument(newURL);
	}
	catch(Exception f){
	    f.printStackTrace();
	}
	
    }

}
