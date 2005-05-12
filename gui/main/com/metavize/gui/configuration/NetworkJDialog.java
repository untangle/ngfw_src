/*
 * NetworkJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.configWindow.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.restartWindow.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.NetworkingConfiguration;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;


/**
 *
 * @author  inieves
 */
public class NetworkJDialog extends ConfigJDialog {

    private static final String NAME_NETWORK_SETTINGS = "Network Settings";

    public NetworkJDialog( ) {
        super(Util.getMMainJFrame());
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);
    }

    protected void generateGui(){
        // GENERAL SETTINGS //////
        NetworkJPanel networkJPanel = new NetworkJPanel();
        JScrollPane contentJScrollPane = new JScrollPane( networkJPanel );
        contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.setTitleAt(0, NAME_NETWORK_SETTINGS);
        this.contentJPanel.add(contentJScrollPane);
        this.setTitle(NAME_NETWORK_SETTINGS);
	super.savableMap.put(NAME_NETWORK_SETTINGS, networkJPanel);
	super.refreshableMap.put(NAME_NETWORK_SETTINGS, networkJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception {
	Util.getNetworkingManager().set( (NetworkingConfiguration) settings);
        new RestartJDialog();
    }
    protected void refreshSettings(){
	settings = Util.getNetworkingManager().get();
    }

    protected void saveAll(){
	// ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        NetworkProceedJDialog networkProceedJDialog = new NetworkProceedJDialog();
        boolean isProceeding = networkProceedJDialog.isProceeding();
        if( isProceeding ) 
            super.saveAll();
    }

}
