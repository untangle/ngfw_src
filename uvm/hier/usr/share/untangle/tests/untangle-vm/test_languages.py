from uvm_setup import UvmSetup

## For py.test to recongize the test it must be prefixed with Test in the class name
class TestLanguages(UvmSetup):
    def test_get_languages( self ):
        language_manager = self.remote_uvm_context.languageManager()

        ## Have to convert from the java list class to a list.
        language_list = language_manager.getLanguagesList()["list"]

        ## Assert that there are multiple languages
        assert len( language_list ) > 1

    def check_language( self, language_list, code ):
        for local in language_list:
            if ( code == local["code"] ):
                return

        ## If the test reaches here, the code is missing and the test has failed.
        assert False

    def test_required_langauges( self ):
        language_manager = self.remote_uvm_context.languageManager()

        ## Have to convert from the java list class to a list.
        language_list = language_manager.getLanguagesList()["list"]

        ## This actually creates 4 different test cases.
        for x in ( "en", "de", "zh", "pt_BR", "fr", "es", "zh_CN", "nl" ):
            yield self.check_language, language_list, x

            
        
