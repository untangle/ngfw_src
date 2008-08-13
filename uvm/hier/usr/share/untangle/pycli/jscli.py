import sys

from StringIO import StringIO

import urllib
import getopt
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from copy import copy

from uvm import Manager

import pycurl

# Always use pycurl.
# 1. cookies are supported on python 2.3 (not supported cleanly in
# urllib2)
# 2. persistent sessions are supported in all version of python.  Up
# to 2.4 this hasn't made it in yet.  There is keepalive from
# urlgrabber, it doesn't support cookies which are required in order
# for java callable references to work.
class CurlRequestHandler(object):
    def __init__(self):
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 10 )
        self.__curl.setopt( pycurl.COOKIEFILE, "" )
        self.__curl.setopt( pycurl.FOLLOWLOCATION, 0 )

    def make_request(self, url, postdata, content_type = "text/plain"  ):
        response = StringIO()

        self.__curl.setopt( pycurl.URL, url )
        self.__curl.setopt( pycurl.POST, 1 )
        if not content_type == None:
            self.__curl.setopt( pycurl.HTTPHEADER, [ "Content-type: ", content_type ] )
        self.__curl.setopt( pycurl.VERBOSE, False )
        self.__curl.setopt( pycurl.POSTFIELDS, str( postdata ))
        self.__curl.setopt( pycurl.WRITEFUNCTION, response.write )
        self.__curl.perform()

        if ( self.__curl.getinfo( pycurl.HTTP_CODE ) != 200 ): raise JSONRPCException("Invalid username or password")

        return response.getvalue()

def printUsage(script):
    print """\
%s Usage:
  optional args:
    -h hostname
    -u username
    -w password
    -t timeout (default 120000)
    -p policy
    -v
  toolbox commands:
    ucli install mackage-name
    ucli uninstall mackage-name
    ucli update
    ucli upgrade
    ucli requestInstall mackage-name
  toolbox lists:
    ucli available
    ucli installed
    ucli uninstalled
    ucli upgradable
    ucli uptodate
  node manager commands:
    ucli instantiate mackage-name [ args ]
    ucli start TID
    ucli stop TID
    ucli destroy TID
    ucli neverStarted
  node manager lists:
    ucli instances
  node live sessions:
    ucli sessions [ TID ]
  admin manager:
    ucli who
    ucli getRegInfo
    ucli passwd [ -a | -d ] login [ password ]
  uvm commands:
    ucli shutdown
    ucli serverStats
    ucli gc
    ucli loadRup
    ucli setProperty key value
  policy manager:
    ucli addPolicy name [notes]
    ucli listPolicies
  reporting manager:
    ucli isReportingEnabled
    ucli areReportsAvailable
    ucli prepareReports [ args ]
    ucli startReports
    ucli stopReports
    ucli isReportingRunning
  logging manager:
    ucli userLogs tid
    ucli resetLogs
    ucli logError [text]
  combo commands:
    ucli loadt short-name [ args ]
    ucli reloadt short-name
    ucli unloadt short-name
  apt commands:
    ucli register mackage-name
    ucli unregister mackage-name
  argon commands:
  nucli server commands:
    ucli restartCliServer
  debugging commands:
    ucli aptTail
""" % script

handler = CurlRequestHandler()
try:
    handler.make_request( "http://localhost/auth/login", urllib.urlencode({ "username" : "admin", "password" : "passwd" }))
except JSONRPCException:
    pass

proxy = ServiceProxy( "http://localhost/webui/JSON-RPC", None, handler )

remoteContext = proxy.RemoteUvmContext

(optlist, args) = getopt.getopt(sys.argv[1:], 'h:u:w:t:p:v')
# XXX do something with the options

calledMethod = False

script = sys.argv[0]

if len(args) == 0:
    printUsage(script)
    sys.exit(1)

method = args.pop(0).lower()

for manager in Manager.managers:
    try:
        dir(manager).index( "api_" + method )
    except ValueError:
        continue

    calledMethod = True
    try:
        remoteManager = manager(remoteContext)
        getattr( remoteManager, "api_" + method )( *args )
    except JSONRPCException, e:
        print "Unable to make the request: ", e.error
    break

if not calledMethod:
    print "Unable to find method: ", method
    printUsage(script)
    sys.exit(1)
