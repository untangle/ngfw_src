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
    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);
    
    private FirewallSettings firewallSettings;
        
    private BlockJPanel blockJPanel;
    private SettingsJPanel settingsJPanel;

    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);

        // SETUP FIREWALL
        blockJPanel = new BlockJPanel();
        super.mTabbedPane.addTab("Block/Pass List", null, blockJPanel );
        
        // SETUP GENERAL SETTINGS
        settingsJPanel = new SettingsJPanel();
        JScrollPane settingsJScrollPane = new JScrollPane( settingsJPanel );
        settingsJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        settingsJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab("General Settings", null, settingsJScrollPane );
        
        
        refreshAll();
    }
    
    public void refreshAll()  {
        boolean isValid = true;
        try {
            firewallSettings = ((Firewall)super.mTransformJPanel.getTransformContext().transform()).getFirewallSettings();
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error getting settings for refresh", e);
        }
        
        try {
            blockJPanel.refresh( firewallSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing Block", e);
        }
        
        try {
            settingsJPanel.refresh( firewallSettings );
        }
        catch(Exception e){
            isValid = false;
            Util.handleExceptionNoRestart("Error refreshing General Settings", e);
        }
        
        if(!isValid){
            // do something more interesting than this
        }  
    }
    
    public void saveAll() {        
        try {
            blockJPanel.save( firewallSettings );
            settingsJPanel.save( firewallSettings );
            firewallSettings.validate();
        }
        catch(Exception e){
            new ValidateFailureDialog( super.mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName(), e.getMessage() );
            return;
        }
        

        try {
            ((Firewall)super.mTransformJPanel.getTransformContext().transform()).setFirewallSettings( firewallSettings );
        }
        catch ( Exception e ) {
            try{
                Util.handleExceptionWithRestart("Error saving settings for firewall", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error saving settings for firewall", f);
                new SaveFailureDialog( super.mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName() );
            }
        }

        

    }

}


