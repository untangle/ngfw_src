/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam.gui;

import com.metavize.tran.spam.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.transform.*;

import com.metavize.mvvm.tran.Transform;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<SpamLog> requestLogList = (List<SpamLog>) ((SpamTransform)super.logTransform).getEventLogs(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( SpamLog requestLog : requestLogList ){
            event = new Vector();
            event.add( requestLog.getTimeStamp().toString() );
	    event.add( requestLog.getAction() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
	    event.add( requestLog.getSubject() );
	    event.add( requestLog.getReceiver() );
	    event.add( requestLog.getSender() );
	    event.add( Float.toString(requestLog.getScore()) );
            event.add( requestLog.getDirection().getDirectionName() );
            event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getServerPort()).toString() );
            allEvents.insertElementAt(event,0);
        }
	
        return allEvents;
    }
    

    
    class LogTableModel extends MSortedTableModel{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ           def
	    addTableColumn( tableColumnModel,  0,  125, true,  false, false, false, String.class, null, "timestamp" );
	    addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
	    addTableColumn( tableColumnModel,  2,  155, true,  false, false, false, String.class, null, sc.html("client") );
	    addTableColumn( tableColumnModel,  3,  100, true,  false, false, false, String.class, null, "subject" );
	    addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, "receiver" );
	    addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, "sender" );
	    addTableColumn( tableColumnModel,  6,  100, true,  false, false, false, String.class, null, sc.html("spam<br>score") );
	    addTableColumn( tableColumnModel,  7,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );
	    addTableColumn( tableColumnModel,  8,  155, true,  false, false, false, String.class, null, "server" );
	    return tableColumnModel;
	}

	public void generateSettings(Object settings, boolean validateOnly) throws Exception {}
	
	public Vector generateRows(Object settings) {
	    return LogJPanel.this.generateRows(null);                                                                              
	}
	
    }       

}
