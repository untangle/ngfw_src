/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;



public class MaintenanceJDialog extends MConfigJDialog {

    private static final String NAME_MAINTENANCE_CONFIG = "Support Config";
    private static final String NAME_REMOTE_SETTINGS    = "Access Restrictions";
    private static final String NAME_PROTOCOL_OVERRIDE  = "Manual Protocol Override";
    private static final String NAME_NETWORK_INTERFACES = "Network Interfaces";
    private static final String NAME_SECRET_PANEL       = "Advanced Support";
    private static final String NAME_PING_TEST          = "Ping Test";

    private static boolean showHiddenPanel;
    public static void setShowHiddenPanel(boolean showHiddenPanelX){ showHiddenPanel = showHiddenPanelX; }

    public MaintenanceJDialog( Frame parentFrame ) {
        super(parentFrame);
        setTitle(NAME_MAINTENANCE_CONFIG);
        setHelpSource("support_config");
        compoundSettings = new MaintenanceCompoundSettings();
    }

    protected void generateGui(){
        // GENERAL SETTINGS //////
        MaintenanceAccessJPanel maintenanceAccessJPanel = new MaintenanceAccessJPanel();
        addScrollableTab(null, NAME_REMOTE_SETTINGS, null, maintenanceAccessJPanel, false, true);
        addSavable(NAME_REMOTE_SETTINGS, maintenanceAccessJPanel);
        addRefreshable(NAME_REMOTE_SETTINGS, maintenanceAccessJPanel);
        maintenanceAccessJPanel.setSettingsChangedListener(this);

        // CASINGS //
        MCasingJPanel[] mCasingJPanels = ((MaintenanceCompoundSettings)compoundSettings).getCasingJPanels();
        if( mCasingJPanels.length > 0 ){
            JTabbedPane overrideJTabbedPane = addTabbedPane(NAME_PROTOCOL_OVERRIDE, null);
            for(MCasingJPanel mCasingJPanel : mCasingJPanels){
                String casingDisplayName = mCasingJPanel.getDisplayName();
                addScrollableTab(overrideJTabbedPane, casingDisplayName, null, mCasingJPanel, false, true);
                addSavable(casingDisplayName, mCasingJPanel);
                addRefreshable(casingDisplayName, mCasingJPanel);
                mCasingJPanel.setSettingsChangedListener(this);
            }
        }
        else {
            JPanel messageJPanel = new JPanel();
            messageJPanel.setLayout(new BorderLayout());
            JLabel messageJLabel = new JLabel("There are currently no protocols being used by the rack.");
            messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            messageJPanel.add(messageJLabel);
            addTab(NAME_PROTOCOL_OVERRIDE, null, messageJPanel);
        }

        // NETWORK INTERFACES //////
        MaintenanceInterfaceJPanel maintenanceInterfaceJPanel = new MaintenanceInterfaceJPanel();
        maintenanceInterfaceJPanel.setMConfigJDialog(this);
        addTab(NAME_NETWORK_INTERFACES, null, maintenanceInterfaceJPanel);
        addSavable(NAME_NETWORK_INTERFACES, maintenanceInterfaceJPanel);
        addRefreshable(NAME_NETWORK_INTERFACES, maintenanceInterfaceJPanel);
        maintenanceInterfaceJPanel.setSettingsChangedListener(this);

        // PING TEST //
        MaintenancePingJPanel maintenancePingJPanel = new MaintenancePingJPanel();
        addTab(NAME_PING_TEST, null, maintenancePingJPanel);

        // SECRET HIDDEN PANEL //////
        if( showHiddenPanel ){
            MaintenanceSecretJPanel maintenanceSecretJPanel = new MaintenanceSecretJPanel();
            addTab(NAME_SECRET_PANEL, null, maintenanceSecretJPanel);
            addSavable(NAME_SECRET_PANEL, maintenanceSecretJPanel);
            addRefreshable(NAME_SECRET_PANEL, maintenanceSecretJPanel);
            maintenanceSecretJPanel.setSettingsChangedListener(this);
        }
    }


    protected boolean shouldSave(){
        NetworkSaveSettingsProceedJDialog networkSaveSettingsProceedJDialog = new NetworkSaveSettingsProceedJDialog(this);
        return networkSaveSettingsProceedJDialog.isProceeding();
    }


}
