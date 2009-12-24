import random

from untangle.ats.uvm_setup import UvmSetup

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
        settings = self.node.getCPDSettings()

        print( settings["pageType"] )
        print( settings )
        
        new_value = "foo-random-value" + str(random.randint( 4500, 7000 ))
        settings["passedClients"] = new_value

        self.node.setCPDSettings( settings )
        settings = self.node.getCPDSettings()
        assert settings["passedClients"] == new_value
