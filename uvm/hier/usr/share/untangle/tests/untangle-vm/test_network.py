import commands
import sets

from untangle.uvm_setup import UvmSetup

## For py.test to recongize the test it must be prefixed with Test in the class name
class TestNetwork(UvmSetup):
    def test_get_internal_address( self ):
        network_manager = self.remote_uvm_context.networkManager()
        
        yield self.check_get_internal_address, network_manager, "1.2.3.4", "192.168.1.1/255.255.255.0"
        yield self.check_get_internal_address, network_manager, "172.16.0.1", "192.168.1.1/255.255.255.0"
        yield self.check_get_internal_address, network_manager, "192.168.254.1", "172.16.0.1/255.255.255.0"

    def test_get_internal_address_null( self ):
        network_manager = self.remote_uvm_context.networkManager()

        basic_settings = network_manager.getBasicSettings()
        address = basic_settings["host"]
        expected = network_manager.getWizardInternalAddressSuggestion( address )
        expected = "%s/%s" % ( expected["network"], expected["netmask"] )
        yield self.check_get_internal_address, network_manager, None, expected

    def check_get_internal_address( self, network_manager, address, expected ):
        response = network_manager.getWizardInternalAddressSuggestion( address )

        ## Convert from an IPNetwork to a simple string
        assert expected == "%s/%s" % ( response["network"], response["netmask"] )
