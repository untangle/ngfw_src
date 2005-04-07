/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.airgap.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
	super.contentJPanel.remove(super.saveJButton);
	super.contentJPanel.remove(super.reloadJButton);
	super.contentJPanel.remove(super.expandJButton);

	JPanel messageJPanel = new JPanel();
	messageJPanel.setLayout(new GridBagLayout());
	JLabel messageJLabel = new JLabel();
	messageJLabel.setText("<html>The Packet Attack Shield has no configurable settings.</html>");
	messageJLabel.setFont(new java.awt.Font("Arial", 0, 12));
	messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	messageJLabel.setVerticalAlignment(SwingConstants.CENTER);
	messageJPanel.add(messageJLabel, new GridBagConstraints(0,0,1,1,0d,0d,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
	super.mTabbedPane.add("General Settings", messageJPanel);
    }
     
    
}
