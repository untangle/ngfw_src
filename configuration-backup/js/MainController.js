Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',

    requires: [
        'Ung.cmp.GoogleDrive'
    ],

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    handleSelectDirectory: function() {
        var me = this,
            v = this.getView(),
            messageData = null;
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager.getGoogleManager(), 'getGoogleCloudApp')
        ]).then(function(result){
            if(result[0] && result[0].clientId) {
                messageData = {
                    action: 'openPicker',
                    clientId: result[0].clientId,
                    appId: result[0].appId,
                    scopes: result[0].scopes,
                    apiKey: result[0].apiKey,
                    relayServerUrl: result[0].relayServerUrl,
                    origin: window.location.protocol + "//" + window.location.host
                };
            }
            me.openIframe(messageData);
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    openIframe: function(messageData) {
        var me = this,
            fileName = null,
            vm = me.getViewModel(),
        iframeWindow = Ext.create('Ext.window.Window', {
            title: 'Google Picker Window',
            width: 800,
            height: 600,
            layout: 'fit',
            listeners: {
                afterrender: function(window) {
                    // Dynamically create an iframe element when the window is rendered
                    var iframe = document.createElement('iframe');
                    iframe.src = messageData.relayServerUrl + "/google-picker.php";
                    iframe.width = '100%';
                    iframe.height = '100%';
                    iframe.allowFullscreen = true;
                    
                    window.body.dom.appendChild(iframe);

                    // Once the iframe has loaded, send a messageData to it
                    iframe.onload = function() {                        
                        iframe.contentWindow.postMessage(messageData, messageData.relayServerUrl + "/google-picker.php");
                    };
                }
            }
        });
        
        iframeWindow.show();

        // Create a promise to track eventListener
        new Promise(function(resolve, reject) {
            window.addEventListener('message', function(event) {
                if (event.origin === messageData.relayServerUrl) {
                    if (event.data.action === 'fileSelected') {
                        fileName = event.data.fileName;
                        resolve();
                    } else if (event.data.action === 'cancel') {
                        resolve();
                    } else if (event.data.action === 'requiredValuesBlank') {
                        Ext.Msg.alert('Error', Ext.String.format("The following values are blank: {0}. Contact Application Developer.".t(), event.data.blankValues));
                        resolve();
                    }
                }
            }, false);
        }).then(function() {
            if(fileName) {
                vm.set('settings.googleDriveDirectory', fileName);
                vm.set('settings.googleDriveDirectorySelected', true);
            }
    
            iframeWindow.close();
            iframeWindow = null;
        });
    },

    backupNow: function (btn) {
        Ext.MessageBox.wait('Backing up... This may take a few minutes.'.t(), 'Please wait'.t());

        Rpc.asyncData(this.getView().appManager, 'sendBackup')
        .then(function(result){
            Ext.MessageBox.hide();
        },function(ext){
            Util.handleException(ex);
            Ext.MessageBox.hide();
        });
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);
            var googleDrive = new Ung.cmp.GoogleDrive();
            vm.set( 'googleDriveIsConfigured', googleDrive.isConfigured() );
            vm.set( 'googleDriveConfigure', function(){ googleDrive.configure(vm.get('policyId')); });

            vm.set('panel.saveDisabled', false);
            if(!vm.get('settings.googleDriveDirectorySelected'))
                vm.set('settings.googleDriveDirectory', '');
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

    }

});
