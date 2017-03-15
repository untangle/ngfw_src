Ext.define('Ung.cmp.GoogleDrive', {
    alias: 'widget.googledrive',

    isConfigured: function () {
        var googleDriveConfigured = false, directoryConnectorLicense, directoryConnectorApp, googleManager;
        try{
            directoryConnectorLicense = Rpc.directData('rpc.UvmContext.licenseManager').isLicenseValid('untangle-app-directory-connector');
            directoryConnectorApp = Rpc.directData('rpc.UvmContext.appManager').app('untangle-app-directory-connector');
            if( directoryConnectorLicense && directoryConnectorApp ){
                googleManager = directoryConnectorApp.getGoogleManager();
                if( googleManager && googleManager.isGoogleDriveConnected() ){
                    googleDriveConfigured = true;
                }
            }
        }catch(e){
            Util.exceptionToast('isConfigured: Unable to determine \'' + e + '\'');
        }
        return googleDriveConfigured;
    },
    configure: function () {
        var app = Rpc.directData('rpc.UvmContext.appManager').app('untangle-app-directory-connector');
        if (app !== null) {
            // var appCmp = Ung.App.getCmp(app.appId);
            var appCmp = Ext.getCmp(app.appId);
            if (appCmp !== null) {
                Ung.Main.target = 'app.untangle-app-directory-connector.Google Connector';
                appCmp.loadSettings();
            }
        } else {
            Ext.MessageBox.alert('Error'.t(), 'Google Drive requires Directory Connector application.'.t());
        }
    }
});
