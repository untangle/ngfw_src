import sys

from StringIO import StringIO

import urllib
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

handler = CurlRequestHandler()
try:
    handler.make_request( "http://localhost/login/login.py", urllib.urlencode({ "username" : "admin", "password" : "passwd" }))
except JSONRPCException:
    pass

proxy = ServiceProxy( "http://localhost/webui/JSON-RPC", None, handler )

remoteContext = proxy.RemoteUvmContext

args = copy( sys.argv )

calledMethod = False

script = args.pop(0)
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

if not calledMethod: print "Unable to find method: ", method

