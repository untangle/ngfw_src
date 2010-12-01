import sys

from StringIO import StringIO

import urllib
import getopt
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from copy import copy

from uvm import Manager

import pycurl
import codecs

# Always use pycurl.
# 1. cookies are supported on python 2.3 (not supported cleanly in
# urllib2)
# 2. persistent sessions are supported in all version of python.  Up
# to 2.4 this hasn't made it in yet.  There is keepalive from
# urlgrabber, it doesn't support cookies which are required in order
# for java callable references to work.
class CurlRequestHandler(object):
    def __init__( self, timeout=30 ):
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 30 )
        self.__curl.setopt( pycurl.TIMEOUT, timeout )
        self.__curl.setopt( pycurl.COOKIEFILE, "" )
        self.__curl.setopt( pycurl.FOLLOWLOCATION, 0 )

    def make_request(self, url, postdata, content_type = None ):
        response = StringIO()

        self.__curl.setopt( pycurl.URL, url )
        self.__curl.setopt( pycurl.POST, 1 )
        if content_type != None:
            self.__curl.setopt( pycurl.HTTPHEADER, [ "Content-Type: %s" % content_type ] )
        self.__curl.setopt( pycurl.VERBOSE, False )
        self.__curl.setopt( pycurl.POSTFIELDS, postdata.encode( "utf-8" ))
        self.__curl.setopt( pycurl.WRITEFUNCTION, response.write )
        self.__curl.perform()

        if ( self.__curl.getinfo( pycurl.HTTP_CODE ) != 200 ): raise JSONRPCException("Invalid username or password")

        return response.getvalue()

class ArgumentParser(object):
    def __init__(self):
        self.hostname = "localhost"
        self.username = None
        self.password = None
        self.timeout = 30
        self.policy_name = None
        self.verbosity = 0

    def set_hostname( self, arg ):
        self.hostname = arg

    def set_username( self, arg ):
        self.username = arg

    def set_password( self, arg ):
        self.password = arg

    def set_timeout( self, arg ):
        self.timeout = int( arg )

    def set_policy( self, arg ):
        self.policy_name = arg

    def increase_verbosity( self, arg ):
        self.verbosity += 1

    def parse_args( self ):
        handlers = {
            '-h' : self.set_hostname,
            '-u' : self.set_username,
            '-w' : self.set_password,
            '-t' : self.set_timeout,
            '-p' : self.set_policy,
            '-v' : self.increase_verbosity
        }

        (optlist, args) = getopt.getopt(sys.argv[1:], 'h:u:w:t:p:v')
        for opt in optlist:
            handlers[opt[0]](opt[1])
        return args

def printUsage():
    sys.stderr.write( """\
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
  logging manager:
    ucli userLogs tid
    ucli resetLogs
    ucli logError [text]
  apt commands:
    ucli register mackage-name
    ucli unregister mackage-name
  argon commands:
  nucli server commands:
    ucli restartCliServer
  debugging commands:
    ucli aptTail
""" % sys.argv[0] )

def make_proxy( parser, timeout=30 ):
    handler = CurlRequestHandler( timeout )

    try:
        if ( parser.username != None and parser.password != None ):
            handler.make_request( "http://" + parser.hostname  + "/auth/login", urllib.urlencode({ "username" : parser.username, "password" : parser.password }))
    except JSONRPCException:
        pass

    proxy = ServiceProxy( "http://" + parser.hostname +  "/webui/JSON-RPC", None, handler )

    return proxy

parser = ArgumentParser()

try:
    if ( sys.stdout.encoding != "UTF-8" ):
        sys.stderr.write( "Changing to UTF-8 encoding\n" )
        (e,d,sr,sw) = codecs.lookup("UTF-8")

        sys.stdout = sw(sys.stdout)
        sys.stderr = sw(sys.stderr)
        
except:
    sys.stderr.write( "Unable to change to UTF-8 encoding\n" )

try:
    script_args = parser.parse_args()
except:
    printUsage()
    sys.exit(1)

proxy = make_proxy( parser, parser.timeout )
remoteContext = proxy.RemoteUvmContext

if ( parser.policy_name != None ):
    Manager.policy = remoteContext.policyManager().getPolicy(parser.policy_name)

Manager.verbosity = parser.verbosity

calledMethod = False

if len(script_args) == 0:
    printUsage()
    sys.exit(1)

method = script_args.pop(0).lower()

for manager in Manager.managers:
    try:
        dir(manager).index( "api_" + method )
    except ValueError:
        continue

    calledMethod = True
    try:
        puts method
        remoteManager = manager(remoteContext)
        getattr( remoteManager, "api_" + method )( *script_args )
    except JSONRPCException, e:
        sys.stderr.write( "Unable to make the request: (%s)\n" % e.error )
    break

if not calledMethod:
    sys.stderr.write( "Unable to find method: (%s)\n" % method )
    printUsage()
    sys.exit(1)
