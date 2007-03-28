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

package com.untangle.tran.ids;

import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import com.untangle.mvvm.api.SessionEndpoints;
import com.untangle.mvvm.tapi.*;
import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tran.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class IDSTest {

    private class TestDataEvent implements IPDataEvent {
        ByteBuffer buffer;
        public TestDataEvent() {
        }

        public void setData(byte[] data) {
            buffer = ByteBuffer.allocate(data.length + 1);
            buffer.put(data);
            buffer.flip();
        }

        public void setData(File fileData) {
            try {
                long len = fileData.length();
                if (len <= 0)
                    throw new IOException("File " + fileData.getPath() + " does not exist");
                FileInputStream fis = new FileInputStream(fileData);
                FileChannel chan = fis.getChannel();
                buffer = ByteBuffer.allocate((int)len + 1);
                while (chan.read(buffer) > 0);
                buffer.flip();
                chan.close();
                fis.close();
            } catch (IOException x) {
                System.out.println("Lose: " + x);
                x.printStackTrace();
            }
        }

        public ByteBuffer data() {
            return buffer;
        }
    }

    private final Logger log = Logger.getLogger(getClass());
    private IDSRuleManager manager = new IDSRuleManager(null);

    public IDSTest()
    {
        log.setLevel(Level.ALL);
    }

    public boolean runTest() {
        generateRuleTest();
        runHeaderTest();
        runSignatureTest();
        //generateRandomRuleHeaders(1000);
        //runTimeTest(1);
        return true;
    }

    public static void main(String[] args) {
        IDSTest test = new IDSTest();
        if (test.runTest()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Test fails");
        }
    }

/*Order is no longer preserved - same headers get compressed :/*/
    private boolean generateRuleTest() {
        String testValidStrings[]   = {
            /** Header 0*/
            "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (content:\"bob\"; msg:\"Rule Zero\"; flow: to_server;)",

            /** Header 1*/
            "alert tcp 10.0.0.101 !5000: -> 10.0.0.1/16 !80 (content: \"BOB\"; offset: 3; nocase; msg:\"Rule one\"; flow: from_server;)",

            /**Header 2*/
            "alert TCP 10.0.0.101 4000:5000 <> 10.0.0.1/24 :6000 (content:\"bob\"; content:\"BOB\"; nocase; msg:  Rule 2; dsize: < 5;)",

            /**Header 3*/
            "alert tcp [10.0.0.101,192.168.1.1,10.0.0.44] !:80 -> any 80 (msg: Rule x3x; dsize: 3<> 10; )",

            /**Header 4*/
            "alert tcp 66.35.250.0/24 any -> 10.0.0.1/24 any (msg:\"Rule 4, Server as client test\")",

            /**Header 5*/
            "alert udp $EXTERNAL_NET any -> $HOME_NET 22 (msg:\"Rule 5(exploit test2)\"; flow:to_server,established; content:\"|00 01|W|00 00 00 18|\"; depth:7; content:\"|FF FF FF FF 00 00|\"; depth:14; offset:8; reference:bugtraq,2347; reference:cve,2001-0144; reference:cve,2001-0572; classtype:shellcode-detect; sid:1327; rev:7;)",

            /**Header 6*/
            "alert udp any any -> any any (msg:\"Rule 6\"; uricontent:\"/is/just/a/\"; nocase;)",
            "alert udp any any -> any any (msg:\"rule 7 (exploit test1)\"; flow:to_server,established; content:\"|90 90 90 90 90 90 90 90 90 90 90 90 90 90 90 90|\"; reference:bugtraq,2347; reference:cve,2001-0144; reference:cve,2001-0572; classtype:shellcode-detect; sid:1326; rev:6;)",

            /**Header 7*/
            "alert tcp any any -> any any (msg:\"Rule 8\"; content:|DE AD BE EF|BOB; nocase;)",
            "alert tcp any any -> any any (msg:\"Rule 9\"; dsize:  > 4 ;)",
            "alert tcp any any -> any any (msg:\"Rule 10\"; pcre:\"/r(a|u)wr/smi\" ;)",
            "alert tcp any any -> any any (msg:\"Rule 11\"; nocase; pcre:\"/(stuff=.*(a|b|c|\\;|d?rar))/i\"; dsize:  > 4 ;)",
            "alert tcp any any -> any any (msg:\"Rule 12\"; content:\"|00|bob|00||00|bob\";)",
            "alert tcp any any -> any any (msg:\"Rule 13\";  content:\"Hi\"; content:\"Bob\"; distance: 2; )",
            "alert tcp any any -> any any (msg:\"Rule 14\";  content:\"Hi\"; content:\"Bob\"; within: 5; )",
            "alert tcp any any -> any any (msg:\"Rule 15\";  content:\"Hi\"; content:\"|02|\"; within:1; distance:1;)",
            "alert tcp any any -> any any (msg:\"Rule 16\";  content:\"Hi\"; distance:1; within:3; content:\"|02|\"; within:2; distance:2;)",

            /** 3550 **/
            "alert tcp $EXTERNAL_NET $HTTP_PORTS -> $HOME_NET any (msg:\"(Rule 17) WEB-CLIENT HTML http scheme hostname overflow attempt\"; flow:to_client,established; content:\"http|3A|//\"; nocase; pcre:\"/=\\x22http\\x3a\\x2f\\x2f[^\\x3a\\x2f@\\s\\x22\\x3F\\x26]{255}|=\\x27http\\x3a\\x2f\\x2f[^\\x3a\\x2f@\\s\\x27\\x3F\\x26]{255}|=http\\x3a\\x2f\\x2f[^\\x3a\\x2f@\\s\\x3F\\x26]{255}/i\"; reference:cve,2005-0553; classtype:attempted-user; sid:3550; rev:5;)",
};

        

        for(int i=0; i < testValidStrings.length; i++) {
            try {
                IDSRule rule = manager.createRule(testValidStrings[i], "Testing");
                rule.setLog(true);
                rule.setKeyValue((long)i+1337l);
                manager.addRule(rule);

            } catch (ParseException e)  { log.error(e.getMessage()); }
        }
        return true;
    }

    private void runSignatureTest() {
        /**These test ignore header matching, and thus can deal with
         * all the test signatures*/

        /**Setup*/
        List<IDSRuleSignature> signatures = new LinkedList<IDSRuleSignature>();
        for(IDSRuleHeader header : manager.getHeaders())
            for(IDSRuleSignature sig : header.getSignatures())
                signatures.add(sig);

        IDSSessionInfo info = new IDSSessionInfo(null);
        info.setC2SSignatures(signatures);
        info.setS2CSignatures(signatures);
        info.setUriPath("/this/is/just/a/test");

        /**Run Tests*/
        TestDataEvent test = new TestDataEvent();
        checkSessionData(info, test, false, 6, true);

        info.setUriPath("rawr");
        byte[] basicNoCase = {'b','o','b'};
        test.setData(basicNoCase);
        checkSessionData(info, test, true, 1, false);
        checkSessionData(info, test, true, 2, true);
        checkSessionData(info, test, true, 3, true);
        checkSessionData(info, test, false, 5, false);
        checkSessionData(info, test, false, 9, false);
        checkSessionData(info, test, false, 12, false);

        byte[] basicNoCase1 = {'1','2','3','B','O','B'};
        test.setData(basicNoCase1);
        checkSessionData(info, test, false, 1, false);
        checkSessionData(info, test, true, 2, false);
        checkSessionData(info, test, false, 6, false);

        byte[] dSizeTest = { 'c', 'c', 'c', 'c', 'c', 'b', 'o', 'b' };
        test.setData(dSizeTest);
        checkSessionData(info, test, true, 1, true);
        checkSessionData(info, test, false, 1, false);
        checkSessionData(info, test, true, 3, true);
        checkSessionData(info, test, false, 4, true);
        ///////////////////////checkSessionData(info, test, true, 0, true);

        byte[] complexContentStuff = { '4','2',(byte)0xDE,(byte)0xAD,(byte)0xBE,(byte)0xEF,'b','o','b',(byte)0x1F,(byte)0x12 };
        test.setData(complexContentStuff);
        checkSessionData(info, test, false,2, false);
        checkSessionData(info, test, true, 1, true);
        checkSessionData(info, test, false, 8, true);

        byte[] nopSled = new byte[20];
        for(int i=0; i<nopSled.length;i++)
            nopSled[i] = (byte) 0x90;
        test.setData(nopSled);
        checkSessionData(info, test, true, 7, false);
        checkSessionData(info, test, false, 7, true);

        byte[] test5 = { (byte)0x00, (byte) 0x01, 'W',  (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x18, 'f', (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte) 0x00, (byte) 0x00 };
        test.setData(test5);
        checkSessionData(info, test, false, 5, true);

        byte[] test5FailDepth = { (byte)0x00, (byte) 0x01, 'W',  (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x18, 'f', 'a','a','a','a','a','a','a','a','a','a','a', (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte) 0x00, (byte) 0x00 };
        test.setData(test5FailDepth);
        checkSessionData(info, test, false, 5, false);

        byte[] distanceTest = { 'H','i','q','q','B','o','b' };
        test.setData(distanceTest);
        checkSessionData(info, test, true, 13, true);
        checkSessionData(info, test, true, 14, true);

        byte[] distanceTest2 = { 'H','i','q','B','o','b' };
        test.setData(distanceTest2);
        checkSessionData(info, test, true, 13, false);
        checkSessionData(info, test, true, 14, true);

        byte[] distanceTest3 = { 'H','i','q','q','q','q','q','q','B','o','b' };
        test.setData(distanceTest3);
        checkSessionData(info, test, true, 13, true);
        checkSessionData(info, test, true, 14, false);

        byte[] withinDebug = { 'H','i','q',(byte)0x02 };
        test.setData(withinDebug);
        checkSessionData(info, test, true, 15, true);

        byte[] withinDebug1 = { 'H','i','q',(byte)0x02 };
        test.setData(withinDebug1);
        //////////////////////checkSessionData(info, test, true, 16, true);

        File test3550 = new File("testneg3550.cap");
        test.setData(test3550);
        checkSessionData(info, test, true, 17, false);

        test3550 = new File("testpos3550.cap");
        test.setData(test3550);
        checkSessionData(info, test, true, 17, true);
    }

    private void checkSessionData(IDSSessionInfo info, IPDataEvent event, boolean isServer,int ruleNum,  boolean answer) {
        info.setEvent(event);
        info.setFlow(isServer);
        if(!checkAnswer(info.testSignature(ruleNum), answer)) {
            log.warn("Option Test Failed on rule: " + ruleNum);
            log.warn("\tSignature contents::\n" + info.getSignature(ruleNum));
            log.warn("Data: " + new String(event.data().array()));
        }
    }


    private void runHeaderTest() {

        List<IDSRuleHeader> ruleList = manager.getHeaders();

        matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.101", 33242, "66.35.250.8", 80, false);
        matchTest(ruleList.get(3), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, true);
        matchTest(ruleList.get(3), Protocol.UDP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
        matchTest(ruleList.get(6), Protocol.UDP, "123.123.123.123", 1254, "10.0.0.123", 22, true);
        matchTest(ruleList.get(7), Protocol.UDP, "123.123.123.123", 1254, "10.0.0.123", 22, false);
        matchTest(ruleList.get(1), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
        matchTest(ruleList.get(2), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
        matchTest(ruleList.get(3), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, true);
        matchTest(ruleList.get(0), Protocol.TCP, "10.0.0.44", 33065, "66.35.250.8", 80, true);
        matchTest(ruleList.get(0), Protocol.TCP, "66.35.250.8", 33065, "10.0.0.44", 80, false);
        matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.43", 1024, "10.0.0.101", 4747, false);
        matchTest(ruleList.get(4), Protocol.TCP, "10.0.0.43", 1024, "10.0.0.101", 4747, false);
        matchTest(ruleList.get(2), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, false);
        matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, true);
        matchTest(ruleList.get(3), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, false);

    }

    private void matchTest(IDSRuleHeader header, Protocol protocol, String clientAddr, int clientPort, String serverAddr, int serverPort, boolean answer) {
            InetAddress clientAddress = null;
            InetAddress serverAddress = null;
            try {
                clientAddress = InetAddress.getByName(clientAddr);
                serverAddress = InetAddress.getByName(serverAddr);
            } catch( Exception e ) { log.error(e); }

            SessionEndpoints se = new TestEndpoints((short)protocol.getId(), clientAddress, clientPort, serverAddress, serverPort);
            if(!checkAnswer(header.matches(se, true, true),answer))
                log.warn("Match Test Failed:\n"  +
                         "Client:" +clientAddress+":"+clientPort +
                         "\nServer:" +serverAddress+":"+serverPort +
                         "\ndoes not match rule:\n" + header +"\n");
    }

    private boolean checkAnswer(boolean eval, boolean correct) {
        if(eval != correct)
            log.warn("Evaluated: "+ eval+ " Should be: " + correct);
        return eval == correct;
    }
/*
    private void runTimeTest(int seconds) {
        long stopTime = System.currentTimeMillis() + seconds*1000;
        int counter = 0;
        manager = IDSDetectionEngine.instance().getRulesForTesting();
        Random rand = new Random();
        while(stopTime > System.currentTimeMillis()) {
            try {
                InetAddress addr1 = InetAddress.getByName(rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256));
                InetAddress addr2 = InetAddress.getByName(rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256));
                IPNewSessionRequest session = buildSessionRequest(Protocol.TCP, addr1, rand.nextInt(65536), addr2, rand.nextInt(65536));
            } catch (Exception e) { log.error("er");}
            counter++;
        }
        double timePerMatch = (double)seconds*1000/(double)counter;
        log.info("Completed " + counter+ " matches in " + seconds + " seconds "+" ("+timePerMatch+" ms/match"+").");
    }
*/
    private void generateRandomRuleHeaders(int num) {
        long startTime = System.currentTimeMillis();
        Random rand = new Random();
        manager.clear();
        for(int i=0;i<num;i++) {

            String dir;
            String prot;
            String clientIP, serverIP;
            String clientPort, serverPort;
            if(rand.nextInt(2) == 0)
                dir = " ->";
            else
                dir = " <>";

            if(rand.nextInt(2) == 0)
                prot = " udp";
            else
                prot = " TCP";
            clientIP = getRandomIPAddress();
            serverIP = getRandomIPAddress();

            clientPort = getRandomPort();
            serverPort = getRandomPort();

        //  try {
                //manager.addRule("alert"+prot+clientIP+clientPort+dir+serverIP+serverPort+" ( content: \"I like spoons\"; msg: \"This is just a test\";)");
        //  } catch(ParseException e) { log.error("Could not parse rule; " + e.getMessage()); }
        }
        long endTime = System.currentTimeMillis() - startTime;
        log.info("Time it took to parse " + num +" rules: " + endTime + " milliseconds");
    }

    private String getRandomPort() {
        String str;
        Random rand = new Random();
        switch(rand.nextInt(3)) {
            case 0:
                str = "any";
                break;
            case 1:
                str = rand.nextInt(65536)+"";
                break;
            case 2:
                int port1 = rand.nextInt(65536);
                int port2 = rand.nextInt(65536);
                str = port1+":"+port2;
                break;
            default:
                str = "any";
        }
        return " "+str;
    }

    private String getRandomIPAddress() {
        String str;
        Random rand = new Random();
        switch(rand.nextInt(4))  {
            case 0:
                str = "any";
                break;
            case 1:
                str ="10.0.0.1/24";
                break;
            case 2:
                str = "[192.168.0.1,192.168.0.30,192.168.0.101]";
                break;
            case 3:
                str = rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256);
                break;
            default:
                str = "any";
        }
        return " "+str;
    }

    // Helper class
    class TestEndpoints implements SessionEndpoints {
        short protocol;
        InetAddress clientAddr;
        InetAddress serverAddr;
        int clientPort;
        int serverPort;
        TestEndpoints(short protocol, InetAddress clientAddr, int clientPort, InetAddress serverAddr, int serverPort) {
            this.protocol = protocol;
            this.clientAddr = clientAddr;
            this.serverAddr = serverAddr;
            this.clientPort = clientPort;
            this.serverPort = serverPort;
        }
        public short protocol() { return protocol; }
        public InetAddress clientAddr() { return  clientAddr; }
        public InetAddress serverAddr() { return  serverAddr; }
        public int clientPort() { return  clientPort; }
        public int serverPort() { return  serverPort; }
        public byte clientIntf() { return 0; } // unused
        public byte serverIntf() { return 0; } // unused
    }
}
