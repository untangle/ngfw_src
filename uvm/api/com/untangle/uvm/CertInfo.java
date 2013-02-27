/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Date;
import java.util.List;

/**
 * Little class to hold interesting parsed information
 * from a cert.  Java has native classes for doing this,
 * but if you want to start using the "X500" and "ldap"
 * classes you're a champ.  This is much easier.
 *
 */
@SuppressWarnings("serial")
public class CertInfo implements java.io.Serializable {

    /**
     * Date when cert becomes valid
     */
    public final Date notBefore;
    /**
     * Date when cert becomes invalid
     */
    public final Date notAfter;
    /**
     * The Distinguished name broken into a Map.  Note that
     * the most insteresting key is "CN" which will be
     * the DNS entry for a web server
     */
    public final DistinguishedName subjectDN;
    /**
     * The Distinguished name broken into a Map.
     */
    public final DistinguishedName issuerDN;
    /**
     * True if the cert is issued as a CA cert.
     */
    public final boolean isCA;

    public Boolean appearsSelfSignedFlag;

    /**
     * A String representation of the cert (from OpenSSL).  Not
     * that pretty, but sufficent for the hard-code nerd who
     * wants the ugly details.
     *
     * Warning - embedded new lines which are likely in Unix format.
     */
    public final String ppString;

    public CertInfo(Date notBefore, Date notAfter, DistinguishedName subjectDN, DistinguishedName issuerDN, boolean isCA, String ppString)
    {
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.subjectDN = subjectDN;
        this.issuerDN = issuerDN;
        this.isCA = isCA;
        this.ppString = ppString;
    }

    /**
     * Convienence method to obtain the <b>C</b>ommon<b>N</b>ame (i.e. the hostname
     * for a web server).
     *
     * @return the CN of the cert's subject (or null if not found).
     */
    public String getSubjectCN()
    {
        //TODO is this stuff case sensitive?
        if(subjectDN == null) {
            return null;
        }
        return subjectDN.getValue("CN");
    }

    public void setAppearsSelfSignedFlag(boolean flag)
    {
        appearsSelfSignedFlag = Boolean.valueOf(flag);
    }

    /**
     * Method to test if a vert seems self-signed.  Note that
     * {@link #isCA CA} certs will be self-signed, as well
     * as any localy-generated certs.
     *
     * @return true if the subjectDN is the same as the issuerDN
     */
    public boolean appearsSelfSigned()
    {
        //Skip this test if someone tells us better
        if (appearsSelfSignedFlag != null) {
            return appearsSelfSignedFlag.booleanValue();
        }

        if(subjectDN == null ||
           issuerDN == null) {
            return issuerDN==null && subjectDN==null;
        }

        List<String> subjectTypes = subjectDN.listTypes();
        for(String type : subjectTypes) {
            String issuerValue = issuerDN.getValue(type);
            if(issuerValue == null) {
                return false;
            }
            if(!(issuerValue.equals(subjectDN.getValue(type)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convience method which prints out debug info.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        String newLine = System.getProperty("line.separator", "\n");
        sb.append("notBefore: ").append(notBefore).append(newLine);
        sb.append("notAfter: ").append(notAfter).append(newLine);
        sb.append("isCA: ").append(isCA?"true":"false").append(newLine);
        sb.append("subjectDN").append(subjectDN.toDistinguishedString()).append(newLine);
        sb.append("issuerDN").append(issuerDN.toDistinguishedString()).append(newLine);
        return sb.toString();
    }

	public Date getNotBefore()
    {
		return notBefore;
	}

	public Date getNotAfter()
    {
		return notAfter;
	}

	public DistinguishedName getSubjectDN()
    {
		return subjectDN;
	}

	public DistinguishedName getIssuerDN()
    {
		return issuerDN;
	}

}
