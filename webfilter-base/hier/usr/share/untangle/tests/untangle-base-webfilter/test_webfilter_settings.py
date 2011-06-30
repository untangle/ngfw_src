import random

from untangle.uvm_setup import UvmSetup

class TestSettings(UvmSetup):
    @classmethod
    def setup_class( cls ):
        UvmSetup.setup_class.im_func( cls )

        node_manager = UvmSetup.node_manager.im_func( cls )
        
        cls.tid = cls.install_node.im_func( cls, "untangle-node-webfilter" )

        cls.node_context = node_manager.nodeContext( cls.tid )
        cls.node = cls.node_context.node()

        cls.original_settings = cls.node.getBaseSettings()

    @classmethod
    def teardown_class( cls ):
        cls.node.setBaseSettings( cls.original_settings )

    def test_unblock_password_enabled( self ):
        settings = self.node.getBaseSettings()

        settings["unblockPasswordEnabled"] = True

        self.node.setBaseSettings( settings )

        settings = self.node.getBaseSettings()

        assert settings["unblockPasswordEnabled"] == True

        settings["unblockPasswordEnabled"] = False
                
        self.node.setBaseSettings( settings )

        settings = self.node.getBaseSettings()

        assert settings["unblockPasswordEnabled"] == False

    def test_unblock_password_admin( self ):
        settings = self.node.getBaseSettings()

        settings["unblockPasswordAdmin"] = True

        self.node.setBaseSettings( settings )

        settings = self.node.getBaseSettings()

        assert settings["unblockPasswordAdmin"] == True

        settings["unblockPasswordAdmin"] = False
                
        self.node.setBaseSettings( settings )

        settings = self.node.getBaseSettings()

        assert settings["unblockPasswordAdmin"] == False

    def test_unblock_password( self ):
        password = str( random.random())
        settings = self.node.getBaseSettings()

        settings["unblockPassword"] = password

        self.node.setBaseSettings( settings )

        settings = self.node.getBaseSettings()

        assert settings["unblockPassword"] == password





        

