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

import java.io.Serializable;
import com.metavize.mvvm.tran.Validatable;

/**
 * Settings for the Nat transform in simple mode.
 */
public interface NatSettings extends Serializable, Validatable, NatCommonSettings, NatBasicSettings
{
    
}
