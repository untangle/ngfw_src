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

package com.untangle.tran.openvpn;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.untangle.mvvm.tran.Validatable;

/**
 * the configuration for a vpn client.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_openvpn_client", schema="settings")
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public class VpnClient extends VpnClientBase implements Validatable
{
    // XXX FIXME private static final long serialVersionUID = -403968809913481068L;
}
