import random

from untangle.uvm_setup import UvmSetup

class TestSettingsCPD(UvmSetup):
    @classmethod
    def setup_class( cls ):
        UvmSetup.setup_class.im_func( cls )

        node_manager = UvmSetup.node_manager.im_func( cls )
        
        cls.tid = cls.install_node.im_func( cls, "untangle-node-cpd" )

        ## Stop the node so the settings don't take effect.
        try:
            node_manager.stop( cls.tid )
        except:
            ## No biggie if this fails
            pass

        cls.node_context = node_manager.nodeContext( cls.tid )
        cls.node = cls.node_context.node()

        cls.original_settings = cls.node.getCPDSettings()

    @classmethod
    def teardown_class( cls ):
        cls.node.setCPDSettings( cls.original_settings )

    def test_passed_clients( self ):
        new_value = self.build_address_list( [self.build_random_ip()], "com.untangle.node.cpd.PassedClient" )
        
        self.node.setPassedClients( new_value )
        settings = self.node.getCPDSettings()
        
        assert settings["passedClients"]["list"][0]["address"] == new_value["list"][0]["address"]
        assert self.node.getPassedClients()["list"][0]["address"] == new_value["list"][0]["address"]

    def test_settings_passed_clients( self ):
        new_value = self.build_address_list( [self.build_random_ip()], "com.untangle.node.cpd.PassedClient" )

        settings = self.node.getCPDSettings()
        settings["passedClients"] = new_value
        self.node.setCPDSettings( settings )

        settings = self.node.getCPDSettings()
        assert settings["passedClients"]["list"][0]["address"] == new_value["list"][0]["address"]
        assert self.node.getPassedClients()["list"][0]["address"] == new_value["list"][0]["address"]

    def test_passed_servers( self ):
        new_value = self.build_address_list( [self.build_random_ip()], "com.untangle.node.cpd.PassedServer" )
        
        self.node.setPassedServers( new_value )
        settings = self.node.getCPDSettings()
        
        assert settings["passedServers"]["list"][0]["address"] == new_value["list"][0]["address"]
        assert self.node.getPassedServers()["list"][0]["address"] == new_value["list"][0]["address"]

    def test_settings_passed_servers( self ):
        new_value = self.build_address_list( [self.build_random_ip()], "com.untangle.node.cpd.PassedServer" )

        settings = self.node.getCPDSettings()
        settings["passedServers"] = new_value
        self.node.setCPDSettings( settings )

        settings = self.node.getCPDSettings()
        assert settings["passedServers"]["list"][0]["address"] == new_value["list"][0]["address"]
        assert self.node.getPassedServers()["list"][0]["address"] == new_value["list"][0]["address"]

    def build_random_ip( self ):
        return str(random.randint( 1, 254 )) + "." + str(random.randint( 1, 254 )) + "." + str(random.randint( 1, 254 )) + "." + str(random.randint( 1, 254 ))

    def build_address_list( self, addresses, java_class ):
        new_list = { "javaClass" : "java.util.LinkedList", "list" : [] }
        for address in addresses:
            new_list["list"].append({
                "live" : True,
                "log" : False,
                "address" : address,
                "name" : "[no name]",
                "category" : "[no category]",
                "description" : "[no description]",
                "javaClass" : java_class
            })

        return new_list



