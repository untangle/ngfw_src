/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.portal;

public interface LocalApplicationManager
{
    Application registerApplication(String name, String description,
                                    boolean isHostService,
                                    Application.Validator validator,
                                    int sortPosition);

    Application[] getApplications();

    public String[] getApplicationNames();

    public Application getApplication(String name);
}
