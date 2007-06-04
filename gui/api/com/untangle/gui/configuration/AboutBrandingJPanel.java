/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: AboutBrandingJPanel.java 10004 2007-05-04 03:19:53Z amread $
 */

package com.untangle.gui.configuration;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;


import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.util.IOUtil;


public class AboutBrandingJPanel extends javax.swing.JPanel
    implements Savable<AboutCompoundSettings>, Refreshable<AboutCompoundSettings> {

//    private static final String EMPTY_DNS2 = "";

    private MConfigJDialog mConfigJDialog;
    private byte[] logoByteArray;

    public AboutBrandingJPanel() {
        initComponents();
        MConfigJDialog.setInitialFocusComponent(defaultEnabledRadioButton);
        Util.addPanelFocus(this, defaultEnabledRadioButton);
        Util.addFocusHighlight(companyNameJTextField);
        Util.addFocusHighlight(companyUrlJTextField);
        Util.addFocusHighlight(contactNameJTextField);
        Util.addFocusHighlight(contactEmailJTextField);       
        this.mConfigJDialog = mConfigJDialog;
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(AboutCompoundSettings aboutCompoundSettings, boolean validateOnly) throws Exception {

        // DEFAULT ENABLED //////////
        boolean isDefaultEnabled = defaultEnabledRadioButton.isSelected();

        // FIELDS ////////////
        String companyName = companyNameJTextField.getText().trim();
        String companyUrl = companyUrlJTextField.getText().trim();
        String contactName = contactNameJTextField.getText().trim();
        String contactEmail = contactEmailJTextField.getText().trim();
                
        
        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            BrandingSettings brandingSettings = aboutCompoundSettings.getBrandingSettings();
            if (isDefaultEnabled) {
                brandingSettings.setLogo( null );
            }
            else {
                brandingSettings.setLogo(logoByteArray);    
            }
            
            brandingSettings.setCompanyName(companyName);
            brandingSettings.setCompanyUrl(companyUrl);
            brandingSettings.setContactName(contactName);
            brandingSettings.setContactEmail(contactEmail);            
        }
    }


    boolean isDefaultEnabledCurrent;
    String companyNameCurrent;
    String companyUrlCurrent;
    String contactNameCurrent;
    String contactEmailCurrent;
    
    public void doRefresh(AboutCompoundSettings aboutCompoundSettings){
        BrandingSettings brandingSettings = aboutCompoundSettings.getBrandingSettings();

        // DEFAULT ENABLED /////               
        logoByteArray = brandingSettings.getLogo();
        if (logoByteArray == null) {
            defaultEnabledRadioButton.setSelected( true );
        }
        else {
            defaultDisabledRadioButton.setSelected( true );
        }
        setDefaultEnabledDependency( logoByteArray == null );

        companyNameCurrent = brandingSettings.getCompanyName();
        companyNameJTextField.setText( companyNameCurrent );
        Util.addSettingChangeListener(settingsChangedListener, this, companyNameJTextField);

        companyUrlCurrent = brandingSettings.getCompanyUrl();
        companyUrlJTextField.setText( companyUrlCurrent );
        Util.addSettingChangeListener(settingsChangedListener, this, companyUrlJTextField);

        contactNameCurrent = brandingSettings.getContactName();
        contactNameJTextField.setText( contactNameCurrent );
        Util.addSettingChangeListener(settingsChangedListener, this, contactNameJTextField);
        
        contactEmailCurrent = brandingSettings.getContactEmail();
        contactEmailJTextField.setText( contactEmailCurrent );
        Util.addSettingChangeListener(settingsChangedListener, this, contactEmailJTextField);
        
    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        defaultButtonGroup = new javax.swing.ButtonGroup();
        brandingJPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        defaultEnabledRadioButton = new javax.swing.JRadioButton();
        defaultDisabledRadioButton = new javax.swing.JRadioButton();
        uploadJButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        contactJPanel = new javax.swing.JPanel();
        companyNameJLabel = new javax.swing.JLabel();
        companyNameJTextField = new javax.swing.JTextField();
        companyUrlJLabel = new javax.swing.JLabel();
        companyUrlJTextField = new javax.swing.JTextField();
        contactNameJLabel = new javax.swing.JLabel();
        contactNameJTextField = new javax.swing.JTextField();
        contactEmailJLabel = new javax.swing.JLabel();
        contactEmailJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 470));
        setMinimumSize(new java.awt.Dimension(563, 470));
        setPreferredSize(new java.awt.Dimension(563, 470));
        brandingJPanel.setLayout(new java.awt.GridBagLayout());

        brandingJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Branding", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("<html>The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 10);
        brandingJPanel.add(jLabel11, gridBagConstraints);

        defaultButtonGroup.add(defaultEnabledRadioButton);
        defaultEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultEnabledRadioButton.setText("<html><b>Use Default Logo</b></html>");
        defaultEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
        defaultEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        brandingJPanel.add(defaultEnabledRadioButton, gridBagConstraints);

        defaultButtonGroup.add(defaultDisabledRadioButton);
        defaultDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultDisabledRadioButton.setText("<html><b>Use Custom Logo</b></html>");
        defaultDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        brandingJPanel.add(defaultDisabledRadioButton, gridBagConstraints);

        uploadJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        uploadJButton.setText("Upload Image");
        uploadJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uploadJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 100, 5, 0);
        brandingJPanel.add(uploadJButton, gridBagConstraints);

        jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        brandingJPanel.add(jSeparator4, gridBagConstraints);

        contactJPanel.setLayout(new java.awt.GridBagLayout());

        companyNameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        companyNameJLabel.setText("Company Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        contactJPanel.add(companyNameJLabel, gridBagConstraints);

        companyNameJTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                companyNameJTextFieldCaretUpdate(evt);
            }
        });
        companyNameJTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                companyNameJTextFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        contactJPanel.add(companyNameJTextField, gridBagConstraints);

        companyUrlJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        companyUrlJLabel.setText("Company URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        contactJPanel.add(companyUrlJLabel, gridBagConstraints);

        companyUrlJTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                companyUrlJTextFieldCaretUpdate(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        contactJPanel.add(companyUrlJTextField, gridBagConstraints);

        contactNameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        contactNameJLabel.setText("Contact Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        contactJPanel.add(contactNameJLabel, gridBagConstraints);

        contactNameJTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                contactNameJTextFieldCaretUpdate(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        contactJPanel.add(contactNameJTextField, gridBagConstraints);

        contactEmailJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        contactEmailJLabel.setText("Contact Email:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        contactJPanel.add(contactEmailJLabel, gridBagConstraints);

        contactEmailJTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                contactEmailJTextFieldCaretUpdate(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        contactJPanel.add(contactEmailJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        brandingJPanel.add(contactJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(brandingJPanel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private void uploadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uploadJButtonActionPerformed
        if ( Util.getIsDemo() ) 
            return;
        
        new ImageChoiceThread();
    }//GEN-LAST:event_uploadJButtonActionPerformed

    private void companyNameJTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_companyNameJTextFieldActionPerformed

    }//GEN-LAST:event_companyNameJTextFieldActionPerformed

    private void contactEmailJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_contactEmailJTextFieldCaretUpdate
        
    }//GEN-LAST:event_contactEmailJTextFieldCaretUpdate

    private void contactNameJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_contactNameJTextFieldCaretUpdate
        
    }//GEN-LAST:event_contactNameJTextFieldCaretUpdate

    private void companyUrlJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_companyUrlJTextFieldCaretUpdate
        
    }//GEN-LAST:event_companyUrlJTextFieldCaretUpdate

    private void companyNameJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_companyNameJTextFieldCaretUpdate
        
    }//GEN-LAST:event_companyNameJTextFieldCaretUpdate

    private void defaultDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultDisabledRadioButtonActionPerformed
        setDefaultEnabledDependency( false );
    }//GEN-LAST:event_defaultDisabledRadioButtonActionPerformed

    private void defaultEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultEnabledRadioButtonActionPerformed
        setDefaultEnabledDependency( true );
    }//GEN-LAST:event_defaultEnabledRadioButtonActionPerformed

    private void setDefaultEnabledDependency(boolean enabled){
        uploadJButton.setEnabled( !enabled );
    }

    private class ImageChoiceThread extends Thread {
        public ImageChoiceThread(){
            super("MVCLIENT-ImageChoiceThread");
            setDaemon(true);            
            this.start();
        }
        public void run() {
            try{
                // PERFORM THE IMAGE UPLOAD
                                
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new LogoFileFilter());
                int retVal = chooser.showSaveDialog(AboutBrandingJPanel.this.getTopLevelAncestor());
                if(retVal == JFileChooser.APPROVE_OPTION){
                    File file = chooser.getSelectedFile();
                    
                    if(!file.exists()){
                        return;
                    }
                
                    // SET THE BYTE ARRAY TO THE FILE
                    AboutBrandingJPanel.this.logoByteArray = IOUtil.fileToBytes(file);
                
                }

            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error choosing image", e);                
                MOneButtonJDialog.factory(AboutBrandingJPanel.this.getTopLevelAncestor(), "",
                                          "image Choice Failure",
                                          "Image Choice Failure Warning", "");
            }
        }
    }
    
    

    private class LogoFileFilter extends javax.swing.filechooser.FileFilter {
        private static final String IMAGE_EXTENSION = ".gif";       
        public boolean accept(File f){
            if(f.isDirectory())
                return true;
            else if(f.getName().endsWith(IMAGE_EXTENSION))
                return true;            
            else
                return false;
        }
        public String getDescription(){
            return "Logo Image Files (*" + IMAGE_EXTENSION + ")";
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel brandingJPanel;
    private javax.swing.JLabel companyNameJLabel;
    public javax.swing.JTextField companyNameJTextField;
    private javax.swing.JLabel companyUrlJLabel;
    public javax.swing.JTextField companyUrlJTextField;
    private javax.swing.JLabel contactEmailJLabel;
    public javax.swing.JTextField contactEmailJTextField;
    private javax.swing.JPanel contactJPanel;
    private javax.swing.JLabel contactNameJLabel;
    public javax.swing.JTextField contactNameJTextField;
    private javax.swing.ButtonGroup defaultButtonGroup;
    public javax.swing.JRadioButton defaultDisabledRadioButton;
    public javax.swing.JRadioButton defaultEnabledRadioButton;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton uploadJButton;
    // End of variables declaration//GEN-END:variables


}
