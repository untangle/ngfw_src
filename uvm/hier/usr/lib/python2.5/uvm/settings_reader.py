import simplejson as json

#-----------------------------------------------------------------------------

def get_node_settings(nodename):

    listfile = "@PREFIX@/usr/share/untangle/settings/untangle-vm/node_manager.js"

    # find the settings for the argumented node name
    try:

        # read the node manager settings
        file = open(listfile, "r")
        data = file.read()
        file.close()

        # get the list of active nodes
        nodeinfo = json.loads(data)
        nodelist = nodeinfo['nodes']['list']

        nodeid = None

        # look for the target node name and grab the node id
        for node in nodelist:
            if (node['nodeName'] != nodename): continue
            nodeid = node['id']
            break

        # not found so return empty string
        if (nodeid == None): return(None)

        # generate the settings file name using the node id we found above
        nodefile = "@PREFIX@/usr/share/untangle/settings/" + nodename + "/settings_" + str(nodeid) + ".js"
        file = open(nodefile, "r")
        data = file.read()
        file.close()

        settings = json.loads(data)

    # for all exceptions we just return empty
    except:
        return(None)

    # return the settings
    return(settings)

#-----------------------------------------------------------------------------

def get_node_settings_item(nodename,itemname):
    return get_settings_item(get_node_settings(nodename), itemname)

#-----------------------------------------------------------------------------

def get_uvm_settings(basename):
    basefile = "@PREFIX@/usr/share/untangle/settings/untangle-vm/" + basename + ".js"
    return get_settings(basefile)

#-----------------------------------------------------------------------------

def get_uvm_settings_item(basename,itemname):
    return get_settings_item(get_uvm_settings(basename), itemname)

#-----------------------------------------------------------------------------

def get_settings(filename):
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

#-----------------------------------------------------------------------------

def get_settings_item(jsonObj,itemname):

    if (jsonObj == None):
        return(None)

    if (not jsonObj.has_key(itemname)):
        return(None)

    value = jsonObj[itemname]
    return(value)
