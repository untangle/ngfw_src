/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.install;

import javax.swing.table.*;
import java.util.Vector;
import java.util.List;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.MSortedTableModel;
import com.untangle.gui.widgets.editTable.MEditTableJPanel;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.networking.Interface;
import com.untangle.mvvm.NetworkingConfiguration;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.gui.util.Util;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

public class InstallDiskJPanel extends MWizardPageJPanel {

    private DiskListCompoundSettings diskListCompoundSettings;
    private InstallWizard installWizard;

    public InstallDiskJPanel(InstallWizard installWizard) {
        initComponents();
        this.installWizard = installWizard;
        diskListCompoundSettings = new DiskListCompoundSettings();
    }


    public boolean enteringForwards(){
        try{
            diskListCompoundSettings.refresh();
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error scanning disks", e);
        }
        doRefresh(diskListCompoundSettings);
        return true;
    }

	private void doRefresh(DiskListCompoundSettings diskListCompoundSettings){
        // clear list
        diskJPanel.removeAll();
        // create button group
        ButtonGroup buttonGroup = new ButtonGroup();
        // create buttons, put them in a group, put them in jpanel
        for( DiskInfo diskInfo : diskListCompoundSettings.getDiskInfoList() ){
            DiskJRadioButton newButton = new DiskJRadioButton(diskInfo.getName());
            newButton.setText(diskInfo.getName() + " (size: " + diskInfo.getGigs() + "GB)");
            newButton.setOpaque(false);
            buttonGroup.add(newButton);
            diskJPanel.add(newButton);
        }
        diskJPanel.revalidate();
        diskJPanel.repaint();
    }

    public boolean leavingForwards(){
        DiskJRadioButton selectedJRadioButton = null;
        for( int i=0; i<diskJPanel.getComponentCount(); i++){
            if(((DiskJRadioButton)diskJPanel.getComponent(i)).isSelected()){
                selectedJRadioButton = (DiskJRadioButton)diskJPanel.getComponent(i);
                break;
            }
        }
        if(selectedJRadioButton != null){
            String selectedDisk = selectedJRadioButton.getDisk();
            InstallWizard.setTargetDisk(selectedDisk);
            return true;
        }
        else{
            MOneButtonJDialog dialog = MOneButtonJDialog.factory(InstallDiskJPanel.this.getTopLevelAncestor(), "Install Wizard",
                                                                 "You must select a boot disk which you would like to install Untangle onto.",
                                                                 "Install Wizard Message", "Message");
            return false;
        }
    }
        
    class DiskListCompoundSettings{
        private List<DiskInfo> diskInfoList;
        public DiskListCompoundSettings(){}
        public void save(){}
        public void refresh() throws Exception{
            Vector<DiskInfo> diskInfoVector = new Vector<DiskInfo>();
            for(String diskName : SystemStats.getAvailableDisks() ){
                diskInfoVector.add(new DiskInfo(diskName,SystemStats.getDiskGigs(diskName)));
            }
            diskInfoList = diskInfoVector;
        }
        public void validate(){}
        public List<DiskInfo> getDiskInfoList(){ return diskInfoList; }
    }

    class DiskInfo{
        private String name;
        private float gigs;
        public DiskInfo(String name, float gigs){
            this.name = name;
            this.gigs = gigs;
        }
        public String getName(){ return name; }
        public float getGigs(){ return gigs; }
    }

    class DiskJRadioButton extends JRadioButton {
        private String disk;
        public DiskJRadioButton(String disk){
            super();
            this.disk = disk;
        }
        public String getDisk(){ return disk; }
    }


        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                contentJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                diskJScrollPane = new javax.swing.JScrollPane();
                diskJPanel = new javax.swing.JPanel();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html>This list shows the boot disks that were found.<br><font color=\"#FF0000\">Please choose a disk for the Untangle Platform installation.  Warning, all the data on the boot disk you select will be deleted.</font></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel1, gridBagConstraints);

                diskJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                diskJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                diskJPanel.setLayout(new javax.swing.BoxLayout(diskJPanel, javax.swing.BoxLayout.Y_AXIS));

                diskJScrollPane.setViewportView(diskJPanel);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(60, 100, 60, 100);
                contentJPanel.add(diskJScrollPane, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JPanel diskJPanel;
        private javax.swing.JScrollPane diskJScrollPane;
        private javax.swing.JLabel jLabel1;
        // End of variables declaration//GEN-END:variables
    
}
