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
package com.untangle.tran.mail.web.euv.tags;

import javax.servlet.jsp.JspException;
import java.util.Iterator;


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
