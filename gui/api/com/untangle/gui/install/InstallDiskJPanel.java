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

package com.untangle.gui.install;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.table.*;

import com.untangle.gui.util.Localizable;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.MOneButtonJDialog;
import com.untangle.gui.widgets.wizard.MWizardPageJPanel;

public class InstallDiskJPanel
    extends MWizardPageJPanel
    implements Localizable
{

    private DiskListCompoundSettings diskListCompoundSettings;
    private InstallWizard installWizard;

    public InstallDiskJPanel(InstallWizard installWizard) {
        initComponents();
        this.installWizard = installWizard;
        diskListCompoundSettings = new DiskListCompoundSettings();
    }

    public void reloadStrings()
    {
        jLabel1.setText(Util.tr("<html>This list shows the boot disks that were found.  <font color='#FF0000'>Please choose a disk for installation.  Warning, all the data on the boot disk you select will be deleted.</font></html>"));
    }

    public boolean enteringForwards(){
        try{
            diskListCompoundSettings.refresh();
        }
        catch(Exception e){
            Util.handleExceptionNoRestart(Util.tr("Error scanning disks"), e);
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
            newButton.setText(diskInfo.getName() + " (size: " + diskInfo.getSize() + "GB)");
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
            MOneButtonJDialog dialog = MOneButtonJDialog.factory(InstallDiskJPanel.this.getTopLevelAncestor(), Util.tr("Installation Wizard"),
                                                                 Util.tr("You must select a boot disk which you would like to install onto."),
                                                                 Util.tr("Installation Wizard Message"), Util.tr("Message"));
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
        private float size;
        public DiskInfo(String name, float size){
            this.name = name;
            this.size = size;
        }
        public String getName(){ return name; }
        public float getSize(){ return size; }
    }

    class DiskJRadioButton extends JRadioButton {
        private String disk;
        public DiskJRadioButton(String disk){
            super();
            this.disk = disk;
        }
        public String getDisk(){ return disk; }
    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
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
        jLabel1.setText(Util.tr("<html>This list shows the boot disks that were found.  <font color='#FF0000'>Please choose a disk for installation.  Warning, all the data on the boot disk you select will be deleted.</font></html>"));
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

    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JPanel diskJPanel;
    private javax.swing.JScrollPane diskJScrollPane;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
