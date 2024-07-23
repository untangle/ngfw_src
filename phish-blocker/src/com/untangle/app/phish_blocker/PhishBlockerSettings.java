/**
 * $Id$
 */
package com.untangle.app.phish_blocker;

import java.io.Serializable;
import com.untangle.app.spam_blocker.SpamSettings;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Settings for the Phish app.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class PhishBlockerSettings extends SpamSettings implements Serializable
{}
