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

package com.untangle.mvvm.logging;

import java.util.List;

public interface EventRepository<E extends LogEvent>
{
    int CACHE_SIZE = 1000;

    RepositoryDesc getRepositoryDesc();
    List<E> getEvents();
}
