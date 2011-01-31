from untangle.ats.uvm_setup import UvmSetup

class TestPackageFlags(UvmSetup):
    def test_hide_on( self ):
        toolbox_manager = self.remote_uvm_context.toolboxManager()

        mackages = toolbox_manager.available()

        mackage_map = {}
        for mackage in mackages:
            mackage_map[mackage["name"]] = mackage

        test_keys = []
        test_keys.append(( "untangle-libitem-opensource-package", "u4w" ))
        test_keys.append(( "untangle-libitem-shield", "" ))
        test_keys.append(( "untangle-libitem-spyware", "" ))
        test_keys.append(( "untangle-libitem-webfilter", "" ))
        test_keys.append(( "untangle-libitem-clam", "" ))
        test_keys.append(( "untangle-libitem-protofilter", "" ))
        test_keys.append(( "untangle-libitem-firewall", "" ))
        test_keys.append(( "untangle-libitem-spamassassin", "" ))
        test_keys.append(( "untangle-libitem-phish", "" ))
        test_keys.append(( "untangle-libitem-openvpn", "u4w" ))
        test_keys.append(( "untangle-libitem-ips", "" ))
        test_keys.append(( "untangle-libitem-reporting", "" ))
        test_keys.append(( "untangle-libitem-adblocker", "" ))

        test_keys.append(("untangle-libitem-professional-package", "u4w"))
        test_keys.append(("untangle-libitem-kav", ""))
        test_keys.append(("untangle-libitem-sitefilter", ""))
        test_keys.append(("untangle-libitem-commtouch", ""))
        test_keys.append(("untangle-libitem-faild", ""))
        test_keys.append(("untangle-libitem-splitd", "u4w"))
        test_keys.append(("untangle-libitem-boxbackup", ""))
        test_keys.append(("untangle-libitem-portal", "u4w, iso"))
        test_keys.append(("untangle-libitem-adconnector", ""))
        test_keys.append(("untangle-libitem-policy", ""))
        test_keys.append(("untangle-libitem-support", ""))
        test_keys.append(("untangle-libitem-pcremote", "u4w, iso"))
        test_keys.append(("untangle-libitem-branding", ""))
        test_keys.append(("untangle-libitem-test", ""))
        test_keys.append(("untangle-libitem-update-service", ""))

        for test_key in test_keys:
            yield self.check_hide_on, mackage_map, test_key[0], test_key[1]
        
    def check_hide_on( self, mackage_map, mackage_name, values ):
        assert mackage_name in mackage_map

        mackage = mackage_map[mackage_name]

        assert mackage["hideOn"] == values
    
