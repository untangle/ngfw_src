/**
 * $Id: UsegAgentString.java,v 1.00 2012/10/15 12:22:11 dmorris Exp $
 */
package com.untangle.node.util;

public class UserAgentString
{
    private String agentString;

    private String browserInfo;
    private String osInfo;
    private String platformInfo;
    private String platformDetails;
    private String enhancementDetails;

    public UserAgentString( String agentString )
    {
        this.agentString = agentString;
        parse(agentString);
    }


    void parse( String agentString )
    {
        /* Format:
         * browserInfo (osInfo) platformInfo (platformDetails) enhancementDetails
         */
        if (agentString == null)
            return;

        String currentString = agentString;

        /* browserInfo */
        /* browserInfo */
        /* browserInfo */
        
        int indexOfParen = currentString.indexOf('(');

        /* if no parens, must be only browserInfo */
        if (indexOfParen == -1) {
            this.browserInfo = currentString;
            return;
        }

        this.browserInfo = currentString.substring(0, indexOfParen).trim();
        //System.out.println("XXX1: currentString: " + currentString + " browserInfo: " + browserInfo);
        currentString = currentString.substring(indexOfParen);
        //System.out.println("XXX2: currentString: " + currentString);

        /* osInfo */
        /* osInfo */
        /* osInfo */

        indexOfParen = currentString.indexOf(')');

        /* if no parens, must be only browserInfo */
        if (indexOfParen == -1) {
            this.osInfo = currentString;
            return;
        }

        this.osInfo = currentString.substring(1, indexOfParen).trim();
        //System.out.println("XXX3: currentString: " + currentString + " osInfo: " + osInfo);
        currentString = currentString.substring(indexOfParen+1);
        //System.out.println("XXX4: currentString: " + currentString);

        /* platformInfo */
        /* platformInfo */
        /* platformInfo */

        indexOfParen = currentString.indexOf('(');

        /* if no parens, return */
        if (indexOfParen == -1) {
            return;
        }

        this.platformInfo = currentString.substring(0, indexOfParen).trim();
        //System.out.println("XXX5: currentString: " + currentString + " platformInfo: " + platformInfo);
        currentString = currentString.substring(indexOfParen);
        //System.out.println("XXX6: currentString: " + currentString);
        
        /* platformDetails */
        /* platformDetails */
        /* platformDetails */

        indexOfParen = currentString.indexOf(')');

        /* if no parens, must be only browserInfo */
        if (indexOfParen == -1) {
            return;
        }

        this.platformDetails = currentString.substring(1, indexOfParen).trim();
        //System.out.println("XXX7: currentString: " + currentString + " platformDetails: " + platformDetails);
        currentString = currentString.substring(indexOfParen+1);
        //System.out.println("XXX8: currentString: " + currentString);

        /* enhancementDetails */
        /* enhancementDetails */
        /* enhancementDetails */

        indexOfParen = currentString.indexOf('(');

        /* if no parens, return */
        if (indexOfParen == -1) {
            return;
        }

        this.enhancementDetails = currentString.substring(0, indexOfParen).trim();
        //System.out.println("XXX9: currentString: " + currentString + " enhancementDetails: " + enhancementDetails);
        currentString = currentString.substring(indexOfParen);
        //System.out.println("XXX0: currentString: " + currentString);
        
        
        
    }

    public String getOsInfo()
    {
        if (browserInfo == null)
            return null;
        /**
         * Only retun OS info if its a mozilla or opera based browser
         * other obscure browsers often dont supply correct OS information
         */
        if (!browserInfo.contains("Mozilla") && !browserInfo.contains("Opera"))
            return null;

        return this.osInfo;
    }

    public String getBrowserInfo()
    {
        return this.browserInfo;
    }
    
    public String getPlatformInfo()
    {
        return this.platformInfo;
    }
    
    public String getPlatformDetails()
    {
        return this.platformDetails;
    }
    
    public String getEnhancementDetails()
    {
        return this.enhancementDetails;
    }
    
    /**
     * For testing
     */
    /*
    public static void main(String [] args)
    {
        String[] agentStrings = {
             "Mozilla/5.0 (compatible; U; ABrowse 0.6; Syllable) AppleWebKit/420+ (KHTML, like Gecko)",
             "Mozilla/5.0 (compatible; ABrowse 0.4; Syllable)",
             "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; GTB5; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; Maxthon; InfoPath.1; .NET CLR 3.5.30729; .NET CLR 3.0.30618)",
             "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser; GTB6; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; InfoPath.1; .NET CLR 3.5.30729; .NET CLR 3.0.30618)",
            "AmigaVoyager/3.2 (AmigaOS/MC680x0)",
            "AmigaVoyager/2.95 (compatible; MC680x0; AmigaOS; SV1)",
            "AmigaVoyager/2.95 (compatible; MC680x0; AmigaOS)",
             "Mozilla/5.0 (compatible; MSIE 9.0; AOL 9.7; AOLBuild 4343.19; Windows NT 6.1; WOW64; Trident/5.0; FunWebProducts)",
             "Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.5; AOLBuild 4337.81; Windows NT 6.0; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.5.30729; .NET CLR 3.0.30618)",
             "Mozilla/4.0 (compatible; MSIE 8.0; AOL 9.1; AOLBuild 4334.5011; Windows NT 6.1; WOW64; Trident/4.0; GTB7.2; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)",
             "Mozilla/4.0 (compatible; MSIE 6.0; AOL 9.1; AOLBuild 4334.5006; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30)",
             "Mozilla/4.0 (compatible; MSIE 6.0; AOL 7.0; Windows 98; Win 9x 4.90; .NET CLR 1.1.4322)",
             "Mozilla/4.0 (compatible; MSIE 5.0; AOL 5.0; Windows 95; DigExt)",
             "Mozilla/5.0 (X11; U; Linux; hu-HU) AppleWebKit/523.15 (KHTML, like Gecko, Safari/419.3) Arora/0.4",
             "Mozilla/5.0 (X11; U; Linux i686; nl; rv:1.8.1b2) Gecko/20060821 BonEcho/2.0b2 (Debian-1.99+2.0b2+dfsg-1)",
             "Mozilla/5.0 (BeOS; U; Haiku BePC; en-US; rv:1.8.1.18) Gecko/20081114 BonEcho/2.0.0.18",
             "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; .NET4.0C; .NET4.0E; .NET CLR 2.0.50727; .NET CLR 1.1.4322; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; Browzar)",
             "Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en; rv:1.8.1.6) Gecko/20070809 Firefox/2.0.0.6 Camino/1.5.1",
             "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/418.9 (KHTML, like Gecko) AppleWebKit/418.9 Cheshire/1.0.ALPHA",
             "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.24 (KHTML, like Gecko) Chrome/19.0.1055.1 Safari/535.24",
             "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.65 Safari/535.11",
             "Mozilla/5.0 Slackware/13.37 (X11; U; Linux x86_64; en-US) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.41",
             "Mozilla/5.0 ArchLinux (X11; Linux x86_64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.41 Safari/535.1",
             "Mozilla/5.0 (X11; CrOS i686 0.13.507) AppleWebKit/534.35 (KHTML, like Gecko) Chrome/13.0.763.0 Safari/534.35",
       };
        for (int i = 0 ; i < agentStrings.length ; i++) {
            UserAgentString uas = new UserAgentString(agentStrings[i]);
                System.out.println("OS: \"" + uas.getOsInfo() + "\"  agentSring: \"" + agentStrings[i] + "\"");
        }
    }
    */
}