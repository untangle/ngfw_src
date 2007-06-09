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

package com.untangle.node.reporting.gui;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;

import com.untangle.gui.util.Util;

public class BrowserLaunchJPanel extends JPanel implements ActionListener {


    private GridBagConstraints noteJLabelConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private GridBagConstraints launchJButtonConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,10,0), 0, 0);
    private GridBagConstraints launchJLabelConstraints = new GridBagConstraints(0, 2, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

    public BrowserLaunchJPanel(){
        this.setLayout(new GridBagLayout());

        JButton launchJButton;
        launchJButton = new JButton("<html>Show Reports in<br>a Web Browser</html>");
        launchJButton.setFont(new java.awt.Font("Arial", 0, 12));
		launchJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
        launchJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        //launchJButton.setPreferredSize(null);
        //launchJButton.setMaximumSize(null);
        //launchJButton.setSize(null);
        //launchJButton.setPreferredSize(new Dimension(225, 25));
        //launchJButton.setMaximumSize(new Dimension(225, 25));
        //launchJButton.setSize(new Dimension(225, 25));
        launchJButton.addActionListener(this);
        this.add(launchJButton, launchJButtonConstraints);

        try{
            JLabel launchJLabel;
            URL newURL = new URL( Util.getServerCodeBase(), "../reports");
            launchJLabel = new JLabel();
            launchJLabel.setFont(new java.awt.Font("Arial", 0, 11));
            launchJLabel.setHorizontalAlignment(SwingConstants.CENTER);
            launchJLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            launchJLabel.setText("<html><center><br>To view reports through a web browser, go to:<br>" + newURL.toString() + "</center></html>");
            this.add(launchJLabel, launchJLabelConstraints);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error:", f);
        }





    }

    public void actionPerformed(ActionEvent e){

        try{
            String authNonce = Util.getAdminManager().generateAuthNonce();
            URL newURL = new URL( Util.getServerCodeBase(), "../reports/?" + authNonce);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("error launching browser for Untangle Reports", f);
        }

    }

}
