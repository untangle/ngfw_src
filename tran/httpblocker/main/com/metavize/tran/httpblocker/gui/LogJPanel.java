/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: LogJPanel.java 194 2005-04-06 19:13:55Z rbscott $
 */


package com.metavize.tran.httpblocker.gui;

import java.util.*;
import javax.swing.table.*;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.httpblocker.Action;
import com.metavize.tran.httpblocker.HttpBlocker;
import com.metavize.tran.httpblocker.HttpRequestLog;
import com.metavize.tran.httpblocker.Reason;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){
        List<HttpRequestLog> requestLogList = (List<HttpRequestLog>) ((HttpBlocker)super.logTransform).getEvents(depthJSlider.getValue());
        Vector allEvents = new Vector();
        Vector event = null;

        for( HttpRequestLog requestLog : requestLogList ){
            event = new Vector(7);
            event.add( requestLog.timeStamp() );
	    event.add( requestLog.getAction().toString() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
	    event.add( requestLog.getUrl().toString() );
	    event.add( requestLog.getReason().toString() );
            event.add( requestLog.getDirection().getDirectionName() );
            event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getServerPort()).toString() );
            allEvents.insertElementAt(event,0);
        }

        return allEvents;
    }



    class LogTableModel extends MSortedTableModel{

    public TableColumnModel getTableColumnModel(){
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
        addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
        addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "client" );
        addTableColumn( tableColumnModel,  3,  200, true,  false, false, false, String.class, null, "request" );
        addTableColumn( tableColumnModel,  4,  140, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
        addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );
        addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, String.class, null, "server" );

        return tableColumnModel;
    }

    public void generateSettings(Object settings, boolean validateOnly) throws Exception {}

    public Vector generateRows(Object settings) {
        return LogJPanel.this.generateRows(null);
    }

    }

}
