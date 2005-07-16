/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Savable.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.transform;

import com.metavize.gui.util.*;

public interface Savable {
    public void doSave(Object settings, boolean validateOnly) throws Exception;
}
