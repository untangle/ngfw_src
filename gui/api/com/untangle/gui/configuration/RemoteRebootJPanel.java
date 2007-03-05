/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.configuration;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class RemoteRebootJPanel extends JPanel implements ActionListener {

    
    private GridBagConstraints rebootJButtonConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,10,0), 0, 0);
    private GridBagConstraints rebootJLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

    public RemoteRebootJPanel(){
	this.setLayout(new GridBagLayout());
        
        JButton rebootJButton;
        rebootJButton = new JButton("Reboot");
        rebootJButton.setFont(new java.awt.Font("Arial", 0, 11));
        rebootJButton.setPreferredSize(new Dimension(225, 25));
        rebootJButton.setMaximumSize(new Dimension(225, 25));
        rebootJButton.setSize(new Dimension(225, 25));
        rebootJButton.addActionListener(this);
        this.add(rebootJButton, rebootJButtonConstraints);

	JLabel rebootJLabel;
	rebootJLabel = new JLabel();
	rebootJLabel.setFont(new java.awt.Font("Arial", 0, 11));
	rebootJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	rebootJLabel.setHorizontalTextPosition(SwingConstants.CENTER);
	rebootJLabel.setText("<html><center><b>Warning: </b>Clicking this button will reboot the Untangle Server, temporarily<br>interrupting network activity.</center></html>");
	this.add(rebootJLabel, rebootJLabelConstraints);

    this.setFocusable(true);
    Util.addPanelFocus(this, rebootJButton);
    }

    public void actionPerformed(ActionEvent ae){
	if( Util.getIsDemo() )
	    return;
	MTwoButtonJDialog warningJDialog = MTwoButtonJDialog.factory((Window)this.getTopLevelAncestor(), "",
				  "You are about to manually reboot.  This will interrupt normal network operations" +
				  " until the Untangle Server is finished automatically restarting.  This may take up to several minutes to complete.",
				  "Manual Reboot Warning", "Warning");
	warningJDialog.setVisible(true);
	if( warningJDialog.isProceeding() ){
	    try{
		Util.getMvvmContext().rebootBox();
	    }
	    catch(Exception e){
		Util.handleExceptionNoRestart("Error: Unable to reboot Untangle Server", e);
	    }
	    Util.exit(0);
	}
	
    }

}
