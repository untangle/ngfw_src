/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.app.smtp.EmailAddressPairRule;
import com.untangle.app.smtp.EmailAddressRule;

/**
 * Settings for the quarantine stuff
 */
@SuppressWarnings("serial")
public class QuarantineSettings implements Serializable
{
    public static final long HOUR = 1000L * 60L * 60L; // millisecs per hour
    public static final long DAY = HOUR * 24L; // millisecs per day
    public static final long WEEK = DAY * 7L; // millisecs per week

    private long maxMailIntern = 2L * WEEK;
    private long maxIdleInbox = 4L * WEEK;
    private String secretKey;
    private byte[] binaryKey;
    private boolean sendDailyDigests = true;
    private int digestHOD;// Hour Of Day
    private int digestMOD;// Minute Of Day
    private long maxQuarantineSz;
    private LinkedList<EmailAddressPairRule> addressRemaps;
    private LinkedList<EmailAddressRule> allowedAddressPatterns;

    /**
     * Get the list of {@link EmailAddressRule} objects, defining the address patterns for-which this server will
     * quarantine emails. The patterns are based on email address syntax ("local@domain"). However, a limited glob
     * syntax is also supported ("*@domain"). The glob matches any characters (0 or more). This should not be confused
     * with "real" regex which is not supported. Only glob. <br>
     * 
     * @return a List of EmailAddressRule objects.
     */
    public List<EmailAddressRule> getAllowedAddressPatterns()
    {
        if (allowedAddressPatterns == null) {
            setAllowedAddressPatterns(null);
        }
        if (allowedAddressPatterns != null)
            allowedAddressPatterns.removeAll(java.util.Collections.singleton(null));
        return allowedAddressPatterns;
    }

    /**
     * Write allwowed address patterns.
     * @param patterns List of EmailAddressRule.
     */
    public void setAllowedAddressPatterns(List<EmailAddressRule> patterns)
    {
        if (patterns == null) {
            patterns = new LinkedList<EmailAddressRule>();
        }
        allowedAddressPatterns = new LinkedList<EmailAddressRule>(patterns);
    }

    /**
     * Set a List of {@link EmailAddressPairRule} objects, defining the "remappings" supported by this server.
     * Remappings associate a pattern with an address. For example, to cause all emails for "sales@foo.com" to be
     * quarantined in the inbox of "joe.salesguy@foo.com" "sales@foo.com" is the pattern and "joe.salesguy@foo.com" is
     * the mapped address. Since the "EmailAddressPairRule" class is generic (doesn't have "pattern" and "address"
     * members, the "address1" member is the pattern and "address2" is the remap. <br>
     * The pattern also suports limited wildcards, based on glob ("*") syntax. The glob matches any characters. For
     * example, to cause all mails for "foo.com" to be quarantined within "fred@moo.com"'s inbox, the pattern would be
     * "*@foo.com" and the remapping would be "fred@moo.com".
     * 
     * 
     * @return a List of EmailAddressPairRule objects.
     */
    public List<EmailAddressPairRule> getAddressRemaps()
    {
        if (addressRemaps == null) {
            setAddressRemaps(null);
        }
        if (addressRemaps != null)
            addressRemaps.removeAll(java.util.Collections.singleton(null));
        return addressRemaps;
    }

    /**
     * Set the list of addresses to be remapped. The argument is a list of {@link EmailAddressPairRule}. These represent
     * the <b>ordered</b> collection of pairs to be remapped. Note also that the <code>addr1</code> property of the pair
     * is the "map from" (i.e. <code>addr1</code> of "*@foo.com" and <code>addr2</code> of "trash@foo.com").
     * 
     * @param remaps
     *            the list of remapped addresses.
     */
    public void setAddressRemaps(List<EmailAddressPairRule> remaps)
    {
        if (remaps == null) {
            remaps = new LinkedList<EmailAddressPairRule>();
        }
        addressRemaps = new LinkedList<EmailAddressPairRule>(remaps);
    }

    /**
     * @return the Hour of the day when digest emails should be sent.
     */
    public int getDigestHourOfDay()
    {
        return digestHOD;
    }

    /**
     * Set the Hour of the day when digest emails should be sent. This should be a value between 0 and 23 (inclusive of
     * both ends).
     * 
     * @param hod
     *            the hour of the day
     */
    public void setDigestHourOfDay(int hod)
    {
        digestHOD = hod;
    }

    /**
     * 
     * @return the Minute of the day when digest emails should be sent.
     */
    public int getDigestMinuteOfDay()
    {
        return digestMOD;
    }

    /**
     * Set the Minute of the day when digest emails should be sent. This should be a value between 0 and 59 (inclusive
     * of both ends).
     * 
     * @param mod
     *            the minute of the day
     */
    public void setDigestMinuteOfDay(int mod)
    {
        digestMOD = mod;
    }

    /**
     * Here are the original comments from the xxxSecretKey() functions:
     * 
     * Password, encrypted with password utils.
     * 
     * @return encrypted password bytes.
     * 
     *         Set the key used to create authentication "tokens". This should really only ever be set once for a given
     *         deployment (or else folks with older emails won't be able to use the links).
     */

    /**
     * Previously this all worked fine since the byte[] array mapped nicely to a bytea column in postgres. The
     * SettingsManager doesn't handle the byte array properly, so I had to get creative.
     * 
     * Now we maintain a copy of the key in two different formats: binaryKey = byte[] secretKey = String
     * 
     * The String version will always be twice as long as the byte version, and is represented as a series of eight
     * 4-bit nibbles, the value of each added to the ASCII value of 'A', which allows the key to be stored in the json
     * file as a simple 8 byte string like "ABCDEFGH".
     * 
     * The getSecretKey() and setSecretKey() functions are the ones that get and set the String version, and are so
     * named so they will be picked up by the SettingsManager magic. I used the names initBinaryKey() and
     * grabBinaryKey() to manipulate the key in byte[] format which keeps the byte[] version from ending up in the
     * settings file.
     */

    /**
     * Return secret key.
     * @return String of secret key.
     */
    public String getSecretKey()
    {
        return (secretKey);
    }

    /**
     * return binary key.
     * @return Array of byte of binary key.
     */
    public byte[] grabBinaryKey()
    {
        return (binaryKey);
    }

    /**
     * Write secret key.
     * @param key String of secret key.
     */
    public void setSecretKey(String key)
    {
        // first we save the argumented key in our string version
        secretKey = key;

        // now we generate the binary version from the string version
        int rawlen = (key.length() / 2);
        binaryKey = new byte[rawlen];

        for (int x = 0; x < rawlen; x++) {
            int lo_nib = (key.charAt((x * 2) + 0) - 'A');
            int hi_nib = (key.charAt((x * 2) + 1) - 'A');
            int value = (((hi_nib << 4) & 0xF0) | lo_nib);
            binaryKey[x] = (byte) value;
        }
    }

    /**
     * Write bnary key.
     * @param key Array of byte of key.
     */
    public void initBinaryKey(byte[] key)
    {
        // first we save the argumented key in our binary version
        binaryKey = key;

        // now we generate the string version from the binary version
        StringBuilder local = new StringBuilder();

        for (int x = 0; x < key.length; x++) {
            char lo_nib = (char) ((key[x] & 0x0F) + 'A');
            char hi_nib = (char) (((key[x] >> 4) & 0x0F) + 'A');
            local.append(lo_nib);
            local.append(hi_nib);
        }

        secretKey = local.toString();
    }

    /**
     * Return the longest period of time (in ms) that a mail may be interned before it is automagically purged.
     * @return Length of time in ms.
     */
    public long getMaxMailIntern()
    {
        return maxMailIntern;
    }

    /**
     * Write the longest period of time (in ms) that a mail may be interned before it is automagically purged.
     * @param max Length of time in ms.
     */
    public void setMaxMailIntern(long max)
    {
        maxMailIntern = max;
    }

    /**
     * Return the maximum relative time (in milliseconds) that inboxes can be idle before they are implicitly cleaned-up.
     * This is a relative unit (ie "2 weeks")
     * @return Max idle time in ms.
     */
    public long getMaxIdleInbox()
    {
        return maxIdleInbox;
    }

    /**
     * Write the maximum relative time (in milliseconds) that inboxes can be idle before they are implicitly cleaned-up.
     * This is a relative unit (ie "2 weeks")
     * @param max Max idle time in ms.
     */
    public void setMaxIdleInbox(long max)
    {
        maxIdleInbox = max;
    }

    /**
     * Determine whether to send daily digests.
     * @return a boolean to determine whether to send daily digests
     */
    public boolean getSendDailyDigests()
    {
        return this.sendDailyDigests;
    }

    /**
     * Set whether to send daily digests
     * @param sendDailyDigests If true, send, otherwise do not.
     */
    public void setSendDailyDigests(boolean sendDailyDigests)
    {
        this.sendDailyDigests = sendDailyDigests;
    }
}
