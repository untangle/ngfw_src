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


package com.untangle.tran.clamphish.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.tran.spam.*;
import com.untangle.mvvm.tran.TransformContext;


import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class SmtpConfigJPanel extends MEditTableJPanel {

    public SmtpConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        SmtpTableModel smtpTableModel = new SmtpTableModel();
        this.setTableModel( smtpTableModel );
    }
}


class SmtpTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = 155; /* action if PHISH detected */
    private static final int C5_MW = 190; /* notification if PHISH detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>PHISH detected"));
        addTableColumn( tableColumnModel,  5, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, SpamSMTPConfig.class, null, "");
        return tableColumnModel;
    }

    private static final String SOURCE_INBOUND = "inbound SMTP";
    private static final String SOURCE_OUTBOUND = "outbound SMTP";

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
	SpamSMTPConfig spamSMTPConfigInbound = null;
	SpamSMTPConfig spamSMTPConfigOutbound = null;

	for( Vector rowVector : tableVector ){
            SpamSMTPConfig spamSMTPConfig = (SpamSMTPConfig) rowVector.elementAt(6);
            spamSMTPConfig.setScan( (Boolean) rowVector.elementAt(3) );
            spamSMTPConfig.setMsgAction( (SMTPSpamMessageAction) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
            spamSMTPConfig.setNotes( (String) rowVector.elementAt(5) );
	    
	    if( ((String)rowVector.elementAt(2)).equals(SOURCE_INBOUND) ){
		spamSMTPConfigInbound = spamSMTPConfig;
	    }
	    else if( ((String)rowVector.elementAt(2)).equals(SOURCE_OUTBOUND) ){
		spamSMTPConfigOutbound = spamSMTPConfig;
	    }  
        }
	
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    SpamSettings spamSettings = (SpamSettings) settings;
	    spamSettings.setSMTPInbound( spamSMTPConfigInbound );
	    spamSettings.setSMTPOutbound( spamSMTPConfigOutbound );
	}

    }

    public Vector<Vector> generateRows(Object settings) {
        SpamSettings spamSettings = (SpamSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(2);
	int rowIndex = 0;

	// INBOUND
	rowIndex++;
	Vector inboundRow = new Vector(7);
        SpamSMTPConfig spamSMTPConfigInbound = spamSettings.getSMTPInbound();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( rowIndex );
        inboundRow.add( SOURCE_INBOUND );
        inboundRow.add( spamSMTPConfigInbound.getScan() );
	inboundRow.add( super.generateComboBoxModel(SMTPSpamMessageAction.getValues(), spamSMTPConfigInbound.getMsgAction()) );
        inboundRow.add( spamSMTPConfigInbound.getNotes() );
	inboundRow.add( spamSMTPConfigInbound );
	allRows.add(inboundRow);

	// OUTBOUND
	rowIndex++;
	Vector outboundRow = new Vector(7);
        SpamSMTPConfig spamSMTPConfigOutbound = spamSettings.getSMTPOutbound();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( rowIndex );
        outboundRow.add( SOURCE_OUTBOUND );
        outboundRow.add( spamSMTPConfigOutbound.getScan() );
	outboundRow.add( super.generateComboBoxModel(SMTPSpamMessageAction.getValues(), spamSMTPConfigOutbound.getMsgAction()) );
        outboundRow.add( spamSMTPConfigOutbound.getNotes() );
	outboundRow.add( spamSMTPConfigOutbound );
	allRows.add(outboundRow);

        return allRows;
    }
}
