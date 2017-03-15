Ext.define('Ung.cmp.GoogleDrive', {
    alias: 'widget.googledrive',

    isConfigured: function () {
        var googleDriveConfigured = false, directoryConnectorLicense, directoryConnectorNode, googleManager;
        try{
            directoryConnectorLicense = Rpc.directData('rpc.UvmContext.licenseManager').isLicenseValid('untangle-node-directory-connector');
            directoryConnectorNode = Rpc.directData('rpc.UvmContext.appManager').app('untangle-node-directory-connector');
            if( directoryConnectorLicense && directoryConnectorNode ){
                googleManager = directoryConnectorNode.getGoogleManager();
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
        var node = Rpc.directData('rpc.UvmContext.appManager').app('untangle-node-directory-connector');
        if (node !== null) {
            // var nodeCmp = Ung.Node.getCmp(node.nodeId);
            var nodeCmp = Ext.getCmp(node.nodeId);
            if (nodeCmp !== null) {
                Ung.Main.target = 'node.untangle-node-directory-connector.Google Connector';
                nodeCmp.loadSettings();
            }
        } else {
            Ext.MessageBox.alert('Error'.t(), 'Google Drive requires Directory Connector application.'.t());
        }
    }
});
