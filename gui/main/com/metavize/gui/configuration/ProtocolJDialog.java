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

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class ProtocolJDialog extends MConfigJDialog {

    private static final String NAME_MANUAL_OVERRIDE_SETTINGS = "Protocol Settings";

    
    public ProtocolJDialog( ) {
    }

    protected void generateGui(){
        this.setTitle(NAME_MANUAL_OVERRIDE_SETTINGS);

        // ADD ALL CASINGS TO THE PANEL
        MCasingJPanel[] mCasingJPanels = Util.getPolicyStateMachine().loadAllCasings(true);
        JScrollPane contentJScrollPane = null;
        String casingDisplayName = null;
        boolean addedSomething = false;
        for(MCasingJPanel mCasingJPanel : mCasingJPanels){
            addedSomething = true;
            contentJScrollPane = new JScrollPane( mCasingJPanel );
            contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
            contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
            casingDisplayName = mCasingJPanel.getMackageDesc().getDisplayName();
            this.contentJTabbedPane.addTab(casingDisplayName, null, contentJScrollPane);
            super.savableMap.put(casingDisplayName, mCasingJPanel);
            super.refreshableMap.put(casingDisplayName, mCasingJPanel);
        }

        if(!addedSomething){
            JPanel messageJPanel = new JPanel();
            messageJPanel.setLayout(new BorderLayout());
            JLabel messageJLabel = new JLabel("There are currently no protocols being used by the rack.");
            messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            messageJPanel.add(messageJLabel);
            this.contentJTabbedPane.addTab("Message", null, messageJPanel);
        }
    }
    
    protected void sendSettings(Object settings) throws Exception {}
    protected void refreshSettings() {}

}
