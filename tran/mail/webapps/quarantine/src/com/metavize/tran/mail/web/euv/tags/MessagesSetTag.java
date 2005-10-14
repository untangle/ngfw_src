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

import javax.servlet.ServletRequest;
import java.util.Iterator;
import java.util.Arrays;


/**
 * Tag which is used to iterate over a series
 * of messages.  The "type" property is currently
 * "info" or "error".
 * <br><br>
 * It then sets-up an OddEvenTag and a MessageTag for each iteration.
 */
public final class MessagesSetTag
  extends IteratingTag<String> {

  private static final String MESSAGES_KEY_PREFIX = "metavize.messages.";
  private static final String ERROR_MSG_SUFFIX = "error";
  private static final String INFO_MSG_SUFFIX = "info";

  private String m_type = null;

  public void setType(String type){
    m_type = type;
  }
  
  public String getType(){
    return m_type;
  }
  
  @Override
  protected Iterator<String> createIterator() {
    String[] messages = getMessages(pageContext.getRequest(), getType());
    
    if(messages == null || messages.length == 0) {
      return null;
    }
    return Arrays.asList(messages).iterator();
  }
  @Override
  protected void setCurrent(String s) {
    MessageTag.setCurrent(pageContext, s);
  }

  public static final void setErrorMessages(ServletRequest request,
    String...messages) {
    setMessages(request, ERROR_MSG_SUFFIX, messages);
  }
  public static final void setInfoMessages(ServletRequest request,
    String...messages) {
    setMessages(request, INFO_MSG_SUFFIX, messages);
  }
  public static final void setMessages(ServletRequest request,
    String msgType,
    String...messages) {
    request.setAttribute(MESSAGES_KEY_PREFIX + msgType, messages);
  }
  public static final void clearMessages(ServletRequest request,
    String msgType) {
    request.removeAttribute(MESSAGES_KEY_PREFIX + msgType);
  }

  /**
   * Returns null if there are no such messages
   */
  static String[] getMessages(ServletRequest request,
    String msgType) {
    return (String[]) request.getAttribute(MESSAGES_KEY_PREFIX + msgType);
  }

  static boolean hasMessages(ServletRequest request,
    String msgType) {
    String[] msgs = getMessages(request, msgType);
    return msgs != null && msgs.length > 0;
  }
}
