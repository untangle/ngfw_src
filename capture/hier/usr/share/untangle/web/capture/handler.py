from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_nodeid_settings
from uvm.settings_reader import get_node_settings
from uvm.settings_reader import get_settings_item

from mod_python import apache
from mod_python import util
import os.path
import zipfile
import pprint

from uvm import Uvm

# global objects that we retrieve from the uvm
uvmContext = None
captureNode = None
captureSettings = None
companyName = None

#-----------------------------------------------------------------------------
# This is the default function that gets called when a client is redirected
# to the captive portal because they have not yet authenticated.

def index(req):

    # get the original destination and other arguments passed
    # in the URL when the redirect was generated
    args = split_args(req.args);
    if (not 'METHOD' in args):  args['METHOD'] = "Empty"
    if (not 'NONCE' in args):   args['NONCE'] = "Empty"
    if (not 'APPID' in args):   args['APPID'] = "Empty"
    if (not 'HOST' in args):    args['HOST'] = "Empty"
    if (not 'URI' in args):     args['URI'] = "Empty"

    # setup the global data
    global_data_setup(req,args['APPID'])

    # pass the reqest object and arguments to the page generator
    page = generate_page(req,args)

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# Called as a POST method by authpage.html when the Login button is clicked.
# Arguments include username and password along with several hidden fields
# that store the details of the page originally requested.

def authpost(req,username,password,method,nonce,appid,host,uri):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # setup the global data
    global_data_setup(req,appid)

    # setup the uvm and node objects so we can make the RPC call
    global_auth_setup(appid)

    # call the node to authenticate the user
    authResult = captureNode.userAuthenticate(address, username, password)

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if (len(captureSettings['redirectUrl']) != 0):
            target = str(captureSettings['redirectUrl'])
        else:
            target = str("http://" + host + uri)

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
        page = generate_page(req,args,"Invalid username or password. Please try again.")
    elif (authResult == 2):
        page = generate_page(req,args,"You are already logged in from another location.")
    else:
        page = generate_page(req,args,"The server returned an unexpected error.")

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
# default of 'empty' which will cause node.userActivate to return false.

def infopost(req,method,nonce,appid,host,uri,agree='empty'):

    # get the network address of the client
    address = req.get_remote_host(apache.REMOTE_NOLOOKUP,None)

    # setup the global data
    global_data_setup(req,appid)

    # setup the uvm and node objects so we can make the RPC call
    global_auth_setup(appid)

    # call the node to authenticate the user
    authResult = captureNode.userActivate(address,agree)

    # on successful login redirect to the redirectUrl if not empty
    # otherwise send them to the page originally requested
    if (authResult == 0):
        if (len(captureSettings['redirectUrl']) != 0):
            target = str(captureSettings['redirectUrl'])
        else:
            target = str("http://" + host + uri)
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
        page = generate_page(req,args,"You must enable the checkbox above to continue.")
    else:
        page = generate_page(req,args,"The server returned an unexpected error.")

    # return the login page we just created
    return(page)

#-----------------------------------------------------------------------------
# This function handles the custom page upload

def custom_upload(req,upload_file,appid):

    # first set the content type for the response
    req.content_type = "text/html"

    # use the path from the request filename to setup the custom path
    custpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(appid) + "/"

    # temporary location to save the uploaded file
    tempfile = "/tmp/custom.upload"

    # first make sure we got a valid form and filename
    if ((not upload_file) or (not upload_file.filename)):
        return extjs_reply(False,'Invalid or missing filename in upload request')

    # save the zip file that was uploaded
    file = open(tempfile,"w")
    file.write(upload_file.file.read())
    file.close()

    # make sure it's really a zip file
    if (not zipfile.is_zipfile(tempfile)):
        return extjs_reply(False,"%s is not a valid ZIP file" % upload_file.filename)

    # open the file and look for the custom.html page
    zfile = zipfile.ZipFile("/tmp/custom.upload","r")
    zlist = zfile.namelist()
    if (not 'custom.html' in zlist):
        return extjs_reply(False,'The uploaded ZIP file does not contain custom.html')

    # setup the message we return to the caller
    detail = "Extracted the following files from " + upload_file.filename + "&LT;HR&GT;"

    # extract all of the files into the custom directory and append the
    # name of each one to the result message we'll be sending back
    try:
        for item in zlist:
            (dirname,filename) = os.path.split(item)
            fd = open(custpath + filename,"w")
            fd.write(zfile.read(item))
            fd.close()
            detail += " " + filename
    except:
        return extjs_reply(False,custpath + filename)
        return extjs_reply(False,'Unknown error extracting ZIP file contents')

    # return the status
    return extjs_reply(True,detail,upload_file.filename)

#-----------------------------------------------------------------------------
# This function handles the custom page cleanup

def custom_remove(req,custom_file,appid):

    # first set the content type for the response
    req.content_type = "text/html"

    # use the path from the request filename to setup the custom path
    custpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(appid) + "/"

    try:
        # get the list of files in the custom directory
        filelist = os.listdir(custpath)
        counter = 0

        # get rid of everything
        for filename in filelist:
            os.remove(custpath + filename)
            counter += 1
    except:
        return extjs_reply(False,"Unknown error removing custom files")

    detail = "Removed %d custom files" % counter
    return extjs_reply(True,detail)

#-----------------------------------------------------------------------------
# This function generates the actual captive portal page

def generate_page(req,args,extra=''):

    # use the path from the request filename to locate the correct template
    if (captureSettings['pageType'] == 'BASIC_LOGIN'):
        name = req.filename[:req.filename.rindex('/')] + "/authpage.html"

    if (captureSettings['pageType'] == 'BASIC_MESSAGE'):
        name = req.filename[:req.filename.rindex('/')] + "/infopage.html"

    if (captureSettings['pageType'] == 'CUSTOM'):
        name = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/custom.html"

    file = open(name, "r")
    page = file.read();
    file.close()

    if (captureSettings['pageType'] == 'BASIC_LOGIN'):
        page = page.replace('$.CompanyName.$', companyName)
        page = page.replace('$.PageTitle.$', captureSettings['basicLoginPageTitle'])
        page = page.replace('$.WelcomeText.$', captureSettings['basicLoginPageWelcome'])
        page = page.replace('$.MessageText.$', captureSettings['basicLoginMessageText'])
        page = page.replace('$.UserLabel.$', captureSettings['basicLoginUsername'])
        page = page.replace('$.PassLabel.$', captureSettings['basicLoginPassword'])
        page = page.replace('$.FooterText.$', captureSettings['basicLoginFooter'])

    if (captureSettings['pageType'] == 'BASIC_MESSAGE'):
        page = page.replace('$.CompanyName.$', companyName)
        page = page.replace('$.PageTitle.$', captureSettings['basicMessagePageTitle'])
        page = page.replace('$.WelcomeText.$', captureSettings['basicMessagePageWelcome'])
        page = page.replace('$.MessageText.$', captureSettings['basicMessageMessageText'])
        page = page.replace('$.FooterText.$', captureSettings['basicMessageFooter'])

        if (captureSettings['basicMessageAgreeBox'] == True):
            page = page.replace('$.AgreeText.$', captureSettings['basicMessageAgreeText'])
            page = page.replace('$.AgreeBox.$','checkbox')
        else:
            page = page.replace('$.AgreeText.$', '')
            page = page.replace('$.AgreeBox.$','hidden')

    if (captureSettings['pageType'] == 'CUSTOM'):
        path = "/capture/custom_" + str(args['APPID'])
        page = page.replace('$.CustomPath.$',path)

    # plug the values into the hidden form fields of the authentication page
    # page by doing  search and replace for each of the placeholder text tags
    page = page.replace('$.method.$', args['METHOD'])
    page = page.replace('$.nonce.$', args['NONCE'])
    page = page.replace('$.appid.$', args['APPID'])
    page = page.replace('$.host.$', args['HOST'])
    page = page.replace('$.uri.$', args['URI'])

    # replace the text in the problem section with the agumented value
    page = page.replace('$.ProblemText.$',extra)

#    debug = create_debug(args)
    debug = ""
    page = page.replace('<!--DEBUG-->',debug)

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
# loads the uvm and capture node objects for the authentication calls

def global_auth_setup(appid=None):

    global uvmContext
    global captureNode

    # first we get the uvm context
    uvmContext = Uvm().getUvmContext()

    # if no appid provided we lookup capture node by name
    # otherwise we use the appid passed to us
    if (appid == None):
        captureNode = uvmContext.nodeManager().node("untangle-node-capture")
    else:
        captureNode = uvmContext.nodeManager().node(long(appid))

    # if we can't find the node then throw an exception
    if (captureNode == None):
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

    if not type(companyName) is str:
        companyName = companyName.encode("utf-8")

    if (appid == None):
        captureSettings = get_node_settings('untangle-node-capture')
    else:
        captureSettings = get_nodeid_settings(long(appid))

    # add some headers to prevent caching any of our stuff
    req.headers_out.add("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0")
    req.headers_out.add("Pragma", "no-cache")
    req.headers_out.add("Expires", "Sat, 1 Jan 2000 00:00:00 GMT");

#-----------------------------------------------------------------------------

# builds a string of debug info which includes the global capture data

def create_debug(args):

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
