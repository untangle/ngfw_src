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
package com.metavize.tran.mail.web.euv.tags;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.BodyContent;

import java.util.Iterator;


/**
 * Base class for BodyTags which output their
 * body at "doEndTag"
 */
public abstract class WritesBodyAtEndTag
  extends BodyTagSupport {

  public int doEndTag() throws JspException{
    try {
      BodyContent body = getBodyContent();
      JspWriter writer = body.getEnclosingWriter();
      String bodyString = body.getString();
      writer.println(bodyString);
      return EVAL_PAGE;
    }
    catch (Exception ioe){
      throw new JspException(ioe.getMessage());
    }
  }   
}
