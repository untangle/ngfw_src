/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Pipeline.java,v 1.2 2005/03/15 02:11:53 amread Exp $
 */

package com.metavize.mvvm.tapi;


public interface Pipeline
{
    Long attach(Object o);
    Object getAttachment(Long key);
    Object detach(Long key);
}
