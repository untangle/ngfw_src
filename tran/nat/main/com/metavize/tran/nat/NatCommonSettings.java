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

package com.metavize.tran.nat;

import java.util.List;

import com.metavize.mvvm.tran.Validatable;
import java.io.Serializable;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.networking.ServicesSettings;

import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.SetupState;


public interface NatCommonSettings extends ServicesSettings, Validatable
{
    // !!!!! private static final long serialVersionUID = 4349679825783697834L;

    /** The current mode(setting the state is a package protected operation) */
    public SetupState getSetupState();

    /**
     * List of the redirect rules.
     *
     */
    public List<RedirectRule> getRedirectList();

    public void setRedirectList( List<RedirectRule> s );
}
