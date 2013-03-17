/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.smtp.quarantine;

public class WebConstants {

    // constant values must be in lower case
    public static final String AUTH_TOKEN_RP = "tkn";
    public static final String ACTION_RP = "action";
    public static final String PURGE_RV = "purge";
    public static final String RESCUE_RV = "rescue";
    public static final String REFRESH_RV = "refresh";
    public static final String SAFELIST_VIEW_RV = "slview";
    public static final String SAFELIST_ADD_RV = "sladd";
    public static final String SAFELIST_REMOVE_RV = "slremove";
    public static final String VIEW_INBOX_RV = "viewibx";
    public static final String MAIL_ID_RP = "mailid";
    //When the "action" is "sladd" or "slremove", this is the
    //key for the value of the (base64 encoded) email address
    public static final String SAFELIST_TARGET_ADDR_RP = "sladdr";

    public static final String MAPVIEW_VIEW_RV = "mapview";

    public static final String UNMAPPER_VIEW_RV = "unmapview";
    public static final String UNMAPPER_REMOVE_RV = "unmapremove";
    //When the "action" is "unmapremove", this is the
    //key for the value of the (base64 encoded) email address
    public static final String UNMAPPER_TARGET_ADDR_RP = "unmapaddr";

    //The action is really redundant, as there is only
    //one "action" for this servlet.  However, with all of
    //the other obnoxious scafolding around web<->java coding
    //what's a few more lines just for consistency sake.
    public static final String MAPPER_DO_REMAP_RV = "remap";
    public static final String MAPPER_VIEW_RV = "remap";
    public static final String MAPPER_TARGET_ADDR_RP = "mapaddr";
}
