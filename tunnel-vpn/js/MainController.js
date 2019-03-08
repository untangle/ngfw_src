Ext.define('Ung.apps.tunnel-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#status': {
            activate: 'getTunnelStatus'
        },
        '#log': {
            activate: 'getTunnelLog'
        },
    },

    getTunnelStatus: function () {
       var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getTunnelStatusList')
        .then(function(result){
            grid.setLoading(false);
            if(Util.isDestroyed(vm)){
                return;
            }

            vm.set('tunnelStatusData', !result ? [] : result.list);

        }, function(ex) {
            Util.handleException(ex);
        });
    },

    recycleTunnel: function(view, row, colIndex, item, e, record) {
        var me = this, grid = this.getView().down('#tunnelStatus');

        grid.setLoading('Recycling...'.t());
        Rpc.asyncData(this.getView().appManager, 'recycleTunnel', record.get("tunnelId"))
        .then(function(result){
            setTimeout(function() {
                if(Util.isDestroyed(grid, me)){
                    return;
                }
                me.getTunnelStatus();
                grid.setLoading(false);
            },2000);

        }, function(ex) {
            if(Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getSettings: function () {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        v.setLoading(true);

        vm.set('providers',{
            Untangle: {
                description: 'Untangle'.t(),
                userAuth: false,
                providerTitle: 'Upload the Untangle OpenVPN config zip',
                providerInstructions: '<li>' + 'Log in the main Untangle server'.t() + '<br/>' +
                    '<li>' + 'Inside "OpenVPN" app settings in Server > Remote Clients add new client and hit Save'.t() + '<br/>' +
                    '<li>' + 'Click Download for the new client and download the configuration zip file for remote Untangle OpenVPN clients'.t() + '<br/>' +
                    '<li>' + 'Upload the zip file below'.t() + '<br/>'
            },
            NordVPN: {
                description: 'NordVPN'.t(),
                userAuth: true,
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
                userAuth: true,
                providerTitle: 'Upload the ExpressVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at expressvpn.com'.t() + '<br/>' +
                    '<li>' + 'Click on "Set up ExpressVPN"'.t() + '<br/>' +
                    '<li>' + 'Click on "Manual Configuration" and choose "OpenVPN"'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password provided by ExpressVPN'.t() + '<br/>' +
                    '<li>' + 'NOTE: This is not your ExpressVPN account username/password'.t() + '<br/>' +
                    '<li>' + 'Choose your server and download the corresponding .ovpn file'.t() + '<br/>' +
                    '<li>' + 'Upload the .ovpn file'.t() + '<br/>'
            },
            PrivateInternetAccess: {
                description: 'PrivateInternetAccess'.t(),
                userAuth: true,
                providerTitle: 'Upload the PrivateInternetAccess OpenVPN config ovpn file'.t(),
                providerInstructions: '<li>' + 'Download the appropriate ovpn file here from:'.t() + '<br/>' +
                    '<li>' + '<a href="https://www.privateinternetaccess.com/pages/openvpn-ios" target="_blank">https://www.privateinternetaccess.com/pages/openvpn-ios</a>' + '<br/>' +
                    '<li>' + 'Edit the .ovpn file and remove the &quot;crl-verify&quot; section'.t() + '<br/>' +
                    '<li>' + 'Upload the .ovpn file'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password of your account'.t() + '<br/>'
            },
            CustomZip: {
                description: 'Custom zip file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>'
            },
            CustomZipPass: {
                description: 'Custom zip file with username/password'.t(),
                userAuth: true,
                providerTitle: 'Upload the Custom OpenVPN config zip with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomOvpn: {
                description: 'Custom ovpn file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN .ovpn file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>'
            },
            CustomOvpnPass: {
                description: 'Custom ovpn file with username/password'.t(),
                userAuth: true,
                providerTitle: 'Upload the Custom OpenVPN .ovpn file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomConf: {
                description: 'Custom conf file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN .conf file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>'
            },
            CustomConfPass: {
                description: 'Custom conf file with username/password'.t(),
                userAuth: true,
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
            data: providerComboListData
        }) );

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings')
        ])
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            var appSettings = result[0];
            var networkSettings = result[1];
            
            vm.set('settings', appSettings);
            var destinationTunnelData = [];
            destinationTunnelData.push([-1, 'Any Available Tunnel'.t()]);
            destinationTunnelData.push([0, 'Route Normally'.t()]);
            if ( appSettings.tunnels && appSettings.tunnels.list ) {
                for (var i = 0 ; i < appSettings.tunnels.list.length ; i++) {
                    var tunnel = appSettings.tunnels.list[i];
                    destinationTunnelData.push([tunnel.tunnelId, tunnel.name]);
                }
            }
            vm.set('destinationTunnelData', destinationTunnelData);

            var interfaceData = [];
            interfaceData.push([0, 'Any Interface'.t()]);
            var intf;
            for (var c = 0 ; c < networkSettings.interfaces.list.length ; c++) {
                intf = networkSettings.interfaces.list[c];
                var name = intf.name;
                var key = intf.interfaceId;
                // only allow addressed WANs (is there any use case for non-WANs? perhaps)
                if ( intf.configType == 'ADDRESSED' && intf.isWan) {
                    interfaceData.push( [ key, name ] );
                }
            }
            vm.set('interfaceData', interfaceData);
            
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

        if (me.validateSettings() != true) return;

        v.setLoading(true);
        var sequence = [
            Rpc.asyncPromise(v.appManager, 'setSettings', vm.get('settings') ),
        ];

        var validSave = true;
        var tunnelNamesToImport = [];
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
                    items.forEach(function(tunnel){
                        if( tunnel.tunnelId == -1 ){
                            var tunnelId = me.getNextAvailableTunnelId(items);
                            if( tunnelId === false){
                                validSave = false;
                                Util.handleException("Unable to obtain a unique tunnel id, cannot add tunnel".t() + ": " + tunnel.name);
                                return;
                            }
                            tunnel.tunnelId = tunnelId;
                        }
                        if(tunnel.tempPath){
                            tunnelNamesToImport.push(tunnel.name);
                            sequence.push( Rpc.asyncPromise(v.appManager, 'importTunnelConfig', tunnel.tempPath, tunnel.provider, tunnel.tunnelId));
                            delete tunnel.tempPath;
                        }
                    });
                }
                vm.set(grid.listProperty, items);
            }
        });

        if( validSave == false ){
            return;
        }

        v.setLoading(true);
        Ext.Deferred.sequence(sequence)
        .then(function(result){
            if(Util.isDestroyed(v, vm, tunnelNamesToImport, sequence)){
                return;
            }

            Util.successToast('Settings saved');

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            result.shift();
            result.forEach(function(result, index){
                Util.successToast('Configuration imported'.t() + ': ' + tunnelNamesToImport[index]);
            });

            me.getSettings();

            if(sequence.length > 1){
                // Added one or more tunnels but not powered on.
                if( !vm.get('state.on')){
                    v.down('appstate').down('button').click();
                }
            }
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getNextAvailableTunnelId: function(current){
        var found = false;
        var tunnel;
        var vm = this.getViewModel();
        var tunnels = vm.get('settings.tunnels.list');
        var virtualInterfaces = Rpc.directData('rpc.networkManager.getNetworkSettings.virtualInterfaces.list');

        for( var i = 200; i < 240; i++ ){
            found = false;
            for(tunnel in tunnels){
                if ( tunnel.tunnelId != null && i == tunnel.tunnelId ) {
                    found = true;
                    break;
                }
            }
            if(current){
                current.forEach( function(tunnel){
                    if ( tunnel.tunnelId != null && i == tunnel.tunnelId ) {
                        found = true;
                        return;
                    }
                });
            }

            if(virtualInterfaces){
                virtualInterfaces.forEach( function( interface ){
                    if( i == interface.interfaceId){
                        found = true;
                        return;
                    }
                });
            }

            if (!found) {
                return i;
            }
        }
        return false;
    },

    validateSettings: function() {
        return(true);
    },

    getTunnelLog: function(cmp)
    {
        var appPanel = cmp.up('apppanel');
        Rpc.asyncData( appPanel.appManager, 'getLogFile')
        .then(function(result){
            if(Util.isDestroyed(appPanel)){
                return;
            }
            appPanel.down('#tunnelLog').setValue(result);
        }, function(ex){
            Util.handleException(ex);
        });
    },

    statics: {
        tunnelidRenderer: function(value){
            if(value == -1){
                return 'New'.t();
            }
            return 'tun' + value;
        }
    }
});

Ext.define('Ung.apps.tunnel-vpn.TunnelGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.untunnelgrid',

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        var me = this,
            vm = me.getViewModel();

        var rulesInUse = [];
        var recordTunnelId = record.get('tunnelId');
        var rules = vm.get('rules');
        rules.each( function(rule){
            if( ( recordTunnelId != -1 ) && ( recordTunnelId == rule.get('tunnelId') ) ){
                rulesInUse.push( rule.get('description') );
            }
        });

        if(rulesInUse.length > 0){
            Util.handleException('Cannot delete tunnel.  It is used by one or more rules'.t() + ': ' + rulesInUse.join(','));
        }else{
            this.callParent(arguments);
        }
    },

    checkauth: function( e, editor){
        var vm = this.getViewModel();

        var providers = vm.get('providers');
        return providers[editor.record.get('provider')].userAuth;
    }

 });

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
            vm = me.getViewModel(),
            component = cmp;

        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        Ext.MessageBox.wait('Uploading and validating...'.t(), 'Please wait'.t());

        component.setValidation(true);

        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                if(Util.isDestroyed(vm)){
                    return;
                }
                var resultMsg = action.result.msg.split('&');
                vm.set('fileResult', resultMsg[1]);
                vm.set('record.tempPath', resultMsg[0]);
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                if(Util.isDestroyed(component, vm)){
                    return;
                }
                vm.set('fileResult', action.result.msg);
                component.setValidation('Must upload valid VPN config file'.t());
            }, this)
        });
    },
    providerChange: function(combo, newValue, oldValue){
        var me = this,
            vm = me.getViewModel();

        vm.set('fileResult', '');
        if( newValue == ''){
            vm.set('tunnelProviderSelected', false);
            vm.set('tunnelProviderTitle', '');
            vm.set('tunnelProviderInstructions', '');
            combo.setValidation('Provider must be selected');
        }else{
            vm.set('tunnelProviderSelected', true);

            var fileButton = Ext.ComponentQuery.query('[name=upload_file]')[0];
            if(vm.get('record.tunnelId') != -1){
                fileButton.setValidation(true);
            }

            var record = vm.get('record');
            if( ( oldValue != null ) &&
                ( newValue != oldValue ) &&
                ( ( typeof record.modified.provider == undefined ) || record.modified.provider != newValue ) ){
                fileButton.setValidation('Provider changed');
            }

            combo.setValidation(true);

            var providers = vm.get('providers');
            for( var provider in providers ){
                if(provider == newValue){
                    vm.set('tunnelProviderTitle', providers[provider].providerTitle);
                    vm.set('tunnelProviderInstructions', providers[provider].providerInstructions);

                    if(providers[provider].userAuth == true){
                        vm.set('tunnelUsernameHidden', false);
                        vm.set('tunnelPasswordHidden', false);
                    }else{
                        vm.set('tunnelUsernameHidden', true);
                        vm.set('tunnelPasswordHidden', true);
                    }
                }
            }
        }
        me.updateTunnelName();
    },

    defaultTunnelName: 'tunnel',

    updateTunnelName: function() {
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
