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

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.*;

import com.untangle.mvvm.portal.*;

import java.awt.*;


public class GlobalLoginSettingsJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {
    

    public GlobalLoginSettingsJPanel() {
        initComponents();
    }
        
    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        // PAGE TITLE ///////////
	String pageTitle = pageTitleJTextField.getText();
        
        // PAGE TEXT //////
	String pageText = pageTextJTextField.getText();
        
	// AUTO CREATE NEW USERS //
	boolean autoCreate = autoJCheckBox.isSelected();

        // SAVE THE VALUES ////////////////////////////////////
	if( !validateOnly ){
	    PortalGlobal portalGlobal = ((PortalSettings)settings).getGlobal();
	    portalGlobal.setLoginPageTitle( pageTitle );
	    portalGlobal.setLoginPageText( pageText );
	    portalGlobal.setAutoCreateUsers( autoCreate );
	}        
    }
    
    String pageTitleCurrent;
    String pageTextCurrent;
    boolean autoCreateCurrent;

    public void doRefresh(Object settings) {
	PortalGlobal portalGlobal = ((PortalSettings)settings).getGlobal();

        // PAGE TITLE ///////////
	pageTitleCurrent = portalGlobal.getLoginPageTitle();
	pageTitleJTextField.setText( pageTitleCurrent );
        
        // PAGE TEXT //////
	pageTextCurrent = portalGlobal.getLoginPageText();
	pageTextJTextField.setText( pageTextCurrent );
        
	// AUTO CREATE USERS //
	autoCreateCurrent = portalGlobal.isAutoCreateUsers();
	autoJCheckBox.setSelected( autoCreateCurrent );

    }

        
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                enabledButtonGroup = new javax.swing.ButtonGroup();
                explanationJPanel = new javax.swing.JPanel();
                jTextArea2 = new javax.swing.JTextArea();
                restrictIPJPanel2 = new javax.swing.JPanel();
                pageTitleJLabel = new javax.swing.JLabel();
                pageTitleJTextField = new javax.swing.JTextField();
                pageTextJLabel = new javax.swing.JLabel();
                pageTextJTextField = new javax.swing.JTextField();
                externalRemoteJPanel = new javax.swing.JPanel();
                jTextArea3 = new javax.swing.JTextArea();
                restrictIPJPanel = new javax.swing.JPanel();
                explorerJLabel = new javax.swing.JLabel();
                autoJCheckBox = new javax.swing.JCheckBox();

                setLayout(new java.awt.GridBagLayout());

                setMinimumSize(new java.awt.Dimension(530, 224));
                setPreferredSize(new java.awt.Dimension(530, 224));
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
                explorerJLabel.setText("Automatically Create New Users From Directory: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(explorerJLabel, gridBagConstraints);

                autoJCheckBox.setFocusable(false);
                autoJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                autoJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel.add(autoJCheckBox, gridBagConstraints);

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
        
    private void autoJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_autoJCheckBoxActionPerformed
        
    private void pageTextJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_pageTextJTextFieldCaretUpdate
	if( !pageTextJTextField.getText().trim().equals(pageTextCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_pageTextJTextFieldCaretUpdate
    
    private void pageTitleJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_pageTitleJTextFieldCaretUpdate
	if( !pageTitleJTextField.getText().trim().equals(pageTitleCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_pageTitleJTextFieldCaretUpdate
            
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JCheckBox autoJCheckBox;
        private javax.swing.ButtonGroup enabledButtonGroup;
        private javax.swing.JPanel explanationJPanel;
        private javax.swing.JLabel explorerJLabel;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextArea jTextArea3;
        private javax.swing.JLabel pageTextJLabel;
        public javax.swing.JTextField pageTextJTextField;
        private javax.swing.JLabel pageTitleJLabel;
        public javax.swing.JTextField pageTitleJTextField;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel2;
        // End of variables declaration//GEN-END:variables
    
}
