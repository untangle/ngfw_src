
"""
  Copyright (c) 2007 Jan-Klaas Kollhof

  This file is part of jsonrpc.

  jsonrpc is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or
  (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this software; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
"""

import urllib.request, urllib.error, urllib.parse
import types

from jsonrpc.json import dumps, loads

class JSONRPCException(Exception):
    def __init__(self, rpcError):
        Exception.__init__(self)
        self.error = rpcError

"""
Extend or redefine this class if you want to use a different mechanism
to make the request.  EG. In python 2.4, urllib2 for instance doesn't
support persistent connections, could use pycurl instead.
"""
class RequestHandler(object):
    def __init__(self):
        self.__opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor())

    def make_request(self, url, postdata):
        print("XXXXXX: ")
        request = urllib.request.Request( url, postdata, headers = { "Content-type" : "text/plain" } )
        return self.__opener.open( request ).read()
        
class ServiceProxy(object):
    __request_id = 1

    def __init__(self, serviceURL, serviceName=None, handler=None, nonce=None):
        self.__serviceURL = serviceURL
        self.__serviceName = serviceName
        self.__handler = handler
        self.nonce = nonce
        if self.__handler == None:
            self.__handler = RequestHandler()
        if self.nonce == None:
            self.getNonce()

    def __getattr__(self, name):
        if self.__serviceName != None:
            name = "%s.%s" % (self.__serviceName, name)
        return ServiceProxy(self.__serviceURL, name, self.__handler, self.nonce)

    def getNonce(self):
        ServiceProxy.__request_id += 1
        postdata = dumps({"method": "system.getNonce", 'params': [], 'id': ServiceProxy.__request_id })
        respdata = self.__handler.make_request( self.__serviceURL, postdata )
        resp = loads(respdata)
        if 'error' in resp and resp['error'] != None:
            print("A little error: ", resp['error'])
            raise JSONRPCException(resp['error'])
        self.nonce = resp['result']

    def __call__(self, *args):
        ServiceProxy.__request_id += 1
        postdata = dumps({"method": self.__serviceName, 'params': args, 'nonce': self.nonce, 'id': ServiceProxy.__request_id })
        respdata = self.__handler.make_request( self.__serviceURL, postdata )
        resp = loads(respdata)
        if 'error' in resp and resp['error'] != None:
            print("A little error: ", resp['error'])
            raise JSONRPCException(resp['error'])
        self.__fix_callable_refs( resp )
        result = resp['result']

        if 'fixups' in resp and type( resp['fixups'] ) is list:
            self.__handle_fixups__( result, resp['fixups'] )
        
        return result

    def __fix_callable_refs( self, json_obj ):
        if json_obj == None:
            return
        if type( json_obj ) is list:
            for index,v in enumerate(json_obj):
                if ( type( v ) is dict ) and 'JSONRPCType' in v and ( v['JSONRPCType'] == "CallableReference" ) and 'objectID' in v:
                    json_obj[index] = ServiceProxy(self.__serviceURL, ".obj#%s" % v['objectID'], self.__handler, self.nonce)
                else:
                    self.__fix_callable_refs( v )
        if type( json_obj ) is dict:
            for k, v in list(json_obj.items()):
                if ( type( v ) is dict ) and 'JSONRPCType' in v and ( v['JSONRPCType'] == "CallableReference" ) and 'objectID' in v:
                    json_obj[k] = ServiceProxy(self.__serviceURL, ".obj#%s" % v['objectID'], self.__handler, self.nonce)
                else:
                    self.__fix_callable_refs( v )

    def __handle_fixups__( self, result, fixups ):
        ## Iterate all of the fixups, 
        for fixup in fixups:
            ## Each fixup is of the form (path_destination, path_source)
            ## Just iterate through and copy source to destination.
            destination = fixup[0]
            source = fixup[1]
            
            original = self.__find_object__( result, source )
            if original == None: continue

            copy = self.__find_object__( result, destination[0:len(destination) -1] )
            if copy == None: continue

            copy[destination[-1]] = original

    def __find_object__( self, base, path ):
        for key in path:
            base = base[key]

        return base

        


 
