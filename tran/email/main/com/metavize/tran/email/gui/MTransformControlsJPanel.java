/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.email.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private VirusConfigJPanel virusConfigJPanel;

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel)  {
        super(mTransformJPanel);
        
	virusConfigJPanel = new VirusConfigJPanel(mTransformJPanel.getTransformContext());

        this.mTabbedPane.insertTab("General Settings", null, new GeneralConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        this.mTabbedPane.insertTab("Block List", null, new BlockConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
	this.mTabbedPane.insertTab("AntiVirus", null, virusConfigJPanel, null, 0);
	//this.mTabbedPane.insertTab("Anti-Virus", null, new VirusConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
	this.mTabbedPane.insertTab("AntiSpam", null, new SPAMConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);

	Util.setEmailAndVirusJPanel( "email-transform", virusConfigJPanel );
	
        
        // this.eventTabbedPane.insertTab("Anti-SPAM", null, new SPAMEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
        // this.eventTabbedPane.insertTab("Anti-Virus", null, new VirusEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
        // this.eventTabbedPane.insertTab("Rule Block List", null, new BlockEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
        // this.eventTabbedPane.insertTab("Size Block List", null, new BlockSizeEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
        
    }
    
}

