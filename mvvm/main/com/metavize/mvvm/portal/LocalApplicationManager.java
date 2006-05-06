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

import java.util.List;

public interface LocalApplicationManager
{
    Application registerApplication(String name, String description,
                                    boolean isHostService,
                                    Application.Validator validator,
                                    int sortPosition, String appJs);

    boolean deregisterApplication(Application app);

    List<Application> getApplications();

    List<String> getApplicationNames();

    Application getApplication(String name);
}
