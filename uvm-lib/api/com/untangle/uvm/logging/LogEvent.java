/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.logging;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * A log event and message.
 *
 * Hibernate mappings for this class are in the UVM resource
 * directory.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class LogEvent implements Comparable, Serializable
{
    public static final int DEFAULT_STRING_SIZE = 255;

    private Long id;
    private Date timeStamp = new Date();

    // constructors -----------------------------------------------------------

    protected LogEvent() { }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="event_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Time the event was logged, as filled in by logger.
     *
     * @return time logged.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="time_stamp")
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * Don't make Aaron angry!  This should only be set by the event
     * logging system unless you're doing tricky things (with Aaron's
     * approval).
     */
    public void setTimeStamp(Date timeStamp)
    {
        if (timeStamp instanceof Timestamp) {
            this.timeStamp = new Date(timeStamp.getTime());
        } else {
            this.timeStamp = timeStamp;
        }
    }

    // public methods ---------------------------------------------------------

    /**
     * LogEvents inserted into the database and syslog when this method
     * returns true.
     *
     * @return true when this event is saved to the database.
     */
    @Transient
    public boolean isPersistent()
    {
        return true;
    }

    // Syslog methods ---------------------------------------------------------

    public abstract void appendSyslog(SyslogBuilder a);

    @Transient
    public String getSyslogId()
    {
        String[] s = getClass().getName().split("\\.");

        return s[s.length - 1];
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(Object o)
    {
        LogEvent le = (LogEvent)o;

        int i = -timeStamp.compareTo(le.getTimeStamp());
        if (0 == i) {
            if (le.id == id) {
                return 0;
            } else {
                Long t = null == id ? 0L : id;
                Long u = null == le.id ? 0L : le.id;
                return t.compareTo(u);
            }
        } else {
            return i;
        }
    }
}
