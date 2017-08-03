Ext.define('Ung.apps.tunnel-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    getSettings: function () {
        var me = this,
            v = me.getView(), 
            vm = me.getViewModel();
        v.setLoading(true);

        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set('settings', result);

            var destinationTunnelData = [];
            destinationTunnelData.push([-1, 'Any Available Tunnel'.t()]);
            destinationTunnelData.push([0, 'Route Normally'.t()]);
            if ( result.tunnels && result.tunnels.list ) {
                for (var i = 0 ; i < result.tunnels.list.length ; i++) {
                    var tunnel = result.tunnels.list[i];
                    destinationTunnelData.push([tunnel.tunnelId, tunnel.name]);
                }
            }
            vm.set('destinationTunnelData', destinationTunnelData);
        });

        // !!! translate title, instructions
        vm.set('providers',{
            Untangle: {
                description: 'Untangle'.t(),
                providerTitle: 'Upload the Untangle OpenVPN config zip',
                providerInstructions: '<li>' + 'Log in the main Untangle server'.t() + '<br/>' +
                    '<li>' + 'Inside "OpenVPN" app settings in Server > Remote Clients add new client and hit Save'.t() + '<br/>' +
                    '<li>' + 'Click Download for the new client and download the configuration zip file for remote Untangle OpenVPN clients'.t() + '<br/>' +
                    '<li>' + 'Upload the zip file below'.t() + '<br/>'
            },
            NordVPN: {
                description: 'NordVPN'.t(),
                providerTitle: 'Upload the NordVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at nordvpn.com'.t() + '<br/>' +
                    '<li>' + 'Click on the "Download area"'.t() + '<br/>' +
                    '<li>' + 'Download the Linux ".OVPN configuration files" zip'.t() + '<br/>' +
                    '<li>' + 'Choose an ovpn file for a server (in your region)'.t() + '<br/>' +
                    '<li>' + 'Upload the chosen ovpn file below'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password of your NordVPN account'.t() + '<br/>'
            },
            ExpressVPN: {
                description: 'ExpressVPN'.t(),
                providerTitle: 'Upload the ExpressVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at expressvpn.com'.t() + '<br/>' +
                    '<li>' + 'FIXME"'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password provided by ExpressVPN'.t() + '<br/>' + 
                    '<li>' + 'NOTE: This is not your ExpressVPN account username/password'.t() + '<br/>'
            },
            CustomZip: {
                description: 'Custom zip file'.t(),
                providerTitle: 'Upload the Custom OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>'
            },
            CustomZipPass: {
                description: 'Custom zip file with username/password'.t(),
                providerTitle: 'Upload the Custom OpenVPN config zip with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomOvpn: {
                description: 'Custom ovpn file'.t(),
                providerTitle: 'Upload the Custom OpenVPN .ovpn file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>'
            },
            CustomOvpnPass: {
                description: 'Custom ovpn file with username/password'.t(),
                providerTitle: 'Upload the Custom OpenVPN .ovpn file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomConf: {
                description: 'Custom conf file'.t(),
                providerTitle: 'Upload the Custom OpenVPN .conf file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>'
            },
            CustomConfPass: {
                description: 'Custom conf file with username/password'.t(),
                providerTitle: 'Upload the Custom OpenVPN .conf file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
        });

        var providers = vm.get('providers');
        var providerComboListData = [['', 'Select'.t()]];
        for( var provider in providers ){
            providerComboListData.push([provider, providers[provider].description]);
        }

        vm.set('providersComboList', Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            // sorters: [{
            //     property: 'name',
            //     direction: 'ASC'
            // }],
            data: providerComboListData
        }) );

    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (me.validateSettings() != true) return;

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;

                var items = Ext.Array.pluck(store.getRange(), 'data');
                if(grid.listProperty == 'settings.tunnels.list'){
                    items.forEach(function(item){
                        if(item.tempPath){
                            console.log(item);
                            var tempPath = item.tempPath;
                            delete item.tempPath;
                            v.appManager.importTunnelConfig(function (result, ex) {
                                v.setLoading(false);
                                if (ex) { Util.handleException(ex); return; }

                                // var settings = appManager.getSettings();
                                // var tunnel = null, i=0;
                                // // Set username/password of tunnel if specified
                                // if (vm.get('username') != null && vm.get('password') != null) {
                                //     if ( settings.tunnels != null && settings.tunnels.list != null ) {
                                //         for (i=0; i< settings.tunnels.list.length ; i++) {
                                //             tunnel = settings.tunnels.list[i];
                                //             if (tunnel['tunnelId'] == vm.get('tunnelId')) {
                                //                 tunnel['username'] = vm.get('username');
                                //                 tunnel['password'] = vm.get('password');
                                //             }
                                //         }
                                //     }
                                // }
                                // Enable tunnel
                                // if ( settings.tunnels != null && settings.tunnels.list != null ) {
                                //     for (i=0; i< settings.tunnels.list.length ; i++) {
                                //         tunnel = settings.tunnels.list[i];
                                //             if (tunnel['tunnelId'] == vm.get('tunnelId')) {
                                //                 tunnel['enabled'] = true;
                                //             }
                                //     }
                                // }
                                // v.appManager.setSettings(settings);
                            }, tempPath, item.provider);
                        }
                    });
                }
                vm.set(grid.listProperty, items);
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, vm.get('settings'));
    },

    validateSettings: function() {
        return(true);
    },

    runWizard: function (btn) {
        var me = this;
        me.wizard = me.getView().add({
            xtype: 'app-tunnel-vpn-wizard',
            appManager: me.getView().appManager,
            listeners: {
                // when wizard is finished, reload settings and try to start the app
                finish: function () {
                    me.getSettings(function (configured) {
                        if (configured && me.getView().appManager.getRunState() !== 'RUNNING') {
                            me.getView().down('appstate > button').click();
                        }
                    });
                }
            }
        });
        me.wizard.show();
    },

    refreshTextArea: function(cmp)
    {
        var tunnelVpnApp = rpc.appManager.app('tunnel-vpn');
        var target;

        switch(cmp.target) {
            case "tunnelLog":
                target = this.getView().down('#tunnelLog');
                target.setValue(tunnelVpnApp.getLogFile());
                break;
        }
    },

    tunnelidRenderer: function(value){
        return 'tun' + value;
    }
});

// Ext.define('Ung.apps.tunnel-vpn.TunnelGridController', {
//     extend: 'Ung.cmp.GridController',

//     alias: 'controller.untunnelgrid',


// //     addRecord: function () {
// //         var me = this;
// //         me.getView().up('app-tunnel-vpn').getController().runWizard();
// //     },



// });

Ext.define('Ung.apps.tunnel-vpn.TunnelRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.untunnelrecordeditor',

    controller: 'untunnelrecordeditorcontroller',

});

Ext.define('Ung.apps.tunnel-vpn.TunnelRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.untunnelrecordeditorcontroller',

    uploadFile: function(cmp) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            appManager = v.up('app-tunnel-vpn').appManager;

        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=upload_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
        }
        Ext.MessageBox.wait('Uploading and validating...'.t(), 'Please wait'.t());

        // console.log(form.down('[name=fileValid]'));
        // var fileValid = form.down('[name=fileValid]');

        var cc = cmp;
        cc.setValidation(true);

        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                console.log('success');
                // tunnel id could go in save routnine.  Or earlier?
                var tunnelId = appManager.getNewTunnelId();
                vm.set("record.tunnelId",tunnelId);
                // vm.set("fileValid",true);
                var resultMsg = action.result.msg.split('&');
                vm.set('fileResult', resultMsg[1]);
                vm.set('record.tempPath', resultMsg[0]);
                //cc.setValidation(true);
                // fileValid.setValidation(true);
            }, this),
            failure: Ext.bind(function( form, action ) {
                console.log('failure');
                Ext.MessageBox.hide();
                // vm.set("fileValid",false);
                console.log(action);
                vm.set('fileResult', action.result.msg);
                cc.setValidation('Must upload valid VPN config file'.t());
                // fileValid.setValidation(action.result.msg);
            }, this)
        });
    },
    providerChange: function(combo, newValue, oldValue){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        vm.set('fileResult', '');
        if( newValue == ''){
            vm.set('providerSelected', false);
            vm.set('providerTitle', '');
            vm.set('providerInstructions', '');
            combo.setValidation('Provider must be selected');
        }else{
            vm.set('providerSelected', true);
            var providers = vm.get('providers');
            for( var provider in providers ){
                if(provider == newValue){
                    vm.set('providerTitle', providers[provider].providerTitle);
                    vm.set('providerInstructions', providers[provider].providerInstructions);
                }
            }
            combo.setValidation(true);

            switch(newValue){
                case 'ExpressVPN':
                case 'NordVPN':
                case 'CustomZipPass':
                case 'CustomConfPass':
                case 'CustomOvpnPass':
                    vm.set('usernameHidden', false);
                    vm.set('passwordHidden', false);
                    break;
                default:
                    vm.set('usernameHidden', true);
                    vm.set('passwordHidden', true);
            }
        }
        me.updateTunnelName();
    },

    defaultTunnelName: 'tunnel',
    updateTunnelName: function(){
        var me = this,
            v = me.getView(),
            tunnelName = v.down('[name=tunnelName]'),
            providerValue = v.down('[name=provider]').getValue();

        var newDefaultTunnelName = 'tunnel';
        if( providerValue != '' ){
            newDefaultTunnelName += '-' + providerValue;
        }
        if(tunnelName.getValue() == me.defaultTunnelName){
            tunnelName.setValue(newDefaultTunnelName);
        }
        me.defaultTunnelName = newDefaultTunnelName;
    }
});

