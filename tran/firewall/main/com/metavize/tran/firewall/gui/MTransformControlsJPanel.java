/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.firewall.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

import com.metavize.tran.firewall.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_BLOCK_LIST = "Block/Pass List";
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";

    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);
                
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);        
    }
    
    protected void generateGui(){

        // SETUP FIREWALL
        BlockJPanel blockJPanel = new BlockJPanel();
        super.mTabbedPane.addTab(NAME_BLOCK_LIST, null, blockJPanel );
	savableMap.put(NAME_BLOCK_LIST, blockJPanel);
	refreshableMap.put(NAME_BLOCK_LIST, blockJPanel);
        
        // SETUP GENERAL SETTINGS
        SettingsJPanel settingsJPanel = new SettingsJPanel();
        JScrollPane settingsJScrollPane = new JScrollPane( settingsJPanel );
        settingsJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        settingsJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab(NAME_GENERAL_SETTINGS, null, settingsJScrollPane );
        savableMap.put(NAME_GENERAL_SETTINGS, settingsJPanel);
	refreshableMap.put(NAME_GENERAL_SETTINGS, settingsJPanel);

	// EVENT LOG
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
    }

}


