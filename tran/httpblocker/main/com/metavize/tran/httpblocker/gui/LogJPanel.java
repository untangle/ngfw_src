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

    private static final String BLOCKED_EVENTS_STRING = "Website blocked events";

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
	queryJComboBox.addItem(BLOCKED_EVENTS_STRING);
    }

    protected void refreshSettings(){
	settings = ((HttpBlocker)super.logTransform).getEvents(depthJSlider.getValue(),
							       queryJComboBox.getSelectedItem().equals(BLOCKED_EVENTS_STRING));
    }
    
    class LogTableModel extends MSortedTableModel{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ               def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
	    addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
	    addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "client" );
	    addTableColumn( tableColumnModel,  3,  200, true,  false, false, true,  String.class, null, "request" );
	    addTableColumn( tableColumnModel,  4,  140, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("direction") );
	    addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, String.class, null, "server" );
	    
	    return tableColumnModel;
	}
	
	public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}
	
	public Vector<Vector> generateRows(Object settings){
	    List<HttpRequestLog> requestLogList = (List<HttpRequestLog>) settings;
	    Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
	    Vector event;
	    
	    for( HttpRequestLog requestLog : requestLogList ){
		event = new Vector(7);
		event.add( requestLog.timeStamp() );
		event.add( requestLog.getAction().toString() );
		event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
		event.add( requestLog.getUrl().toString() );
		event.add( requestLog.getReason().toString() );
		event.add( requestLog.getDirection().getDirectionName() );
		event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getServerPort()).toString() );
		allEvents.add( event );
	    }
	    
	    return allEvents;
	}
	
    }

}
