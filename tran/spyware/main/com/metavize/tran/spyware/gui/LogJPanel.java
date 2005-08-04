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

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.spyware.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<SpywareLog> requestLogList = (List<SpywareLog>) ((Spyware)super.logTransform).getEventLogs(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( SpywareLog requestLog : requestLogList ){
            event = new Vector();
            event.add( requestLog.getCreateDate().toString() );
	    event.add( requestLog.getAction() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
	    event.add( requestLog.getLocation() + " : " + requestLog.getIdent() );
	    event.add( requestLog.getReason() );
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
	    addTableColumn( tableColumnModel,  3,  200, true,  false, false, false, String.class, null, "request" );
	    addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );
	    addTableColumn( tableColumnModel,  6,  155, true,  false, false, false, String.class, null, "server" );
	    return tableColumnModel;
	}

	public void generateSettings(Object settings, boolean validateOnly) throws Exception {}
	
	public Vector generateRows(Object settings) {
	    return LogJPanel.this.generateRows(null);                                                                              
	}
	
    }       

}
