Ext.define('Ung.cmp.GoogleDrive', {
    alias: 'widget.googledrive',

    isConfigured: function () {
        var googleDriveConfigured = false, directoryConnectorLicense, directoryConnectorApp, googleManager;
        try{
            directoryConnectorLicense = Rpc.directData('rpc.UvmContext.licenseManager').isLicenseValid('directory-connector');
            directoryConnectorApp = Rpc.directData('rpc.UvmContext.appManager').app('directory-connector');
            if( directoryConnectorLicense && directoryConnectorApp ){
                googleManager = directoryConnectorApp.getGoogleManager();
                if( googleManager && googleManager.isGoogleDriveConnected() ){
                    googleDriveConfigured = true;
                }
            }
        }catch(e){
            Util.handleException('isConfigured: Unable to determine \'' + e + '\'');
        }
        return googleDriveConfigured;
    },
    configure: function (policyId) {
        var app = Rpc.directData('rpc.UvmContext.appManager').app('directory-connector');
        if (app !== null) {
            Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/google');
        } else {
            Ext.MessageBox.alert('Error'.t(), 'Google Drive requires Directory Connector application.'.t());
        }
    }
});
