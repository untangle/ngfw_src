from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_nodeid_settings
from uvm.settings_reader import get_node_settings
from uvm.settings_reader import get_settings_item
from mod_python import apache
from uvm import Uvm
import pprint


# global objects that we retrieve from the uvm
uvmContext = None
captureList = None
captureSettings = None
companyName = None

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

    # setup the global data
    global_data_setup(req)

    # setup the uvm and node objects so we can make the RPC call
    global_auth_setup()

    # track the number of successful calls to userLogout
    exitCount = 0

    # call the logout function for each node instance
    for node in captureList:
        exitResult = node.userLogout(address)
        if (exitResult == 0):
            exitCount = exitCount + 1

    if (exitCount == 0):
        page = replace_marker(page,'$.ExitMessage.$', 'You were already logged out')
        page = replace_marker(page,'$.ExitStyle.$', 'styleProblem')
    else:
        page = replace_marker(page,'$.ExitMessage.$', 'You have successfully logged out')
        page = replace_marker(page,'$.ExitStyle.$', 'styleNormal')

    page = replace_marker(page,'$.CompanyName.$', companyName)
    page = replace_marker(page,'$.PageTitle.$', captureSettings['basicLoginPageTitle'])

    # return the logout page we just created
    return(page)

#-----------------------------------------------------------------------------
# loads the uvm and capture node objects for the authentication calls

def global_auth_setup(appid=None):

    global uvmContext
    global captureList

    # create a list for all of the nodes we discover
    captureList = list()

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    # if no appid provided we lookup capture nodes by name
    if (appid == None):
        nodelist = uvmContext.nodeManager().nodeInstancesIds()
        for item in nodelist['list']:
            node = uvmContext.nodeManager().node(long(item))
            name = node.getNodeSettings()['nodeName']
            if (name == 'untangle-node-capture'):
                captureList.append(node)
    # appid was passed so use it
    else:
        node = uvmContext.nodeManager().node(long(appid))
        captureList.append(node)

    # if we can't find the node then throw an exception
    if (len(captureList) == 0):
        raise Exception("The uvm node manager could not locate untangle-node-capture")

#-----------------------------------------------------------------------------
# loads the node settings and company name info into global variables

def global_data_setup(req,appid=None):

    global captureSettings
    global companyName

    companyName = 'Untangle'

    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if (oemName != None):
        companyName = oemName

    brandco = get_node_settings_item('untangle-node-branding','companyName')
    if (brandco != None):
        companyName = brandco

    if (appid == None):
        captureSettings = get_node_settings('untangle-node-capture')
    else:
        captureSettings = get_nodeid_settings(long(appid))

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Sat, 1 Jan 2000 00:00:00 GMT");

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def replace_marker(page,marker,output):
    if not type(output) is str:
        output = output.encode("utf-8")

    page = page.replace(marker,output)

    return(page)
