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

package com.untangle.gui.configuration;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.jnlp.*;
import javax.swing.JDialog;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.*;
import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.node.*;
import com.untangle.uvm.security.*;
import org.apache.log4j.Logger;

public class NetworkIPJPanel extends javax.swing.JPanel
    implements Savable<NetworkCompoundSettings>, Refreshable<NetworkCompoundSettings> {

    private Logger logger = Logger.getLogger(getClass());

    private MConfigJDialog mConfigJDialog;

    public NetworkIPJPanel(MConfigJDialog mConfigJDialog) {
        initComponents();
        this.mConfigJDialog = mConfigJDialog;
    }

    public void doSave(NetworkCompoundSettings networkCompoundSettings,
                       boolean validateOnly)
        throws Exception { }


    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doRefresh(NetworkCompoundSettings networkCompoundSettings){
        BasicNetworkSettings basicSettings = networkCompoundSettings.getBasicSettings();
    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        dhcpButtonGroup = new javax.swing.ButtonGroup();
        tcpWindowButtonGroup = new javax.swing.ButtonGroup();
        externalAdminButtonGroup = new javax.swing.ButtonGroup();
        internalAdminButtonGroup = new javax.swing.ButtonGroup();
        restrictAdminButtonGroup = new javax.swing.ButtonGroup();
        sshButtonGroup = new javax.swing.ButtonGroup();
        dhcpJPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        launchAlpacaJButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        connectivityTestJButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 470));
        setMinimumSize(new java.awt.Dimension(563, 470));
        setPreferredSize(new java.awt.Dimension(563, 470));
        dhcpJPanel.setLayout(new java.awt.GridBagLayout());

        dhcpJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "External IP Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("<html>The External IP Settings are used to configure the \"External\" network interface to communicate with the Internet or some other external network.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 10);
        dhcpJPanel.add(jLabel11, gridBagConstraints);

        launchAlpacaJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        launchAlpacaJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
        launchAlpacaJButton.setText("Launch Network Settings");
        launchAlpacaJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        launchAlpacaJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launchAlpacaJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        dhcpJPanel.add(launchAlpacaJButton, gridBagConstraints);

        jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        dhcpJPanel.add(jSeparator3, gridBagConstraints);

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("<html>The <b>Connectivity Test</b> tells you if the server can contact DNS and the internet.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        dhcpJPanel.add(jLabel10, gridBagConstraints);

        connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        connectivityTestJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconTest_16x16.png")));
        connectivityTestJButton.setText("Connectivity Test");
        connectivityTestJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        connectivityTestJButton.setMaximumSize(null);
        connectivityTestJButton.setMinimumSize(null);
        connectivityTestJButton.setPreferredSize(null);
        connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectivityTestJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        dhcpJPanel.add(connectivityTestJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(dhcpJPanel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents

    private class TestThread extends Thread {
        public TestThread(){
            setName("MVCLIENT-TestThread");
            setDaemon(true);
            start();
        }
        public void run(){
            if( ((MConfigJDialog)NetworkIPJPanel.this.getTopLevelAncestor()).getSettingsChanged() ){
                TestSaveSettingsJDialog dialog = new TestSaveSettingsJDialog((JDialog)NetworkIPJPanel.this.getTopLevelAncestor());
                if(!dialog.isProceeding())
                    return;

                if( !((MConfigJDialog)NetworkIPJPanel.this.getTopLevelAncestor()).saveSettings() )
                    return;
            }
            try{
                NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog((JDialog)NetworkIPJPanel.this.getTopLevelAncestor());
                connectivityJDialog.setVisible(true);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
            }
        }
    }

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
        if( Util.getIsDemo() )
            return;

        new TestThread();
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    private void launchAlpacaJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchAlpacaJButtonActionPerformed
        try {
            URL scb = Util.getServerCodeBase();
            String q = "?" + Util.getRemoteAdminManager().generateAuthNonce();
            String alpacaNonce = Util.getRemoteAdminManager().getAlpacaNonce();
            q += null == alpacaNonce ? "" : "&argyle=" + alpacaNonce;
            URL url = new URL("http://" + scb.getHost() + "/alpaca/" + q);
            System.out.println("URL: " + url);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(url);
        } catch (MalformedURLException exn) {
            logger.warn("could not launch alpaca", exn);
        } catch (UnavailableServiceException exn) {
            logger.warn("could not launch alpaca", exn);
        }
    }//GEN-LAST:event_launchAlpacaJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton connectivityTestJButton;
    private javax.swing.ButtonGroup dhcpButtonGroup;
    private javax.swing.JPanel dhcpJPanel;
    private javax.swing.ButtonGroup externalAdminButtonGroup;
    private javax.swing.ButtonGroup internalAdminButtonGroup;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JButton launchAlpacaJButton;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.ButtonGroup sshButtonGroup;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    // End of variables declaration//GEN-END:variables


}
