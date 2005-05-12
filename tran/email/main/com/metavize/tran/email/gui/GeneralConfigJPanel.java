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
package com.metavize.tran.email.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.tran.email.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {
    
    public GeneralConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel();
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW), 120); /* description */


    
    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        Vector tempRowVector;
	Iterator dataVectorIterator = this.getDataVector().iterator();

        // pop3Postmaster
        tempRowVector = (Vector) dataVectorIterator.next();
	String pop3Postmaster = (String) tempRowVector.elementAt(3);
	String pop3PostmasterDetails = (String) tempRowVector.elementAt(4);
        
        // imap4Postmaster
        tempRowVector = (Vector) dataVectorIterator.next();
        String imap4Postmaster = (String) tempRowVector.elementAt(3);
	String imap4PostmasterDetails = (String) tempRowVector.elementAt(4);
        
        // msgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
	int msgSzLimit = ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024;
	String msgSzLimitDetails = (String) tempRowVector.elementAt(4);
        
	// spamMsgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
	int spamMsgSzLimit = ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024;
	String spamMsgSzLimitDetails = (String) tempRowVector.elementAt(4);
        
	// virusMsgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
	int virusMsgSzLimit = ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024;
	String virusMsgSzLimitDetails = (String) tempRowVector.elementAt(4);
        
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    EmailSettings emailSettings = (EmailSettings) settings;
	    CTLDefinition control = emailSettings.getControl();
	    control.setPop3Postmaster( pop3Postmaster );
	    control.setPop3PostmasterDetails( pop3PostmasterDetails );
	    control.setImap4Postmaster( imap4Postmaster );
	    control.setImap4PostmasterDetails( imap4PostmasterDetails );
	    control.setMsgSzLimit( msgSzLimit );
	    control.setMsgSzLimitDetails( msgSzLimitDetails );
	    control.setSpamMsgSzLimit( spamMsgSzLimit );
	    // control.setSpamMsgSzLimitDetails( spamMsgSzLimitDetails );
	    control.setVirusMsgSzLimit( virusMsgSzLimit );
	    // control.setVirusMsgSzLimitDetails( virusMsgSzLimitDetials );
	}

    }
    
    public Vector generateRows(Object settings){
        EmailSettings emailSettings = (EmailSettings) settings;
        Vector allRows = new Vector(4);
        Vector tempRowVector;
        
        CTLDefinition control = emailSettings.getControl();
	SSCTLDefinition spamInboundCTL = emailSettings.getSpamInboundCtl();
	SSCTLDefinition spamOutboundCTL = emailSettings.getSpamOutboundCtl();
	VSCTLDefinition virusInboundCTL = emailSettings.getVirusInboundCtl();
	VSCTLDefinition virusOutboundCTL = emailSettings.getVirusOutboundCtl();

        
        // pop3Postmaster
        tempRowVector = new Vector(4);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(1));
        tempRowVector.add("POP3 Postmaster");
        tempRowVector.add( control.getPop3Postmaster() );
        tempRowVector.add( control.getPop3PostmasterDetails() );
        allRows.add( tempRowVector );
        
        // imap4Postmaster
        tempRowVector = new Vector(4);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(2));
        tempRowVector.add("IMAP4 Postmaster");
        tempRowVector.add( control.getImap4Postmaster() );
        tempRowVector.add( control.getImap4PostmasterDetails() );
        allRows.add( tempRowVector );
        
        // msgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(3));
        tempRowVector.add("max message size to scan (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getMsgSzLimitDetails() );
        allRows.add( tempRowVector );

	// spamMsgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(4));
        tempRowVector.add("max message size to scan for spam (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getSpamMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getSpamSzLimitDetails() );
        allRows.add( tempRowVector );

	// virusMsgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add(new Integer(5));
        tempRowVector.add("max message size to scan for viruses (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getVirusMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getVirusSzLimitDetails() );
        allRows.add( tempRowVector );

        
        return allRows;
    }
}
