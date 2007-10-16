/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.alerts.MessageQueue;

/**
 * Simple message queue implementation.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class MessageQueueImpl<M> implements MessageQueue
{
    private final List<M> q = new LinkedList<M>();

    public List<M> getMessages()
    {
        List<M> l;
        synchronized (q) {
            if (0 == q.size()) {
                l = Collections.emptyList();
            } else {
                l = new LinkedList<M>();
                l.addAll(q);
                q.clear();
            }
        }

        return l;
    }

    void enqueue(M m)
    {
        synchronized (q) {
            q.add(m);
        }
    }
}
