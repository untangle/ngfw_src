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

import java.awt.*;
import java.util.*;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.client.*;
import com.untangle.uvm.node.HostAddress;
import com.untangle.node.openvpn.*;

public class ClientWizardServerJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_ADDRESS_FORMAT = "The \"Server Address\" is not a valid IP address.";
    private static final String EXCEPTION_NO_PASSWORD = "Please supply a password will be used to connect to the server.";
    private static final String EXCEPTION_KEY_UNREAD = "You must click \"Read USB Key\" before proceeding.";
    private static final String EXCEPTION_NO_SELECTION = "You must click \"Read USB Key\", and select a valid configuration " +
        "from the drop-down-list before proceeding.";


    private VpnNode vpnNode;

    private static final String NO_CONFIGURATIONS = "[No Configurations]";

    public ClientWizardServerJPanel(VpnNode vpnNode) {
        this.vpnNode = vpnNode;
        initComponents();
        Util.addFocusHighlight(serverJTextField);
        Util.addFocusHighlight(passwordJPasswordField);
        setServerSelectedDependency(serverJRadioButton.isSelected());
        keyJComboBox.addItem(NO_CONFIGURATIONS);
        keyJComboBox.setSelectedItem(NO_CONFIGURATIONS);
    }

    public void initialFocus(){
        serverJRadioButton.requestFocus();
    }


    boolean useServer;
    String address;
    HostAddress addressIPaddr;
    int serverPort;
    String password;
    boolean keyRead = false;
    String selection;
    Exception exception;
    MProgressJDialog mProgressJDialog;
    JProgressBar jProgressBar;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            serverJTextField.setBackground( Color.WHITE );
            passwordJPasswordField.setBackground( Color.WHITE );

            useServer = serverJRadioButton.isSelected();
            address = serverJTextField.getText().trim();
            password = passwordJPasswordField.getText().trim();
            selection = (String) keyJComboBox.getSelectedItem();

            exception = null;

            // SERVER SELECTED
            if( useServer ){
                if( address.length() <= 0 ){
                    serverJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                    exception = new Exception(EXCEPTION_ADDRESS_FORMAT);
                    return;
                }

                try{
                    /* RBS This code is here to allow for <ip address>:<port>
                     * it should be in a richer object, like a IPAddressAndPort */
                    String[] values = address.split( ":" );
                    if ( values.length == 1 ) {
                        /* XXX Magic number */
                        serverPort = 443;
                    } else if ( values.length == 2 ) {
                        serverPort = Integer.parseInt( values[1] );
                    } else {
                        exception = new Exception(EXCEPTION_ADDRESS_FORMAT);
                        return;
                    }
                    addressIPaddr = HostAddress.parse( values[0] );
                }
                catch(Exception e){
                    exception = new Exception(EXCEPTION_ADDRESS_FORMAT);
                    return;
                }

                if( password.length() <= 0 ){
                    passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                    exception = new Exception(EXCEPTION_NO_PASSWORD);
                    return;
                }
            }
            // USB KEY SELECTED
            else{
                if( !keyRead ){
                    exception = new Exception(EXCEPTION_KEY_UNREAD);
                    return;
                }

                if( NO_CONFIGURATIONS.equals(selection) || (selection == null) ) {
                    exception = new Exception(EXCEPTION_NO_SELECTION);
                    return;
                }
            }

        }});

        if( exception != null )
            throw exception;


        if( !validateOnly ){
            ClientWizard.getInfiniteProgressJComponent().startLater("Downloading Configuration... (This may take up to one minute)");
            try{
                // DOWNLOAD THE STUFFS
                if( useServer )
                    vpnNode.downloadConfig( addressIPaddr, serverPort, password );
                else
                    vpnNode.downloadConfigUsb( selection );

                // SHOW RESULTS AND REMOVE DOWNLOADING DIALOG
                ClientWizard.getInfiniteProgressJComponent().stopLater(2000l);
                ClientWizard.getInfiniteProgressJComponent().startLater("Finished Downloading");
                ClientWizard.getInfiniteProgressJComponent().stopLater(2000l);
            }
            catch(Exception e){
                ClientWizard.getInfiniteProgressJComponent().stopLater(-1l);
                if( useServer ){
                    Util.handleExceptionNoRestart("Error downloading config from server:", e);
                    throw new Exception("Your VPN Client configuration could not be downloaded from the server.  Please try again.");
                }
                else{
                    Util.handleExceptionNoRestart("Error downloading config from USB key:", e);
                    throw new Exception("Your VPN Client configuration could not be downloaded from the USB key.  Please try again.");
                }
            }

            vpnNode.completeConfig();
        }
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        methodButtonGroup = new javax.swing.ButtonGroup();
        jLabel2 = new javax.swing.JLabel();
        serverJRadioButton = new javax.swing.JRadioButton();
        serverJTextField = new javax.swing.JTextField();
        serverJLabel1 = new javax.swing.JLabel();
        passwordJPasswordField = new javax.swing.JPasswordField();
        serverJLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        keyJRadioButton = new javax.swing.JRadioButton();
        refreshKeyJButton = new javax.swing.JButton();
        keyJComboBox = new javax.swing.JComboBox();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>\nPlease specify where your VPN Client configuration should come<br>\nfrom.  You may specify a Server or USB Key.  If you choose USB Key,<br>\nyou must press \"Read USB Key\" to load configurations from the key,<br>\nand then choose a configuration from the drop-down-list.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 400, -1));

        methodButtonGroup.add(serverJRadioButton);
        serverJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        serverJRadioButton.setSelected(true);
        serverJRadioButton.setText("<html><b>Download from Server</b></html>");
        serverJRadioButton.setOpaque(false);
        serverJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    serverJRadioButtonActionPerformed(evt);
                }
            });

        add(serverJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 100, -1, -1));

        serverJTextField.setColumns(19);
        serverJTextField.setMaximumSize(new java.awt.Dimension(213, 19));
        serverJTextField.setMinimumSize(new java.awt.Dimension(213, 19));
        add(serverJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 120, 213, 19));

        serverJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        serverJLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        serverJLabel1.setText("Server IP Address:");
        add(serverJLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 122, -1, -1));

        passwordJPasswordField.setMaximumSize(new java.awt.Dimension(213, 19));
        passwordJPasswordField.setMinimumSize(new java.awt.Dimension(213, 19));
        passwordJPasswordField.setPreferredSize(new java.awt.Dimension(213, 19));
        add(passwordJPasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 140, 213, 19));

        serverJLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        serverJLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        serverJLabel2.setText("Password:");
        add(serverJLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(157, 142, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/node/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        methodButtonGroup.add(keyJRadioButton);
        keyJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        keyJRadioButton.setText("<html><b>Download from USB Key</b></html>");
        keyJRadioButton.setOpaque(false);
        keyJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    keyJRadioButtonActionPerformed(evt);
                }
            });

        add(keyJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 170, -1, -1));

        refreshKeyJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        refreshKeyJButton.setText("Read USB Key");
        refreshKeyJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    refreshKeyJButtonActionPerformed(evt);
                }
            });

        add(refreshKeyJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 190, -1, -1));

        keyJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        add(keyJComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 190, 180, -1));

    }//GEN-END:initComponents

    private void refreshKeyJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshKeyJButtonActionPerformed
        try{
            keyJComboBox.removeAllItems();
            java.util.List<String> usbList = vpnNode.getAvailableUsbList();
            if( usbList.size() == 0 ){
                keyJComboBox.addItem(NO_CONFIGURATIONS);
                keyJComboBox.setSelectedItem(NO_CONFIGURATIONS);
                MOneButtonJDialog.factory((Window)this.getTopLevelAncestor(), "OpenVPN Setup Wizard Warning",
                                          "The USB Key was read, but no configurations were found.  " +
                                          "Please make sure there are valid configurations on your USB Key and then try again.",
                                          "OpenVPN Setup Wizard Warning", "Warning");
            }
            else{
                for( String entry : usbList )
                    keyJComboBox.addItem(entry);
                keyRead = true;
            }
        }
        catch(Exception e){
            MOneButtonJDialog.factory((Window)this.getTopLevelAncestor(), "OpenVPN Setup Wizard Warning",
                                      "The USB Key could not be read.  Please make sure the key is properly inserted, and try again.",
                                      "OpenVPN Setup Wizard Warning", "Warning");
        }
    }//GEN-LAST:event_refreshKeyJButtonActionPerformed

    private void keyJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyJRadioButtonActionPerformed
        setServerSelectedDependency(false);
    }//GEN-LAST:event_keyJRadioButtonActionPerformed

    private void serverJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverJRadioButtonActionPerformed
        setServerSelectedDependency(true);
    }//GEN-LAST:event_serverJRadioButtonActionPerformed

    private void setServerSelectedDependency(boolean enabled){
        serverJLabel1.setEnabled(enabled);
        serverJLabel2.setEnabled(enabled);
        serverJTextField.setEnabled(enabled);
        passwordJPasswordField.setEnabled(enabled);
        refreshKeyJButton.setEnabled(!enabled);
        keyJComboBox.setEnabled(!enabled);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JComboBox keyJComboBox;
    private javax.swing.JRadioButton keyJRadioButton;
    private javax.swing.ButtonGroup methodButtonGroup;
    private javax.swing.JPasswordField passwordJPasswordField;
    private javax.swing.JButton refreshKeyJButton;
    private javax.swing.JLabel serverJLabel1;
    private javax.swing.JLabel serverJLabel2;
    private javax.swing.JRadioButton serverJRadioButton;
    private javax.swing.JTextField serverJTextField;
    // End of variables declaration//GEN-END:variables

}
