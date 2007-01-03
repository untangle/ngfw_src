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

package com.untangle.mvvm;

import com.untangle.mvvm.tran.Validatable;

import com.untangle.mvvm.networking.RemoteSettings;
import com.untangle.mvvm.networking.BasicNetworkSettings;

/* This interface joins together the interfaces for the Remote
 * Settings and the Basic network settings.   As time permits, this
 * interface will go away and the client/classes will request the 
 * correct interface based on what they need.*/
public interface NetworkingConfiguration extends Validatable, RemoteSettings, BasicNetworkSettings
{
}
