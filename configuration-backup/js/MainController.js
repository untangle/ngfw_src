Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',


    // Configuration for Google API
    SCOPES: 'https://www.googleapis.com/auth/drive.file',
    CLIENT_ID: 'myClientId',
    CLIENT_SECRET: 'myClientSecret',
    API_KEY: 'myAPIkey',
    APP_ID: 'myAPIId',
    REDIRECT_URI: 'https://auth-relay.untangle.com/oauth2.php',

    // Internal variables
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

    /**
     * Dynamically load external Google APIs using Ext.Loader
     */
    loadExternalScripts: function () {
        var me = this;

        // Load gapi.js first
        Ext.Loader.loadScript({
            url: 'https://apis.google.com/js/api.js',
            onLoad: function () {
                // me.gapiLoaded();
            },
            onError: function () {
                Ext.Msg.alert('Error', 'Failed to load gapi.js');
            }
        });

        // Load Google Identity Services after gapi.js
        Ext.Loader.loadScript({
            url: 'https://accounts.google.com/gsi/client',
            onLoad: function () {
                if(!me.callmethod) me.handleSelectDirectory();
            },
            onError: function () {
                Ext.Msg.alert('Error', 'Failed to load Google Identity Services');
            }
        });
        console.log("External Scripts Loading");
        
    },

    wait: function(ms) {
        return new Promise(function(resolve) {setTimeout(resolve, ms);});
    },

    handleSelectDirectory: function() {
        var me = this,
            clientId = this.CLIENT_ID,
            redirectUri = this.REDIRECT_URI,
            scope = this.SCOPES,
            state = encodeURIComponent('http://192.168.56.177/admin/index.do#/gdrive/picker'),

        // Manually construct the authorization URL
            authUrl = Ext.String.format('https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id={0}&redirect_uri={1}&scope={2}&state={3}',clientId,redirectUri,scope,state),

            popupWindow = window.open(authUrl, 'Google OAuth2 Login', 'width=500,height=600');

        // Check if the popup was blocked
        if (!popupWindow || popupWindow.closed || typeof popupWindow.closed === 'undefined') {
            Ext.Msg.alert('Error', 'Popup was blocked. Please allow popups for this site or click the link below.');
            return;
        }

        // Track if the popup has been closed
        var pollTimer = setInterval(function() {
            if (popupWindow.closed) {
                // clearInterval(pollTimer);
                // me.handlePopupClose();
            }
        }, 1000); // Check every second

        // Listen for messages from the popup
        window.addEventListener('message', function(event) {
            // Verify the origin of the message
            console.log(event.origin);
            // if (event.origin !== me.REDIRECT_URI) {
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
        }, function(ex) {
            Util.handleException(ex);
        });

        me.callmethod = true;
        this.loadExternalScripts();
        this.wait(2000).then(function() {
            console.log("This runs after 2 seconds");

            me.onApiLoad();
        });
    },

    onPickerApiLoad: function() {
        var me = this;
        // The API is loaded, now create the DocsView and Picker
        console.log(me.accessToken);
        var view = new google.picker.View(google.picker.ViewId.FOLDERS);
        var docsView = new google.picker.DocsView()
          .setIncludeFolders(true) 
          .setMimeTypes('application/vnd.google-apps.folder')
          .setSelectFolderEnabled(true);
    
        var picker = new google.picker.PickerBuilder()
            .enableFeature(google.picker.Feature.NAV_HIDDEN)
            .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
            .setDeveloperKey(me.API_KEY)
            .setAppId(me.APP_ID)
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

    onApiLoad: function() {
        var me = this;

        // gapi.load('auth', {'callback': function() { me.onAuthApiLoad(); }});
        gapi.load('picker', {'callback': function() { me.onPickerApiLoad(); }});
    },

    onAuthApiLoad: function() {
        var me = this;

        gapi.auth2.init({
            client_id: '', // Replace with your OAuth client ID
            scope: 'https://www.googleapis.com/auth/drive.file'
        }).then(function() {
            me.signIn();
        });
    },

    signIn: function() {
        var me = this;
        var GoogleAuth = gapi.auth2.getAuthInstance();

        GoogleAuth.signIn().then(function(user) {
            me.accessToken = user.getAuthResponse().access_token;
            // gapi.load('picker', {'callback': function() { me.createPicker(); }});
        }).catch(function(error) {
            Ext.Msg.alert('Sign In Error', error.error);
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
        if(localStorage.getItem('code')) {
            this.accessCode = localStorage.getItem('code');
            localStorage.removeItem('code');
            this.clickCodeClient(this.accessCode);
        }
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
