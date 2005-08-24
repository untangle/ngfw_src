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

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        Vector tempRowVector;
	
        // outgoing mail server
        tempRowVector = (Vector) dataVector.elementAt(0);
	String outgoingMailServer = (String) tempRowVector.elementAt(3);
	String outgoingMailServerDetails = (String) tempRowVector.elementAt(4);
        
	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    MailSettings mailSettings = Util.getAdminManager().getMailSettings();
	    mailSettings.setSmtpHost( outgoingMailServer ); 
	    Util.getAdminManager().setMailSettings( (MailSettings) mailSettings );
	}

    }
    
    public Vector generateRows(Object settings){
	MailSettings mailSettings = Util.getAdminManager().getMailSettings();
        Vector allRows = new Vector(1);
	int rowIndex = 0;
        Vector tempRow;

        // outgoing mail server
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "outgoing email server" );
        tempRow.add( mailSettings.getSmtpHost() );
        tempRow.add( "An SMTP email host (either IP address or hostname), which is required to send internal report emails." );
        allRows.add( tempRow );

        return allRows;
    }
}
