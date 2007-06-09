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

package com.untangle.uvm.logging;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;

public interface ListEventFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();
    void warm(Session s, List<E> l, int limit, Map<String, Object> params);
    boolean accept(E e);
}
