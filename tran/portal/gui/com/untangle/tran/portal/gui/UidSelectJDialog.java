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

package com.untangle.tran.portal.gui;

import com.untangle.gui.util.*;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.addrbook.UserEntry;
import com.untangle.mvvm.addrbook.RepositoryType;
import com.untangle.gui.configuration.DirectoryCompoundSettings;
import com.untangle.gui.configuration.DirectoryJDialog;

import java.util.TreeMap;
import java.util.List;
import java.util.Vector;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;
import javax.swing.SwingUtilities;

public class UidSelectJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private static InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();

    private static final String PLEASE_SELECT_USER = "Please select...";

    private String uid;
    private boolean isProceeding;

    public static UidSelectJDialog factory(Container topLevelContainer){
	UidSelectJDialog uidSelectJDialog;
	if(topLevelContainer instanceof Frame)
	    uidSelectJDialog = new UidSelectJDialog((Frame)topLevelContainer);
	else
	    uidSelectJDialog = new UidSelectJDialog((Dialog)topLevelContainer);
	return uidSelectJDialog;
    }
    
    public UidSelectJDialog(Dialog topLevelDialog) {
        super( topLevelDialog, true);
	init( topLevelDialog );
	
    }
    
    public UidSelectJDialog(Frame topLevelFrame) {
        super( topLevelFrame, true);
	init( topLevelFrame );
    }
    
    private void init(Window topLevelWindow) {
        initComponents();
        this.addWindowListener(this);
		pack();
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
	setGlassPane(infiniteProgressJComponent);
	new PerformRefreshThread();
    }

    public String getUid(){ return uid; }

    public void updateUidModel(List<UserEntry> userEntries){
	uidJComboBox.removeAllItems();
	uidJComboBox.addItem(PLEASE_SELECT_USER);
	uidJComboBox.setSelectedItem(PLEASE_SELECT_USER);
	TreeMap<UserEntryWrapper,Object> treeMap = new TreeMap<UserEntryWrapper,Object>();
	for( UserEntry userEntry : userEntries )
	    treeMap.put(new UserEntryWrapper(userEntry), null);
	for( UserEntryWrapper userEntryWrapper : treeMap.keySet() )		
	    uidJComboBox.addItem( userEntryWrapper );
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                iconJLabel = new javax.swing.JLabel();
                dividerJPanel = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                uidJComboBox = new javax.swing.JComboBox();
                jLabel2 = new javax.swing.JLabel();
                jButton1 = new javax.swing.JButton();
                jPanel2 = new javax.swing.JPanel();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Portal Question");
                setModal(true);
                setResizable(false);
                iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogQuestion_96x96.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
                getContentPane().add(iconJLabel, gridBagConstraints);

                dividerJPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(154, 154, 154)));
                dividerJPanel.setMaximumSize(new java.awt.Dimension(1, 1600));
                dividerJPanel.setMinimumSize(new java.awt.Dimension(1, 10));
                dividerJPanel.setOpaque(false);
                dividerJPanel.setPreferredSize(new java.awt.Dimension(1, 10));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
                getContentPane().add(dividerJPanel, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setMaximumSize(null);
                jPanel1.setOpaque(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Select a user ID/Login:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(labelJLabel, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html><center>\nYou may choose a user ID/Login that exists in the User Directory<br>\n(either local LDAP or remote Active Directory), or you can add a new<br>\nuser to the User Directory, and then choose that user.\n</center></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(messageJLabel, gridBagConstraints);

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Select an existing user:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                jPanel1.add(jLabel1, gridBagConstraints);

                uidJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
                uidJComboBox.setFocusable(false);
                uidJComboBox.setMaximumSize(new java.awt.Dimension(250, 24));
                uidJComboBox.setMinimumSize(new java.awt.Dimension(250, 24));
                uidJComboBox.setPreferredSize(new java.awt.Dimension(250, 24));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(uidJComboBox, gridBagConstraints);

                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add a new user:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                jPanel1.add(jLabel2, gridBagConstraints);

                jButton1.setFont(new java.awt.Font("Dialog", 0, 12));
                jButton1.setText("Open User Directory");
                jButton1.setFocusable(false);
                jButton1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton1ActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(jButton1, gridBagConstraints);

                jPanel2.setLayout(new java.awt.GridBagLayout());

                jPanel2.setMaximumSize(null);
                jPanel2.setMinimumSize(null);
                jPanel2.setOpaque(false);
                jPanel2.setPreferredSize(null);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setText("<html><b>Cancel</b></html>");
                cancelJButton.setFocusable(false);
                cancelJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                cancelJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                cancelJButton.setMaximumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setMinimumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setPreferredSize(new java.awt.Dimension(130, 25));
                cancelJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                cancelJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                jPanel2.add(cancelJButton, gridBagConstraints);

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setText("<html><b>Proceed</b></html>");
                proceedJButton.setFocusPainted(false);
                proceedJButton.setFocusable(false);
                proceedJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                proceedJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                proceedJButton.setMaximumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setMinimumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setPreferredSize(new java.awt.Dimension(150, 25));
                proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                proceedJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                jPanel2.add(proceedJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 6;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(jPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
                getContentPane().add(jPanel1, gridBagConstraints);

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

        }//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
	try{
	    DirectoryJDialog directoryJDialog = new DirectoryJDialog((Dialog)this);
	    directoryJDialog.setVisible(true);
	    DirectoryCompoundSettings directoryCompoundSettings = (DirectoryCompoundSettings) directoryJDialog.getCompoundSettings();
	    new PerformRefreshThread();
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing directory", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing directory", f); }
	}
    }//GEN-LAST:event_jButton1ActionPerformed
    
    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
	Object selectedObject = uidJComboBox.getSelectedItem();
	if( selectedObject.equals(PLEASE_SELECT_USER) ){
	    MOneButtonJDialog.factory(UidSelectJDialog.this, "Portal", "Please choose a user id/login or press Cancel.",
				      "Portal Warning", "Portal Message");
	    return;
	}
	isProceeding = true;
	uid = ((UserEntryWrapper) selectedObject).getUserEntry().getUID();
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed
    
    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
	isProceeding = false;
	uid = null;
        windowClosing(null);
    }//GEN-LAST:event_cancelJButtonActionPerformed
    
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	if( !isProceeding )
	    uid = null;
        this.setVisible(false);
        dispose();
    }    


    class PerformRefreshThread extends Thread {
	public PerformRefreshThread(){
	    super("MVCLIENT-PerformRefreshThread");
	    setDaemon(true);
	    infiniteProgressJComponent.start("Refreshing...");
	    start();
	}
	public void run(){
	    try{
            final List<UserEntry> allUserEntries = new Vector<UserEntry>();
            try{
                allUserEntries.addAll( Util.getAddressBook().getUserEntries(RepositoryType.MS_ACTIVE_DIRECTORY) );
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error doing refresh", f);
                MOneButtonJDialog.factory(UidSelectJDialog.this, "Portal", "There was a problem refreshing Active Directory users.  Please check your Active Directory settings and then try again.",
                                          "Portal Warning", "Portal Warning");
            }

            try{
                allUserEntries.addAll( Util.getAddressBook().getUserEntries(RepositoryType.LOCAL_DIRECTORY) );
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error doing refresh", f);
                MOneButtonJDialog.factory(UidSelectJDialog.this, "Portal", "There was a problem refreshing LDAP Directory users.  Please check your LDAP Directory settings and try again.",
                                          "Portal Warning", "Portal Warning");
            }
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                updateUidModel(allUserEntries);
            }});
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error doing refresh", e); }
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing refresh", f);
		    MOneButtonJDialog.factory(UidSelectJDialog.this, "Portal", "There was a problem refreshing.  Please try again.",
					      "Portal Warning", "Portal Warning");
		}
	    }
	    infiniteProgressJComponent.stopLater(1000l);
	}
    }

    class UserEntryWrapper implements Comparable<UserEntryWrapper>{
	private UserEntry userEntry;
	public UserEntryWrapper(UserEntry userEntry){
	    this.userEntry = userEntry;
	}
	public String toString(){
	    String repository = "UNKNOWN";
	    switch(userEntry.getStoredIn()){
	    case MS_ACTIVE_DIRECTORY : repository = "Active Directory";
		break;
	    case LOCAL_DIRECTORY : repository = "LDAP";
		break;
	    case NONE : repository = "UNKNOWN";
	    default :;
		break;
	    }
	    return userEntry.getUID() + " (" + repository + ")";
	}
	public UserEntry getUserEntry(){
	    return userEntry;
	}
	public boolean equals(Object obj){
	    if( ! (obj instanceof UserEntryWrapper) )
		return false;
	    UserEntry other = ((UserEntryWrapper) obj).getUserEntry();
	    return getUserEntry().equals(other);
	}
	public int compareTo(UserEntryWrapper userEntryWrapper){
	    switch(userEntry.getStoredIn().compareTo(userEntryWrapper.userEntry.getStoredIn())){
	    case -1 :
		return -1;
	    case 1 :
		return 1;
	    default:
	    case 0 :
		return userEntry.getUID().compareTo(userEntryWrapper.userEntry.getUID());
	    }
	}
    }

    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.ButtonGroup buttonGroup1;
        protected javax.swing.JButton cancelJButton;
        private javax.swing.JPanel dividerJPanel;
        private javax.swing.JLabel iconJLabel;
        private javax.swing.JButton jButton1;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        private javax.swing.JComboBox uidJComboBox;
        // End of variables declaration//GEN-END:variables
    
}
