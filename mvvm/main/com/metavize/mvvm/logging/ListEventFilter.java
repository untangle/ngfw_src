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

package com.metavize.mvvm.logging;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;

public interface ListEventFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();
    List<E> warm(Session s, List<E> l, int limit, Map<String, Object> params);
    boolean accept(E e);
}
