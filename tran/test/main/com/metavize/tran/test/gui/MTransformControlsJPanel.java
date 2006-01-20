/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.test.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import com.metavize.mvvm.client.*;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);        
    }

    protected void generateGui(){
	// SOME LIST //
	javax.swing.JPanel someJPanel = new javax.swing.JPanel();
  addTab(NAME_SOME_LIST, null, someJPanel);

  super.mTabbedPane.addTab("bscott test", null, new AddrBookTestPanel());
  
	//super.savableMap.put(NAME_SOME_LIST, someJPanel);
	//super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

	// EVENT LOG /////
	//LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	//addTab(NAME_LOG, null, logJPanel);
	//addShutdownable(NAME_LOG, logJPanel);
    }


  class AddrBookTestPanel extends JPanel {

    private JTextArea m_textArea;
  
    AddrBookTestPanel() {
      setLayout(new GridBagLayout());

      GridBagConstraints gbc = new GridBagConstraints();

      gbc.gridx=0;
      gbc.gridy=0;

      JButton b1 = new JButton("1");
      b1.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b1Pushed();
        }
      });

      add(b1, gbc);      

      gbc.gridx=0;
      gbc.gridy=1;      

      JButton b2 = new JButton("2");
      b2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b2Pushed();
        }
      });

      add(b2, gbc);


            

      gbc.gridx=0;
      gbc.gridy=2;      

      
      JButton b3 = new JButton("2");
      b3.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          b3Pushed();
        }
      });
      add(b3, gbc);      

      m_textArea = new JTextArea("Stuff Goes Here", 30, 250);
      gbc.gridx=0;
      gbc.gridy=3;
      gbc.gridwidth = 2;
      gbc.weightx=100;
      gbc.fill = GridBagConstraints.HORIZONTAL;


      JScrollPane scrollPane = new JScrollPane(m_textArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        
      add(scrollPane/*scrollPane*/, gbc);
      




      
    }

    private void b1Pushed() {
      MvvmRemoteContext ctx = Util.getMvvmContext();
      m_textArea.append("b1 pushed\n");
    }
    private void b2Pushed() {
      m_textArea.append("b2 pushed\n");
    }
    private void b3Pushed() {
      m_textArea.append("b3 pushed\n");
    }        
  
  }
    
}
