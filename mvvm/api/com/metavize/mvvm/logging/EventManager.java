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

public interface EventManager<E extends LogEvent>
{
    List<RepositoryDesc> getRepositoryDescs();
    EventRepository<E> getRepository(String filterName);
    List<EventRepository<E>> getRepositories();
    void setLimit(int limit);
    int getLimit();
}
