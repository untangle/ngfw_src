/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: GeneralConfigJPanel.java,v 1.11 2005/03/19 02:16:52 inieves Exp $
 */
package com.metavize.tran.email.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.tran.email.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {
    
    public GeneralConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel(transformContext);
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW), 120); /* description */
    private static final StringConstants sc = StringConstants.getInstance();


    GeneralTableModel(TransformContext transformContext){
        super(transformContext);        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, "setting value");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    public EmailSettings generateSettings(Vector dataVector){
        Vector tempRowVector;
	Iterator dataVectorIterator = dataVector.iterator();
        EmailTransform tran = (EmailTransform) transformContext.transform();
        EmailSettings settings = tran.getEmailSettings();
        CTLDefinition control = settings.getControl();
	SSCTLDefinition spamInboundCTL = settings.getSpamInboundCtl();
	SSCTLDefinition spamOutboundCTL = settings.getSpamOutboundCtl();
	VSCTLDefinition virusInboundCTL = settings.getVirusInboundCtl();
	VSCTLDefinition virusOutboundCTL = settings.getVirusOutboundCtl();

        // pop3Postmaster
        tempRowVector = (Vector) dataVectorIterator.next();
        control.setPop3Postmaster( (String)tempRowVector.elementAt(3) );
        control.setPop3PostmasterDetails( (String)tempRowVector.elementAt(4) );
        
        // imap4Postmaster
        tempRowVector = (Vector) dataVectorIterator.next();
        control.setImap4Postmaster( (String)tempRowVector.elementAt(3) );
        control.setImap4PostmasterDetails( (String)tempRowVector.elementAt(4) );
        
        // msgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
        control.setMsgSzLimit( ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024 );
        control.setMsgSzLimitDetails( (String) tempRowVector.elementAt(4) );
        
	// spamMsgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
        control.setSpamMsgSzLimit( ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024 );
        control.setSpamSzLimitDetails( (String) tempRowVector.elementAt(4) );
        
	// virusMsgSzLimit
        tempRowVector = (Vector) dataVectorIterator.next();
        control.setVirusMsgSzLimit( ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue()*1024 );
        control.setVirusSzLimitDetails( (String) tempRowVector.elementAt(4) );

	/*        
	// copyOnBlock inbound Spam
	tempRowVector = (Vector) dataVectorIterator.next();
	spamInboundCTL.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	spamInboundCTL.setCopyOnBlockDetails( (String) tempRowVector.elementAt(4) );

	// copyOnBlock outbound Spam
	tempRowVector = (Vector) dataVectorIterator.next();
	spamOutboundCTL.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	spamOutboundCTL.setCopyOnBlockDetails( (String) tempRowVector.elementAt(4) );

	// copyOnBlock inbound Virus
	tempRowVector = (Vector) dataVectorIterator.next();
	virusInboundCTL.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusInboundCTL.setCopyOnBlockDetails( (String) tempRowVector.elementAt(4) );

	// copyOnBlock outbound Virus
	tempRowVector = (Vector) dataVectorIterator.next();
	virusOutboundCTL.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusOutboundCTL.setCopyOnBlockDetails( (String) tempRowVector.elementAt(4) );
	*/

        // generateCriticalAlerts
        /*
        tempRowVector = (Vector) dataVector.elementAt(3);
        control.setAlertsOnParseException().generateCriticalAlerts( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        control.setAlertsDetails( (String)tempRowVector.elementAt(4) );
        
        // generateSummaryAlerts
        tempRowVector = (Vector) dataVector.elementAt(4);
        control.setAlertsOnParseException().generateSummaryAlerts( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        control.setLogDetails( (String)tempRowVector.elementAt(4) );
        */
        
        settings.setControl( control );
        return settings;
    }
    
    public Vector generateRows(Object settin){
        EmailSettings settings = (EmailSettings) settin;
        Vector allRows = new Vector(4);
        Vector tempRowVector;
        
        CTLDefinition control = settings.getControl();
	SSCTLDefinition spamInboundCTL = settings.getSpamInboundCtl();
	SSCTLDefinition spamOutboundCTL = settings.getSpamOutboundCtl();
	VSCTLDefinition virusInboundCTL = settings.getVirusInboundCtl();
	VSCTLDefinition virusOutboundCTL = settings.getVirusOutboundCtl();

        /*
        Alerts alerts = control.getAlertsOnParseException();
        if(alerts == null){
            alerts = new Alerts();
            control.setAlertsOnParseException(alerts);
        }
        */
        
        // pop3Postmaster
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(1));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("POP3 Postmaster");
        tempRowVector.add( control.getPop3Postmaster() );
        tempRowVector.add( control.getPop3PostmasterDetails() );
        allRows.add( tempRowVector );
        
        // imap4Postmaster
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(2));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("IMAP4 Postmaster");
        tempRowVector.add( control.getImap4Postmaster() );
        tempRowVector.add( control.getImap4PostmasterDetails() );
        allRows.add( tempRowVector );
        
        // msgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(3));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("max message size to scan (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getMsgSzLimitDetails() );
        allRows.add( tempRowVector );

	// spamMsgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(4));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("max message size to scan for spam (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getSpamMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getSpamSzLimitDetails() );
        allRows.add( tempRowVector );

	// virusMsgSzLimit
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(5));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("max message size to scan for viruses (KB)");
        tempRowVector.add( new SpinnerNumberModel((int)control.getVirusMsgSzLimit()/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MIN/1024, (int)CTLDefinition.MSG_SZ_LIMIT_MAX/1024, 128) );
        tempRowVector.add( control.getMsgSzLimitDetails() );
        allRows.add( tempRowVector );
	/*
        // copyOnBlock inbound Spam
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(6));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("copy blocked inbound Spam");
        tempRowVector.add( spamInboundCTL.isCopyOnBlock() );
        tempRowVector.add( spamInboundCTL.getCopyOnBlockDetails() );
        allRows.add( tempRowVector );

        // copyOnBlock outbound Spam
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(7));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("copy blocked outbound Spam");
        tempRowVector.add( spamOutboundCTL.isCopyOnBlock() );
        tempRowVector.add( spamOutboundCTL.getCopyOnBlockDetails() );
        allRows.add( tempRowVector );

        // copyOnBlock inbound Virus
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(8));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("copy blocked inbound Virus");
        tempRowVector.add( virusInboundCTL.isCopyOnBlock() );
        tempRowVector.add( virusInboundCTL.getCopyOnBlockDetails() );
        allRows.add( tempRowVector );

        // copyOnBlock outbound Virus
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(9));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("copy blocked inbound Virus");
        tempRowVector.add( virusOutboundCTL.isCopyOnBlock() );
        tempRowVector.add( virusOutboundCTL.getCopyOnBlockDetails() );
        allRows.add( tempRowVector );
	*/

        /*
        // generateCriticalAlerts
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(4));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("alert when damaged email arrives");
        tempRowVector.add( new Boolean( control.alertsOnParseException().generateCriticalAlerts() ) );
        tempRowVector.add( control.alertsDetails() );
        allRows.add( tempRowVector );
        
        // generateSummaryAlerts
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(5));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("log when damaged email arrives");
        tempRowVector.add( new Boolean( control.alertsOnParseException().generateSummaryAlerts() ) );
        tempRowVector.add( control.logDetails() );
        allRows.add( tempRowVector );

        */
        
        return allRows;
    }
}
