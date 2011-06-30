import commands
import os
import tempfile

from untangle.uvm_setup import UvmSetup

class TestLicenseManager(UvmSetup):
    @classmethod
    def setup_class( cls ):
        cls.prefix = "/"
        if ( "PREFIX" in os.environ ):
            cls.prefix = os.environ["PREFIX"]

        UvmSetup.setup_class.im_func(cls)
        cls.tid = cls.install_node.im_func( cls, "untangle-node-faild", delete_existing = False )

        node_manager = UvmSetup.node_manager.im_func( cls )
        cls.node_context = node_manager.nodeContext( cls.tid )
        cls.node = cls.node_context.node()

        cls.license_manager = cls.remote_uvm_context.licenseManager()

        status = cls.license_manager.getLicenseStatus( "untangle-faild" )

        if ( status["isExpired"] == True ):
            raise Exception("Unable to run test, faild is expired.")

        cls.license_path = os.path.join( cls.prefix, "usr", "share", "untangle", "conf", "licenses" )

        cls.temp_dir = tempfile.mkdtemp()

        commands.getstatusoutput( "cp %s/ufaild*.ulf %s" % ( cls.license_path, cls.temp_dir ))
        
    @classmethod
    def teardown_class( cls ):
        ## Restore all of the class files
        commands.getstatusoutput( "cp %s/ufaild*.ulf %s" % ( cls.temp_dir, cls.license_path ))
        cls.license_manager.reloadLicenses()        

    def test_is_enabled(self):
        try:
            self.node_manager().start( self.tid )
        except:
            pass
        
        assert self.is_faild_running() == True

    def test_expire_license( self ):
        ## Move all of the licenses to temp
        commands.getstatusoutput( "rm %s/ufaild*.ulf" % self.license_path )
        self.license_manager.reloadLicenses()

        assert self.license_manager.getLicenseStatus( "untangle-faild" )["isExpired"] == True
        assert self.is_faild_running() == False

    def test_is_reenabled(self):
        ## Restore all of the class files
        commands.getstatusoutput( "cp %s/ufaild*.ulf %s" % ( self.temp_dir, self.license_path ))
        self.license_manager.reloadLicenses()

        try:
            self.node_manager().start( self.tid )
        except:
            pass

        assert self.license_manager.getLicenseStatus( "untangle-faild" )["isExpired"] == False
        assert self.is_faild_running() == True

    def is_faild_running(self):
        node_instances = self.node_manager().allNodeStates()

        return node_instances["map"][str(self.tid["id"])] == "RUNNING"
        

        
        
        
