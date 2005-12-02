/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.NetworkingConfiguration;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class NetworkJDialog extends MConfigJDialog {

    private static final String NAME_NETWORK_SETTINGS = "Network Settings";
    private static final String NAME_EMAIL_SETTINGS = "Email Settings";
    private static final String NAME_ALIAS_PANEL      = "Network Aliases";
    private static final String NAME_SECRET_PANEL      = "Advanced Support";

    private static boolean showHiddenPanel;
    public static void setShowHiddenPanel(boolean showHiddenPanelX){ showHiddenPanel = showHiddenPanelX; }

    public NetworkJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_NETWORK_SETTINGS);
        
        // NETWORK SETTINGS //////
        NetworkIPJPanel ipJPanel = new NetworkIPJPanel();
        JScrollPane ipJScrollPane = new JScrollPane( ipJPanel );
        ipJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        ipJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_NETWORK_SETTINGS, null, ipJScrollPane);
	super.savableMap.put(NAME_NETWORK_SETTINGS, ipJPanel);
	super.refreshableMap.put(NAME_NETWORK_SETTINGS, ipJPanel);

        // EMAIL SETTINGS /////
        NetworkEmailJPanel emailJPanel = new NetworkEmailJPanel();
        JScrollPane emailJScrollPane = new JScrollPane( emailJPanel );
        emailJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        emailJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.contentJTabbedPane.addTab(NAME_EMAIL_SETTINGS, null, emailJPanel );
	super.savableMap.put(NAME_EMAIL_SETTINGS, emailJPanel );
	super.refreshableMap.put(NAME_EMAIL_SETTINGS, emailJPanel );
        
        // ALIASES /////
        NetworkAliasJPanel aliasJPanel = new NetworkAliasJPanel();
        super.contentJTabbedPane.addTab(NAME_ALIAS_PANEL, null, aliasJPanel );
	super.savableMap.put(NAME_ALIAS_PANEL, aliasJPanel );
	super.refreshableMap.put(NAME_ALIAS_PANEL, aliasJPanel );

	// SECRED HIDDEN PANEL //////
	if( showHiddenPanel ){
	    NetworkSecretJPanel secretJPanel = new NetworkSecretJPanel();
	    super.contentJTabbedPane.addTab(NAME_SECRET_PANEL, null, secretJPanel);
	    super.savableMap.put(NAME_SECRET_PANEL, secretJPanel);
	    super.refreshableMap.put(NAME_SECRET_PANEL, secretJPanel);
	}
    }
    
    protected void sendSettings(Object settings) throws Exception {
	Util.getNetworkingManager().set( (NetworkingConfiguration) settings);
        
    }
    protected void refreshSettings(){
	settings = Util.getNetworkingManager().get();
    }

    protected void saveAll(){
	// ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        SaveSettingsProceedJDialog saveSettingsProceedJDialog = new SaveSettingsProceedJDialog();
        boolean isProceeding = saveSettingsProceedJDialog.isProceeding();
        if( isProceeding ){ 
            super.saveAll();
        }
    }

}
