Ext.define('Ung.apps.directory-connector.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-directory-connector',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    refreshGoogleTask: null,

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
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

        var googleDrive = new Ung.cmp.GoogleDrive();
        vm.set( 'googleDriveIsConfigured', googleDrive.isConfigured() );
        vm.set( 'googleDriveConfigure', function(){ googleDrive.configure(vm.get('policyId')); });

        me.googleRefreshTaskBuild();
    },

    setSettings: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);
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

    downloadUserApiScript: function(){
        window.open("../userapi/");
    },

    closeWindow: function (button) {
        button.up('window').close();
    },


    activeDirectoryUsers: function(domain){
        var v = this.getView();

        if(typeof(domain) == "object"){
            domain = null;
        }

        Ext.MessageBox.wait( "Obtaining users...".t(), "Active Directory Users".t());
        var dialog = v.add({
            xtype: 'app-directory-connector-activedirectoryusers',
            title: domain ? domain + ' ' + 'Users'.t(): 'All Users'.t()
        });

        Rpc.asyncData( v.appManager.getActiveDirectoryManager(), 'getUsers', domain)
        .then(function(result){
            if(Util.isDestroyed(v, dialog)){
                return;
            }
            dialog.show();
            dialog.down("[name=mapGrid]").getStore().loadData(result);
            Ext.MessageBox.close();
        }, function(ex){
            Ext.MessageBox.close();
            Util.handleException(ex);
        });

    },

    activeDirectoryGroupMap: function(domain){
        var v = this.getView();

        if(typeof(domain) == "object"){
            domain = null;
        }

        Ext.MessageBox.wait( "Obtaining user group map...".t(), "Active Directory Users".t());
        var dialog = v.add({
            xtype: 'app-directory-connector-activedirectorygroups',
            title: 'All Groups'.t()
        });

        Rpc.asyncData( v.appManager.getActiveDirectoryManager(), 'getUsers', domain)
        .then(function(result){
            if(Util.isDestroyed(v, dialog)){
                return;
            }
            var groups = [];
            result.forEach(function(user){
                var groupIndex = null;
                user["groups"].forEach(function(userGroup){
                    var group = Ext.Array.findBy(groups, function(row, index){
                        if(row['group'] == userGroup){
                            return true;
                        }
                    });
                    if(group == null){
                        groups.push({
                            'group': userGroup,
                            'users': [],
                            'domains': []
                        });
                        group = groups[groups.length - 1];
                    }
                    if(group['users'].indexOf(user['uid']) == -1){
                        group['users'].push(user['uid']);
                    }
                    if(group['domains'].indexOf(user['domain']) == -1){
                        group['domains'].push(user['domain']);
                    }
                });
            });

            dialog.show();
            dialog.down("[name=mapGrid]").getStore().loadData(groups);
            Ext.MessageBox.close();
        }, function(ex){
            Ext.MessageBox.close();
            Util.handleException(ex);
        });

    },

    activeDirectoryGroupRefreshCache: function(){
        Ext.MessageBox.wait( "Refreshing Group Cache...".t(), "Refresh Group Cache".t());
        Rpc.asyncData( this.getView().appManager, 'refreshGroupCache')
        .then(function(result){
            Ext.MessageBox.close();
        }, function(ex){
            Ext.MessageBox.close();
            Util.handleException(ex);
        });
    },

    radiusTest: function(){
        var v = this.getView(), vm = this.getViewModel();

        Ext.MessageBox.wait( "Testing RADIUS...".t(), "RADIUS Test".t());
        var username = v.down('textfield[name=radiusTestUsername]').getValue();
        var password = v.down('textfield[name=radiusTestPassword]').getValue();

        Rpc.asyncData( v.appManager.getRadiusManager(), 'getRadiusStatusForSettings', vm.get('settings'), username, password )
        .then(function(result){
            var message = result.t();
            Ext.MessageBox.alert("RADIUS Test".t(), message);
        }, function(ex){
            Ext.MessageBox.close();
            Util.handleException(ex);
        });
    },

    googleRefreshTaskBuild: function() {
        var me = this;

        if(me.refreshGoogleTask != null){
            return;
        }

        me.refreshGoogleTask = {
            // update interval in millisecond
            updateFrequency: 3000,
            count:0,
            maxTries: 40,
            started: false,
            intervalId: null,
            app: me,
            start: function() {
                this.stop();
                this.count=0;
                this.intervalId = window.setInterval(this.run, this.updateFrequency);
                this.started = true;
            },
            stop: function() {
                if (this.intervalId !== null) {
                    window.clearInterval(this.intervalId);
                    this.intervalId = null;
                }
                this.started = false;
            },
            run: Ext.bind(function () {
                var me = this, v = this.getView();
                if(!me || !v.rendered) {
                    return;
                }
                if(Util.isDestroyed(me, v)){
                    return;
                }
                me.refreshGoogleTask.count++;

                if ( me.refreshGoogleTask.count > me.refreshGoogleTask.maxTries ) {
                    me.refreshGoogleTask.stop();
                    return;
                }

                Rpc.asyncData( v.appManager.getGoogleManager(), 'isGoogleDriveConnected')
                .then(function(result){
                    if(Util.isDestroyed(me, v)){
                        return;
                    }
                    var isConnected = result;

                    v.down('[name=fieldsetDriveEnabled]').setVisible(isConnected);
                    v.down('[name=fieldsetDriveDisabled]').setVisible(!isConnected);

                    if ( isConnected ){
                        me.refreshGoogleTask.stop();
                        return;
                    }
                }, function(ex){
                    Util.handleException(ex);
                });

            },this)
        };
    },

    googleDriveConfigure: function(){
        this.refreshGoogleTask.start();
        window.open(Rpc.directData(this.getView().appManager.getGoogleManager(), 'getAuthorizationUrl', window.location.protocol, window.location.host));
    },

    googleDriveDisconnect: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        Rpc.directData(v.appManager.getGoogleManager(), 'disconnectGoogleDrive');
        me.refreshGoogleTask.run();
        vm.set('settings.googleSettings.authenticationEnabled', false);
    }

});

Ext.define('Ung.apps.directory-connector.ActiveDirectoryServerGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unadserversgrid',

    ouFilterRenderer: function(u,cell,record){
        return record.get('OUFilters').list.join(', ');
    },

    azureChanger: function(elem, rowIndex, checked){
        var record;
        if( typeof(rowIndex) == 'object'){
            record = rowIndex;
        }else{
            record = elem.getView().getRecord(rowIndex);
        }

        var azureValue;
        if(checked === undefined){
            azureValue = elem.getValue();
        }else{
            azureValue = checked;
        }

        if(azureValue){
            record.set('LDAPSecure', true);
            this.portChanger(null, record, true);
        }
    },

    portChanger: function(elem, rowIndex, checked){
        var record;
        if( typeof(rowIndex) == 'object'){
            record = rowIndex;
        }else{
            record = elem.getView().getRecord(rowIndex);
        }

        var secureValue;
        if(checked === undefined){
            secureValue = elem.getValue();
        }else{
            secureValue = checked;
        }

        var currentPortValue = record.get('LDAPPort');
        if( secureValue ){
            if( currentPortValue == "389"){
                record.set('LDAPPort', '636');
            }
        }else{
            if( currentPortValue == "636"){
                record.set('LDAPPort', '389');
            }
        }
    },

    serverTest: function( element, rowIndex, columnIndex, column, pos, record){
        var v = this.getView();

        settings = Ext.clone(record.data);
        var ouFilterGrid = v.query('[itemId=unoufiltergrid]');
        if(ouFilterGrid.length){
            ouFilterGrid.forEach( function( grid ){
                var ouFilters = [];
                grid.getStore().each( function(record){
                    if (record.get('markedForDelete')){
                        return;
                    }
                    ouFilters.push(record.get('field1'));
                });
                settings.OUFilters.list = ouFilters;
                settings.ouFiltersQuery = ouFilters.join('<br>');
            });
        }else{
            settings.OUFilter = record.get('OUFilters');
            settings.ouFiltersQuery = record.get('OUFilters')['list'].join('<br>');
        }

        Ext.create('Ung.apps.directory-connector.cmp.ActiveDirectoryServerTest', {
            settings: settings,
            appManager: v.up('[itemId=appCard]').appManager
        });
    },

    serverUsers: function( element, rowIndex, columnIndex, column, pos, record){
        this.getView().up('[itemId=appCard]').getController().activeDirectoryUsers(record.get('domain'));
    },

    serverGroupMap: function( element, rowIndex, columnIndex, column, pos, record){
        this.getView().up('[itemId=appCard]').getController().activeDirectoryGroupMap(record.get('domain'));
    },

    isServerDisabled: function(table, rowIndex, colIndex, item, record){
        return !record.get('enabled');
    },

});

Ext.define('Ung.apps.directory-connector.cmp.ActiveDirectoryServerTest', {
    extend: 'Ext.window.Window',
    modal: true,

    alias: 'widget.apps.directory-connector.activedirectoryservertest',

    controller: 'app.activedirectoryservertest',

    title: 'Active Directory Test'.t(),
    autoShow: true,

    layout: 'fit',
    plain: false,
    bodyBorder: false,
    border: false,

    items: [{
        xtype: 'form',
        layout: 'anchor',
        items: [{
            xtype: 'fieldset',
            title: 'Settings'.t(),
            items: [{
                xtype: 'displayfield',
                fieldLabel: 'Domain',
                bind:{
                    value: '{settings.domain}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'OU',
                hidden: true,
                bind:{
                    value: '{settings.ouFiltersQuery}',
                    hidden: '{settings.ouFiltersQuery == ""}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Azure',
                bind:{
                    value: '{settings.azure}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Host',
                bind:{
                    value: '{settings.LDAPHost}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Secure',
                bind:{
                    value: '{settings.LDAPSecure}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'User',
                bind:{
                    value: '{settings.superuser}'
                }
            }]
        }, {
            xtype: 'progressbar',
            name: 'checkUpgradesProgressbar',
            bind:{
                visible: '{completed == false}',
                hidden: '{completed == true}'
            }
        }, {
            xtype: 'fieldset',
            title: 'Status'.t(),
            hidden: true,
            bind:{
                hidden: '{completed == false}'
            },
            items: [{
                xtype: 'displayfield',
                fieldLabel: 'Result',
                bind: {
                    value: '<i class="fa {statusIcon} fa-fw" style="color: {statusIconColor};"></i> <b>{statusText}</b>',
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Search Base(s)',
                bind:{
                    value: '{status.searchBases}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Users found',
                hidden: true,
                bind:{
                    value: '{status.userCount}',
                    hidden: '{statusResult == "FAIL"}'
                }
            },{
                xtype: 'displayfield',
                fieldLabel: 'Groups found',
                hidden: true,
                bind:{
                    value: '{status.groupCount}',
                    hidden: '{statusResult == "FAIL"}'
                }
            }]
        }],

        buttons: [{
            text: 'Ok'.t(),
            handler: 'close'
        }]
    }],

    viewModel: {
        data: {
            processingIcon: null,
            processing: false
        },
    }

});

Ext.define('Ung.apps.directory-connector.cmp.ActiveDirectoryServerTestController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app.activedirectoryservertest',

    control: {
        '#': {
            afterrender: 'afterrender',
        }
    },

    afterrender: function(){
        var v = this.getView(), vm = this.getViewModel();

        vm.set('completed', false);

        var progressbar = v.down('progressbar');

        progressbar.wait('Checking...');
        vm.set('settings', v.settings);

        Rpc.asyncData( v.appManager.getActiveDirectoryManager(), 'getStatusForSettings', vm.get('settings'))
        .then(function(status){
            if(Util.isDestroyed(v, vm, progressbar)){
                return;
            }
            progressbar.reset();
            progressbar.updateText('Completed'.t());

            vm.set('completed', true);
            var statusResults = status['status'].split('_');
            vm.set('statusResult', statusResults[0]);
            var statusIcon = 'fa-close';
            var statusIconColor = 'red';
            if(statusResults[0] == 'PASS'){
                statusIcon = 'fa-check';
                statusIconColor = 'green';
            }else if(statusResults[0] == 'WARN'){
                statusIcon = 'fa-exclamation-triangle';
                statusIconColor = '#CCCC00';
            }
            vm.set('statusIcon', statusIcon);
            vm.set('statusIconColor', statusIconColor);

            switch(status['status']){
                case 'WARN_QUERY_NO_USERS':
                    statusText = 'Successfully queried but no users found.'.t();
                    break;
                case 'FAIL_EMPTY_SETTINGS':
                    statusText = 'Empty settings!'.t();
                    break;
                case 'FAIL_QUERY':
                    statusText = 'Query failed!'.t();
                    break;
                case 'FAIL_QUERY_NONE':
                    statusText = 'Query failed, cannot find object defined by domain and OU'.t();
                    break;
                case 'PASS':
                    statusText = 'Success!'.t();
                    break;
            }
            vm.set('statusText', statusText);
            status.searchBases = status.searchBases.join("<br>");
            vm.set('status', status);
        }, function(ex){
            Ext.MessageBox.close();
            Util.handleException(ex);
        });
    },

    close: function () {
        this.getView().close();
    }

});


Ext.define('Ung.apps.directory-connector.cmp.ActiveDirectoryServerRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unactivedirectoryserverrecordeditor',

    controller: 'unactivedirectoryserverrecordeditorcontroller'

});

Ext.define('Ung.apps.directory-connector.cmp.ActiveDirectoryServerRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unactivedirectoryserverrecordeditorcontroller',

    onApply: function () {
        var v = this.getView(), vm = this.getViewModel();

        if (!this.action) {
            for (var fieldName in vm.get('record').modified) {
                v.record.set(fieldName, vm.get('record').get(fieldName));
            }
        }else if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }

        v.query('[itemId=unoufiltergrid]').forEach( function( grid ){
            var ouFiltersData = vm.get('record').get('OUFilters');
            var ouFilters = [];
            grid.getStore().each( function(record){
                if (record.get('markedForDelete')){
                    return;
                }
                ouFilters.push(record.get('field1'));
            });
            ouFiltersData.list = ouFilters;
            vm.get('record').set('OUFilters', ouFiltersData);
            v.up('grid').getView().refresh();
        });
        v.close();
    },

    azureChanger: function(element){
        this.getView().up('grid').getController().azureChanger(element, this.getViewModel().get('record') );
    },

    portChanger: function(element){
        this.getView().up('grid').getController().portChanger(element, this.getViewModel().get('record') );
    },

    serverTest: function(element){
        this.getView().up('grid').getController().serverTest( element, 0, 0, null, null, this.getViewModel().get('record'));
    }
});
