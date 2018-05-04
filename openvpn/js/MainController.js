Ext.define('Ung.apps.openvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-openvpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#server': {
            activate: Ung.controller.Global.onSubtabActivate,
        },
        '#server #server': {
            beforetabchange: Ung.controller.Global.onBeforeSubtabChange
        }
    },

    getActiveClients: function () {
        var grid = this.getView().down('#activeClients'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getActiveClients')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }
            vm.set('clientStatusData', result.list);

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getRemoteServers: function () {
        var grid = this.getView().down('#remoteServers'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getRemoteServersStatus')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }

            vm.set('serverStatusData', result.list);

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            // set a flag on existing users to prevent changing the name
            for(var i = 0 ; i < result.remoteClients.list.length ; i++) {
                result.remoteClients.list[i].existing = true;
            }

            vm.set('settings', result);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

        // trigger active clients/servers fetching when instance run state changes
        vm.bind('{state.on}', function (stateon) {
            if (stateon) {
                me.getActiveClients();
                me.getRemoteServers();
            } else {
                vm.set({
                    clientStatusData: [],
                    serverStatusData: []
                });
            }
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

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
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

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
    },

    validateSettings: function() {
        var v = this.getView();

        var clientStore = v.query('app-openvpn-remote-clients-grid')[0].getStore();
        var groupStore = v.query('app-openvpn-groups-grid')[0].getStore();
        var message = "";
        var problem = 0;
        var counter = 0;
        var clientNames = {};
        var groupNames = {};

        // make sure they don't try to delete all groups
        counter = 0;
        groupStore.each(function(record) {
            if (!record.get('markedForDelete')) counter++;
        });

        if ((counter == 0) && (groupStore.data.length > 0)) {
            groupStore.each(function(record) {
                record.set('markedForDelete', false);
            });
            problem++;
            Ext.MessageBox.alert("Delete Group Failed".t(), "There must be at least one group.".t());
        }
        if (problem != 0) return(false);

        // make sure they don't try to delete a group with active remote clients
        groupStore.each(function(record) {
            if (record.get('markedForDelete')) {
                var finder = clientStore.findRecord('groupId' , record.get('groupId'));
                if ((finder != null) && (!finder.get('markedForDelete'))) {
                    message = Ext.String.format("The group: {0} cannot be deleted because it is being used by the client: {1} in the Remote Clients list.".t(), record.get('name'), finder.get('name'));
                    record.set('markedForDelete', false);
                    problem++;
                    Ext.MessageBox.alert("Delete Group Failed".t(), message);
                }
            }
        });
        if (problem != 0) return(false);

        // check for duplicate client names
        counter = 1;
        clientStore.each(function(record) {
            var clientName = record.get('name');
            if (clientNames[clientName] != null) {
                message = Ext.String.format("The client name: {0} in row: {1} already exists.", clientName, counter);
                problem++;
                Ext.MessageBox.alert("Add Remote Client Failed".t(), message);
            }
            if (! record.get('markedForDelete')) clientNames[clientName] = true;
            counter++;
        });
        if (problem != 0) return(false);

        // check for duplicate group names
        counter = 1;
        groupStore.each(function(record) {
            var groupName = record.get('name');
            if (groupNames[groupName] != null) {
                message = Ext.String.format("The group name: {0} in row: {1} already exists.", groupName, counter);
                problem++;
                Ext.MessageBox.alert("Add Group Failed".t(), message);
            }
            if (! record.get('markedForDelete')) groupNames[groupName] = true;
            counter++;
        });
        if (problem != 0) return(false);

        // we must not allow port 443 or Apache will get very upset
        var pfield = Ext.ComponentQuery.query('textfield[fieldIndex=listenPort]')[0];
        var pvalue = pfield.getValue();
        if (pvalue === '443') {
            Ext.MessageBox.alert("Invalid Port".t(), "The configured port value is reserved. Please enter a different value.".t());
            return(false);
        }

        return(true);
    },

    uploadFile: function(cmp) {
        var me = this;
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=uploadConfigFileName]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
        }
        Ext.MessageBox.wait("Uploading File...".t(), "Please Wait".t());
        form.submit({
            url: "/openvpn/uploadConfig",
            success: Ext.bind(function( form, action ) {
                if(Util.isDestroyed(me)){
                    return;
                }
                Ext.MessageBox.hide();
                Ext.MessageBox.alert('Success'.t(), 'The configuration has been imported.'.t());
                me.getSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                Ext.MessageBox.alert('Failure'.t(), 'Import failure'.t() + ": " + action.result.code);
            }, this)
        });
    },

    configureAuthenticationMethod: function (btn) {
        var me = this, vm = this.getViewModel();
        var policyId = vm.get('policyId');
        var authType = this.getViewModel().get('settings.authenticationType');

        Rpc.asyncData('rpc.appManager.app', 'directory-connector')
        .then( function(directoryConnectorApp){
            if(Util.isDestroyed(me, policyId, authType)){
                return;
            }

            // Default to local directory
            var checkDirectoryConnector = false;
            url = '#config/local-directory';
            switch (authType) {
                case 'RADIUS':
                    checkDirectoryConnector = true;
                    url = '#apps/' + policyId + '/directory-connector/radius';
                    break;
                case 'ACTIVE_DIRECTORY':
                    checkDirectoryConnector = true;
                    url = '#apps/' + policyId + '/directory-connector/active-directory';
                    break;
                case 'ANY_DIRCON':
                    checkDirectoryConnector = true;
                    url = '#apps/' + policyId + '/directory-connector';
                    break;
            }
            if( checkDirectoryConnector && directoryConnectorApp == null){
                me.showMissingServiceWarning();
            }else{
                Ung.app.redirectTo(url);
            }

        },function(ex){
            Util.handleException(ex);
        });
    },

    showMissingServiceWarning: function() {
        Ext.MessageBox.alert('Service Not Installed'.t(), 'The Directory Connector application must be installed to use this feature.'.t());
    }
});



Ext.define('Ung.apps.openvpn.SpecialGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-openvpn-special',

    downloadClient: function(view, row, colIndex, item, e, record) {
        if( record.dirty ){
            Ext.MessageBox.alert(
                "Cannot download".t(),
                "Remote Client information has been modified.  You must Save before downloading the client.".t()
            );
            return;
        }
        this.getDistributeWindow().populate(record);
    },

    getDistributeWindow: function() {
        this.distributeWindow = Ext.create('Ext.window.Window', {
            title: 'Download OpenVPN Client'.t(),
            items: [{
                xtype: 'panel',
                items: [{
                    xtype: 'fieldset',
                    title: 'Download'.t(),
                    margin: 10,
                    defaults: { margin: 10 },
                    items: [{
                        xtype: 'component',
                        html: 'These files can be used to configure your Remote Clients.'.t(),
                    }, {
                        xtype: 'component',
                        name: 'downloadWindowsInstaller',
                        html:  " "
                    }, {
                        xtype: 'component',
                        name: 'downloadGenericConfigurationFile',
                        html: " "
                    }, {
                        xtype: 'component',
                        name: 'downloadUntangleConfigurationFile',
                        html: " "
                    }, {
                        xtype: 'component',
                        html: '<BR>'
                    }, {
                        xtype: 'component',
                        html: 'This file can be used to configure Chromebook clients.  On the target device, browse to <b>chrome://net-internals#chromeos</b> and use Import ONC file.'.t(),
                    }, {
                        xtype: 'component',
                        name: 'downloadChromebookConfigurationFile',
                        html: " "
                    }]
                }]
            }],
            bbar: ['->', {
                name: 'close',
                iconCls: 'fa fa-window-close',
                text: 'Close'.t(),
                handler: function() {
                    this.distributeWindow.close();
                },
                scope: this
            }],
            closeWindow: function() {
                this.destroy();
            },
            populate: function( record ) {
                var me = this;
                this.record = record;
                this.setTitle('Download OpenVPN Client'.t() + ' | ' + record.data.name);

                var clients = [{
                  name: 'downloadWindowsInstaller',
                  type: 'exe',
                  message: 'Click here to download this client\'s Windows setup.exe file.'.t()
                },{
                  name: 'downloadUntangleConfigurationFile',
                  type: 'zip',
                  message: 'Click here to download this client\'s configuration zip file for remote Untangle OpenVPN clients or other OS\'s (apple/linux/etc).'.t()
                },{
                  name: 'downloadGenericConfigurationFile',
                  type: 'ovpn',
                  message: 'Click here to download this client\'s configuration as a single ovpn file with all certificates included inline.'.t()
                },{
                  name: 'downloadChromebookConfigurationFile',
                  type: 'onc',
                  message: 'Click here to download this client\'s configuration onc file for Chromebook.'.t()
                }];

                Ext.MessageBox.wait("Building OpenVPN Clients...".t(), "Please Wait".t());
                var builders = [];
                clients.forEach( function(client){
                    client["link"] = me.down('[name="' + client.name + '"]');
                    client["link"].update('Loading...'.t());
                    builders.push( Rpc.asyncPromise( 'rpc.appManager.app(openvpn).getClientDistributionDownloadLink', me.record.data.name, client.type ) );
                });

                Ext.Deferred.sequence(builders)
                .then(function(result){
                    if(Util.isDestroyed(clients)){
                        return;
                    }
                    result.forEach( function(client, index){
                        clients[index].link.update('&bull;&nbsp;<a href="'+client+'" target="_blank">'+clients[index].message + '</a>');
                    });
                },function(ex){
                    Util.handleException(ex);
                });
                Ext.MessageBox.hide();
            }
        });

        this.distributeWindow.show();
        return this.distributeWindow;
    },

    statics:{
        groupRenderer: function(value, meta, record, row, col, store, grid){
            var groupList = this.getViewModel().get('groups');
            var grpname = 'Unknown'.t();
            groupList.each(function(record) { if (record.get('groupId') == value) grpname = record.get('name'); });
            return(grpname);
        }
    }

});
