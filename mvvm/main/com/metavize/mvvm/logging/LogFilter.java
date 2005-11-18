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

public interface LogFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();
    int getLimit();
    void setLimit(int limit);
    List<E> getEvents();
}
