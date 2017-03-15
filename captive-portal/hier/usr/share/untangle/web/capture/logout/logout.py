from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_nodeid_settings
from uvm.settings_reader import get_node_settings
from uvm.settings_reader import get_settings_item
from mod_python import apache
from uvm import Uvm
from mod_python import Cookie
import pprint
import uvm.i18n_helper

_ = uvm.i18n_helper.get_translation('untangle').lgettext

#-----------------------------------------------------------------------------
# This is the default function that gets called for a client logout request

def index(req):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # use the path from the request filename to locate the correct template
    name = req.filename[:req.filename.rindex('/')] + "/exitpage.html"
    file = open(name, "r")
    page = file.read();
    file.close()

    # load the node settings
    captureSettings = load_capture_settings(req)

    # setup the uvm and node objects so we can make the RPC call
    captureList = load_rpc_manager_list()

    # track the number of successful calls to userLogout
    exitCount = 0

    # call the logout function for each node instance
    cookie_key = "__ngfwcp"
    for node in captureList:
        exitResult = node.userLogout(address)
        cookies = Cookie.get_cookie(req, cookie_key)
        if cookies != None:
            value = {}
            cookie = Cookie.MarshalCookie(cookie_key, value, secret=str(captureSettings["secretKey"]))
            cookie.path = "/"
            cookie.expires = 0
            Cookie.add_cookie(req, cookie)

        if (exitResult == 0):
            exitCount = exitCount + 1

    if (exitCount == 0):
        page = replace_marker(page,'$.ExitMessage.$', _('You were already logged out') )
        page = replace_marker(page,'$.ExitStyle.$', 'styleProblem')
    else:
        page = replace_marker(page,'$.ExitMessage.$', _('You have successfully logged out') )
        page = replace_marker(page,'$.ExitStyle.$', 'styleNormal')

    page = replace_marker(page,'$.CompanyName.$', captureSettings['companyName'])
    page = replace_marker(page,'$.PageTitle.$', captureSettings['basicLoginPageTitle'])

    # return the logout page we just created
    return(page)

#-----------------------------------------------------------------------------
# loads and returns the node RPC objects needed for the authentication calls

def load_rpc_manager_list(appid=None):

    # create a list for all of the nodes we discover
    captureList = list()

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    if (appid == None):
        nodelist = uvmContext.nodeManager().nodeInstancesIds()
        for item in nodelist['list']:
            node = uvmContext.nodeManager().node(long(item))
            name = node.getNodeSettings()['nodeName']
            if (name == 'captive-portal'):
                captureList.append(node)
    # appid was passed so use it
    else:
        node = uvmContext.nodeManager().node(long(appid))
        captureList.append(node)

    # if we can't find the node then throw an exception
    if (len(captureList) == 0):
        raise Exception("The uvm node manager could not locate captive-portal")

    return(captureList)

#-----------------------------------------------------------------------------
# loads the node settings

def load_capture_settings(req,appid=None):

    companyName = 'Untangle'

    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if (oemName != None):
        companyName = oemName

    brandco = get_node_settings_item('branding-manager','companyName')
    if (brandco != None):
        companyName = brandco

    if (appid == None):
        captureSettings = get_node_settings('captive-portal')
    else:
        captureSettings = get_nodeid_settings(long(appid))

    # add the company name to the node settings dictionary
    captureSettings['companyName'] = companyName

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Sat, 1 Jan 2000 00:00:00 GMT");

    return(captureSettings)

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def replace_marker(page,marker,output):
    if not type(output) is str:
        output = output.encode("utf-8")

    page = page.replace(marker,output)

    return(page)
