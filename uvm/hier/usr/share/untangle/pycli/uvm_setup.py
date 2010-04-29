## Install python-codespeak-lib in order to run tests.

import logging

import pycurl
from StringIO import StringIO


from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException



class CurlRequestHandler(object):
    def __init__( self, timeout=30 ):
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 30 )
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
    

class UvmSetup:
    ## Defaulting the timeout to 30 seconds
    def setup_class( cls ):
        ## Each test uses its own proxy, one test cannot polute the object space of the next.
        ## Username and password are not supported
        cls.handler = CurlRequestHandler()
        cls.proxy = ServiceProxy( "http://localhost/webui/JSON-RPC", None, cls.handler )
        cls.remote_uvm_context = cls.proxy.RemoteUvmContext
        cls.logger = logging.getLogger( cls.__name__ )

    ## This sets the timeout for all of the proxies, since testing is single threaded,
    ## this should be reset after the test runs
    def set_timeout( self, timeout ):
        cls.handler.set_timeout( timeout )


