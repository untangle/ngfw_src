/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

import com.metavize.mvvm.tran.Validatable;

import com.metavize.mvvm.networking.RemoteSettings;
import com.metavize.mvvm.networking.BasicNetworkSettings;

/* This interface joins together the interfaces for the Remote
 * Settings and the Basic network settings.   As time permits, this
 * interface will go away and the client/classes will request the 
 * correct interface based on what they need.*/
public interface NetworkingConfiguration extends Validatable, RemoteSettings, BasicNetworkSettings
{
}
