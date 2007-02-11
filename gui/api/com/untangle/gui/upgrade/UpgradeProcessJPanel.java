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

package com.untangle.gui.upgrade;

import com.untangle.mvvm.toolbox.*;
import com.untangle.mvvm.policy.Policy;
import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.MEditTableJPanel;
import com.untangle.gui.widgets.editTable.MSortedTableModel;
import com.untangle.gui.util.Util;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.*;
import java.util.*;
import java.awt.Insets;

public class UpgradeProcessJPanel extends JPanel
    implements Refreshable<UpgradeCompoundSettings> {

    private static final int DOWNLOAD_SLEEP_MILLIS = 1000;
    private static final int DOWNLOAD_FINAL_SLEEP_MILLIS = 3000;
    		
    private UpgradeTableModel upgradeTableModel;
    private MEditTableJPanel mEditTableJPanel;

    public UpgradeProcessJPanel() {
	// UPGRADE TABLE //
        mEditTableJPanel = new MEditTableJPanel(false, true);
        mEditTableJPanel.setInsets(new Insets(0,0,0,0));
        mEditTableJPanel.setTableTitle("Available Upgrades");
        mEditTableJPanel.setDetailsTitle("Upgrade Details");
        mEditTableJPanel.setAddRemoveEnabled(false);
        upgradeTableModel = new UpgradeTableModel();
        mEditTableJPanel.setTableModel( upgradeTableModel );
        mEditTableJPanel.getJTable().setRowHeight(49);

	initComponents();
    }
    

    public void doRefresh(UpgradeCompoundSettings upgradeCompoundSettings){
	upgradeTableModel.doRefresh(upgradeCompoundSettings);
	if( Util.isArrayEmpty(upgradeCompoundSettings.getUpgradableMackageDescs()) ){
	    upgradeJButton.setEnabled(false);
	}
	else{
	    upgradeJButton.setEnabled(true);
	}
    }


    class UpgradeTableModel extends MSortedTableModel<UpgradeCompoundSettings> {
	
	public TableColumnModel getTableColumnModel(){	    
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #  min  rsz    edit   remv   desc   typ            def
	    addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
	    addTableColumn( tableColumnModel,  1,  49, false, false, false, false, ImageIcon.class, null, "");
	    addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class, null, "name");
	    addTableColumn( tableColumnModel,  3,  75, false, false, false, false, String.class, null, sc.html("new<br>version"));
	    addTableColumn( tableColumnModel,  4, 125, false, false, false, false, String.class, null, "type");
	    addTableColumn( tableColumnModel,  5,  70, true,  false, false, false, Integer.class, null, sc.html("size<br>(KB)"));
	    addTableColumn( tableColumnModel,  6, 125, false, false, true,  true,  String.class, null, "description");	    
	    return tableColumnModel;
	}
	
	public void generateSettings(UpgradeCompoundSettings upgradeCompoundSettings,
				     Vector<Vector> tableVector, boolean validateOnly) throws Exception { }
	
	public Vector<Vector> generateRows(UpgradeCompoundSettings upgradeCompoundSettings){
	    MackageDesc[] mackageDescs = upgradeCompoundSettings.getUpgradableMackageDescs();

	    if( mackageDescs == null ) 	// deal with the case of an unreachable store
		return new Vector<Vector>();

	    Vector<Vector> allRows = new Vector<Vector>(mackageDescs.length);
	    Vector tempRow = null;
	    int rowIndex = 0;
	    
	    for( MackageDesc mackageDesc : mackageDescs ){
		if( mackageDesc.getType() == MackageDesc.CASING_TYPE ||
		    mackageDesc.getType() == MackageDesc.TRANSFORM_BASE_TYPE)
		    continue;
		try{
		    rowIndex++;
		    tempRow = new Vector(7);
		    tempRow.add( rowIndex );
		    
		    byte[] descIcon = mackageDesc.getDescIcon();
		    
		    if( descIcon != null)
			tempRow.add( new ImageIcon(descIcon) );
		    else
			tempRow.add( new ImageIcon(getClass().getResource("/com/untangle/gui/transform/IconDescUnknown42x42.png"))) ;
		    
		    tempRow.add( mackageDesc.getDisplayName() );
		    tempRow.add( mackageDesc.getAvailableVersion() );
		    if( mackageDesc.getType() == MackageDesc.SYSTEM_TYPE )
			tempRow.add( "System Component" );
		    else if( mackageDesc.getType() == MackageDesc.TRANSFORM_TYPE )
			tempRow.add( "Product" );
		    else
			tempRow.add( "Unknown" );
		    tempRow.add( Integer.toString(mackageDesc.getSize()/1000));
		    tempRow.add( mackageDesc.getLongDescription() );
		    allRows.add( tempRow );
		}
		catch(Exception e){
		    Util.handleExceptionNoRestart("Error adding upgrade row", e);
		}
	    }
	    return allRows;
	}
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                contentJPanel = mEditTableJPanel;
                actionJPanel = new javax.swing.JPanel();
                upgradeJButton = new javax.swing.JButton();

                setLayout(new java.awt.GridBagLayout());

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                add(contentJPanel, gridBagConstraints);

                actionJPanel.setLayout(new java.awt.GridBagLayout());

                upgradeJButton.setText("Upgrade");
                upgradeJButton.setDoubleBuffered(true);
                upgradeJButton.setFocusPainted(false);
                upgradeJButton.setFocusable(false);
                upgradeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                upgradeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                upgradeJButton.setMaximumSize(new java.awt.Dimension(100, 25));
                upgradeJButton.setMinimumSize(new java.awt.Dimension(100, 25));
                upgradeJButton.setPreferredSize(new java.awt.Dimension(100, 25));
                upgradeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                upgradeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                actionJPanel.add(upgradeJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
                add(actionJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void upgradeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	ProceedJDialog proceedJDialog = new ProceedJDialog();
	if( !proceedJDialog.isUpgrading() )
	    return;
	new PerformUpgradeThread();
    }//GEN-LAST:event_upgradeJButtonActionPerformed
		
    private class PerformUpgradeThread extends Thread {
        public PerformUpgradeThread(){
            super("MVCLIENT-PerformUpgradeThread");
            setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
            ((MConfigJDialog)UpgradeProcessJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().setProgressBarVisible(true);
            ((MConfigJDialog)UpgradeProcessJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().start("Downloading updates...");
            this.start();
        }
        public void run() {
            try{
                // DISABLE ALL GRAPHS SO NO EXCEPTIONS ARE CAUSED
                Util.getPolicyStateMachine().stopAllGraphs();
                Util.getStatsCache().doShutdown();

                // DO THE DOWNLOAD AND INSTALL
                long key = Util.getToolboxManager().upgrade();
                com.untangle.gui.util.Visitor visitor =
                    new com.untangle.gui.util.Visitor(((MConfigJDialog)UpgradeProcessJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().getProgressBar());
                while (true) {
                    java.util.List<InstallProgress> lip = Util.getToolboxManager().getProgress(key);
                    for (InstallProgress ip : lip) {
                        ip.accept(visitor);
                        if( visitor.isDone() )
                            break;
                    }
                    if( visitor.isDone() )
                        break;
                    if (0 == lip.size()) {
                        Thread.currentThread().sleep(DOWNLOAD_SLEEP_MILLIS);
                    }
                }

                if( visitor.isSuccessful() ){
                    // LET THE USER KNOW WERE FINISHED NORMALLY
                    ((MConfigJDialog)UpgradeProcessJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().setTextLater("Download Complete!");
                    MOneButtonJDialog.factory(UpgradeProcessJPanel.this.getTopLevelAncestor(), "",
					      "The updates have successfully downloaded.  The client will now exit while the upgrade is performed.",
					      "Upgrade Success", "");
                }
                else{
                    throw new Exception();
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Termination of upgrade:", e);
                ((MConfigJDialog)UpgradeProcessJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().setTextLater("Upgrade Failure");
                MOneButtonJDialog.factory(UpgradeProcessJPanel.this.getTopLevelAncestor(), "",
					  "The upgrade procedure did not finish properly.  " +
					  "Please contact Untangle Support.",
					  "Upgrade Failure Warning", "");
            }
            finally{
                RestartDialog.factory(UpgradeProcessJPanel.this.getTopLevelAncestor());
            }
        }
    }
		
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel actionJPanel;
        private javax.swing.JPanel contentJPanel;
        protected javax.swing.JButton upgradeJButton;
        // End of variables declaration//GEN-END:variables
		
}
