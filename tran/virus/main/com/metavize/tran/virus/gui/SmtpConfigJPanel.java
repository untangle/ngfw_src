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


package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.tran.virus.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;
import com.metavize.mvvm.tran.TransformContext;


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


class SmtpTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = 140; /* action if SPAM detected */
    private static final int C5_MW = 190; /* notification if SPAM detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>Virus detected"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("notification if<br>Virus detected"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    private static final String SOURCE_INBOUND = "inbound SMTP";
    private static final String SOURCE_OUTBOUND = "outbound SMTP";

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
	VirusSMTPConfig virusSMTPConfigInbound = null;
	VirusSMTPConfig virusSMTPConfigOutbound = null;

	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            VirusSMTPConfig virusSMTPConfig = new VirusSMTPConfig();
            virusSMTPConfig.setScan( (Boolean) rowVector.elementAt(3) );
	    String actionString = (String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem();
	    SMTPVirusMessageAction messageAction = SMTPVirusMessageAction.getInstance( actionString );
            virusSMTPConfig.setMsgAction( messageAction );
	    String notifyString = (String) ((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem();
	    SMTPNotifyAction notifyAction = SMTPNotifyAction.getInstance( notifyString );
            virusSMTPConfig.setNotifyAction( notifyAction );
            virusSMTPConfig.setNotes( (String) rowVector.elementAt(6) );
	    
	    if( ((String)rowVector.elementAt(2)).equals(SOURCE_INBOUND) ){
		virusSMTPConfigInbound = virusSMTPConfig;
	    }
	    else if( ((String)rowVector.elementAt(2)).equals(SOURCE_OUTBOUND) ){
		virusSMTPConfigOutbound = virusSMTPConfig;
	    }  
        }
	
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    VirusSettings virusSettings = (VirusSettings) settings;
	    virusSettings.setSMTPInbound( virusSMTPConfigInbound );
	    virusSettings.setSMTPOutbound( virusSMTPConfigOutbound );
	}


    }

    public Vector generateRows(Object settings) {
        VirusSettings virusSettings = (VirusSettings) settings;
        Vector allRows = new Vector();

	// INBOUND
	Vector inboundRow = new Vector();
        VirusSMTPConfig virusSMTPConfigInbound = virusSettings.getSMTPInbound();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( new Integer(1) );
        inboundRow.add( SOURCE_INBOUND );
        inboundRow.add( virusSMTPConfigInbound.getScan() );
        ComboBoxModel inboundActionComboBoxModel =  super.generateComboBoxModel( SMTPVirusMessageAction.getValues(), virusSMTPConfigInbound.getMsgAction() );
        inboundRow.add( inboundActionComboBoxModel );
        ComboBoxModel inboundNotificationComboBoxModel = super.generateComboBoxModel( SMTPNotifyAction.getValues(), virusSMTPConfigInbound.getNotifyAction() );
        inboundRow.add( inboundNotificationComboBoxModel );
        inboundRow.add( virusSMTPConfigInbound.getNotes() );
	allRows.add(inboundRow);

	// OUTBOUND
	Vector outboundRow = new Vector();
        VirusSMTPConfig virusSMTPConfigOutbound = virusSettings.getSMTPOutbound();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( new Integer(1) );
        outboundRow.add( SOURCE_OUTBOUND );
        outboundRow.add( virusSMTPConfigOutbound.getScan() );
        ComboBoxModel outboundActionComboBoxModel =  super.generateComboBoxModel( SMTPVirusMessageAction.getValues(), virusSMTPConfigOutbound.getMsgAction() );
        outboundRow.add( outboundActionComboBoxModel );
        ComboBoxModel outboundNotificationComboBoxModel = super.generateComboBoxModel( SMTPNotifyAction.getValues(), virusSMTPConfigOutbound.getNotifyAction() );
        outboundRow.add( outboundNotificationComboBoxModel );
        outboundRow.add( virusSMTPConfigOutbound.getNotes() );
	allRows.add(outboundRow);

        return allRows;
    }
}
