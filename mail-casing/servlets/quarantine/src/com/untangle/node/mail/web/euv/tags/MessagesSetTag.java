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

import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.ServletRequest;


/**
 * Tag which is used to iterate over a series
 * of messages.  The "type" property is currently
 * "info" or "error".
 * <br><br>
 * It then sets-up an OddEvenTag and a MessageTag for each iteration.
 */
public final class MessagesSetTag
    extends IteratingTag<String> {

    private static final String MESSAGES_KEY_PREFIX = "untangle.messages.";
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
        ArrayList<String> messages = getMessages(pageContext.getRequest(), getType());

        if(messages == null || messages.size() == 0) {
            return null;
        }
        return messages.iterator();
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
        for(String msg : messages) {
            addMessage(request, msgType, msg);
        }
    }
    public static final void addErrorMessage(ServletRequest request,
                                             String message) {
        addMessage(request, ERROR_MSG_SUFFIX, message);
    }
    public static final void addInfoMessage(ServletRequest request,
                                            String message) {
        addMessage(request, INFO_MSG_SUFFIX, message);
    }
    public static final void addMessage(ServletRequest request,
                                        String msgType,
                                        String msg) {
        ArrayList<String> list = getMessages(request, msgType);
        if(list == null) {
            list = new ArrayList<String>();
            request.setAttribute(MESSAGES_KEY_PREFIX + msgType, list);
        }
        list.add(msg);
    }
    public static final void clearMessages(ServletRequest request,
                                           String msgType) {
        request.removeAttribute(MESSAGES_KEY_PREFIX + msgType);
    }

    /**
     * Returns null if there are no such messages
     */
    private static ArrayList<String> getMessages(ServletRequest request,
                                                 String msgType) {
        return (ArrayList<String>) request.getAttribute(MESSAGES_KEY_PREFIX + msgType);
    }

    static boolean hasMessages(ServletRequest request,
                               String msgType) {
        ArrayList<String> msgs = getMessages(request, msgType);
        return msgs != null && msgs.size() > 0;
    }
}
