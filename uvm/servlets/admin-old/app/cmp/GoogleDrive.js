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
    configure: function (policyId) {
        Ung.app.redirectTo('#config/administration/google');
    }
});
