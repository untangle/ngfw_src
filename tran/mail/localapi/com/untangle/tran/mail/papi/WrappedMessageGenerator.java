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

package com.untangle.tran.mail.papi;

import com.untangle.mvvm.tran.Template;
import com.untangle.mvvm.tran.TemplateValues;
import com.untangle.mvvm.tran.TemplateValuesChain;
import com.untangle.tran.mime.*;
import com.untangle.tran.util.*;
import org.apache.log4j.Logger;

/**
 * Class which wraps a MIMEMessage with another,
 * providing templates for the resulting
 * subject and body.
 * <br><br>
 * This class uses the {@link #com.untangle.tran.util.Template Template}
 * class internally.  You can create templates which derefference
 * keys found in any number of TemplateValues, passed-into the
 * {@link #wrap wrap method}
 * <br><br>
 * Note that the {@link #getBodyTemplate Body}
 * and {@link #getSubjectTemplate Subject}
 * templates may be null.  If The subject template
 * is null, the subject will not be modified.  If the
 * body template is null, the message will not be
 * wrapped (i.e. disables wrapping).
 *
 */
public class WrappedMessageGenerator
    extends MessageGenerator {

    private final Logger m_logger = Logger.getLogger(getClass());

    public WrappedMessageGenerator() {
        this(null, null);
    }

    /**
     * Full constructor.  Please read the class-docs
     * to understand impact of null templates
     *
     * @param subjectTemplate the subject template
     * @param bodyTemplate the bodyTemplate
     */
    public WrappedMessageGenerator(String subjectTemplate,
                                   String bodyTemplate) {
        super(subjectTemplate, bodyTemplate);
    }

    /**
     * Wrap the given MIMEMessage.  Only the message
     * itself will provide any substitution values
     * for the body or subject templates.
     *
     * @param msg the Message to be wrapped
     *
     * @return the wrapped message, or the original
     *         (possibly with modified subject) if the body
     *         template was null.
     */
    public MIMEMessage wrap(MIMEMessage msg) {
        return wrap(msg, new TemplateValuesChain());
    }

    /**
     * Wrap the given MIMEMessage, using the provided TemplateValues
     * objects as sources for any substitution keys found within the
     * {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject}
     * templates.
     *
     * @param msg the Message to be wrapped
     * @param values the source of any substitution values
     *
     * @return the wrapped message, or the original
     *         (possibly with modified subject) if the body
     *         template was null.
     */
    public MIMEMessage wrap(MIMEMessage msg, TemplateValues... values) {
        return wrap(msg, new TemplateValuesChain(values));
    }

    /**
     * Wrap the given MIMEMessage, using the provided TemplateValuesChain
     * as the source for any substitution keys found within the
     * {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject}
     * templates.
     *
     * @param msg the Message to be wrapped
     * @param values the source of any substitution values
     *
     * @return the wrapped message, or the original
     *         (possibly with modified subject) if the body
     *         template was null.
     */
    public MIMEMessage wrap(MIMEMessage msg, TemplateValuesChain values) {
        //Add the original message to the chain
        values.append(msg);

        MIMEMessage ret = msg;
        Template bodyTemplate = getBodyTemplate();
        if(bodyTemplate != null) {
            try {
                m_logger.debug("Wrapping body");
                ret = MIMEUtil.simpleWrap(bodyTemplate.format(values), msg);
            }
            catch(Exception ex) {
                m_logger.error(ex);
            }
        }
        else {
            m_logger.debug("No template to wrap body");
        }
        Template subjectTemplate = getSubjectTemplate();
        if(subjectTemplate != null) {
            try {
                m_logger.debug("Wrapping subject");
                ret.getMMHeaders().setSubject(subjectTemplate.format(values));
            }
            catch(Exception ex) {
                m_logger.error(ex);
            }
        }
        else {
            m_logger.debug("No template for new subject");
        }
        return ret;
    }

}
