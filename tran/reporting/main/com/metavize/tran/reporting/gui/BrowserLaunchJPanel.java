
package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.net.URL;

public class BrowserLaunchJPanel extends JPanel implements ActionListener {

    
    private GridBagConstraints noteJLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private GridBagConstraints launchJButtonConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,10,0), 0, 0);
    private GridBagConstraints launchJLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

    public BrowserLaunchJPanel(){
	this.setLayout(new GridBagLayout());
        
        JButton launchJButton;
        launchJButton = new JButton("Launch Web Browser");
        launchJButton.setFocusPainted(false);
        launchJButton.setFont(new java.awt.Font("Arial", 0, 11));
        launchJButton.setPreferredSize(new Dimension(225, 25));
        launchJButton.setMaximumSize(new Dimension(225, 25));
        launchJButton.setSize(new Dimension(225, 25));
        launchJButton.addActionListener(this);
        this.add(launchJButton, launchJButtonConstraints);

        if( Util.isLocal() ){
            launchJButton.setEnabled(false);
            JLabel noteJLabel = new JLabel();
            noteJLabel.setFont(new java.awt.Font("Arial", 0, 11));
	    noteJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	    noteJLabel.setHorizontalTextPosition(SwingConstants.CENTER);
	    noteJLabel.setText("<html><center><b>Note:</b> No web browser is installed on this machine for security reasons.<br>Please re-connect from an extrernal machine.</center></html>");
            this.add(noteJLabel, noteJLabelConstraints);
        }
        
	try{
            JLabel launchJLabel;
	    URL newURL = new URL( Util.getServerCodeBase(), "../reports");
	    launchJLabel = new JLabel();
	    launchJLabel.setFont(new java.awt.Font("Arial", 0, 11));
	    launchJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	    launchJLabel.setHorizontalTextPosition(SwingConstants.CENTER);
	    launchJLabel.setText("<html><center><br>To view reports a web browser, go to:<br>" + newURL.toString() + "</center></html>");
	    this.add(launchJLabel, launchJLabelConstraints);
	}
	catch(Exception f){
	    Util.handleExceptionNoRestart("Error:", f);
	}
	



							    
    }

    public void actionPerformed(ActionEvent e){
	 
	try{
	    URL newURL = new URL( Util.getServerCodeBase(), "../reports");;
	    ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
	}
	catch(Exception f){
            Util.handleExceptionNoRestart("error launching browser for EdgeReport", f);
	}
	
    }

}
