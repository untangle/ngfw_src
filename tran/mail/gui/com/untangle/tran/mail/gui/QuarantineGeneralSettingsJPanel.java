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

package com.untangle.tran.mail.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.configuration.EmailCompoundSettings;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.security.*;
import com.untangle.tran.mail.*;
import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.quarantine.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class QuarantineGeneralSettingsJPanel extends MEditTableJPanel {

    public QuarantineGeneralSettingsJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        super.setFillJButtonEnabled(false);
        
        // create actual table model
        QuarantineGeneralSettingsTableModel tableModel = new QuarantineGeneralSettingsTableModel();
        this.setTableModel( tableModel );
    }
}


class QuarantineGeneralSettingsTableModel extends MSortedTableModel<EmailCompoundSettings>{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 215; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    public QuarantineGeneralSettingsTableModel(){
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

    public void generateSettings(EmailCompoundSettings emailCompoundSettings,
				 Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        Vector tempRowVector;
	
        // MAX HOLDING TIME
        tempRowVector = tableVector.elementAt(0);
        int maxHoldingDays = (Integer) ((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue();
        
        // SENDING TIME
        tempRowVector = tableVector.elementAt(1);
        Date sendingTime = (Date) ((SpinnerDateModel)tempRowVector.elementAt(3)).getValue();
        Calendar tempCalendar = new GregorianCalendar();
        tempCalendar.setTime(sendingTime);
        int sendingHour = tempCalendar.get(Calendar.HOUR_OF_DAY);
        int sendingMinute = tempCalendar.get(Calendar.MINUTE);

        // MAX STORAGE SPACE
        //tempRowVector = tableVector.elementAt(2);
        //int totalSize = (Integer) ((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue();


        // SAVE SETTINGS //////////
        if( !validateOnly ){
            QuarantineSettings quarantineSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings()).getQuarantineSettings();
            
            quarantineSettings.setMaxMailIntern( ((long)maxHoldingDays) * 1440l * 60 * 1000l );
            quarantineSettings.setDigestHourOfDay( sendingHour );
            quarantineSettings.setDigestMinuteOfDay( sendingMinute );
            //quarantineSettings.setMaxQuarantineTotalSz( ((long) totalSize)*1024l*1024l*1024l );
        }
    }
    
    public Vector<Vector> generateRows(EmailCompoundSettings emailCompoundSettings){
        MailTransformCompoundSettings mailTransformCompoundSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings());
        QuarantineSettings quarantineSettings = mailTransformCompoundSettings.getQuarantineSettings();
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
	
        // MAX SPACE
        /*
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "Maximum Quarantine Space (MB)" );
        int min = (int) mailTransformCompoundSettings.getMinStorageGigs();
        int max = (int) mailTransformCompoundSettings.getMaxStorageGigs();
        tempRow.add( new SpinnerNumberModel( (int)(quarantineSettings.getMaxQuarantineTotalSz()/1024l/1024l/1024l), min, max, 1) );
        tempRow.add( "This is the maximum amount of disk space (in MB) that will be used to quarantine emails." );
        allRows.add( tempRow );
        */

        return allRows;
    }
}
