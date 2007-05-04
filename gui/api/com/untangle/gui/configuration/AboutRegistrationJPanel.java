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

package com.untangle.gui.configuration;

import java.awt.Color;
import javax.swing.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.mvvm.security.*;

public class AboutRegistrationJPanel extends JPanel
    implements Savable<AboutCompoundSettings>, Refreshable<AboutCompoundSettings> {

    private static final String EXCEPTION_COMPANY_MISSING = "You must fill out the company name.";
    private static final String EXCEPTION_FIRST_NAME_MISSING = "You must fill out your first name.";
    private static final String EXCEPTION_LAST_NAME_MISSING = "You must fill out your last name.";
    private static final String EXCEPTION_EMAIL_MISSING = "You must fill out your email address.";
    private static final String EXCEPTION_COMPUTER_COUNT_MISSING = "You must fill out the number of computers protected by Untangle.";

    public AboutRegistrationJPanel() {
        initComponents();
        Util.addFocusHighlight(companyJTextField);
        Util.addFocusHighlight(firstNameJTextField);
        Util.addFocusHighlight(lastNameJTextField);
        Util.addFocusHighlight(address1JTextField);
        Util.addFocusHighlight(address2JTextField);
        Util.addFocusHighlight(cityJTextField);
        Util.addFocusHighlight(stateJTextField);
        Util.addFocusHighlight(zipcodeJTextField);
        Util.addFocusHighlight(phoneJTextField);
        Util.addFocusHighlight(emailJTextField);
        Util.addFocusHighlight(countJTextField);

    }


    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    Exception exception;

    public void doSave(AboutCompoundSettings aboutCompoundSettings, boolean validateOnly) throws Exception {

        String company;
        String firstName;
        String lastName;
        String address1;
        String address2;
        String city;
        String state;
        String zipcode;
        String phone;
        String email;
        String countString;
        int count = 0;

        companyJTextField.setBackground( Color.WHITE );
        firstNameJTextField.setBackground( Color.WHITE );
        lastNameJTextField.setBackground( Color.WHITE );
        emailJTextField.setBackground( Color.WHITE );
        countJTextField.setBackground( Color.WHITE );

        company = companyJTextField.getText().trim();
        firstName = firstNameJTextField.getText().trim();
        lastName = lastNameJTextField.getText().trim();
        address1 = address1JTextField.getText().trim();
        address2 = address2JTextField.getText().trim();
        city = cityJTextField.getText().trim();
        state = stateJTextField.getText().trim();
        zipcode = zipcodeJTextField.getText().trim();
        phone = phoneJTextField.getText().trim();
        email = emailJTextField.getText().trim();
        countString = countJTextField.getText().trim();

        exception = null;

        if(company.length() == 0){
            companyJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            exception = new Exception(EXCEPTION_COMPANY_MISSING);
        }

        if(firstName.length() == 0){
            firstNameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            exception = new Exception(EXCEPTION_FIRST_NAME_MISSING);
        }

        if(lastName.length() == 0){
            lastNameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            exception = new Exception(EXCEPTION_LAST_NAME_MISSING);
        }

        if(email.length() == 0){
            emailJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            exception = new Exception(EXCEPTION_EMAIL_MISSING);
        }

        try{
            if(countString.length() == 0)
                throw new Exception();
            count = Integer.parseInt(countString);
            if( count < 0 )
                throw new Exception();
        }
        catch(Exception e){
            countJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            exception = new Exception(EXCEPTION_COMPUTER_COUNT_MISSING);
        }


        if( exception != null)
            throw exception;

        if( !validateOnly ){
            RegistrationInfo registrationInfo = new RegistrationInfo(company, firstName, lastName, email, count);
            registrationInfo.setAddress1(address1);
            registrationInfo.setAddress2(address2);
            registrationInfo.setCity(city);
            registrationInfo.setState(state);
            registrationInfo.setZipcode(zipcode);
            registrationInfo.setPhone(phone);
            aboutCompoundSettings.setRegistrationInfo(registrationInfo);
        }
    }


    public void doRefresh(AboutCompoundSettings aboutCompoundSettings){
        RegistrationInfo registrationInfo = aboutCompoundSettings.getRegistrationInfo();

        // COMPANY NAME /////
        String company = registrationInfo.getCompanyName();
        companyJTextField.setText( company );
        companyJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, companyJTextField);

        // FIRST NAME /////
        String firstName = registrationInfo.getFirstName();
        firstNameJTextField.setText( firstName );
        firstNameJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, firstNameJTextField);

        // LAST NAME /////
        String lastName = registrationInfo.getLastName();
        lastNameJTextField.setText( lastName );
        lastNameJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, lastNameJTextField);

        // ADDRESS 1 /////
        String address1 = registrationInfo.getAddress1();
        address1JTextField.setText( address1 );
        Util.addSettingChangeListener(settingsChangedListener, this, address1JTextField);

        // ADDRESS 2 /////
        String address2 = registrationInfo.getAddress2();
        address2JTextField.setText( address2 );
        Util.addSettingChangeListener(settingsChangedListener, this, address2JTextField);

        // CITY NAME /////
        String city = registrationInfo.getCity();
        cityJTextField.setText( city );
        Util.addSettingChangeListener(settingsChangedListener, this, cityJTextField);

        // STATE NAME /////
        String state = registrationInfo.getState();
        stateJTextField.setText( state );
        Util.addSettingChangeListener(settingsChangedListener, this, stateJTextField);

        // ZIPCODE NAME /////
        String zipcode = registrationInfo.getZipcode();
        zipcodeJTextField.setText( zipcode );
        Util.addSettingChangeListener(settingsChangedListener, this, zipcodeJTextField);

        // PHONE NAME /////
        String phone = registrationInfo.getPhone();
        phoneJTextField.setText( phone );
        Util.addSettingChangeListener(settingsChangedListener, this, phoneJTextField);

        // EMAIL NAME /////
        String email = registrationInfo.getEmailAddr();
        emailJTextField.setText( email );
        emailJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, emailJTextField);

        // COMPUTER COUNT //
        int count = registrationInfo.getNumSeats();
        countJTextField.setText( Integer.toString(count) );
        countJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, countJTextField);

    }
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        companyJTextField = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        firstNameJTextField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        lastNameJTextField = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        address1JTextField = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        address2JTextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        cityJTextField = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        stateJTextField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        zipcodeJTextField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        phoneJTextField = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        emailJTextField = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        countJTextField = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Registration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        jLabel17.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel17.setText("Company Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel17, gridBagConstraints);

        companyJTextField.setColumns(15);
        companyJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        companyJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(companyJTextField, gridBagConstraints);

        jLabel18.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel18.setText("First Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel18, gridBagConstraints);

        firstNameJTextField.setColumns(15);
        firstNameJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(firstNameJTextField, gridBagConstraints);

        jLabel19.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel19.setText("Last Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel19, gridBagConstraints);

        lastNameJTextField.setColumns(15);
        lastNameJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        lastNameJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(lastNameJTextField, gridBagConstraints);

        jLabel20.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel20.setText("Address 1:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel20, gridBagConstraints);

        address1JTextField.setColumns(15);
        address1JTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        address1JTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(address1JTextField, gridBagConstraints);

        jLabel21.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel21.setText("Address 2:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel21, gridBagConstraints);

        address2JTextField.setColumns(15);
        address2JTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        address2JTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(address2JTextField, gridBagConstraints);

        jLabel22.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel22.setText("City :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel22, gridBagConstraints);

        cityJTextField.setColumns(15);
        cityJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        cityJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(cityJTextField, gridBagConstraints);

        jLabel23.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel23.setText("State:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel23, gridBagConstraints);

        stateJTextField.setColumns(15);
        stateJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        stateJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(stateJTextField, gridBagConstraints);

        jLabel24.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel24.setText("Zipcode:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel24, gridBagConstraints);

        zipcodeJTextField.setColumns(15);
        zipcodeJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        zipcodeJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(zipcodeJTextField, gridBagConstraints);

        jLabel25.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel25.setText("Phone #:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel25, gridBagConstraints);

        phoneJTextField.setColumns(15);
        phoneJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        phoneJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(phoneJTextField, gridBagConstraints);

        jLabel26.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel26.setText("Email:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel26, gridBagConstraints);

        emailJTextField.setColumns(15);
        emailJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        emailJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(emailJTextField, gridBagConstraints);

        jLabel27.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel27.setText("(required)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel27, gridBagConstraints);

        jLabel28.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel28.setText("(required)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel28, gridBagConstraints);

        jLabel29.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel29.setText("(required)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel29, gridBagConstraints);

        jLabel30.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel30.setText("(required)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel30, gridBagConstraints);

        jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("<html>Number of computers<br>protected by Untangle:</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel1.add(jLabel16, gridBagConstraints);

        countJTextField.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        jPanel1.add(countJTextField, gridBagConstraints);

        jLabel31.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel31.setText("(required)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel31, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel2.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(jPanel2, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField address1JTextField;
    private javax.swing.JTextField address2JTextField;
    private javax.swing.JTextField cityJTextField;
    private javax.swing.JTextField companyJTextField;
    private javax.swing.JTextField countJTextField;
    private javax.swing.JTextField emailJTextField;
    private javax.swing.JTextField firstNameJTextField;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField lastNameJTextField;
    private javax.swing.JTextField phoneJTextField;
    private javax.swing.JTextField stateJTextField;
    private javax.swing.JTextField zipcodeJTextField;
    // End of variables declaration//GEN-END:variables

}
