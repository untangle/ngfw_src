Ext.define('Ung.apps.configurationbackup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-configuration-backup',


    // Configuration for Google API
    // config: {
    SCOPES: 'https://www.googleapis.com/auth/drive.file',
    CLIENT_ID: 'myclientid',
    CLIENT_SECRET: 'myclientsecrete',
    API_KEY: 'myapikey',
    APP_ID: 'myappid',
    REDIRECT_URI: 'https://auth-relay.untangle.com/oauth2.php',
    // },

    // Internal variables
    accessCode: null,
    tokenClient: null,
    accessToken: null,
    pickerInited: false,
    gisInited: false,

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
                me.handleSelectDirectory();
            },
            onError: function () {
                Ext.Msg.alert('Error', 'Failed to load Google Identity Services');
            }
        });
        console.log("External Scripts Loading");
        // this.wait(2000).then(function() {
        //     console.log("This runs after 2 seconds");
        //     me.handleSelectDirectory();
        // });
        
    },

    wait: function(ms) {
        return new Promise(function(resolve) {setTimeout(resolve, ms);});
    },

    handleSelectDirectory: function() {
        var me = this;
        this.getAccessCode();
    },

    /**
     * Handle the Code Client click to get access token
     */
    clickCodeClient: function (code) {
        var me = this,
            v = this.getView();

        console.log(v.appManager.getGoogleManager());
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager.getGoogleManager(), 'exchangeCodeForToken', code)
        ]).then(function(result){
            console.log(result);
            // me.accessToken = result;
        }, function(ex) {
            Util.handleException(ex);
        });

        // me.createPicker();
    },

    /**
     * Initialize Code Client and retrieve access token
     */
    getAccessCode: function () {
        var me = this;
        console.log("In getAccessToken");
        new Promise(function (resolve, reject) {
            var codeClient = google.accounts.oauth2.initCodeClient({
                client_id: me.CLIENT_ID,
                scope: me.SCOPES,
                redirect_uri: me.REDIRECT_URI,
                state: 'http://192.168.56.187/admin/index.do#service/configuration-backup/google-connector',
                ux_mode: 'redirect'
            });
            console.log(codeClient);

            // Trigger the authorization flow
            codeClient.requestCode();
        }).catch(function (error) {
            console.error('Error retrieving access token:', error);
            Ext.Msg.alert('Error', 'Failed to retrieve access token.');
        });
    },

    /**
     * Create and display the Google Picker
     */
    // createPicker: function () {
    //     var me = this;

    //     var docsView = new google.picker.DocsView()
    //         .setIncludeFolders(true)
    //         .setMimeTypes('application/vnd.google-apps.folder')
    //         .setSelectFolderEnabled(true);

    //     var picker = new google.picker.PickerBuilder()
    //         .enableFeature(google.picker.Feature.NAV_HIDDEN)
    //         .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
    //         .setDeveloperKey(this.API_KEY)
    //         .setAppId(this.APP_ID)
    //         .setOAuthToken(this.accessToken)
    //         .addView(docsView)
    //         .setCallback(this.pickerCallback.bind(this))
    //         .build();
    //     picker.setVisible(true);
    // },

    /**
     * Callback for Picker actions
     */
    // pickerCallback: async function (data) {
    //     if (data.action === google.picker.Action.PICKED) {
    //         var content = 'Picker response:\n' + JSON.stringify(data, null, 2) + '\n';
    //         var document = data[google.picker.Response.DOCUMENTS][0];
    //         var fileId = document[google.picker.Document.ID];
    //         console.log(fileId);

    //         try {
    //             var res = await gapi.client.drive.files.get({
    //                 'fileId': fileId,
    //                 'fields': '*',
    //             });
    //             content += 'Drive API response for first document:\n' + JSON.stringify(res.result, null, 2) + '\n';
    //             this.down('#content').update(content);
    //         } catch (error) {
    //             console.error('Error fetching file details:', error);
    //             Ext.Msg.alert('Error', 'Failed to fetch file details.');
    //         }
    //     }
    // },

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
