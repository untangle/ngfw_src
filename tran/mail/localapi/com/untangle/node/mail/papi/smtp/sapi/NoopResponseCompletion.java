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

package com.untangle.node.mail.papi.smtp.sapi;

import com.untangle.node.mail.papi.smtp.Response;

/**
 * Convienence implementation of ResponseCompletion
 * which does nothing
 *
 */
public class NoopResponseCompletion
    implements ResponseCompletion {


    public void handleResponse(Response resp,
                               Session.SmtpResponseActions actions) {
        //Nothing to do...
    }

}
