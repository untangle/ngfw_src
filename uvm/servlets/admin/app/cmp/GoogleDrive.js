Ext.define('Ung.cmp.GoogleDrive', {
    alias: 'widget.googledrive',

    isConfigured: function () {
        var googleDriveConfigured = false, googleManager;
        googleManager = Rpc.directData('rpc.UvmContext.googleManager');
        if( googleManager && googleManager.isGoogleDriveConnected() ) {
            googleDriveConfigured = true;
        }
        return googleDriveConfigured;
    },
    getRootDirectory: function () {
        var googleManager = Rpc.directData('rpc.UvmContext.googleManager');
            rootDirectory = googleManager.getAppSpecificGoogleDrivePath(null);
        return rootDirectory;
    },
    configure: function (policyId) {
        Ung.app.redirectTo('#config/administration/google');
    }
});
