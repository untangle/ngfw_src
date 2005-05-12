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
    
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_BLOCK_LIST = "Block List";
    private static final String NAME_ANTI_VIRUS = "AntiVirus";
    private static final String NAME_ANTI_SPAM = "AntiSpam";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel)  {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// GENERAL SETTINGS ///////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        this.mTabbedPane.insertTab(NAME_GENERAL_SETTINGS, null, generalConfigJPanel, null, 0);
	super.savableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);
	super.refreshableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);
	
	// BLOCK LIST ////////
	BlockConfigJPanel blockConfigJPanel = new BlockConfigJPanel();
        this.mTabbedPane.insertTab(NAME_BLOCK_LIST, null, blockConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK_LIST, blockConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK_LIST, blockConfigJPanel);

	// ANTIVIRUS ////////
	VirusConfigJPanel virusConfigJPanel = new VirusConfigJPanel();
	this.mTabbedPane.insertTab(NAME_ANTI_VIRUS, null, virusConfigJPanel, null, 0);
	super.savableMap.put(NAME_ANTI_VIRUS, virusConfigJPanel);
	super.refreshableMap.put(NAME_ANTI_VIRUS, virusConfigJPanel);

	// ANTISPAM ////////
	SPAMConfigJPanel spamConfigJPanel = new SPAMConfigJPanel();
	this.mTabbedPane.insertTab(NAME_ANTI_SPAM, null, spamConfigJPanel, null, 0);
	super.savableMap.put(NAME_ANTI_SPAM, spamConfigJPanel);
	super.refreshableMap.put(NAME_ANTI_SPAM, spamConfigJPanel);

	// VIRUS DETECTION /////////
	Util.setEmailAndVirusJPanel( "email-transform", virusConfigJPanel );
	        
    }
    
}

