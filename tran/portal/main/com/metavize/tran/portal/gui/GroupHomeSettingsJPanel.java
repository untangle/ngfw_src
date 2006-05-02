/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;

import com.metavize.mvvm.portal.*;

import java.awt.*;


public class GroupHomeSettingsJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {
    
    private PortalGroup portalGroup;

    public GroupHomeSettingsJPanel(PortalGroup portalGroup) {
	this.portalGroup = portalGroup;
        initComponents();
    }
        
    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// ENABLED //
	boolean enabled = settingsEnabledJRadioButton.isSelected();
        
        // PAGE TITLE ///////////
	String pageTitle = pageTitleJTextField.getText();
        
        // PAGE TEXT //////
	String pageText = pageTextJTextField.getText();
        
        // BOOKMARKS TITLE ///////
	String bookmarksText = bookmarksJTextField.getText();

	// EXPLORER //
	boolean showExplorer = explorerJCheckBox.isSelected();

	// BOOKMARKS //
	boolean showBookmarks = bookmarksJCheckBox.isSelected();

	// ADD BOOKMARKS //
	boolean showAddBookmarks = addJCheckBox.isSelected();

        // SAVE THE VALUES ////////////////////////////////////
	if( !validateOnly ){
	    PortalHomeSettings portalHomeSettings = portalGroup.getPortalHomeSettings();
	    if( enabled ){
		if( portalHomeSettings == null ){
		    portalHomeSettings = new PortalHomeSettings();
		    portalGroup.setPortalHomeSettings( portalHomeSettings );
		}
		portalHomeSettings.setHomePageTitle( pageTitle );
		portalHomeSettings.setHomePageText( pageText );
		portalHomeSettings.setBookmarkTableTitle( bookmarksText );
		portalHomeSettings.setShowExploder( showExplorer );
		portalHomeSettings.setShowBookmarks( showBookmarks );
		portalHomeSettings.setShowAddBookmark( showAddBookmarks );
	    }
	    else{
		portalGroup.setPortalHomeSettings( null );
	    }
	}        
    }
    
    String pageTitleCurrent;
    String pageTextCurrent;
    String bookmarksTextCurrent;
    boolean showExplorerCurrent;
    boolean showBookmarksCurrent;
    boolean showAddBookmarksCurrent;

    public void doRefresh(Object settings) {
	PortalHomeSettings portalHomeSettings = portalGroup.getPortalHomeSettings();
	
	if( portalHomeSettings == null ){
	    settingsDisabledJRadioButton.setSelected(true);
	    setTextEnabledDependency(false);
	    return;
	}
	else{
	    settingsEnabledJRadioButton.setSelected(true);
	    setTextEnabledDependency(true);
	}

        // PAGE TITLE ///////////
	pageTitleCurrent = portalHomeSettings.getHomePageTitle();
	pageTitleJTextField.setText( pageTitleCurrent );
        
        // PAGE TEXT //////
	pageTextCurrent = portalHomeSettings.getHomePageText();
	pageTextJTextField.setText( pageTextCurrent );
        
        // BOOKMARKS TITLE ///////
	bookmarksTextCurrent = portalHomeSettings.getBookmarkTableTitle();
	bookmarksJTextField.setText( bookmarksTextCurrent );

	// EXPLORER //
	showExplorerCurrent = portalHomeSettings.isShowExploder();
	explorerJCheckBox.setSelected( showExplorerCurrent );

	// BOOKMARKS //
	showBookmarksCurrent = portalHomeSettings.isShowBookmarks();
	bookmarksJCheckBox.setSelected( showBookmarksCurrent );

	// ADD BOOKMARKS //
	showAddBookmarksCurrent = portalHomeSettings.isShowAddBookmark();
	addJCheckBox.setSelected( showAddBookmarksCurrent );
        
    }

        
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                enabledButtonGroup = new javax.swing.ButtonGroup();
                explanationJPanel1 = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                settingsEnabledJRadioButton = new javax.swing.JRadioButton();
                settingsDisabledJRadioButton = new javax.swing.JRadioButton();
                explanationJPanel = new javax.swing.JPanel();
                jTextArea2 = new javax.swing.JTextArea();
                restrictIPJPanel2 = new javax.swing.JPanel();
                pageTitleJLabel = new javax.swing.JLabel();
                pageTitleJTextField = new javax.swing.JTextField();
                pageTextJLabel = new javax.swing.JLabel();
                pageTextJTextField = new javax.swing.JTextField();
                bookmarksTitleJLabel = new javax.swing.JLabel();
                bookmarksJTextField = new javax.swing.JTextField();
                externalRemoteJPanel = new javax.swing.JPanel();
                jTextArea3 = new javax.swing.JTextArea();
                restrictIPJPanel = new javax.swing.JPanel();
                explorerJLabel = new javax.swing.JLabel();
                userBookmarksJLabel = new javax.swing.JLabel();
                addUserBookmarksJLabel = new javax.swing.JLabel();
                explorerJCheckBox = new javax.swing.JCheckBox();
                bookmarksJCheckBox = new javax.swing.JCheckBox();
                addJCheckBox = new javax.swing.JCheckBox();

                setLayout(new java.awt.GridBagLayout());

                setMinimumSize(new java.awt.Dimension(530, 376));
                setPreferredSize(new java.awt.Dimension(530, 376));
                explanationJPanel1.setLayout(new java.awt.GridBagLayout());

                explanationJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Override", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jPanel1.setLayout(new java.awt.GridBagLayout());

                enabledButtonGroup.add(settingsEnabledJRadioButton);
                settingsEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                settingsEnabledJRadioButton.setText("Use Settings Below");
                settingsEnabledJRadioButton.setFocusPainted(false);
                settingsEnabledJRadioButton.setFocusable(false);
                settingsEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                settingsEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(settingsEnabledJRadioButton, gridBagConstraints);

                enabledButtonGroup.add(settingsDisabledJRadioButton);
                settingsDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                settingsDisabledJRadioButton.setText("Use Global Settings");
                settingsDisabledJRadioButton.setFocusPainted(false);
                settingsDisabledJRadioButton.setFocusable(false);
                settingsDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                settingsDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(settingsDisabledJRadioButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                explanationJPanel1.add(jPanel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel1, gridBagConstraints);

                explanationJPanel.setLayout(new java.awt.GridBagLayout());

                explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Text", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea2.setEditable(false);
                jTextArea2.setLineWrap(true);
                jTextArea2.setText("This is the text that will appear on the user's home page.");
                jTextArea2.setWrapStyleWord(true);
                jTextArea2.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                explanationJPanel.add(jTextArea2, gridBagConstraints);

                restrictIPJPanel2.setLayout(new java.awt.GridBagLayout());

                pageTitleJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                pageTitleJLabel.setText("Page Title: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel2.add(pageTitleJLabel, gridBagConstraints);

                pageTitleJTextField.setMaximumSize(new java.awt.Dimension(300, 19));
                pageTitleJTextField.setMinimumSize(new java.awt.Dimension(300, 19));
                pageTitleJTextField.setPreferredSize(new java.awt.Dimension(300, 19));
                pageTitleJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                pageTitleJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel2.add(pageTitleJTextField, gridBagConstraints);

                pageTextJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                pageTextJLabel.setText("Page Text: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel2.add(pageTextJLabel, gridBagConstraints);

                pageTextJTextField.setMaximumSize(new java.awt.Dimension(300, 19));
                pageTextJTextField.setMinimumSize(new java.awt.Dimension(300, 19));
                pageTextJTextField.setPreferredSize(new java.awt.Dimension(300, 19));
                pageTextJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                pageTextJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel2.add(pageTextJTextField, gridBagConstraints);

                bookmarksTitleJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                bookmarksTitleJLabel.setText("Bookmarks Title: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel2.add(bookmarksTitleJLabel, gridBagConstraints);

                bookmarksJTextField.setMaximumSize(new java.awt.Dimension(300, 19));
                bookmarksJTextField.setMinimumSize(new java.awt.Dimension(300, 19));
                bookmarksJTextField.setPreferredSize(new java.awt.Dimension(300, 19));
                bookmarksJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                bookmarksJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel2.add(bookmarksJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 5, 0);
                explanationJPanel.add(restrictIPJPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel, gridBagConstraints);

                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Features", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea3.setEditable(false);
                jTextArea3.setLineWrap(true);
                jTextArea3.setText("These are the features that available to the user on the home page.");
                jTextArea3.setWrapStyleWord(true);
                jTextArea3.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                externalRemoteJPanel.add(jTextArea3, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                explorerJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                explorerJLabel.setText("Run File Explorer: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(explorerJLabel, gridBagConstraints);

                userBookmarksJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                userBookmarksJLabel.setText("Run User Bookmarks: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(userBookmarksJLabel, gridBagConstraints);

                addUserBookmarksJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addUserBookmarksJLabel.setText("Add User Bookmarks: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(addUserBookmarksJLabel, gridBagConstraints);

                explorerJCheckBox.setFocusable(false);
                explorerJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                explorerJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel.add(explorerJCheckBox, gridBagConstraints);

                bookmarksJCheckBox.setFocusable(false);
                bookmarksJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                bookmarksJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel.add(bookmarksJCheckBox, gridBagConstraints);

                addJCheckBox.setFocusable(false);
                addJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                addJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel.add(addJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(externalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void addJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_addJCheckBoxActionPerformed
    
    private void bookmarksJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookmarksJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_bookmarksJCheckBoxActionPerformed
    
    private void explorerJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_explorerJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_explorerJCheckBoxActionPerformed
    
    private void bookmarksJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_bookmarksJTextFieldCaretUpdate
	if( !bookmarksJTextField.getText().trim().equals(bookmarksTextCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_bookmarksJTextFieldCaretUpdate
    
    private void pageTextJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_pageTextJTextFieldCaretUpdate
	if( !pageTextJTextField.getText().trim().equals(pageTextCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_pageTextJTextFieldCaretUpdate
    
    private void pageTitleJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_pageTitleJTextFieldCaretUpdate
	if( !pageTitleJTextField.getText().trim().equals(pageTitleCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_pageTitleJTextFieldCaretUpdate
    
    private void settingsEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsEnabledJRadioButtonActionPerformed
	setTextEnabledDependency(true);
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_settingsEnabledJRadioButtonActionPerformed
    
    private void settingsDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsDisabledJRadioButtonActionPerformed
	setTextEnabledDependency(false);
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_settingsDisabledJRadioButtonActionPerformed
    
    
    private void setTextEnabledDependency(boolean enabled){
	pageTitleJLabel.setEnabled( enabled );
	pageTitleJTextField.setEnabled( enabled );
	pageTextJLabel.setEnabled( enabled );
	pageTextJTextField.setEnabled( enabled );
	bookmarksTitleJLabel.setEnabled( enabled );
	bookmarksJTextField.setEnabled( enabled );

	explorerJLabel.setEnabled( enabled );
	explorerJCheckBox.setEnabled( enabled );
	userBookmarksJLabel.setEnabled( enabled );
	bookmarksJCheckBox.setEnabled( enabled );
	addUserBookmarksJLabel.setEnabled( enabled );
	addJCheckBox.setEnabled( enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JCheckBox addJCheckBox;
        private javax.swing.JLabel addUserBookmarksJLabel;
        private javax.swing.JCheckBox bookmarksJCheckBox;
        public javax.swing.JTextField bookmarksJTextField;
        private javax.swing.JLabel bookmarksTitleJLabel;
        private javax.swing.ButtonGroup enabledButtonGroup;
        private javax.swing.JPanel explanationJPanel;
        private javax.swing.JPanel explanationJPanel1;
        private javax.swing.JCheckBox explorerJCheckBox;
        private javax.swing.JLabel explorerJLabel;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextArea jTextArea3;
        private javax.swing.JLabel pageTextJLabel;
        public javax.swing.JTextField pageTextJTextField;
        private javax.swing.JLabel pageTitleJLabel;
        public javax.swing.JTextField pageTitleJTextField;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel2;
        public javax.swing.JRadioButton settingsDisabledJRadioButton;
        public javax.swing.JRadioButton settingsEnabledJRadioButton;
        private javax.swing.JLabel userBookmarksJLabel;
        // End of variables declaration//GEN-END:variables
    
}
