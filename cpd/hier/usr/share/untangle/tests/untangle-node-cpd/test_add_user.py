import random
import time

from untangle.ats.uvm_setup import UvmSetup

class TestCPDAddUser(UvmSetup):
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

        cls.phone_book = cls.remote_uvm_context.phoneBook()


    def test_add_user( self ):
        new_user = "foo-random-user" + str(random.randint( 4500, 7000 ))
        ip_address = "1.2.3.4"

        self.node.registerUser( ip_address, new_user, self.build_date( time.time() + 360 ))

        user_map = self.node.getUserMap()
        user_map = user_map["map"]
        assert user_map[ip_address] == new_user

    def test_replace_user( self ):
        new_user = "foo-random-user" + str(random.randint( 4500, 7000 ))
        ip_address = "1.2.3.4"

        self.node.registerUser( ip_address, new_user, self.build_date( time.time() + 360 ))

        value = self.node.registerUser( ip_address, "no", self.build_date( time.time() + 360 ))
        assert value == new_user
        
        user_map = self.node.getUserMap()
        user_map = user_map["map"]
        assert user_map[ip_address] == "no"

    def test_remove_user( self ):
        new_user = "foo-random-user" + str(random.randint( 4500, 7000 ))
        ip_address = "1.2.3.4"

        self.node.registerUser( ip_address, new_user, self.build_date( time.time() + 360 ))

        value = self.node.registerUser( ip_address, "no", self.build_date( time.time() + 360 ))
        assert value == new_user


        
