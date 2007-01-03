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

package com.untangle.mvvm.engine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.MessageQueue;

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
