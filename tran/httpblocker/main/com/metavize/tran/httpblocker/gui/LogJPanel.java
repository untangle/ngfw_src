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

package com.metavize.tran.httpblocker.gui;

import java.util.*;

import com.metavize.gui.widgets.editTable.MLogTableJPanel;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.httpblocker.Action;
import com.metavize.tran.httpblocker.HttpBlocker;
import com.metavize.tran.httpblocker.HttpRequestLog;
import com.metavize.tran.httpblocker.Reason;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform){
        super(transform);
    }

    public Vector generateRows(Object settings){

        List<HttpRequestLog> requestLogList = (List<HttpRequestLog>) ((HttpBlocker)super.logTransform).getEvents(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( HttpRequestLog requestLog : requestLogList ){
            event = new Vector();
            event.add( requestLog.timeStamp().toString() );

            Action action = requestLog.getAction();
            Reason reason = requestLog.getReason();

            if( action == Action.PASS ){
                event.add( "pass" );
                event.add( requestLog.getUrl() );
                event.add( reason.toString() );
            }
            else if( action == Action.BLOCK ){
                event.add( "block" );
                event.add( requestLog.getUrl() );
                event.add( reason.toString() );
            }
            else if( action == null ){
                event.add( "untouched" );
                event.add( requestLog.getUrl() );
                event.add( "no rule applied" );
            }
            event.add( "unknown" );
            event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getSServerPort()).toString() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getCClientPort()).toString() );
            allEvents.insertElementAt(event,0);
        }

        return allEvents;
    }

}
