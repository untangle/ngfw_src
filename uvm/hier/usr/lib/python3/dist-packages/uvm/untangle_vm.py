import logging
import pycurl
import urllib.request, urllib.parse, urllib.error
import traceback
from io import BytesIO
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
    def __init__( self, timeout=240, verify_certs=True):
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 60 )
        self.__curl.setopt( pycurl.TIMEOUT, timeout )
        self.__curl.setopt( pycurl.COOKIEFILE, "" )
        self.__curl.setopt( pycurl.FOLLOWLOCATION, 0 )
        if verify_certs is False:
            self.__curl.setopt( pycurl.SSL_VERIFYPEER, 0 )
            self.__curl.setopt( pycurl.SSL_VERIFYHOST, 0 )

    def make_request(self, url, postdata, content_type = "text/plain; charset=utf-8", follow_redirects=False ):
        response = BytesIO()

        self.__curl.setopt( pycurl.URL, url )
        self.__curl.setopt( pycurl.POST, 1 )
        if not content_type == None:
            self.__curl.setopt( pycurl.HTTPHEADER, [ "Content-type: ", content_type ] )
        self.__curl.setopt( pycurl.VERBOSE, False )
        self.__curl.setopt( pycurl.POSTFIELDS, postdata.encode('utf-8'))
        self.__curl.setopt( pycurl.WRITEFUNCTION, response.write )
        if follow_redirects:
            self.__curl.setopt(pycurl.FOLLOWLOCATION, 1)
        try:
            self.__curl.perform()
        except Exception as e:
            print(("Problem while asking for " + url))
            raise e

        http_code = self.__curl.getinfo( pycurl.HTTP_CODE ) 
        if ( http_code != 200 ): 
            if http_code == 302:
                raise JSONRPCException( "Invalid username or password [code: %i]" % http_code )
            elif http_code == 500:
                raise JSONRPCException( "Internal server error [code: %i]" % http_code )
            elif http_code == 502 or http_code == 503:
                raise JSONRPCException( "Service unavailable [code: %i]" % http_code )
            else:
                raise JSONRPCException( "An error occurred [code: %i] response: %s" % (http_code, response.getvalue()) )

        return response.getvalue()

    ## Change the timeout for receiving a response
    def set_timeout( self, timeout ):
        self.__curl.setopt( pycurl.TIMEOUT, timeout )

class Uvm:
    def getUvmContext( self, hostname="127.0.0.1", username=None, password=None, timeout=240, scheme="http" ):
        handler = CurlRequestHandler( timeout, verify_certs=False)

        uri = f"{scheme}://{hostname}"
        try:
            if ( username != None and password != None ):
                handler.make_request( f"{uri}/auth/login?url=/admin&realm=Administrator", urllib.parse.urlencode({ "fragment":"", "username" : username, "password" : password }), follow_redirects=True)
        except JSONRPCException as e:
            print("Login error: ")
            traceback.print_exc(e)
            return None
        
        proxy = ServiceProxy( f"{uri}/admin/JSON-RPC", None, handler, None )
        return proxy.UvmContext




