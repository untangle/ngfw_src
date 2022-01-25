import simplejson as json

#-----------------------------------------------------------------------------

#@deprecated
def get_node_settings(appname):
    return get_app_settings(appname)

def get_app_settings(appname):

    listfile = "@PREFIX@/usr/share/untangle/settings/untangle-vm/apps.js"

    # find the settings for the argumented app name
    try:

        # read the app manager settings
        file = open(listfile, "r")
        data = file.read()
        file.close()

        # get the list of active apps
        appinfo = json.loads(data)
        applist = appinfo['apps']['list']

        appid = None

        # look for the target app name and grab the app id
        for app in applist:
            if (app['appName'] != appname): continue
            appid = app['id']
            break

        # not found so return empty string
        if (appid == None): return(None)

        # generate the settings file name using the app id we found above
        appfile = "@PREFIX@/usr/share/untangle/settings/" + appname + "/settings_" + str(appid) + ".js"
        file = open(appfile, "r")
        data = file.read()
        file.close()

        settings = json.loads(data)

    # for all exceptions we just return empty
    except:
        return(None)

    # return the settings
    return(settings)

#-----------------------------------------------------------------------------

#@deprecated
def get_nodeid_settings(appid):
    return get_appid_settings(appid)

def get_appid_settings(appid):

    listfile = "@PREFIX@/usr/share/untangle/settings/untangle-vm/apps.js"

    # find the settings for the argumented app id
    try:

        # read the app manager settings
        file = open(listfile, "r")
        data = file.read()
        file.close()

        # get the list of active apps
        appinfo = json.loads(data)
        applist = appinfo['apps']['list']

        appname = None

        # look for the target app id and grab the app name
        for app in applist:
            if (app['id'] != appid): continue
            appname = app['appName']
            break

        # not found so return empty string
        if (appname == None): return(None)

        # generate the settings file name using the app id we found above
        appfile = "@PREFIX@/usr/share/untangle/settings/" + appname + "/settings_" + str(appid) + ".js"
        file = open(appfile, "r")
        data = file.read()
        file.close()

        settings = json.loads(data)

    # for all exceptions we just return empty
    except:
        return(None)

    # return the settings
    return(settings)

#-----------------------------------------------------------------------------

#@deprecated
def get_node_settings_item(appname,itemname):
    return get_app_settings_item(appname,itemname)

def get_app_settings_item(appname,itemname):
    return get_settings_item_json(get_app_settings(appname), itemname)

#-----------------------------------------------------------------------------

#@deprecated
def get_nodeid_settings_item(appid,itemname):
    return get_appid_settings_item(appid,itemname)

def get_appid_settings_item(appid,itemname):
    return get_settings_item_json(get_appid_settings(appid), itemname)

#-----------------------------------------------------------------------------

def get_uvm_settings(basename):
    basefile = "@PREFIX@/usr/share/untangle/settings/untangle-vm/" + basename + ".js"
    return get_settings_jsonobj(basefile)

#-----------------------------------------------------------------------------

def get_uvm_settings_item(basename,itemname):
    return get_settings_item_json(get_uvm_settings(basename), itemname)

#-----------------------------------------------------------------------------

def get_settings_item(file,itemname):
    return get_settings_item_json(get_settings_jsonobj(file), itemname)

#-----------------------------------------------------------------------------

def get_settings_item_json(jsonObj,itemname):

    if (jsonObj == None):
        return(None)

    if (itemname not in jsonObj):
        return(None)

    value = jsonObj[itemname]
    return(value)

#-----------------------------------------------------------------------------

def get_settings_jsonobj(filename):
    try:
        # read the settings
        file = open(filename, "r")
        data = file.read()
        file.close()

        # parse the settings
        baseinfo = json.loads(data)

    # for all exceptions we just return empty
    except:
        return(None)

    # return the settings
    return(baseinfo)

