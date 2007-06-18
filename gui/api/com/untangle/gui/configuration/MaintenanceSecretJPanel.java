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
import javax.swing.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.security.*;
import com.untangle.uvm.snmp.*;
import com.untangle.uvm.node.*;

public class MaintenanceSecretJPanel extends javax.swing.JPanel
    implements Savable<MaintenanceCompoundSettings>, Refreshable<MaintenanceCompoundSettings> {


    public MaintenanceSecretJPanel() {
        initComponents();
        jScrollPane1.getVerticalScrollBar().setFocusable(false);
        jScrollPane1.getHorizontalScrollBar().setFocusable(false);
        jScrollPane2.getVerticalScrollBar().setFocusable(false);
        jScrollPane2.getHorizontalScrollBar().setFocusable(false);
        Util.addPanelFocus(this, script1JTextArea);
        Util.addFocusHighlight(script1JTextArea);
        Util.addFocusHighlight(script2JTextArea);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // SCRIPT ///
        String script = script1JTextArea.getText();
        script1JTextArea.setBackground( Color.WHITE );

        // CUSTOM RULES ///
        String rules = script2JTextArea.getText();
        script2JTextArea.setBackground( Color.WHITE );

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();
            miscSettings.setPostConfigurationScript( script );
            miscSettings.setCustomRules( rules );
        }
    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
        MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();

        // SCRIPT //
        script1JTextArea.setBackground( Color.WHITE );
        String script1 = miscSettings.getPostConfigurationScript();
        script1JTextArea.setText( script1 );
        Util.addSettingChangeListener(settingsChangedListener, this, script1JTextArea);

        // CUSTOM RULES //
        script2JTextArea.setBackground( Color.WHITE );
        String rules = miscSettings.getCustomRules();
        script2JTextArea.setText( rules );
        Util.addSettingChangeListener(settingsChangedListener, this, script2JTextArea);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        snmpButtonGroup = new javax.swing.ButtonGroup();
        trapButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        script1JTextArea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        script2JTextArea = new javax.swing.JTextArea();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Support Script", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        externalRemoteJPanel.setMinimumSize(new java.awt.Dimension(88, 323));
        externalRemoteJPanel.setPreferredSize(new java.awt.Dimension(730, 323));
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("<html><b><font color=\"#ff0000\">Warning: Do not make any changes to the text below unless told to do so explicitly by Untangle Support.</b></font></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        externalRemoteJPanel.add(jLabel1, gridBagConstraints);

        script1JTextArea.setFont(new java.awt.Font("Monospaced", 0, 12));
        jScrollPane1.setViewportView(script1JTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        externalRemoteJPanel.add(jScrollPane1, gridBagConstraints);

        script2JTextArea.setFont(new java.awt.Font("Monospaced", 0, 12));
        jScrollPane2.setViewportView(script2JTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        externalRemoteJPanel.add(jScrollPane2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(externalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea script1JTextArea;
    private javax.swing.JTextArea script2JTextArea;
    private javax.swing.ButtonGroup snmpButtonGroup;
    private javax.swing.ButtonGroup trapButtonGroup;
    // End of variables declaration//GEN-END:variables


}
