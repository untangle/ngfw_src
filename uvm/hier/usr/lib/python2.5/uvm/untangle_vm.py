import logging
import pycurl
from StringIO import StringIO
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException

# Always use pycurl.
# 1. cookies are supported on python 2.3 (not supported cleanly in
# urllib2)
# 2. persistent sessions are supported in all version of python.  Up
# to 2.4 this hasn't made it in yet.  There is keepalive from
# urlgrabber, it doesn't support cookies which are required in order
# for java callable references to work.
class CurlRequestHandler(object):
    def __init__( self, timeout=60 ):
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 60 )
        self.__curl.setopt( pycurl.TIMEOUT, timeout )
        self.__curl.setopt( pycurl.COOKIEFILE, "" )
        self.__curl.setopt( pycurl.FOLLOWLOCATION, 0 )

    def make_request(self, url, postdata, content_type = "text/plain" ):
        response = StringIO()

        self.__curl.setopt( pycurl.URL, url )
        self.__curl.setopt( pycurl.POST, 1 )
        if not content_type == None:
            self.__curl.setopt( pycurl.HTTPHEADER, [ "Content-type: ", content_type ] )
        self.__curl.setopt( pycurl.VERBOSE, False )
        self.__curl.setopt( pycurl.POSTFIELDS, str( postdata ))
        self.__curl.setopt( pycurl.WRITEFUNCTION, response.write )
        try:
            self.__curl.perform()
        except Exception, e:
            print "Problem while asking for " + url
            raise e

        if ( self.__curl.getinfo( pycurl.HTTP_CODE ) != 200 ): raise JSONRPCException("Invalid username or password")

        return response.getvalue()

    ## Change the timeout for receiving a response
    def set_timeout( self, timeout ):
        self.__curl.setopt( pycurl.TIMEOUT, timeout )
    

class Uvm:
    def getUvmContext( self, hostname="127.0.0.1", username=None, password=None, timeout=60 ):
        handler = CurlRequestHandler( timeout )

        try:
            if ( username != None and password != None ):
                handler.make_request( "http://" + hostname  + "/auth/login", urllib.urlencode({ "username" : username, "password" : password }))
        except JSONRPCException:
            pass
        
        proxy = ServiceProxy( "http://" + hostname +  "/webui/JSON-RPC", None, handler )

        return proxy.UvmContext



