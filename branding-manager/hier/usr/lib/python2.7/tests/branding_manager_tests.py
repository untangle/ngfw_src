import unittest2
import os
import re
import urllib
import sys
reload(sys)
sys.setdefaultencoding("utf-8")

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import global_functions
from global_functions import uvmContextLongTimeout
import pdb

node = None
nodeWeb = None
newCompanyName = "Some new long name"
newURL = "https://test.untangle.com/cgi-bin/myipaddress.py"
newContactName = "Skynet"
newContactEmail = "skynet@untangle.com"

defaultRackId = 1

def setDefaultBrandingManagerSettings():
    nodeData = {
        "javaClass": "com.untangle.node.branding_manager.BrandingManagerSettings",
        "companyName": "Untangle",
        "companyUrl": "http://untangle.com/",
        "contactName": "your network administrator",
        "contactEmail": None,
        "bannerMessage": None,
        "defaultLogo": True
    }
    node.setSettings(nodeData)
    
class BrandingManagerTests(unittest2.TestCase):
    
    @staticmethod
    def nodeName():
        return "branding-manager"

    @staticmethod
    def nodeNameWeb():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global nodeData, node, nodeWeb
        if (uvmContextLongTimeout.nodeManager().isInstantiated(self.nodeName())):
            print "ERROR: Node %s already installed" % self.nodeName()
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContextLongTimeout.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodeData = node.getSettings()
        if (uvmContextLongTimeout.nodeManager().isInstantiated(self.nodeNameWeb())):
            print "ERROR: Node %s already installed" % self.nodeNameWeb()
            raise Exception('node %s already instantiated' % self.nodeNameWeb())
        nodeWeb = uvmContextLongTimeout.nodeManager().instantiate(self.nodeNameWeb(), defaultRackId)

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_changeBranding(self):
        global node, nodeWeb, nodeData
        nodeData['companyName'] = newCompanyName;
        nodeData['companyUrl'] = newURL;
        nodeData['contactName'] = newContactName;
        nodeData['contactEmail'] = newContactEmail;
        node.setSettings(nodeData)
        # test blockpage has all the changes
        result = remote_control.run_command("wget -q -O - \"$@\" www.playboy.com",stdout=True)

        # Verify Title of blockpage as company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print "looking for: \"%s\""%newCompanyName
        print "in :\"%s\""%matchText
        assert(newCompanyName in matchText)

        # Verify email address is in the contact link
        myRegex = re.compile('mailto:(.*?)\?', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print "looking for: \"%s\""%newContactEmail
        print "in :\"%s\""%matchText
        assert(newContactEmail in matchText)

        # Verify contact name is in the mailto
        myRegex = re.compile('mailto:.*?>(.*?)<\/a>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.strip()
        print "looking for: \"%s\""%newContactName
        print "in :\"%s\""%matchText
        assert(newContactName in matchText)

        # Verify URL is in the Logo box
        myRegex = re.compile('<a href\=\"(.*?)\"><img .* src\=\"\/images\/BrandingLogo', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        print "looking for: \"%s\""%newURL
        print "in :\"%s\""%matchText
        assert(newURL in matchText)
       
        # Check login page for branding
        internalAdmin = None
        # print "IP address <%s>" % internalAdmin
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        # print "page is <%s>" % result
        # Verify Title of blockpage as company name
        myRegex = re.compile('<title>(.*?)</title>', re.IGNORECASE|re.DOTALL)
        matchText = myRegex.search(result).group(1)
        matchText = matchText.split("|")[0]
        matchText = matchText.strip()
        print "looking for: \"%s\""%newCompanyName
        print "in :\"%s\""%matchText
        assert(newCompanyName in matchText)

    def test_021_changeBranding_bannerMessage_added(self):
        global node, nodeWeb, nodeData
        nodeData['companyName'] = newCompanyName;
        nodeData['companyUrl'] = newURL;
        nodeData['contactName'] = newContactName;
        nodeData['contactEmail'] = newContactEmail;
        nodeData['bannerMessage'] = "A regulation banner requirement containing a mix of text including <b>html</b> and\nmultiple\nlines"
        node.setSettings(nodeData)

        internalAdmin = None
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html<\/b> and<br\/>multiple<br\/>lines.*', re.DOTALL|re.MULTILINE)
        if re.match(myRegex,result):
            assert(True)
        else:
            assert(False)
        
    def test_022_changeBranding_bannerMessage_removed(self):
        global node, nodeWeb, nodeData
        nodeData['companyName'] = newCompanyName;
        nodeData['companyUrl'] = newURL;
        nodeData['contactName'] = newContactName;
        nodeData['contactEmail'] = newContactEmail;
        nodeData['bannerMessage'] = ""
        node.setSettings(nodeData)

        internalAdmin = None
        result = remote_control.run_command("wget -q -O - \"$@\" " + global_functions.get_http_url() ,stdout=True)
        myRegex = re.compile('.*A regulation banner requirement containing a mix of text including <b>html<\/b> and<br\/>multiple<br\/>lines.*', re.DOTALL|re.MULTILINE)
        if re.match(myRegex,result):
            assert(False)
        else:
            assert(True)
        
    @staticmethod
    def finalTearDown(self):
        global node, nodeWeb
        if node != None:
            # Restore original settings to return to initial settings
            setDefaultBrandingManagerSettings()
            uvmContextLongTimeout.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeWeb != None:
            uvmContextLongTimeout.nodeManager().destroy( nodeWeb.getNodeSettings()["id"] )
            nodeWeb = None

test_registry.registerNode("branding-manager", BrandingManagerTests)
