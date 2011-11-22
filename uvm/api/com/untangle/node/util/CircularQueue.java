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

package com.untangle.node.util;



/**
 * Queue which has a fixed size, and ejects the oldest members when that
 * capacity is overflowed.
 */
public class CircularQueue<E> {

    private final E[] m_list;
    private final int m_capacity;
    private int m_head = 0;
    private int m_tail = 0;

    @SuppressWarnings("unchecked")
    public CircularQueue(int len) {
        m_capacity = len+1;
        m_list = (E[]) new Object[m_capacity];
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

    @SuppressWarnings("unchecked")
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
