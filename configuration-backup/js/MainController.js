Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',


    // Configuration for Google API
    API_KEY: 'AIzaSyBB_xnIoR-hnGJKoxADvtqFlh6GumFbmDw',

    // Internal variables
    clientId: null,
    appId: null,
    scopes: null,
    redirectUrl: null,

    accessCode: null,
    tokenClient: null,
    accessToken: null,
    pickerInited: false,
    gisInited: false,
    callmethod: false,

    requires: [
        'Ung.cmp.GoogleDrive'
    ],

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    handleSelectDirectory: function() {
        var me = this;
        me.loadMultipleScripts( ['https://apis.google.com/js/api.js','https://accounts.google.com/gsi/client'] )
        .then(function() {
            // Code here will run after all scripts are loaded
            console.log('All scripts loaded successfully');
            me.setClientConfig();
        }).catch(function(error) {
            // Handle the error if any script fails
            console.error('Script loading failed: ', error);
        });
    },

    loadScriptPromise: function(url) {
        return new Ext.Promise(function(resolve, reject) {
            Ext.Loader.loadScript({
                url: url,
                onLoad: function() {
                    console.log('Loaded script: ' + url);
                    resolve(); // Resolve the promise if loaded
                },
                onError: function() {
                    console.error('Failed to load script: ' + url);
                    reject('Failed to load script: ' + url); // Reject the promise if it fails
                }
            });
        });
    },

    loadMultipleScripts: function(scripts) {
        var promises = [],
            me = this;
        Ext.Array.each(scripts, function(script) {
            promises.push(me.loadScriptPromise(script));
        });

        // Return a single promise that resolves when all scripts are loaded
        return Ext.Promise.all(promises);
    },

    setClientConfig: function() {
        var me = this,
            v = this.getView();
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager.getGoogleManager(), 'getGoogleCloudApp')
        ]).then(function(result){
            console.log(result);
            if(result[0] && result[0].clientId) {
                me.clientId = result[0].clientId;
                me.appId = result[0].appId;
                me.redirectUrl = result[0].redirectUrl;
                me.scopes = result[0].scopes;
            }
            if(!me.callmethod) me.getAuthCode();
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    wait: function(ms) {
        return new Promise(function(resolve) {setTimeout(resolve, ms);});
    },

    getAuthCode: function() {
        var me = this,
            clientId = this.clientId,
            redirectUri = this.redirectUrl,
            scopes = this.scopes,
            state = encodeURIComponent(window.location.protocol + "//" + window.location.host + '/admin/index.do#/gdrive/picker'),
            // state = encodeURIComponent('https://192.168.56.177/admin/index.do#/gdrive/picker'),

        // Manually construct the authorization URL
            authUrl = Ext.String.format('https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id={0}&redirect_uri={1}&scope={2}&state={3}', clientId, redirectUri, scopes, state),

            popupWindow = window.open(authUrl, 'Google OAuth2 Login', 'width=500,height=600');

        // Check if the popup was blocked
        if (!popupWindow || popupWindow.closed || typeof popupWindow.closed === 'undefined') {
            Ext.Msg.alert('Error', 'Popup was blocked. Please allow popups for this site or click the link below.');
            return;
        }

        // Track if the popup has been closed
        var pollTimer = setInterval(function() {
            if (popupWindow.closed) {
                clearInterval(pollTimer);
                me.handlePopupClose();
            }
        }, 1000); // Check every second

        // Listen for messages from the popup
        window.addEventListener('message', function(event) {
            // Verify the origin of the message
            console.log(event.origin);
            // if (event.origin !== me.redirectUrl) {
            //     return; // Ignore messages from unknown origins
            // }

            var authCode = event.data; // Assuming the code is sent as the message
            if (authCode) {
                // Close the popup window
                popupWindow.close();
                clearInterval(pollTimer);
                // Handle the authorization code
                me.exchangeCodeForToken(authCode);
            }
        }, false);
    },

    handlePopupClose: function() {
        console.log('Popup was closed without completing authentication.');
        Ext.Msg.alert('Notice', 'Authentication was cancelled. Please try again.');
    },

    /**
     * Handle the Code Client click to get access token
     */
    exchangeCodeForToken: function (code) {
        var me = this,
            v = this.getView();

        console.log('Authorization code:', code);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager.getGoogleManager(), 'exchangeCodeForToken', code)
        ]).then(function(result){
            console.log(result);
            me.accessToken = result[0].access_token;
            me.loadPickerAPI();
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    onPickerApiLoad: function() {
        var me = this;
        // The API is loaded, now create the DocsView and Picker
        console.log(me.accessToken);
        var docsView = new google.picker.DocsView()
          .setIncludeFolders(true) 
          .setMimeTypes('application/vnd.google-apps.folder')
          .setSelectFolderEnabled(true);
    
        var picker = new google.picker.PickerBuilder()
            .enableFeature(google.picker.Feature.NAV_HIDDEN)
            .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
            .setDeveloperKey(me.API_KEY)
            .setAppId(me.appId)
            .setOAuthToken(me.accessToken)
            .addView(docsView)
            // .addView(google.picker.ViewId.DOCS)
            .setCallback(me.pickerCallback.bind(me))
            .build();
        picker.setVisible(true);
    },

    pickerCallback: function(data) {
        if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
            var fileId = data[google.picker.Response.DOCUMENTS][0][google.picker.Document.ID];
            Ext.Msg.alert('File Selected', 'File ID: ' + fileId);
        }
    },

    loadPickerAPI: function() {
        var me = this;
        gapi.load('picker', {'callback': function() { me.onPickerApiLoad(); }});
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
