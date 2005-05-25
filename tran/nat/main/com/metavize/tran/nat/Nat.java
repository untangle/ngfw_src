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
package com.metavize.tran.nat;

import com.metavize.mvvm.tran.Transform;

public interface Nat extends Transform
{
    NatSettings getNatSettings();
    void setNatSettings( NatSettings settings ) throws Exception;
}
