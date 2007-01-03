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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


public class GlobalHomeSettingsJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_TIMEOUT_RANGE = "You must choose a timeout between " + 
	(int)(PortalHomeSettings.IDLE_TIMEOUT_MIN)/60000 + " and " + (int)(PortalHomeSettings.IDLE_TIMEOUT_MAX)/60000;
    
    public GlobalHomeSettingsJPanel() {
        initComponents();
	timeoutJSpinner.setModel(new SpinnerNumberModel((int)(PortalHomeSettings.IDLE_TIMEOUT_DEFAULT/60000l),
							(int)(PortalHomeSettings.IDLE_TIMEOUT_MIN/60000l),
							(int)(PortalHomeSettings.IDLE_TIMEOUT_MAX/60000l), 1));
    }
        
    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// LOGIN TITLE //
	String loginTitle = loginTitleJTextField.getText();

	// LOGIN TEXT //
	String loginText = loginTextJTextField.getText();

        // AUTO ADD USER //
	boolean autoCreate = autoCreateJCheckBox.isSelected();

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

	// TIMEOUT //
	((JSpinner.DefaultEditor)timeoutJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	int timeout = 0;
	try{ timeoutJSpinner.commitEdit(); }
	catch(Exception e){ 
	    ((JSpinner.DefaultEditor)timeoutJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
	    throw new Exception(EXCEPTION_TIMEOUT_RANGE);
	}
        timeout = (Integer) timeoutJSpinner.getValue();

        // SAVE THE VALUES ////////////////////////////////////
	if( !validateOnly ){
	    PortalGlobal portalGlobal = ((PortalSettings)settings).getGlobal();
	    portalGlobal.setLoginPageTitle( loginTitle );
	    portalGlobal.setLoginPageText( loginText );
	    portalGlobal.setAutoCreateUsers( autoCreate );
	    PortalHomeSettings portalHomeSettings = portalGlobal.getPortalHomeSettings();
	    portalHomeSettings.setHomePageTitle( pageTitle );
	    portalHomeSettings.setHomePageText( pageText );
	    portalHomeSettings.setBookmarkTableTitle( bookmarksText );
	    portalHomeSettings.setShowExploder( showExplorer );
	    portalHomeSettings.setShowBookmarks( showBookmarks );
	    portalHomeSettings.setShowAddBookmark( showAddBookmarks );
	    portalHomeSettings.setIdleTimeout( ((long)timeout)*60000l );
	}        
    }
    
    String loginTitleCurrent;
    String loginTextCurrent;
    boolean autoCreateCurrent;
    String pageTitleCurrent;
    String pageTextCurrent;
    String bookmarksTextCurrent;
    boolean showExplorerCurrent;
    boolean showBookmarksCurrent;
    boolean showAddBookmarksCurrent;
    int timeoutCurrent;

    public void doRefresh(Object settings) {
	PortalGlobal portalGlobal = ((PortalSettings)settings).getGlobal();
	PortalHomeSettings portalHomeSettings = portalGlobal.getPortalHomeSettings();

	// LOGIN TITLE //
	loginTitleCurrent = portalGlobal.getLoginPageTitle();
	loginTitleJTextField.setText( loginTitleCurrent );

	// LOGIN TEXT //
	loginTextCurrent = portalGlobal.getLoginPageText();
	loginTextJTextField.setText( loginTextCurrent );

        // AUTO ADD USER //
	autoCreateCurrent = portalGlobal.isAutoCreateUsers();
	autoCreateJCheckBox.setSelected( autoCreateCurrent );

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
	addJCheckBox.setEnabled(showBookmarksCurrent);
	addUserBookmarksJLabel.setEnabled(showBookmarksCurrent);
        
	// TIMEOUT //
	timeoutCurrent = (int)(portalHomeSettings.getIdleTimeout()/60000l);
	timeoutJSpinner.setValue( timeoutCurrent );
	((JSpinner.DefaultEditor)timeoutJSpinner.getEditor()).getTextField().setText(Integer.toString(timeoutCurrent));
	((JSpinner.DefaultEditor)timeoutJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

    }

        
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                enabledButtonGroup = new javax.swing.ButtonGroup();
                explanationJPanel1 = new javax.swing.JPanel();
                jTextArea5 = new javax.swing.JTextArea();
                restrictIPJPanel3 = new javax.swing.JPanel();
                pageTitleJLabel1 = new javax.swing.JLabel();
                loginTitleJTextField = new javax.swing.JTextField();
                pageTextJLabel1 = new javax.swing.JLabel();
                loginTextJTextField = new javax.swing.JTextField();
                externalRemoteJPanel2 = new javax.swing.JPanel();
                jTextArea6 = new javax.swing.JTextArea();
                restrictIPJPanel4 = new javax.swing.JPanel();
                explorerJLabel1 = new javax.swing.JLabel();
                autoCreateJCheckBox = new javax.swing.JCheckBox();
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
                externalRemoteJPanel1 = new javax.swing.JPanel();
                jTextArea4 = new javax.swing.JTextArea();
                restrictIPJPanel1 = new javax.swing.JPanel();
                timeoutJLabel = new javax.swing.JLabel();
                timeoutJSpinner = new javax.swing.JSpinner();
                timeoutJLabel1 = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setMinimumSize(new java.awt.Dimension(515, 598));
                setPreferredSize(new java.awt.Dimension(515, 598));
                explanationJPanel1.setLayout(new java.awt.GridBagLayout());

                explanationJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Login Page Text", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea5.setEditable(false);
                jTextArea5.setLineWrap(true);
                jTextArea5.setText("This text will appear on the portal login page.");
                jTextArea5.setWrapStyleWord(true);
                jTextArea5.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                explanationJPanel1.add(jTextArea5, gridBagConstraints);

                restrictIPJPanel3.setLayout(new java.awt.GridBagLayout());

                pageTitleJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                pageTitleJLabel1.setText("Page Title: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel3.add(pageTitleJLabel1, gridBagConstraints);

                loginTitleJTextField.setMaximumSize(new java.awt.Dimension(300, 19));
                loginTitleJTextField.setMinimumSize(new java.awt.Dimension(300, 19));
                loginTitleJTextField.setPreferredSize(new java.awt.Dimension(300, 19));
                loginTitleJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginTitleJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel3.add(loginTitleJTextField, gridBagConstraints);

                pageTextJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                pageTextJLabel1.setText("Page Text: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel3.add(pageTextJLabel1, gridBagConstraints);

                loginTextJTextField.setMaximumSize(new java.awt.Dimension(300, 19));
                loginTextJTextField.setMinimumSize(new java.awt.Dimension(300, 19));
                loginTextJTextField.setPreferredSize(new java.awt.Dimension(300, 19));
                loginTextJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginTextJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel3.add(loginTextJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 5, 0);
                explanationJPanel1.add(restrictIPJPanel3, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel1, gridBagConstraints);

                externalRemoteJPanel2.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Login Page Features", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea6.setEditable(false);
                jTextArea6.setLineWrap(true);
                jTextArea6.setText("These features will be available at the login page.");
                jTextArea6.setWrapStyleWord(true);
                jTextArea6.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                externalRemoteJPanel2.add(jTextArea6, gridBagConstraints);

                restrictIPJPanel4.setLayout(new java.awt.GridBagLayout());

                explorerJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                explorerJLabel1.setText("Create Accounts On Demand From User Directory: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel4.add(explorerJLabel1, gridBagConstraints);

                autoCreateJCheckBox.setFocusable(false);
                autoCreateJCheckBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                autoCreateJCheckBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel4.add(autoCreateJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                externalRemoteJPanel2.add(restrictIPJPanel4, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel2, gridBagConstraints);

                explanationJPanel.setLayout(new java.awt.GridBagLayout());

                explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Home Page Text", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea2.setEditable(false);
                jTextArea2.setLineWrap(true);
                jTextArea2.setText("This text will appear on the user's home page.");
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
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(explanationJPanel, gridBagConstraints);

                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Home Page Features", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea3.setEditable(false);
                jTextArea3.setLineWrap(true);
                jTextArea3.setText("These features will be available on the user's home page.");
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
                explorerJLabel.setText("Show Application List: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(explorerJLabel, gridBagConstraints);

                userBookmarksJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                userBookmarksJLabel.setText("Show Bookmarks: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(userBookmarksJLabel, gridBagConstraints);

                addUserBookmarksJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addUserBookmarksJLabel.setText("Allow User Added Bookmarks: ");
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
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

                externalRemoteJPanel1.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Timeout", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea4.setEditable(false);
                jTextArea4.setLineWrap(true);
                jTextArea4.setText("This sets how long a login can be idle before being logged out.");
                jTextArea4.setWrapStyleWord(true);
                jTextArea4.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                externalRemoteJPanel1.add(jTextArea4, gridBagConstraints);

                restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

                timeoutJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                timeoutJLabel.setText("Timeout: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(timeoutJLabel, gridBagConstraints);

                timeoutJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                timeoutJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                timeoutJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                timeoutJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                timeoutJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                timeoutJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel1.add(timeoutJSpinner, gridBagConstraints);

                timeoutJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                timeoutJLabel1.setText(" (minutes)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(timeoutJLabel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                externalRemoteJPanel1.add(restrictIPJPanel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(externalRemoteJPanel1, gridBagConstraints);

        }//GEN-END:initComponents

    private void autoCreateJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoCreateJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_autoCreateJCheckBoxActionPerformed

    private void loginTextJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginTextJTextFieldCaretUpdate
	if( !loginTextJTextField.getText().trim().equals(loginTextCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_loginTextJTextFieldCaretUpdate
    
    private void loginTitleJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginTitleJTextFieldCaretUpdate
	if( !loginTitleJTextField.getText().trim().equals(loginTitleCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_loginTitleJTextFieldCaretUpdate

    private void timeoutJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_timeoutJSpinnerStateChanged
	if( ((Integer)timeoutJSpinner.getValue() != timeoutCurrent) && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_timeoutJSpinnerStateChanged

    private void addJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_addJCheckBoxActionPerformed
    
    private void bookmarksJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookmarksJCheckBoxActionPerformed
	if( settingsChangedListener != null )
	    settingsChangedListener.settingsChanged(this);
	addJCheckBox.setEnabled(bookmarksJCheckBox.isSelected());
	addUserBookmarksJLabel.setEnabled(bookmarksJCheckBox.isSelected());
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
            
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JCheckBox addJCheckBox;
        private javax.swing.JLabel addUserBookmarksJLabel;
        private javax.swing.JCheckBox autoCreateJCheckBox;
        private javax.swing.JCheckBox bookmarksJCheckBox;
        public javax.swing.JTextField bookmarksJTextField;
        private javax.swing.JLabel bookmarksTitleJLabel;
        private javax.swing.ButtonGroup enabledButtonGroup;
        private javax.swing.JPanel explanationJPanel;
        private javax.swing.JPanel explanationJPanel1;
        private javax.swing.JCheckBox explorerJCheckBox;
        private javax.swing.JLabel explorerJLabel;
        private javax.swing.JLabel explorerJLabel1;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel1;
        private javax.swing.JPanel externalRemoteJPanel2;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextArea jTextArea3;
        private javax.swing.JTextArea jTextArea4;
        private javax.swing.JTextArea jTextArea5;
        private javax.swing.JTextArea jTextArea6;
        public javax.swing.JTextField loginTextJTextField;
        public javax.swing.JTextField loginTitleJTextField;
        private javax.swing.JLabel pageTextJLabel;
        private javax.swing.JLabel pageTextJLabel1;
        public javax.swing.JTextField pageTextJTextField;
        private javax.swing.JLabel pageTitleJLabel;
        private javax.swing.JLabel pageTitleJLabel1;
        public javax.swing.JTextField pageTitleJTextField;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        private javax.swing.JPanel restrictIPJPanel2;
        private javax.swing.JPanel restrictIPJPanel3;
        private javax.swing.JPanel restrictIPJPanel4;
        private javax.swing.JLabel timeoutJLabel;
        private javax.swing.JLabel timeoutJLabel1;
        private javax.swing.JSpinner timeoutJSpinner;
        private javax.swing.JLabel userBookmarksJLabel;
        // End of variables declaration//GEN-END:variables
    
}
