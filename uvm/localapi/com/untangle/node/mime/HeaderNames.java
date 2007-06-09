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

package com.untangle.node.mime;

/**
 * Constants for popular header names.
 */
public interface HeaderNames {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String SUBJECT = "Subject";
    public static final String TO = "To";
    public static final String CC = "cc";
    public static final String FROM = "From";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String DATE = "Date";
    public static final String MIME_VERSION = "Mime-Version";


    public static final LCString CONTENT_TYPE_LC = new LCString(CONTENT_TYPE);
    public static final LCString CONTENT_TRANSFER_ENCODING_LC = new LCString(CONTENT_TRANSFER_ENCODING);
    public static final LCString SUBJECT_LC = new LCString(SUBJECT);
    public static final LCString TO_LC = new LCString(TO);
    public static final LCString CC_LC = new LCString(CC);
    public static final LCString FROM_LC = new LCString(FROM);
    public static final LCString CONTENT_DISPOSITION_LC = new LCString(CONTENT_DISPOSITION);


}
