import random

from untangle.uvm_setup import UvmSetup

class TestSaveDistributionKey(UvmSetup):
    @classmethod
    def setup_class(cls):
        UvmSetup.setup_class.im_func(cls)
        node_manager = UvmSetup.node_manager.im_func( cls )

        cls.openvpn_tid = UvmSetup.install_node.im_func( cls, "untangle-node-openvpn", delete_existing = True )

        cls.node_context = node_manager.nodeContext( cls.openvpn_tid )
        cls.node = cls.node_context.node()

        ## Start the wizard in openvpn mode
        cls.node.startConfig("SERVER_ROUTE")

        ## Generate the certificate
        cls.node.generateCertificate({
            "javaClass" : "com.untangle.node.openvpn.CertificateParameters",
            "organization" : "testing",
            "domain" : "",
            "country" : "US",
            "state" : "ca",
            "locality" : "san mateo",
            "storeCaUsb" : False
            })

        ## Get and save the exported address list
        exports = cls.node.getExportedAddressList()
        cls.node.setExportedAddressList( exports )

        ## Finish the configuartion, but do not turn on the node.
        cls.node.completeConfig()
        
        ## Add a few sample clients
        vpn_settings = cls.node.getVpnSettings()

        vpn_group = vpn_settings["groupList"]["list"][0]
        
        client_list = vpn_settings["clientList"]
        client_list["list"].append({
            "javaClass" : "com.untangle.node.openvpn.VpnClient",
            "group" : vpn_group,
            "isUntanglePlatform" : False,
            "isLive" : True,
            "name" : "test_1"
        })
        client_list["list"].append({
            "javaClass" : "com.untangle.node.openvpn.VpnClient",
            "group" : vpn_group,
            "isUntanglePlatform" : False,
            "isLive" : True,
            "name" : "test_2"
        })
        client_list["list"].append({
            "javaClass" : "com.untangle.node.openvpn.VpnClient",
            "group" : vpn_group,
            "isUntanglePlatform" : False,
            "isLive" : True,
            "name" : "test_3"
        })
        cls.node.setVpnSettings( vpn_settings )
        
    @classmethod
    def teardown_class(cls):
        cls.node_manager().destroy( cls.openvpn_tid )

    def test_distribute_key(self):
        ## Distribute the key for test_1.
        vpn_settings = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings, "test_1" )
        client["distributionEmail"] = "root@localhost"
        self.node.distributeClientConfig( client )

        ## Now the settings have been put in there.
        vpn_settings_2 = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings_2, "test_1" )
        assert client["distributionKey"] != None
        assert len( client["distributionKey"] ) > 0
        distribution_key = client["distributionKey"]

        ## Save the settings (without the key, key should be preserved.)
        self.node.setVpnSettings( vpn_settings )
        vpn_settings_2 = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings_2, "test_1" )
        assert client["distributionKey"] == distribution_key

    def test_modify_key(self):
        ## Distribute the key for test_2.
        vpn_settings = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings, "test_2" )
        client["distributionEmail"] = "root@localhost"
        self.node.distributeClientConfig( client )

        ## Now the settings have been put in there.
        vpn_settings_2 = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings_2, "test_2" )
        assert client["distributionKey"] != None
        assert len( client["distributionKey"] ) > 0
        distribution_key = client["distributionKey"]

        test_key = "%08x%08x"% ( random.randint(0, 0x7FFFFFFF), random.randint(0, 0x7FFFFFFF))

        client = self.find_client_base( vpn_settings, "test_2" )
        client["distributionKey"] = test_key

        ## Save the settings (modify the key, it should take the new value)
        self.node.setVpnSettings( vpn_settings )
        vpn_settings_2 = self.node.getVpnSettings()
        client = self.find_client_base( vpn_settings_2, "test_2" )
        assert client["distributionKey"] == test_key

    def find_client_base( self, vpn_settings, name ):
        for client in vpn_settings["clientList"]["list"]:
            if ( client["name"] == name ):
                return client

        raise Exception("Unable to find the client %s" % name )
    

