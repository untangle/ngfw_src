from mod_python import apache
from mod_python import util
from mod_python import Cookie
import sys

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python3/dist-packages')

from uvm import Uvm
import urllib.request, urllib.parse, urllib.error
import pprint
import imp
import time
import os
import uvm.i18n_helper

from urllib.parse import urlparse

from uvm.settings_reader import get_app_settings_item
from uvm.settings_reader import get_appid_settings
from uvm.settings_reader import get_app_settings
from uvm.settings_reader import get_settings_item

_ = uvm.i18n_helper.get_translation('untangle').lgettext

# Dictionary of Oauth providers by name, each with the following fields:
# platform  Identifier to pass to auth-relay
# method    Captive Portal application method name to register the session
OAUTH_PROVIDERS = {
    "GOOGLE": {
        "platform": "365238258169-6k7k0ett96gv2c8392b9e1gd602i88sr.apps.googleusercontent.com",
        "method": "googleLogin",
        "uri": "https://accounts.google.com/o/oauth2/v2/auth?client_id=365238258169-6k7k0ett96gv2c8392b9e1gd602i88sr.apps.googleusercontent.com&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=email&state=$.GoogleState.$"
    },
    "FACEBOOK": {
        "platform": "1840471182948119",
        "method": "facebookLogin",
        "uri": "https://www.facebook.com/v2.9/dialog/oauth?client_id=1840471182948119&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=email&state=$.FacebookState.$"
    },
    "MICROSOFT": {
        "platform": "f8285e96-b240-4036-8ea5-f37cf6b981bb",
        "method": "microsoftLogin",
        "uri" : "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=f8285e96-b240-4036-8ea5-f37cf6b981bb&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=openid%20User.Read&state=$.MicrosoftState.$"
    },
    "ANY_OAUTH": {
        "platform": None,
        "method": None,
        "uri": None
    }
}

## PythonOption ApplicationPath /
#mod_python.session.application_path /

PSOCookieSession_Path = "/"

class HandlerCookie:
    cookie_key = "__ngfwcp"
    def __init__(self,req,appid=None):
        self.req = req
        if appid == None:
            args = split_args(self.req.args);
            appid = args['APPID']
        self.captureSettings = load_capture_settings(self.req,appid)
        self.cookie = Cookie.get_cookies(self.req, Class=Cookie.MarshalCookie, secret=str(self.captureSettings["secretKey"]))

    def is_valid(self):
        if self.get_field("username") != None:
            return True

        return False

    def get_field(self,key):
        value = self.cookie.get(self.cookie_key, None)
        if value:
            if ((type(value) is Cookie.MarshalCookie) and
                key in value.value ):
                return value.value[key]
        return None

    def set(self,username):
        value = {
            "username": username
        }
        cookie = Cookie.MarshalCookie(self.cookie_key, value, secret=str(self.captureSettings["secretKey"]))
        cookie.path = "/"
        cookie.expires = time.time() + int(self.captureSettings["sessionCookiesTimeout"])
        Cookie.add_cookie(self.req, cookie)

    def expire(self):
        value = {}
        cookie = Cookie.MarshalCookie(self.cookie_key, value, secret=str(self.captureSettings["secretKey"]))
        cookie.path = "/"
        cookie.expires = 0
        Cookie.add_cookie(self.req, cookie)

#-----------------------------------------------------------------------------
# This is the default function that gets called when a client is redirected
# to the captive portal because they have not yet authenticated.

def index(req):

    # get the original destination and other arguments passed
    # in the URL when the redirect was generated
    args_keys = ['AUTHCODE', 'AUTHMODE', 'APPID', 'METHOD', 'NONCE', 'HOST','URI']
    args = split_args(req.args);
    for key in args_keys:
        if not key in args:
            args[key] = "Empty"
        else:
            if type(args[key]) == bytes:
                args[key] = args[key].decode('utf-8')

    # load the configuration data
    appid = args['APPID']
    captureSettings = load_capture_settings(req,appid)
    captureApp = None

    authcode = args['AUTHCODE']
    authmode = args['AUTHMODE']

    if (authcode != "Empty"):
        authenticationType = captureSettings.get("authenticationType")
        uri_base = None
        if authenticationType in list(OAUTH_PROVIDERS.keys()):
            if authmode == "Empty":
                authmode = authenticationType
            ut = Uvm().getUvmContext().uriManager().getUriTranslationByHost("auth-relay.untangle.com")
            port = ""
            if ut['port'] != -1:
                ut['port'] = ":" + str(ut['port'])
            uri_base = ut['scheme'] + '://' + ut['host'] + port + "/cgi-bin/getClientToken?authPlatform={authPlatform}&authCode={authCode}"

            alt_raw = None
            if authmode in OAUTH_PROVIDERS and OAUTH_PROVIDERS[authmode] is not None and OAUTH_PROVIDERS[authmode]["platform"] is not None:
                # Call the relay server with the authcode that was returned to the client
                # This will confirm the user is actually authenticated and return the email address
                alt_res = urllib.request.urlopen(str(urlparse(uri_base.format(authPlatform=OAUTH_PROVIDERS[authmode]["platform"], authCode=authcode)).geturl()))
                alt_raw = alt_res.read().decode()

            if (alt_raw is None or "ERROR:" in alt_raw):
                ## Authentication failure.
                if alt_raw is None:
                    alt_raw = f"Unknown authmode: {authmode}"
                page = f"<HTML><HEAD><TITLE>Login Failure</TITLE></HEAD><BODY><H1>{alt_raw}</H1></BODY></HTML>"
                return(page)

            nonce = args['NONCE']
            host = args['HOST']
            uri = args['URI']
            raw = urllib.parse.unquote(uri)
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)
            if captureApp == None:
                captureApp = load_rpc_manager(appid)

            # Notify app of authentication success
            loginMethod=getattr(captureApp,OAUTH_PROVIDERS[authmode]["method"])
            loginMethod(address, alt_raw)

            redirectUrl = captureSettings.get('redirectUrl')
            if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                # Use redirect URI
                target = str(redirectUrl)
            else:
                if ((host == 'Empty') or (uri == 'Empty')):
                    # No host or URI
                    page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                    return(page)

                raw = urllib.parse.unquote(uri)
                if (nonce == 'a1b2c3d4e5f6'):
                    scheme = "https"
                else:
                    scheme = "http"
                target = f"{scheme}://{host}{raw}"
            redirectUrl = captureSettings.get('redirectUrl')
            util.redirect(req, target)

            return

    # if configured for any OAuth provider create and return the selection page
    if (captureSettings.get("authenticationType") == "ANY_OAUTH"):
        page = generate_page(req,captureSettings,args)
        return(page)

    if captureSettings.get("sessionCookiesEnabled") == True and 'Cookie' in req.headers_in:
        cookie = HandlerCookie(req)
        if cookie.get_field("username") != None:
            # Process cookie if exists.
            address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

            if captureApp == None:
                captureApp = load_rpc_manager(appid)
            if captureApp.isUserInCookieTable(address,cookie.get_field("username")):
                # User was found in expired cookie table.
                captureApp.removeUserFromCookieTable(address)
                cookie.expire()
            elif ((cookie != None) and
                (cookie.is_valid() == True) and
                (captureApp.userLogin(address,cookie.get_field("username")) == 0)):
                # Cookie checks out.  Active them, let them through.
                redirectUrl = captureSettings.get('redirectUrl')
                if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                    target = str(redirectUrl)
                else:
                    nonce = args['NONCE']
                    host = args['HOST']
                    uri = args['URI']
                    raw = urllib.parse.unquote(uri)
                    if ((host == 'Empty') or (uri == 'Empty')):
                        page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                        return(page)
                    if (nonce == 'a1b2c3d4e5f6'):
                        target = str("https://" + host + raw)
                    else:
                        target = str("http://" + host + raw)
                util.redirect(req, target)
                return

    # if not using a custom capture page we generate and return a standard page
    if (captureSettings.get('pageType') != 'CUSTOM'):
        page = generate_page(req,captureSettings,args)
        return(page)

    # if we make it here they are using a custom page so we have to
    # look to see if they are also using a custom.py script
    rawpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/"
    webpath = "/capture/custom_" + str(args['APPID']) + "/"

    # found a custom.py file so load it up, grab the index function reference
    # and call the index function to generate the capture page
    if (os.path.exists(rawpath + "custom.py")):
        cust = import_file(rawpath + "custom.py")
        if not cust:
            raise Exception("Unable to locate or import custom.py")
        func = getattr(cust,"index")
        if not func:
            raise Exception("Unable to locate index function in custom.py")
        if not hasattr(func,'__call__'):
            raise Exception("The index in custom.py is not a callable function")
        page = func(req,rawpath,webpath,str(args['APPID']),str(args['HOST']),str(args['URI']))
    # no custom.py file so we generate the capture page ourselves
    else:
        page = generate_page(req,captureSettings,args)

    # return the capture page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by authpage.html when the Login button is clicked.
# Arguments include username and password along with several hidden fields
# that store the details of the page originally requested.

def authpost(req,username,password,method,nonce,appid,host,uri):
    if type(username) == bytes:
        username = username.decode('utf-8')
    if type(password) == bytes:
        password = password.decode('utf-8')
    if type(method) == bytes:
        method = method.decode('utf-8')
    if type(nonce) == bytes:
        nonce = nonce.decode('utf-8')
    if type(host) == bytes:
        host = host.decode('utf-8')
    if type(uri) == bytes:
        uri = uri.decode('utf-8')

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # load the app settings
    captureSettings = load_capture_settings(req,appid)

    # setup the uvm and retrieve the app object so we can make the RPC call
    captureApp = load_rpc_manager(appid)

    # call the app to authenticate the user
    authResult = captureApp.userAuthenticate(address, username, urllib.parse.quote(password))

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if captureSettings.get("sessionCookiesEnabled") == True:
            # Hand the user a cookie
            cookie = HandlerCookie(req,appid)
            cookie.set(username)
        redirectUrl = captureSettings.get('redirectUrl')
        if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
            target = str(redirectUrl)
        else:
            if ((host == 'Empty') or (uri == 'Empty')):
                page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                return(page)
            raw = urllib.parse.unquote(uri)
            if (nonce == 'a1b2c3d4e5f6'):
                target = str("https://" + host + raw)
            else:
                target = str("http://" + host + raw)
        util.redirect(req, target)
        return

    # authentication failed so re-create the list of args that
    # we can pass to the login page generator
    args = {}
    args['METHOD'] = method
    args['NONCE'] = nonce
    args['APPID'] = appid
    args['HOST'] = host
    args['URI'] = uri

    # pass the request object and post arguments to the page generator
    if (authResult == 1):
        page = generate_page(req,captureSettings,args, _("Invalid username or password. Please try again.") )
    elif (authResult == 2):
        page = generate_page(req,captureSettings,args, _("You are already logged in from another location.") )
    else:
        page = generate_page(req,captureSettings,args, _("The server returned an unexpected error.") )

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by infopage.html when the Continue button is clicked.
# Static arguments are the hidden fields that store the details of the page
# originally requested.  The agree field is special.  When no agree box is
# included on the page, it is configured as a hidden field with the value
# set to 'agree'.  When the agree box is enabled, it is configured as a checkbox
# which will return 'agree' if checked.  If unchecked, it will not be included
# in the POST data.  To handle this scenario, we use a function parameter
# default of 'empty' which will cause app.userActivate to return false.

def infopost(req,method,nonce,appid,host,uri,agree=b'empty'):
    if type(method) == bytes:
        method = method.decode('utf-8')
    if type(nonce) == bytes:
        nonce = nonce.decode('utf-8')
    if type(appid) == bytes:
        appid = appid.decode('utf-8')
    if type(host) == bytes:
        host = host.decode('utf-8')
    if type(uri) == bytes:
        uri = uri.decode('utf-8')
    if type(agree) == bytes:
        agree = agree.decode('utf-8')

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # load the app settings
    captureSettings = load_capture_settings(req,appid)

    args = {}
    args['METHOD'] = method
    args['NONCE'] = nonce
    args['APPID'] = appid
    args['HOST'] = host
    args['URI'] = uri

    # setup the uvm and app objects so we can make the RPC call
    captureApp = load_rpc_manager(appid)

    if agree != "agree":
        page = generate_page(req,captureSettings,args, _("You must enable the checkbox above to continue.") )
        return page

    authentication_type = captureSettings.get("authenticationType")
    if authentication_type in list(OAUTH_PROVIDERS.keys()):
        if authentication_type == "ANY_OAUTH":
            page = generate_page(req,captureSettings,args,"",page=None,template_name="pickpage.html")
        else:
            target = generate_page(req,captureSettings,args,"",OAUTH_PROVIDERS[authentication_type]["uri"])
            req.log_error(f"handler.py: target={target}")
            util.redirect(req, target)
            return
    else:
        # No authentication, just agree

        # call the app to authenticate the user
        authResult = captureApp.userActivate(address,agree)

        # on successful login redirect to the redirectUrl if not empty
        # otherwise send them to the page originally requested
        if (authResult == 0):
            redirectUrl = captureSettings.get('redirectUrl')
            if (redirectUrl != None and len(redirectUrl) != 0 and (not redirectUrl.isspace())):
                target = str(redirectUrl)
            else:
                if ((host == 'Empty') or (uri == 'Empty')):
                    page = "<HTML><HEAD><TITLE>Login Success</TITLE></HEAD><BODY><H1>Login Success</H1></BODY></HTML>"
                    return(page)
                raw = urllib.parse.unquote(uri)
                if (nonce == 'a1b2c3d4e5f6'):
                    target = str("https://" + host + raw)
                else:
                    target = str("http://" + host + raw)
            util.redirect(req, target)
            return

    # return the page we just created
    return(page)

#-----------------------------------------------------------------------------
# This function generates the actual captive portal page

def generate_page(req,captureSettings,args,extra='',page=None,template_name=None):

    # We use the path from the request filename to locate the correct template
    # and start with the OAuth selection page if that authentication type is
    # enabled. Otherwise we use the configured page type to decide.
    if page is None:
        if template_name is None:
            if (captureSettings.get('pageType') == 'BASIC_LOGIN'):
                # name = req.filename[:req.filename.rindex('/')] + "/authpage.html"
                template_name = "authpage.html"
            elif (captureSettings.get('pageType') == 'BASIC_MESSAGE'):
                # name = req.filename[:req.filename.rindex('/')] + "/infopage.html"
                template_name = "infopage.html"
            elif (captureSettings.get('pageType') == 'CUSTOM'):
                # name = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/custom.html"
                template_name = f"custom_{args['APPID']}/custom.html"
            else:
                page = "<html><head><title>Captive Portal Error</title></head><body><h2>Invalid Captive Portal configuration</h2></body></html>"
                return(page)

        path = req.filename[:req.filename.rindex('/')]
        template_file_name = f"{path}/{template_name}"

        webfile = open(template_file_name, "r")
        page = webfile.read();
        webfile.close()

    if (not 'certificateDetection' in captureSettings):
        captureSettings['certificateDetection'] = 'DISABLE_DETECTION'

    if captureSettings['certificateDetection'] == 'CHECK_CERTIFICATE':
        page = replace_marker(page,'$.SecureEndpointCheck.$','checkSecureEndpoint(false);')
    elif captureSettings['certificateDetection'] == 'REQUIRE_CERTIFICATE':
        page = replace_marker(page,'$.SecureEndpointCheck.$','checkSecureEndpoint(true);')
    else:
        page = replace_marker(page,'$.SecureEndpointCheck.$','')

    if (captureSettings.get('pageType') == 'BASIC_LOGIN'):
        page = replace_marker(page,'$.CompanyName.$', captureSettings.get('companyName'))
        page = replace_marker(page,'$.PageTitle.$', captureSettings.get('basicLoginPageTitle'))
        page = replace_marker(page,'$.WelcomeText.$', captureSettings.get('basicLoginPageWelcome'))
        page = replace_marker(page,'$.MessageText.$', captureSettings.get('basicLoginMessageText'))
        page = replace_marker(page,'$.UserLabel.$', captureSettings.get('basicLoginUsername'))
        page = replace_marker(page,'$.PassLabel.$', captureSettings.get('basicLoginPassword'))
        page = replace_marker(page,'$.FooterText.$', captureSettings.get('basicLoginFooter'))

    if (captureSettings.get('pageType') == 'BASIC_MESSAGE'):
        page = replace_marker(page,'$.CompanyName.$', captureSettings.get('companyName'))
        page = replace_marker(page,'$.PageTitle.$', captureSettings.get('basicMessagePageTitle'))
        page = replace_marker(page,'$.WelcomeText.$', captureSettings.get('basicMessagePageWelcome'))
        page = replace_marker(page,'$.MessageText.$', captureSettings.get('basicMessageMessageText'))
        page = replace_marker(page,'$.FooterText.$', captureSettings.get('basicMessageFooter'))

        if (captureSettings.get('basicMessageAgreeBox') == True):
            page = replace_marker(page,'$.AgreeText.$', captureSettings.get('basicMessageAgreeText'))
            page = replace_marker(page,'$.AgreeBox.$','checkbox')
        else:
            page = replace_marker(page,'$.AgreeText.$', '')
            page = replace_marker(page,'$.AgreeBox.$','hidden')

    if (captureSettings.get('pageType') == 'CUSTOM'):
        path = "/capture/custom_" + str(args['APPID'])
        page = replace_marker(page,'$.CustomPath.$',path)

    if captureSettings.get("authenticationType") in list(OAUTH_PROVIDERS.keys()):
        uvmContext = Uvm().getUvmContext()
        networkSettings = uvmContext.networkManager().getNetworkSettings()

        port = None

        if (captureSettings.get("alwaysUseSecureCapture" == True)):
            schema = "https://"
            if (networkSettings.get('httpsPort') != 443):
                port = networkSettings.get('httpsPort')
        else:
            schema = "http://"
            if (networkSettings.get('httpPort') != 80):
                port = networkSettings.get('httpPort')

        if port is not None:
            port = f":{port}"
        else:
            port = ""

        target = f"{schema}{req.hostname}{port}/capture/handler.py/index?nonce={args['NONCE']}&method={args['METHOD']}&appid={args['APPID']}&host={args['HOST']}&uri={args['URI']}"

        page = replace_marker(page,'$.GoogleState.$', urllib.parse.quote(target + "&authmode=GOOGLE"))
        page = replace_marker(page,'$.FacebookState.$', urllib.parse.quote(target + "&authmode=FACEBOOK"))
        page = replace_marker(page,'$.MicrosoftState.$', urllib.parse.quote(target + "&authmode=MICROSOFT"))

        page = replace_marker(page,'$.AuthRelayUri.$', uvmContext.uriManager().getUri("https://auth-relay.untangle.com/callback.php"))

    # plug the values into the hidden form fields of the authentication page
    # page by doing  search and replace for each of the placeholder text tags
    page = replace_marker(page,'$.method.$', args['METHOD'])
    page = replace_marker(page,'$.nonce.$', args['NONCE'])
    page = replace_marker(page,'$.appid.$', args['APPID'])
    page = replace_marker(page,'$.host.$', args['HOST'])
    page = replace_marker(page,'$.uri.$', args['URI'])

    # replace the text in the problem section with the agumented value
    page = replace_marker(page,'$.ProblemText.$',extra)

    # debug = create_debug(args,captureSettings)
    debug = ""
    page = replace_marker(page,'<!--DEBUG-->',debug)

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Pulls page arguments out of a URI and stores them in a list.

def split_args(args):

    canon_args = {}                     # Start an empty list
    if args == None:                    # Return the empty list if no args
        return(canon_args)

    arglist = args.split('&')           # Split into list of name=value strings

    for arg in arglist:                 # Now split each name=value and
        tmp = arg.split('=')            # turn them into sub-lists
        if len(tmp) == 1:               # with name in the first part
            canon_args[tmp[0]] = None   # and value in the second part
        else:
            canon_args[tmp[0].upper()] = tmp[1]
    return(canon_args)

#-----------------------------------------------------------------------------
# loads and returns the app RPC object needed for the authentication calls

def load_rpc_manager(appid=None):

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    # if no appid provided we lookup capture app by name
    # otherwise we use the appid passed to us
    if (appid == None):
        captureApp = uvmContext.appManager().app("captive-portal")
    else:
        captureApp = uvmContext.appManager().app(int(appid))

    # if we can't find the app then throw an exception
    if (captureApp == None):
        raise Exception("The uvm app manager could not locate captive-portal")

    return(captureApp)

#-----------------------------------------------------------------------------
# loads the app settings

def load_capture_settings(req,appid=None):

    captureSettings = None

    # start with our company name
    companyName = 'Arista'

    # if there is an OEM name configured we use that instead of our company name
    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if (oemName != None):
        companyName = oemName

    # if there is a company name in the branding manager it wins over everything else
    brandco = get_app_settings_item('branding-manager','companyName')
    if (brandco != None):
        companyName = brandco

    try:
        if (appid == None):
            captureSettings = get_app_settings('captive-portal')
        else:
            captureSettings = get_appid_settings(int(appid))
    except Exception as e:
        req.log_error("handler.py: Exception loading settings: %s" % str(e))

    if (captureSettings == None):
        req.log_error("handler.py: Unable to load capture settings for appid: %s" % str(appid))
        return None
    if (captureSettings.get('pageType') == None):
        req.log_error("handler.py: Missing required setting: pageType")
        return None

    # add the company name to the app settings dictionary
    captureSettings['companyName'] = companyName

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Mon, 10 Jan 2000 00:00:00 GMT")
    req.headers_out.add("Connection", "close")

    return(captureSettings)

#-----------------------------------------------------------------------------
# builds a string of debug info

def create_debug(args,captureSettings):

    debug = "<BR><HR><BR>";
    debug += "<BR>===== ARGUMENTS =====<BR>\r\n"
    debug += pprint.pformat(args)
    debug += "<BR>===== CAPTURE SETTINGS =====<BR>\r\n"
    debug += pprint.pformat(captureSettings)
    return(debug)

#-----------------------------------------------------------------------------
# generates a simply reply object that the extjs script uses to determine
# the status of a custom upload form post request

def extjs_reply(status,message,filename=""):

    if (status == True):
        result = "{success:true,msg:\"%s\",filename:\"%s\"}" % (message,filename)
    else:
        result = "{success:false,msg:\"%s\",filename:\"%s\"}" % (message,filename)

    return(result)

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def replace_marker(page,marker,output):

    if type(marker) == bytes:
        marker = marker.decode("utf-8")
    if type(output) == bytes:
        output = output.decode("utf-8")

    page = page.replace(marker,output)

    return(page)

#-----------------------------------------------------------------------------
# handler for custom.py integration which dynamically loads the custom.py
# script and calls the handler function passing the apache request object,
# full path to the custom files, and the appid.

def custom_handler(req):

    # first we need to extract the args so we can find the appid
    args = split_args(req.args);

    # make sure we have a valid appid
    if (not 'APPID' in args):
        raise Exception("The appid argument was not passed to custom_hander")

    # construct the absolute and relative paths to the custom files
    rawpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/"
    webpath = "/capture/custom_" + str(args['APPID']) + "/"

    # import the custom.py
    cust = import_file(rawpath + "custom")
    if not cust:
        raise Exception("Unable to locate or import custom.py")

    # get a reference to the handler function
    func = getattr(cust,"handler")
    if not func:
        raise Exception("Unable to locate handler function in custom.py")
    if not hasattr(func,'__call__'):
        raise Exception("The handler in custom.py is not a callable function")

    # call the handler and return anything we get back
    page = func(req,rawpath,webpath,str(args['APPID']))
    return(page)

#-----------------------------------------------------------------------------
def import_file(filename):
    (path, name) = os.path.split(filename)
    (name, ext) = os.path.splitext(name)
    (file, filename, data) = imp.find_module(name, [path])
    return imp.load_module(name, file, filename, data)
