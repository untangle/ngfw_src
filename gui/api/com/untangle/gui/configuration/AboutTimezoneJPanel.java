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
import java.text.SimpleDateFormat;
import javax.swing.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.snmp.*;

public class AboutTimezoneJPanel extends javax.swing.JPanel
    implements Savable<AboutCompoundSettings>, Refreshable<AboutCompoundSettings> {



    public AboutTimezoneJPanel() {
        initComponents();
        Util.addPanelFocus(this, timezoneJComboBox);
        for(TimeZone tz : TimeZone.values()){
            timezoneJComboBox.addItem(tz);
        }

    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(AboutCompoundSettings aboutCompoundSettings, boolean validateOnly) throws Exception {

        // TIMEZONE ///////
        String timezone = ((TimeZone) timezoneJComboBox.getSelectedItem()).getKey();

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(timezone);
            aboutCompoundSettings.setTimeZone( tz );
            java.util.TimeZone.setDefault(tz);
        }

    }

    public void doRefresh(AboutCompoundSettings aboutCompoundSettings){

        // TIMEZONE ////
        TimeZone tz = TimeZone.getValue(aboutCompoundSettings.getTimeZone().getID());
        timezoneJComboBox.setSelectedItem(tz);
        Util.addSettingChangeListener(settingsChangedListener, this, timezoneJComboBox);


        // DATE //
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, MMM d yyyy   h:mm a");
        simpleDateFormat.setTimeZone(aboutCompoundSettings.getTimeZone());
        String dateString = simpleDateFormat.format(aboutCompoundSettings.getDate());
        dateString += "   " + tz.getGmtValue();
        timeJLabel.setText(dateString);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        snmpButtonGroup = new javax.swing.ButtonGroup();
        trapButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        enableRemoteJPanel = new javax.swing.JPanel();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        timezoneJComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        timeJLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Time Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Timezone:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel5, gridBagConstraints);

        timezoneJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        timezoneJComboBox.setMaximumSize(new java.awt.Dimension(425, 24));
        timezoneJComboBox.setMinimumSize(new java.awt.Dimension(425, 24));
        timezoneJComboBox.setPreferredSize(new java.awt.Dimension(425, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 0);
        restrictIPJPanel.add(timezoneJComboBox, gridBagConstraints);

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Time at Refresh:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        restrictIPJPanel.add(jLabel6, gridBagConstraints);

        timeJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        restrictIPJPanel.add(timeJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 5, 0);
        enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel enableRemoteJPanel;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.ButtonGroup snmpButtonGroup;
    private javax.swing.JLabel timeJLabel;
    private javax.swing.JComboBox timezoneJComboBox;
    private javax.swing.ButtonGroup trapButtonGroup;
    // End of variables declaration//GEN-END:variables


}
