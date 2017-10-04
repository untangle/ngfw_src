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
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set('settings', result);
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

        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, vm.get('settings'));
    },

    downloadUserApiScript: function(){
        window.open("../userapi/");
    },

    portChanger: function(elem){
        var me = this, v = this.getView(), vm = this.getViewModel();

        console.log(vm);
        console.log(vm.get('record'));

        var secureValue = elem.getValue();
        var currentPortValue = vm.get("settings.activeDirectorySettings.LDAPPort");
        if( secureValue ){
            if( currentPortValue == "389"){
                vm.set("settings.activeDirectorySettings.LDAPPort", "636");
            }
        }else{
            if( currentPortValue == "636"){
                vm.set("settings.activeDirectorySettings.LDAPPort", "389");
            }
        }
    },

    rpc: {},

    getActiveDirectoryManager: function(forceReload) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        if (forceReload || this.rpc.activeDirectoryManager === undefined) {
            try {
                this.rpc.activeDirectoryManager = v.appManager.getActiveDirectoryManager();
            } catch (e) {
                Util.handleException(e);
            }
        }
        return this.rpc.activeDirectoryManager;
    },

    getRadiusManager: function(forceReload) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        if (forceReload || this.rpc.radiusManager === undefined) {
            try {
                this.rpc.radiusManager = v.appManager.getRadiusManager();
            } catch (e) {
                Util.handleException(e);
            }
        }
        return this.rpc.radiusManager;
    },

    closeWindow: function (button) {
        button.up('window').close();
    },


    activeDirectoryUsers: function(domain){
        var me = this, v = this.getView(), vm = this.getViewModel();

        if(typeof(domain) == "object"){
            domain = null;
        }

        Ext.MessageBox.wait( "Obtaining users...".t(), "Active Directory Users".t());
        var dialog = v.add({
            xtype: 'app-directory-connector-activedirectoryusers',
            title: domain ? domain + ' ' + 'Users'.t(): 'All Users'.t()
        });
        this.getActiveDirectoryManager().getUsers( Ext.bind(function( result, exception ) {
            if (exception) { Util.handleException(exception); return; }
            dialog.show();
            dialog.down("[name=mapGrid]").getStore().loadData(result);
            Ext.MessageBox.close();
            dialog.down('ungridstatus').fireEvent('update');
        }, this), domain);

    },

    activeDirectoryGroupMap: function(domain){
        var me = this, v = this.getView(), vm = this.getViewModel();

        if(typeof(domain) == "object"){
            domain = null;
        }

        Ext.MessageBox.wait( "Obtaining user group map...".t(), "Active Directory Users".t());
        var dialog = v.add({
            xtype: 'app-directory-connector-activedirectorygroups',
            title: 'All Groups'.t()
        });
        this.getActiveDirectoryManager().getUserGroupMap( Ext.bind(function(result, exception) {
            if (exception) { Util.handleException(ex); return; }
            dialog.show();
            dialog.down("[name=mapGrid]").getStore().loadData(result);
            Ext.MessageBox.close();
            dialog.down('ungridstatus').fireEvent('update');
        },this), domain);

    },

    activeDirectoryGroupRefreshCache: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        Ext.MessageBox.wait( "Refreshing Group Cache...".t(), "Refresh Group Cache".t());
        v.appManager.refreshGroupCache(Ext.bind(function(result, exception) {
            if (exception) { Util.handleException(ex); return; }
            Ext.MessageBox.close();
        }, this));
    },

    radiusTest: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();

        Ext.MessageBox.wait( "Testing RADIUS...".t(), "RADIUS Test".t());
        var username = v.down('textfield[name=radiusTestUsername]').getValue();
        var password = v.down('textfield[name=radiusTestPassword]').getValue();

        var message = this.getRadiusManager().getRadiusStatusForSettings( Ext.bind(function(result, exception) {
            if (exception) { Util.handleException(ex); return; }
            var message = result.t();
            Ext.MessageBox.alert("RADIUS Test".t(), message);
        }, this), vm.get('settings'), username, password);
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
                var me = this, v = this.getView(), vm = this.getViewModel();
                if(!me || !v.rendered) {
                    return;
                }
                me.refreshGoogleTask.count++;

                if ( me.refreshGoogleTask.count > me.refreshGoogleTask.maxTries ) {
                    me.refreshGoogleTask.stop();
                    return;
                }

                v.appManager.getGoogleManager().isGoogleDriveConnected(Ext.bind(function(result, exception) {
                    if (exception) { Util.handleException(ex); return; }
                    var isConnected = result;

                    v.down('[name=fieldsetDriveEnabled]').setVisible(isConnected);
                    v.down('[name=fieldsetDriveDisabled]').setVisible(!isConnected);

                    if ( isConnected ){
                        this.refreshGoogleTask.stop();
                        return;
                    }
                }, me));

            },this)
        };
    },

    googleDriveConfigure: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        me.refreshGoogleTask.start();
        window.open(v.appManager.getGoogleManager().getAuthorizationUrl(window.location.protocol, window.location.host));
    },

    googleDriveDisconnect: function(){
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.appManager.getGoogleManager().disconnectGoogleDrive();
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
        var me = this, v = this.getView(), vm = this.getViewModel();

        Ext.MessageBox.wait( record.data.domain + "<br/><br/>" + "Testing...".t(), "Active Directory Test".t());
        v.up('[itemId=appCard]').getController().getActiveDirectoryManager().getStatusForSettings( Ext.bind(function(result, exception) {
            if (exception) { Util.handleException(ex); return; }
            Ext.WindowManager.bringToFront(Ext.MessageBox.alert( "Active Directory Test".t(), record.data.domain + "<br/><br/>" + result.t() ));
        }, this), record.data);
    },

    serverUsers: function( element, rowIndex, columnIndex, column, pos, record){
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.up('[itemId=appCard]').getController().activeDirectoryUsers(record.get('domain'));
    },

    serverGroupMap: function( element, rowIndex, columnIndex, column, pos, record){
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.up('[itemId=appCard]').getController().activeDirectoryGroupMap(record.get('domain'));
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
        var me = this, v = this.getView(), vm = this.getViewModel();

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
        });
        v.close();
    },

    portChanger: function(element){
        this.getView().up('grid').getController().portChanger(element, this.getViewModel().get('record') );
    },

    serverTest: function(element){
        this.getView().up('grid').getController().serverTest( element, 0, 0, null, null, this.getViewModel().get('record'));
    }
});
