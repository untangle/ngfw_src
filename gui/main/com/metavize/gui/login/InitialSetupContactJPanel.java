/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.login;

import com.metavize.mvvm.security.*;
import com.metavize.gui.transform.Savable;
import com.metavize.gui.util.Util;
import javax.swing.SwingUtilities;

public class InitialSetupContactJPanel extends javax.swing.JPanel implements Savable {
    
    public InitialSetupContactJPanel() {
        initComponents();
        countJSpinner.setModel(new javax.swing.SpinnerNumberModel(15,0,10000,1));
    }

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
    int count;
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {
	
	SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
	    address1 = address1JTextField.getText().trim();
	    address2 = address2JTextField.getText().trim();
	    city = cityJTextField.getText().trim();
	    state = stateJTextField.getText().trim();
	    zipcode = zipcodeJTextField.getText().trim();
	    phone = phoneJTextField.getText().trim();        
	    email = emailJTextField.getText().trim();
	    count = (Integer) countJSpinner.getValue();
	}});

	company = companyJTextField.getText().trim();
	if(company.length() == 0)
	    throw new Exception("You must fill out the company name.");
        
	firstName = firstNameJTextField.getText().trim();
	if(firstName.length() == 0)
	    throw new Exception("You must fill out your first name.");
        
	lastName = lastNameJTextField.getText().trim();
	if(lastName.length() == 0)
	    throw new Exception("You must fill out your last name.");

	if(email.length() == 0)
	    throw new Exception("You must fill out your email address.");
        
        if( !validateOnly ){
	    RegistrationInfo registrationInfo = new RegistrationInfo(company, firstName, lastName, email, count);
	    registrationInfo.setAddress1(address1);
	    registrationInfo.setAddress2(address2);
	    registrationInfo.setCity(city);
	    registrationInfo.setState(state);
	    registrationInfo.setZipcode(zipcode);
	    registrationInfo.setPhone(phone);
            Util.getAdminManager().setRegistrationInfo(registrationInfo);            
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        companyJTextField = new javax.swing.JTextField();
        firstNameJTextField = new javax.swing.JTextField();
        lastNameJTextField = new javax.swing.JTextField();
        address1JTextField = new javax.swing.JTextField();
        address2JTextField = new javax.swing.JTextField();
        cityJTextField = new javax.swing.JTextField();
        stateJTextField = new javax.swing.JTextField();
        zipcodeJTextField = new javax.swing.JTextField();
        phoneJTextField = new javax.swing.JTextField();
        emailJTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        countJSpinner = new javax.swing.JSpinner();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please take a moment to register yourself as the<br> operator of the EdgeGuard.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Company Name:");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, -1, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("First Name:");
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, -1, -1));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Last Name:");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, -1, -1));

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Address 1:");
        add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 130, -1, -1));

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Address 2:");
        add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 150, -1, -1));

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel7.setText("City :");
        add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 170, -1, -1));

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("State:");
        add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 190, -1, -1));

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Zipcode:");
        add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 210, -1, -1));

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Phone #:");
        add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 230, -1, -1));

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Email:");
        add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 250, -1, -1));

        companyJTextField.setColumns(15);
        add(companyJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 70, -1, -1));

        firstNameJTextField.setColumns(15);
        add(firstNameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 90, -1, -1));

        lastNameJTextField.setColumns(15);
        add(lastNameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 110, -1, -1));

        address1JTextField.setColumns(15);
        add(address1JTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, -1, -1));

        address2JTextField.setColumns(15);
        add(address2JTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 150, -1, -1));

        cityJTextField.setColumns(15);
        add(cityJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 170, -1, -1));

        stateJTextField.setColumns(15);
        add(stateJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 190, -1, -1));

        zipcodeJTextField.setColumns(15);
        add(zipcodeJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 210, -1, -1));

        phoneJTextField.setColumns(15);
        add(phoneJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 230, -1, -1));

        emailJTextField.setColumns(15);
        add(emailJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 250, -1, -1));

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText("(required)");
        add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 70, -1, -1));

        jLabel13.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel13.setText("(required)");
        add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 90, -1, -1));

        jLabel14.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel14.setText("(required)");
        add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 110, -1, -1));

        jLabel15.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel15.setText("(required)");
        add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 250, -1, -1));

        jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("Computers protected by EdgeGuard:");
        add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 300, -1, -1));

        add(countJSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 300, 70, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField address1JTextField;
    private javax.swing.JTextField address2JTextField;
    private javax.swing.JTextField cityJTextField;
    private javax.swing.JTextField companyJTextField;
    private javax.swing.JSpinner countJSpinner;
    private javax.swing.JTextField emailJTextField;
    private javax.swing.JTextField firstNameJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField lastNameJTextField;
    private javax.swing.JTextField phoneJTextField;
    private javax.swing.JTextField stateJTextField;
    private javax.swing.JTextField zipcodeJTextField;
    // End of variables declaration//GEN-END:variables
    
}
