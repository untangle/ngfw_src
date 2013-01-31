from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_nodeid_settings
from uvm.settings_reader import get_node_settings
from uvm.settings_reader import get_settings_item
from mod_python import apache
from mod_python import util
from uvm import Uvm
import os.path
import zipfile
import pprint

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

    # if not using a custom capture page we generate and return a standard page
    if (captureSettings['pageType'] != 'CUSTOM'):
        page = generate_page(req,args)
        return(page)

    # if we make it here they are using a custom page so we have to
    # look to see if they are also using a custom.py script
    rawpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(args['APPID']) + "/"
    webpath = "/capture/custom_" + str(args['APPID']) + "/"

    # found a custom.py file so load it up, grab the index function reference
    # and call the index function to generate the capture page
    if (os.path.exists(rawpath + "custom.py")):
        cust = __import__(rawpath + "custom")
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
        page = generate_page(req,args)

    # return the capture page we just created
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
        if (len(captureSettings['redirectUrl']) != 0) and (captureSettings['redirectUrl'].isspace() == False):
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
        if (len(captureSettings['redirectUrl']) != 0) and (captureSettings['redirectUrl'].isspace() == False):
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

    # first we call the custom_remove function to make sure the
    # target directory is empty.
    custom_remove(req,upload_file,appid)

    # now set the content type for the response
    req.content_type = "text/html"

    # use the path from the request filename to setup the custom path
    custpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(appid)

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

    # open the file and look for the custom.html page or custom.py script
    zfile = zipfile.ZipFile("/tmp/custom.upload","r")
    zlist = zfile.namelist()
    checker = 0
    if ('custom.html' in zlist): checker += 1
    if ('custom.py' in zlist): checker += 1
    if (checker == 0):
        return extjs_reply(False,'The uploaded ZIP file does not contain custom.html or custom.py')

    # setup the message we return to the caller
    detail = "Extracted the following files and directories from " + upload_file.filename + "&LT;HR&GT;"

    # extract all of the files into the custom directory and append the
    # name of each one to the result message we sending back
    try:
        for item in zlist:
            (filepath,filename) = os.path.split(item)
            if (filename == ''):
                if not os.path.exists(custpath + '/' + filepath):
                    os.mkdir(custpath + '/' + filepath)
            else:
                if (filepath == ''):
                    fd = open(custpath + '/' + filename,"w")
                else:
                    fd = open(custpath + '/' + filepath + '/' + filename,"w")
                fd.write(zfile.read(item))
                fd.close()

            if (filepath == ''):
                detail += " " + filename
            else:
                detail += ' ' + filepath + '/' + filename
    except:
        return extjs_reply(False,custpath + '/' + filepath + '/' + filename)

    # return the status
    return extjs_reply(True,detail,upload_file.filename)

#-----------------------------------------------------------------------------
# This function handles the custom page cleanup

def custom_remove(req,custom_file,appid):
    # first set the content type for the response.  note that the custom_file
    # argument is not used.  It's just a placeholder for the form field where
    # the current custom upload file is displayed on the extjs page and thus
    # is passed to our handler when the form is submitted.
    req.content_type = "text/html"

    # use the path from the request filename to setup the custom path
    custpath = req.filename[:req.filename.rindex('/')] + "/custom_" + str(appid)
    dcount = fcount = 0

    # remove all the files and all the dirs in the custom path
    try:
        for cust,dlist,flist in os.walk(custpath,topdown=False):
            for item in flist:
                os.remove(cust + '/' + item)
                fcount += 1
            for item in dlist:
                os.rmdir(cust + '/' + item)
                dcount += 1
    except:
        return extjs_reply(False,"Unknown error removing custom files")

    detail = "Removed %d files and %d directories" % (fcount,dcount)
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
        page = replace_marker(page,'$.CompanyName.$', companyName)
        page = replace_marker(page,'$.PageTitle.$', captureSettings['basicLoginPageTitle'])
        page = replace_marker(page,'$.WelcomeText.$', captureSettings['basicLoginPageWelcome'])
        page = replace_marker(page,'$.MessageText.$', captureSettings['basicLoginMessageText'])
        page = replace_marker(page,'$.UserLabel.$', captureSettings['basicLoginUsername'])
        page = replace_marker(page,'$.PassLabel.$', captureSettings['basicLoginPassword'])
        page = replace_marker(page,'$.FooterText.$', captureSettings['basicLoginFooter'])

    if (captureSettings['pageType'] == 'BASIC_MESSAGE'):
        page = replace_marker(page,'$.CompanyName.$', companyName)
        page = replace_marker(page,'$.PageTitle.$', captureSettings['basicMessagePageTitle'])
        page = replace_marker(page,'$.WelcomeText.$', captureSettings['basicMessagePageWelcome'])
        page = replace_marker(page,'$.MessageText.$', captureSettings['basicMessageMessageText'])
        page = replace_marker(page,'$.FooterText.$', captureSettings['basicMessageFooter'])

        if (captureSettings['basicMessageAgreeBox'] == True):
            page = replace_marker(page,'$.AgreeText.$', captureSettings['basicMessageAgreeText'])
            page = replace_marker(page,'$.AgreeBox.$','checkbox')
        else:
            page = replace_marker(page,'$.AgreeText.$', '')
            page = replace_marker(page,'$.AgreeBox.$','hidden')

    if (captureSettings['pageType'] == 'CUSTOM'):
        path = "/capture/custom_" + str(args['APPID'])
        page = replace_marker(page,'$.CustomPath.$',path)

    # plug the values into the hidden form fields of the authentication page
    # page by doing  search and replace for each of the placeholder text tags
    page = replace_marker(page,'$.method.$', args['METHOD'])
    page = replace_marker(page,'$.nonce.$', args['NONCE'])
    page = replace_marker(page,'$.appid.$', args['APPID'])
    page = replace_marker(page,'$.host.$', args['HOST'])
    page = replace_marker(page,'$.uri.$', args['URI'])

    # replace the text in the problem section with the agumented value
    page = replace_marker(page,'$.ProblemText.$',extra)

#    debug = create_debug(args)
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

#-----------------------------------------------------------------------------
# forces stuff loaded from settings files to be UTF-8 when plugged
# into the page template files

def replace_marker(page,marker,output):

    if not type(output) is str:
        output = output.encode("utf-8")

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
    cust = __import__(rawpath + "custom")
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
