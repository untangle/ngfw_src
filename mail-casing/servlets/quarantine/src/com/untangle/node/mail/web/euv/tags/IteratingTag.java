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
package com.untangle.node.mail.web.euv.tags;

import java.util.Iterator;
import javax.servlet.jsp.JspException;


/**
 * Base class for iterating tags.  Takes care
 * of odd/even stuff
 */
public abstract class IteratingTag<T>
    extends WritesBodyAtEndTag {


    private Iterator<T> m_it;
    private boolean m_even = true;


    protected abstract Iterator<T> createIterator();

    protected abstract void setCurrent(T t);

    public final int doStartTag() throws JspException {
        m_even = true;
        m_it = createIterator();

        if(m_it == null || !m_it.hasNext()) {
            //Putting a silly print here didn't do anything.
            mvSetEmpty();
            return SKIP_BODY;
        }
        OddEvenTag.setCurrent(pageContext, m_even);
        m_even = !m_even;
        setCurrent(m_it.next());
        return EVAL_BODY_TAG;
    }

    public final int doAfterBody() throws JspException {
        //Check if we're "done"
        if(m_it == null || !m_it.hasNext()) {
            return SKIP_BODY;
        }
        OddEvenTag.setCurrent(pageContext, m_even);
        setCurrent(m_it.next());
        m_even = !m_even;
        return EVAL_BODY_TAG;
    }

    @Override
    public int doEndTag() throws JspException{
        m_it = null;
        return super.doEndTag();
    }
}
