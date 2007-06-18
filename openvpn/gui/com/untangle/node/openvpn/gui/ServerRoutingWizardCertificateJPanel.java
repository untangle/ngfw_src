/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.gui;

import java.awt.Color;
import javax.swing.SwingUtilities;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.security.*;
import com.untangle.node.openvpn.*;

public class ServerRoutingWizardCertificateJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_ORGANIZATION_MISSING = "You must fill out the name of your organization.";
    private static final String EXCEPTION_COUNTRY_MISSING = "You must fill out the name of your country.";
    private static final String EXCEPTION_COUNTRY_LENGTH = "The country code must be 2 characters long.";
    private static final String EXCEPTION_STATE_MISSING = "You must fill out the name of your state.";
    private static final String EXCEPTION_LOCALITY_MISSING = "You must fill out the name of your locality.";

    private VpnNode vpnNode;

    public ServerRoutingWizardCertificateJPanel(VpnNode vpnNode) {
        this.vpnNode = vpnNode;
        initComponents();
        Util.addFocusHighlight(organizationJTextField);
        Util.addFocusHighlight(countryJTextField);
        Util.addFocusHighlight(stateJTextField);
        Util.addFocusHighlight(localityJTextField);
    }

    public void initialFocus(){
        organizationJTextField.requestFocus();
    }

    String organization;
    String country;
    String state;
    String locality;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
            organizationJTextField.setBackground( Color.WHITE );
            countryJTextField.setBackground( Color.WHITE );
            stateJTextField.setBackground( Color.WHITE );
            localityJTextField.setBackground( Color.WHITE );

            organization = organizationJTextField.getText().trim();
            country = countryJTextField.getText().trim();
            state = stateJTextField.getText().trim();
            locality = localityJTextField.getText().trim();

            exception = null;

            if(organization.length() == 0){
                organizationJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_ORGANIZATION_MISSING);
                return;
            }

            if(country.length() == 0){
                countryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_COUNTRY_MISSING);
                return;
            }

            if(country.length() != 2){
                countryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_COUNTRY_LENGTH);
                return;
            }

            if(state.length() == 0){
                stateJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_STATE_MISSING);
                return;
            }

            if(locality.length() == 0){
                localityJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_LOCALITY_MISSING);
                return;
            }

        }});

        if( exception != null)
            throw exception;

        if( !validateOnly ){
            ServerRoutingWizard.getInfiniteProgressJComponent().startLater("Generating Certificate...");
            try{
                CertificateParameters certificateParameters = new CertificateParameters( organization, "", country, state, locality, false );
                vpnNode.generateCertificate( certificateParameters );
                ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(1500l);
            }
            catch(Exception e){
                ServerRoutingWizard.getInfiniteProgressJComponent().stopLater(-1l);
                throw e;
            }
        }
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        organizationJTextField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        countryJTextField = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        stateJTextField = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        localityJTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please take a moment to specify some information about your location, to be used to generate a secure digital certificate.<br><b>This information is required.</b></html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 400, -1));

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        jLabel17.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel17.setText("Organization:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel17, gridBagConstraints);

        organizationJTextField.setColumns(15);
        organizationJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        organizationJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(organizationJTextField, gridBagConstraints);

        jLabel19.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel19.setText("Country:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel19, gridBagConstraints);

        countryJTextField.setColumns(15);
        countryJTextField.setText("US");
        countryJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(countryJTextField, gridBagConstraints);

        jLabel20.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel20.setText("State:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel20, gridBagConstraints);

        stateJTextField.setColumns(15);
        stateJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        stateJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(stateJTextField, gridBagConstraints);

        jLabel21.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel21.setText("City:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel21, gridBagConstraints);

        localityJTextField.setColumns(15);
        localityJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
        localityJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        jPanel1.add(localityJTextField, gridBagConstraints);

        add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, 350, 110));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField countryJTextField;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField localityJTextField;
    private javax.swing.JTextField organizationJTextField;
    private javax.swing.JTextField stateJTextField;
    // End of variables declaration//GEN-END:variables

}
