/*
 * $HeadURL:$
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

public class InitialSetupKeyJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_KEY_FORMAT = "The key must be exactly 16 alpha-numeric digits long (excluding dashes and spaces).  " +
        "Please make sure your key is the correct length.";

    public InitialSetupKeyJPanel() {
        initComponents();
        Util.addFocusHighlight(keyJTextField);
    }

    public void initialFocus(){
        keyJTextField.requestFocus();
    }

    String key;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            keyJTextField.setBackground( Color.WHITE );

            key = keyJTextField.getText().replaceAll("-","").replaceAll(" ","").toLowerCase();

            exception = null;

            if( key.length() != 16 ){
                keyJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_KEY_FORMAT);
                return;
            }
        }});

        if( exception != null )
            throw exception;


        if( !validateOnly){
            try{
                InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Key...");
                URL url = Util.getServerCodeBase();
                boolean isActivated = com.untangle.uvm.client.UvmRemoteContextFactory.factory().isActivated( url.getHost(), url.getPort(), 0, Util.isSecureViaHttps() );
                if( !isActivated ){
                    UvmRemoteContext uvmContext = UvmRemoteContextFactory.factory().activationLogin( url.getHost(), url.getPort(),
                                                                                                        key,
                                                                                                        0,
                                                                                                        Util.getClassLoader(),
                                                                                                        Util.isSecureViaHttps() );

                    Util.setUvmContext(uvmContext);
                    KeepAliveThread keepAliveThread = new KeepAliveThread(uvmContext);
                    InitialSetupWizard.setKeepAliveThread(keepAliveThread);
                    InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
                }
            }
            catch(javax.security.auth.login.FailedLoginException fle){
                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
                throw new Exception("That key is not valid.  Please type in a valid key.");
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
        jLabel16 = new javax.swing.JLabel();
        keyJTextField = new javax.swing.JTextField();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please enter your 16-digit activation key. (with or without dashes)<br>\nThe key can be found on the side of your Untangle Server, and also on your QuickStart guide.<br>\n<br>\nIf you downloaded the Untangle Platform, you can acquire a key from http://www.untangle.com/download or by emailing support@untangle.com<br>\n<b>This information is required.</b></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setMinimumSize(new java.awt.Dimension(275, 19));
        jPanel1.setOpaque(false);
        jPanel1.setPreferredSize(new java.awt.Dimension(275, 19));
        jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("Key:");
        jPanel1.add(jLabel16, new java.awt.GridBagConstraints());

        keyJTextField.setColumns(19);
        keyJTextField.setMinimumSize(null);
        keyJTextField.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(keyJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        add(backgroundJPabel, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField keyJTextField;
    // End of variables declaration//GEN-END:variables

}
