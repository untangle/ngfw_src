from untangle.ats.apache_setup import ApacheSetup

class TestApacheUvm(ApacheSetup):    
    def test_root_access_outside_admin_enable( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True,
                                   "isInsideInsecureEnabled" : True })

        ## This should be run as root, so it should access these
        yield self.check_access, "http://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://localhost:64157/webui/startPage.do", 200, "main=new Ung.Main"

    def test_root_access_outside_admin_disabled( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False,
                                   "isInsideInsecureEnabled" : True })

        ## This should be run as root, so it should be able to access
        yield self.check_access, "http://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://localhost:64157/webui/startPage.do", 200, "main=new Ung.Main"

    def test_nonroot_access_outside_admin_enable( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True,
                                   "isInsideInsecureEnabled" : True })

        ## Root only applies to localhost, these should ask for papers.
        yield self.check_access, "http://192.0.2.43/webui/startPage.do", 302, "/auth/login"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://192.0.2.43:64157/webui/startPage.do", 302, "/auth/login"

    def test_nonroot_access_outside_admin_disabled( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False,
                                   "isInsideInsecureEnabled" : True })

        ## Root only applies to localhost, these should ask for papers.
        yield self.check_access, "http://192.0.2.43/webui/startPage.do", 302, "/auth/login"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://192.0.2.43/webui/startPage.do", 403, "Forbidden"
        yield self.check_access, "https://192.0.2.43:64157/webui/startPage.do", 302, "/auth/login"

    def test_root_access_outside_admin_enable_no_insecure( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True,
                                   "isInsideInsecureEnabled" : False })

        ## This should be run as root, so it should access these
        yield self.check_access, "http://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://localhost:64157/webui/startPage.do", 200, "main=new Ung.Main"

    def test_root_access_outside_admin_disabled_no_insecure( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False,
                                   "isInsideInsecureEnabled" : False })

        ## This should be run as root, so it should be able to access
        yield self.check_access, "http://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://localhost:64157/webui/startPage.do", 200, "main=new Ung.Main"

    def test_nonroot_access_outside_admin_enable_no_insecure( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True,
                                   "isInsideInsecureEnabled" : False })

        ## Root only applies to localhost, these should ask for papers.
        yield self.check_access, "http://192.0.2.43/webui/startPage.do", 403, "Forbidden"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://localhost/webui/startPage.do", 200, "main=new Ung.Main"
        yield self.check_access, "https://192.0.2.43:64157/webui/startPage.do", 302, "/auth/login"

    def test_nonroot_access_outside_admin_disabled_no_insecure( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False,
                                   "isInsideInsecureEnabled" : False })

        ## Root only applies to localhost, these should ask for papers.
        yield self.check_access, "http://192.0.2.43/webui/startPage.do", 403, "Forbidden"
        yield self.check_access, "http://localhost:64156/webui/startPage.do", 404, "Not Found"
        yield self.check_access, "https://192.0.2.43/webui/startPage.do", 403, "Forbidden"
        yield self.check_access, "https://192.0.2.43:64157/webui/startPage.do", 302, "/auth/login"
