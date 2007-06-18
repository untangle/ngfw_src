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

import com.untangle.gui.node.*;
import com.untangle.uvm.security.*;




public class RemoteCertStatusJPanel extends javax.swing.JPanel
    implements Refreshable<RemoteCompoundSettings> {

    public RemoteCertStatusJPanel() {
        initComponents();
    }

    public void doRefresh(RemoteCompoundSettings remoteCompoundSettings){
        CertInfo certInfo = remoteCompoundSettings.getCurrentCertInfo();

        // SELF SIGNED /////
        try{
            boolean isSelfSigned = certInfo.appearsSelfSigned();
            if( isSelfSigned )
                statusJLabel.setText("Self-Signed");
            else
                statusJLabel.setText("Signed / Trusted");
        }
        catch(Exception e){ statusJLabel.setText(""); }

        // START DATE //
        try{
            String startDate = certInfo.notBefore.toString();
            validStartJLabel.setText(startDate);
        }
        catch(Exception e){ validStartJLabel.setText(""); }

        // END DATE //
        try{
            String endDate = certInfo.notAfter.toString();
            validEndJLabel.setText(endDate);
        }
        catch(Exception e){ validEndJLabel.setText(""); }

        // SUBJECT DN ///
        try{
            String subjectDN = certInfo.subjectDN.toString();
            subjectDNJLabel.setText(subjectDN);
        }
        catch(Exception e){ subjectDNJLabel.setText(""); }

        // ISSUER DN ///
        try{
            String issuerDN = certInfo.issuerDN.toString();
            issuerDNJLabel.setText(issuerDN);
        }
        catch(Exception e){ issuerDNJLabel.setText(""); }

    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        statusJPanel = new javax.swing.JPanel();
        statusJLabel = new javax.swing.JLabel();
        someJLabel = new javax.swing.JLabel();
        serverRoutingJPanel = new javax.swing.JPanel();
        validStartLabelJLabel = new javax.swing.JLabel();
        validStartJLabel = new javax.swing.JLabel();
        validEndLabelJLabel = new javax.swing.JLabel();
        validEndJLabel = new javax.swing.JLabel();
        subjectDNLabelJLabel = new javax.swing.JLabel();
        subjectDNJLabel = new javax.swing.JLabel();
        issuerDNLabelJLabel = new javax.swing.JLabel();
        issuerDNJLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 270));
        setMinimumSize(new java.awt.Dimension(563, 270));
        setPreferredSize(new java.awt.Dimension(563, 270));
        statusJPanel.setLayout(new java.awt.GridBagLayout());

        statusJPanel.setBorder(new javax.swing.border.EtchedBorder());
        statusJPanel.setMaximumSize(new java.awt.Dimension(1061, 29));
        statusJPanel.setMinimumSize(new java.awt.Dimension(1061, 29));
        statusJPanel.setPreferredSize(new java.awt.Dimension(1061, 29));
        statusJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        statusJLabel.setText("Self-Signed");
        statusJLabel.setMaximumSize(null);
        statusJLabel.setMinimumSize(null);
        statusJLabel.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
        statusJPanel.add(statusJLabel, gridBagConstraints);

        someJLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        someJLabel.setText("Current Certificate Type:");
        someJLabel.setMaximumSize(new java.awt.Dimension(200, 15));
        someJLabel.setMinimumSize(new java.awt.Dimension(200, 15));
        someJLabel.setPreferredSize(new java.awt.Dimension(200, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        statusJPanel.add(someJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 20, 40);
        add(statusJPanel, gridBagConstraints);

        serverRoutingJPanel.setLayout(new java.awt.GridBagLayout());

        serverRoutingJPanel.setBorder(new javax.swing.border.EtchedBorder());
        validStartLabelJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        validStartLabelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        validStartLabelJLabel.setText("<html><b>Valid starting:</b></html>");
        validStartLabelJLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        validStartLabelJLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        validStartLabelJLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        serverRoutingJPanel.add(validStartLabelJLabel, gridBagConstraints);

        validStartJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        validStartJLabel.setText("something");
        validStartJLabel.setMaximumSize(null);
        validStartJLabel.setMinimumSize(null);
        validStartJLabel.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
        serverRoutingJPanel.add(validStartJLabel, gridBagConstraints);

        validEndLabelJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        validEndLabelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        validEndLabelJLabel.setText("<html><b>Valid until:</b></html>");
        validEndLabelJLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        validEndLabelJLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        validEndLabelJLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        serverRoutingJPanel.add(validEndLabelJLabel, gridBagConstraints);

        validEndJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        validEndJLabel.setText("something");
        validEndJLabel.setMaximumSize(null);
        validEndJLabel.setMinimumSize(null);
        validEndJLabel.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
        serverRoutingJPanel.add(validEndJLabel, gridBagConstraints);

        subjectDNLabelJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        subjectDNLabelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        subjectDNLabelJLabel.setText("<html><b>Subject DN:</b></html>");
        subjectDNLabelJLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        subjectDNLabelJLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        subjectDNLabelJLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        serverRoutingJPanel.add(subjectDNLabelJLabel, gridBagConstraints);

        subjectDNJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        subjectDNJLabel.setText("something");
        subjectDNJLabel.setMaximumSize(null);
        subjectDNJLabel.setMinimumSize(null);
        subjectDNJLabel.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
        serverRoutingJPanel.add(subjectDNJLabel, gridBagConstraints);

        issuerDNLabelJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        issuerDNLabelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        issuerDNLabelJLabel.setText("<html><b>Issuer DN:</b></html>");
        issuerDNLabelJLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        issuerDNLabelJLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        issuerDNLabelJLabel.setPreferredSize(new java.awt.Dimension(100, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        serverRoutingJPanel.add(issuerDNLabelJLabel, gridBagConstraints);

        issuerDNJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        issuerDNJLabel.setText("something");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 0);
        serverRoutingJPanel.add(issuerDNJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(20, 40, 0, 40);
        add(serverRoutingJPanel, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel issuerDNJLabel;
    private javax.swing.JLabel issuerDNLabelJLabel;
    private javax.swing.JPanel serverRoutingJPanel;
    private javax.swing.JLabel someJLabel;
    private javax.swing.JLabel statusJLabel;
    private javax.swing.JPanel statusJPanel;
    private javax.swing.JLabel subjectDNJLabel;
    private javax.swing.JLabel subjectDNLabelJLabel;
    private javax.swing.JLabel validEndJLabel;
    private javax.swing.JLabel validEndLabelJLabel;
    private javax.swing.JLabel validStartJLabel;
    private javax.swing.JLabel validStartLabelJLabel;
    // End of variables declaration//GEN-END:variables

}
