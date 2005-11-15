/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_EVT"
 * mutable="false"
 */
public class IDSLogEvent extends LogEvent {

    private int sessionId;
    private String message;
    private boolean blocked;

    // constructors -----------------------------------------------------------

    public IDSLogEvent() { }

    public IDSLogEvent(int sessionId, String message, boolean blocked) {
        this.sessionId = sessionId;
        this.message = message;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * The action taken.
     *
     * @return the session id
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }


    /**
     * Message of signature that generated this event.
     *
     * @return the message
     * @hibernate.property
     * column="MESSAGE"
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Was it blocked.
     *
     * @return whether or not the session was blocked (closed)
     * @hibernate.property
     * column="BLOCKED"
     */
      public boolean isBlocked() {
          return blocked;
      }

      public void setBlocked(boolean blocked) {
          this.blocked = blocked;
      }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.addField("sid", sessionId);
        sb.addField("message", message);
        sb.addField("blocked", blocked);
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return blocked ? SyslogPriority.INFORMATIONAL : SyslogPriority.DEBUG;
    }

    // Object methods ---------------------------------------------------------

    public String toString() {
        return "IDSLogEvent id: " + getId() + " Message: " + message;
    }
}
