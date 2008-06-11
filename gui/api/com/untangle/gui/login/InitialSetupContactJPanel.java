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

package com.untangle.gui.login;

import java.awt.Color;
import java.net.URL;
import javax.swing.SwingUtilities;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.client.*;
import com.untangle.uvm.security.*;

public class InitialSetupContactJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_COMPANY_MISSING = "You must fill out the company name.";
    private static final String EXCEPTION_FIRST_NAME_MISSING = "You must fill out your first name.";
    private static final String EXCEPTION_LAST_NAME_MISSING = "You must fill out your last name.";
    private static final String EXCEPTION_EMAIL_MISSING = "You must fill out your email address.";
    private static final String EXCEPTION_COMPUTER_COUNT_MISSING = "You must fill out the number of computers protected by the Untangle Server.";

    public InitialSetupContactJPanel() {
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

    public void initialFocus(){
        companyJTextField.requestFocus();
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
    String countString;
    int count;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
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
                return;
            }

            if(firstName.length() == 0){
                firstNameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_FIRST_NAME_MISSING);
                return;
            }

            if(lastName.length() == 0){
                lastNameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_LAST_NAME_MISSING);
                return;
            }

            if(email.length() == 0){
                emailJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_EMAIL_MISSING);
                return;
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
                return;
            }
        }});

        if( exception != null)
            throw exception;

        if( !validateOnly ){
            try{
                InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Contact Information...");
                RegistrationInfo registrationInfo = new RegistrationInfo(company, firstName, lastName, email, count);
                registrationInfo.setAddress1(address1);
                registrationInfo.setAddress2(address2);
                registrationInfo.setCity(city);
                registrationInfo.setState(state);
                registrationInfo.setZipcode(zipcode);
                registrationInfo.setPhone(phone);
                // KEY, IF NOT UNTANGLE APPLIANCE
                if(!Util.isUntangleAppliance()){
                    URL url = Util.getServerCodeBase();
                    boolean isActivated = com.untangle.uvm.client.RemoteUvmContextFactory.factory().
			isActivated( url.getHost(),
				     url.getPort(),
				     0,
				     Util.isSecureViaHttps() );
                    if( !isActivated ){
                        RemoteUvmContext uvmContext = RemoteUvmContextFactory.factory().
			    activationLogin( url.getHost(), url.getPort(),
					     "0000-0000-0000-0000",
					     0,
					     Util.getClassLoader(),
					     Util.isSecureViaHttps() );
                        Util.setUvmContext(uvmContext);
                        KeepAliveThread keepAliveThread = new KeepAliveThread(uvmContext);
                        InitialSetupWizard.setKeepAliveThread(keepAliveThread);
                    }
                }

                Util.getRemoteAdminManager().setRegistrationInfo(registrationInfo);

                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
            }
            catch(Exception e){
                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
                Util.handleExceptionNoRestart("Error sending data", e);
                throw new Exception("A network communication error occurred.  Please retry.");
            }
        }
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
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
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please take a moment to register yourself as the operator of the Untangle Server.<br>  <b>This information is required.</b></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel2, gridBagConstraints);

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
        jLabel16.setText("<html>Number of computers protected<br>by the Untangle Server:</html>");
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField address1JTextField;
    private javax.swing.JTextField address2JTextField;
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JTextField cityJTextField;
    private javax.swing.JTextField companyJTextField;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JTextField countJTextField;
    private javax.swing.JTextField emailJTextField;
    private javax.swing.JTextField firstNameJTextField;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
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
    private javax.swing.JTextField lastNameJTextField;
    private javax.swing.JTextField phoneJTextField;
    private javax.swing.JTextField stateJTextField;
    private javax.swing.JTextField zipcodeJTextField;
    // End of variables declaration//GEN-END:variables

}
