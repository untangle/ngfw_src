from uvm.manager import Manager

import getopt
import sys
import random
import getpass

class AdminManager(Manager):
    __registrationKeys = [ "companyName", "firstName", "lastName", "emailAddr", "numSeats",  "address1", "address2", "city", "state", "zipcode", "phone", ]
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__adminManager = self.__remoteContext.adminManager()

    def api_getreginfo(self):
        regInfo = self.__adminManager.getRegistrationInfo()
        if ( regInfo == None ): print "No registration info found!"
        else : print self.__printRegistrationInfo( regInfo )

    def api_shutdown(self):
        self.__remoteContext.shutdown()

    def api_passwd(self, *passwd_args):
        add_user = False
        del_user = False
        
        (optlist, args) = getopt.getopt(passwd_args, 'ad')
        for opt in optlist:
            if ( opt[0] == "-a" ): add_user = True
            if ( opt[0] == "-d" ): del_user = True

        if (( del_user and add_user ) or ( len( args ) < 1 )):
            print "Usage: "
            print "\tucli passwd [-a | -d ] login [ password ]"
            sys.exit(-1)

        login = args[0]
        password = None
        if ( len( args ) > 1 ): password = args[1]
        admin_settings = self.__adminManager.getAdminSettings()
        users = admin_settings["users"]

        user_key = None
        user = None

        ## The hash strings in the user set contain non-printable
        ## strings because of the password hash is embedded in there,
        ## this creates a new set that has those values replaces with the
        ## login name.
        clean_user_set = {}
        for u_key, u in users["set"].items():
            ## Remove the passwords they are not serializable.
            u["password"] = None

            ## Put the user in the new set.
            new_key = u["login"] + "::" + str( random.random())
            clean_user_set[new_key] = u

            if ( login == u["login"] ):
                if ( add_user ):
                    print "Error: User '" + login +"' already exists. Aborting"
                    sys.exit(-1)
                    
                user_key = new_key
                user = u

        users["set"] = clean_user_set
        
        if (( not add_user ) and user == None ):
            print( "Error: User not found. Aborting" )
            sys.exit(-1)

        if ( del_user ):
            del( clean_user_set[user_key])
            message = "Removed user with login: " + login
            
        else:
            if ( password == None ): password = getpass.getpass( "Password: " )
                
            if ( add_user ):
                clean_user_set["new_user_string::" + login] = {
                    'id' : 0,
                    'login' : login,
                    'clearPassword' : password,
                    'name' : "[no description]",
                    'readOnly' : False,
                    'email' : '[no email]',
                    'javaClass' : "com.untangle.uvm.security.User"
                }
                message = "Created new user with login: " + login
            else:
                user["clearPassword"] = password
                message = "Set password of user with login: " + login

        self.__adminManager.setAdminSettings( admin_settings )
        print message

    def __printRegistrationInfo( self, regInfo ):
        isFirst = True
        print "RegistrationInfo ["
        for field in ToolboxManager.__registrationKeys:
            if ( regInfo.hasKey( field )):
                if ( not isFirst ): print ",",
                isFirst = False
                print " %s = %s" % ( field, regInfo["field"] ),

        misc = regInfo["misc"]["map"]
        for field in misc.keys():
            if ( not isFirst ): print ",",
            isFirst = False
            print " %s = %s" % ( field, misc[field] ),

        print " ]"

Manager.managers.append( AdminManager )
