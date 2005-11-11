/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.MPasswordField;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.security.*;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.quarantine.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class QuarantineGeneralSettingsJPanel extends MEditTableJPanel {

    public QuarantineGeneralSettingsJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
	MailTransform mailTransform = ((MailTransform)transformContext.transform());

        QuarantineGeneralSettingsTableModel tableModel = new QuarantineGeneralSettingsTableModel(mailTransform);
        this.setTableModel( tableModel );
    }
}


class QuarantineGeneralSettingsTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 215; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    private MailTransform mailTransform;

    public QuarantineGeneralSettingsTableModel(MailTransform mailTransform){
	this.mailTransform = mailTransform;
    }
    
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
	
        // MAX HOLDING TIME
        tempRowVector = tableVector.elementAt(0);
	int maxHoldingDays = (Integer) ((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue();
        
	// FROM ADDRESS
        tempRowVector = tableVector.elementAt(1);
	String fromAddress = (String) tempRowVector.elementAt(3);

	// SENDING TIME
        tempRowVector = tableVector.elementAt(2);
	Date sendingTime = (Date) ((SpinnerDateModel)tempRowVector.elementAt(3)).getValue();
	Calendar tempCalendar = new GregorianCalendar();
	tempCalendar.setTime(sendingTime);
	int sendingHour = tempCalendar.get(Calendar.HOUR_OF_DAY);
	int sendingMinute = tempCalendar.get(Calendar.MINUTE);

	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    MailTransformSettings mailTransformSettings = mailTransform.getMailTransformSettings();
	    QuarantineSettings quarantineSettings = mailTransformSettings.getQuarantineSettings();

	    quarantineSettings.setMaxMailIntern( ((long)maxHoldingDays) * 1440l * 60 * 1000l );
	    quarantineSettings.setDigestFrom( fromAddress );
	    quarantineSettings.setDigestHourOfDay( sendingHour );
	    quarantineSettings.setDigestMinuteOfDay( sendingMinute );

	    mailTransformSettings.setQuarantineSettings( quarantineSettings );
	    mailTransform.setMailTransformSettings( mailTransformSettings );
	}
    }
    
    public Vector<Vector> generateRows(Object settings){
	MailTransformSettings mailTransformSettings = mailTransform.getMailTransformSettings();
	QuarantineSettings quarantineSettings = mailTransformSettings.getQuarantineSettings();

        Vector<Vector> allRows = new Vector<Vector>(3);
	int rowIndex = 0;
        Vector tempRow;

        // MAX HOLDING TIME
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Maximum Holding Time (days)" );
        tempRow.add( new SpinnerNumberModel( (int)(quarantineSettings.getMaxMailIntern()/(1440l*60l*1000l)), 1, 60, 1) );
        tempRow.add( "The number of days a quarantined email will be held, before it is automatically purged. (min=1, max=60)" );
        allRows.add( tempRow );

	// SENDING ADDRESS
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Digest From Address" );
        tempRow.add( quarantineSettings.getDigestFrom() );
        tempRow.add( "The \"From Address\" of the digest email that will be sent to inform people that some of their email has been quarantined." );
        allRows.add( tempRow );

	// SENDING TIME
	rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Digest Sending Time" );
	Calendar calendar = new GregorianCalendar();
	calendar.set(Calendar.HOUR_OF_DAY, quarantineSettings.getDigestHourOfDay());
	calendar.set(Calendar.MINUTE, quarantineSettings.getDigestMinuteOfDay());
	SpinnerDateModel dateModel = new SpinnerDateModel(calendar.getTime(), null, null, Calendar.MINUTE);
        tempRow.add( dateModel );
        tempRow.add( "The time, each day, that a digest email will be sent to inform people that some of their email has been quarantined." );
        allRows.add( tempRow );
	
        return allRows;
    }
}
