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

import java.util.*;
import javax.swing.*;

import com.untangle.gui.main.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;


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



    public JScrollPane getJScrollPane(){ return mPipelineJScrollPane; }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        mPipelineJScrollPane = new javax.swing.JScrollPane();
        transformJPanel = new MRackJPanel();
        scrollbarBackground = new com.untangle.gui.widgets.MTiledIconLabel();

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 51, 51));
        setMinimumSize(new java.awt.Dimension(800, 500));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(800, 500));
        mPipelineJScrollPane.setBackground(new java.awt.Color(51, 51, 51));
        mPipelineJScrollPane.setBorder(null);
        mPipelineJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mPipelineJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
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

        scrollbarBackground.setBackground(new java.awt.Color(204, 51, 0));
        scrollbarBackground.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/pipeline/VerticalScrollBar42x100.png")));
        scrollbarBackground.setMaximumSize(null);
        scrollbarBackground.setMinimumSize(null);
        scrollbarBackground.setOpaque(true);
        scrollbarBackground.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 1.0;
        add(scrollbarBackground, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane mPipelineJScrollPane;
    private javax.swing.JLabel scrollbarBackground;
    private javax.swing.JPanel transformJPanel;
    // End of variables declaration//GEN-END:variables


}
