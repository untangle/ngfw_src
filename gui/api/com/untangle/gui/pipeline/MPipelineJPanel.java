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

package com.untangle.gui.pipeline;

import com.untangle.gui.main.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;

import com.untangle.mvvm.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;

//import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;


public class MPipelineJPanel extends javax.swing.JPanel {


    public MPipelineJPanel() {
        Util.setMPipelineJPanel(this);

        // INITIALIZE GUI
        initComponents();
        mPipelineJScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        mPipelineJScrollPane.getVerticalScrollBar().setBorder( new javax.swing.border.EmptyBorder(15, 0, 15, 0) );
        mPipelineJScrollPane.getVerticalScrollBar().setOpaque(false);
		mPipelineJScrollPane.getVerticalScrollBar().setFocusable(false);
    }
    
	public void setStoreWizardButtonVisible(boolean visible){
		storeWizardJButton.setVisible(visible);
	}
	
    public JScrollPane getJScrollPane(){ return mPipelineJScrollPane; }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                jPanel1 = new javax.swing.JPanel();
                storeWizardJButton = new javax.swing.JButton();
                mPipelineJScrollPane = new javax.swing.JScrollPane();
                transformJPanel = new MRackJPanel();
                scrollbarBackground = new com.untangle.gui.widgets.MTiledIconLabel();

                setLayout(new java.awt.GridBagLayout());

                setBackground(new java.awt.Color(0, 51, 51));
                setMinimumSize(new java.awt.Dimension(800, 500));
                setOpaque(false);
                setPreferredSize(new java.awt.Dimension(800, 500));
                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setOpaque(false);
                storeWizardJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                storeWizardJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconWizard_32x32.png")));
                storeWizardJButton.setText("What should I put in my rack?");
                storeWizardJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                storeWizardJButton.setOpaque(false);
                storeWizardJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                storeWizardJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.insets = new java.awt.Insets(150, 0, 0, 0);
                jPanel1.add(storeWizardJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 42);
                add(jPanel1, gridBagConstraints);

                mPipelineJScrollPane.setBackground(new java.awt.Color(51, 51, 51));
                mPipelineJScrollPane.setBorder(null);
                mPipelineJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                mPipelineJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                mPipelineJScrollPane.setDoubleBuffered(true);
                mPipelineJScrollPane.setMinimumSize(new java.awt.Dimension(720, 21));
                mPipelineJScrollPane.setOpaque(false);
                mPipelineJScrollPane.getViewport().setOpaque(false);
                transformJPanel.setBackground(new java.awt.Color(51, 255, 51));
                transformJPanel.setMaximumSize(null);
                transformJPanel.setOpaque(false);
                mPipelineJScrollPane.setViewportView(transformJPanel);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                add(mPipelineJScrollPane, gridBagConstraints);

                scrollbarBackground.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/pipeline/VerticalScrollBar42x100.png")));
                scrollbarBackground.setDoubleBuffered(true);
                scrollbarBackground.setOpaque(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.weighty = 1.0;
                add(scrollbarBackground, gridBagConstraints);

        }//GEN-END:initComponents

		private void storeWizardJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_storeWizardJButtonActionPerformed
				try{
                    String authNonce = Util.getAdminManager().generateAuthNonce();
					URL newURL = new URL( Util.getServerCodeBase(), "../onlinestore/index.php?option=com_wizard&Itemid=92&" + authNonce);
					((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
				}
				catch(Exception f){
					Util.handleExceptionNoRestart("Error showing store wizard.", f);
				}
		}//GEN-LAST:event_storeWizardJButtonActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane mPipelineJScrollPane;
        private javax.swing.JLabel scrollbarBackground;
        private javax.swing.JButton storeWizardJButton;
        private javax.swing.JPanel transformJPanel;
        // End of variables declaration//GEN-END:variables


}
