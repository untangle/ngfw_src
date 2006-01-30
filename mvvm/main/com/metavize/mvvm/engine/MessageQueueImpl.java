/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MessageQueue;

class MessageQueueImpl<M> implements MessageQueue
{
    private final List<M> q = new LinkedList<M>();

    public List<M> getMessages()
    {
        List l = new LinkedList<M>();
        synchronized (q) {
            l.addAll(q);
            q.clear();
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
