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

package com.untangle.tran.util;



/**
 * Queue which has a fixed size, and ejects the oldest members when that
 * capacity is overflowed.
 */
public class CircularQueue<E> {

    private final E[] m_list;
    private final int m_capacity;
    private int m_head = 0;
    private int m_tail = 0;

    public CircularQueue(int len) {
        m_capacity = len+1;
        m_list = (E[])new Object[m_capacity];
    }

    public void push(E e) {
        m_list[m_tail] = e;
        m_tail = next(m_tail);
        if(m_tail == m_head) {
            m_list[m_tail] = null;
            m_head = next(m_head);
        }
    }

    public E pop() {
        if(isEmpty()) {
            return null;
        }
        E ret = m_list[m_head];
        m_list[m_head] = null;
        m_head = next(m_head);
        if(m_head == m_tail) {
            m_head = 0;
            m_tail = 0;
        }
        return ret;
    }

    public int length() {
        return m_tail < m_head?
            (m_capacity-1):
            (m_head<m_tail?
             (m_tail-m_head):
             (0));
    }

    public void clear() {
        m_head = 0;
        m_tail = 0;
        for(int i = 0; i<m_list.length; i++) {
            m_list[i] = null;
        }
    }

    public boolean isEmpty() {
        return m_head==m_tail;
    }

    public E[] popAll(E[] a) {
        E[] ret = peekAll(a);
        clear();
        return ret;
    }

    public E[] peekAll(E[] a) {
        int len = length();
        if(a.length < len) {
            a = (E[])java.lang.reflect.Array.newInstance(
                                                         a.getClass().getComponentType(), len);
        }

        if(m_head > m_tail) {
            System.arraycopy(m_list, m_head, a, 0, (m_capacity-m_head));
            System.arraycopy(m_list, 0, a, (m_capacity-m_head), m_tail);
        }
        else if(m_head < m_tail) {
            System.arraycopy(m_list, m_head, a, 0, m_tail-m_head);
        }
        if(a.length > len) {
            a[len] = null;
        }
        return a;
    }

    private final int next(int i) {
        return (++i >= m_capacity)?0:i;
    }
}
