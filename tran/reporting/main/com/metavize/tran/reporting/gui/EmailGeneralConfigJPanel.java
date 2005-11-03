/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EmailGeneralConfigJPanel.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.reporting.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.MPasswordField;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.virus.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class EmailGeneralConfigJPanel extends MEditTableJPanel {

    public EmailGeneralConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        EmailGeneralTableModel tableModel = new EmailGeneralTableModel();
        this.setTableModel( tableModel );
    }
}


class EmailGeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 215; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, true,  true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        Vector tempRowVector;
	
        // OUTGOING MAIL SERVER
        tempRowVector = tableVector.elementAt(0);
	String smtpHost = (String) tempRowVector.elementAt(3);
	String smtpHostDetails = (String) tempRowVector.elementAt(4);
        
	// OUTGOING MAIL PORT
        tempRowVector = tableVector.elementAt(1);
	int smtpPort = (Integer) ((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue();
	String smtpPortDetails = (String) tempRowVector.elementAt(4);

	// FROM ADDRESS
        tempRowVector = tableVector.elementAt(2);
	String fromAddress = (String) tempRowVector.elementAt(3);
	String fromAddressDetails = (String) tempRowVector.elementAt(4);	

	// SMTP AUTHENTICATION LOGIN
	tempRowVector = tableVector.elementAt(3);
	String authUser = (String) tempRowVector.elementAt(3);
	String authUserDetails = (String) tempRowVector.elementAt(4);

	// SMTP AUTHENTICATION PASSWORD
	tempRowVector = tableVector.elementAt(4);
	String authPass = new String(((MPasswordField) tempRowVector.elementAt(3)).getPassword());
	String authPassDetails = (String) tempRowVector.elementAt(4);

	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    MailSettings mailSettings = Util.getAdminManager().getMailSettings();

	    // WARN THE USER IF EMAIL ADDRESSES ^ MAIL SERVER ARE SET xxx this should not be in the save.. -> above
	    if( (smtpHost.length() != 0) && (mailSettings.getReportEmail().length() == 0) ){
		new MOneButtonJDialog("EdgeReport", "<html>You have set your Outgoing Email Server, but you have not specified any email recipients.  You must specify both an Outgoing Email Server and at least one email recipient in order to receive reports.</html>");
	    }
	    else if( (smtpHost.length() == 0) && (mailSettings.getReportEmail().length() != 0) ){
		new MOneButtonJDialog("EdgeReport", "<html>You have set at least one Email Recipient, but you have not specified an Outgoing Email Server.  You must specify both an Outgoing Email Server and at least one email recipient in order to receive reports.</html>");
	    }
	    
	    mailSettings.setSmtpHost( (smtpHost.length()==0?null:smtpHost) );
	    mailSettings.setSmtpPort( smtpPort );
	    mailSettings.setFromAddress( fromAddress );
	    mailSettings.setAuthUser( (authUser.length()==0?null:authUser) );
	    mailSettings.setAuthPass( (authPass.length()==0?null:authPass) );
	    Util.getAdminManager().setMailSettings( (MailSettings) mailSettings );
	}

    }
    
    public Vector<Vector> generateRows(Object settings){
	MailSettings mailSettings = Util.getAdminManager().getMailSettings();
        Vector<Vector> allRows = new Vector<Vector>(1);
	int rowIndex = 0;
        Vector tempRow;

        // OUTGOING MAIL SERVER
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Outgoing Email Server" );
        tempRow.add( (mailSettings.getSmtpHost()==null?"":mailSettings.getSmtpHost()) );
        tempRow.add( "An SMTP email host (either IP address or hostname), which is required to send internal report emails." );
        allRows.add( tempRow );

	// OUTGOING MAIL SERVER PORT
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Outgoing Email Server Port" );
        tempRow.add( new SpinnerNumberModel( mailSettings.getSmtpPort(), 0, 65536, 1) );
        tempRow.add( "An SMTP email server port, which is required to send internal report emails." );
        allRows.add( tempRow );

	// FROM ADDRESS
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "From address" );
        tempRow.add( mailSettings.getFromAddress() );
        tempRow.add( "The email address which report emails will appear to have been sent from." );
        allRows.add( tempRow );
	
	// SMTP AUTHENTICATION LOGIN
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "SMTP Authentication Login" );
        tempRow.add( (mailSettings.getAuthUser()==null?"":mailSettings.getAuthUser()) );
        tempRow.add( "The login name to use for SMTP Authentication."
		     + "  If this field (or the password field below) is blank, SMTP Authentication will not be used." );
        allRows.add( tempRow );

	// SMTP AUTHENTICATION PASSWORD
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "SMTP Authentication Password" );
        tempRow.add( new MPasswordField((mailSettings.getAuthPass()==null?"":mailSettings.getAuthPass())) );
        tempRow.add( "The password to use for SMTP Authentication."
		     + "  If this field (or the login name field above) is blank, SMTP Authentication will not be used." );
        allRows.add( tempRow );

        return allRows;
    }
}
