/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.gui.util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.BorderLayout;

public class EmailDetectionJPanel extends JPanel{
    
    private JLabel messageJLabel;
    private String detectedMessage = "<html>eMail SpamGuard detected.<br>eMail virus scanning settings are located in the eMail SpamGuard.<br><b>AntiVirus Scanner does not need to be turned \"on\" to scan eMail for viruses.</b></html>";
    private String undetectedMessage = "<html>No eMail SpamGuard detected.<br>eMail will not be scanned for viruses.<br>Please procure and install the eMail SpamGuard to gain eMail virus scanning capabilities.</html>";


    public EmailDetectionJPanel(){
	super();

	this.setLayout(new BorderLayout());
	messageJLabel = new JLabel();
	messageJLabel.setFont(new java.awt.Font("Arial", 0, 11));
	messageJLabel.setHorizontalAlignment(JLabel.CENTER);

	this.add(messageJLabel);
    }

    public void setDetected(boolean detected){
	if(detected){
	    messageJLabel.setText(detectedMessage);
	}
	else{
	    messageJLabel.setText(undetectedMessage);
	}
    }
}
