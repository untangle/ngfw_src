Ext.define('Ung.apps.captive-portal.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-captive-portal',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#activeUsers': {
            afterrender: 'getActiveUsers'
        }
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

    getActiveUsers: function (cmp) {
        var vm = this.getViewModel();
        var grid = this.getView().down('#activeUsers');

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getActiveUsers')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }
            vm.set('activeUsers', result.list);

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    previewCaptivePage: function () {
        var vm = this.getViewModel();
        if (!vm.get('state.on')) {
            Ext.MessageBox.alert('Captive Portal is Disabled'.t(),
                'You must turn on the Captive Portal to preview the Captive Page.'.t());
            return;
        }

        var custfile = this.getViewModel().get('settings.customFilename');
        var pagetype = vm.get('settings.pageType');

        if ( (pagetype == 'CUSTOM') && ((custfile == null) || (custfile.length === 0)) ) {
            Ext.MessageBox.alert('Missing Custom Captive Page'.t(),
                'You must upload a custom captive page to use this feature.'.t());
            return;
        }

        window.open('/capture/handler.py/index?appid=' + vm.get('instance.id') , '_blank');
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
            var url = '#config/local-directory';
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
    },

    uploadCustomFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=upload_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
            }
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Upload Success'.t(), action.result.msg);
                this.getViewModel().set('settings.customFilename', action.result.msg);
                this.setSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Upload Failure'.t(), action.result.msg);
            }, this)
        });
    },

    removeCustomFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=remove_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=custom_file]')[0].value;
        if ( file == null || file.length === 0 ) {
            return;
            }
        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Remove Success'.t(), action.result.msg);
                this.getViewModel().set('settings.customFilename', null);
                this.setSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Custom Page Remove Failure'.t(), action.result.msg);
            }, this)
        });
    },

    logoutUser: function(view, row, col, item, e, record) {
        var me = this;
        var vm = this.getViewModel();
        var netaddr = record.get("userAddress");
        var grid = this.getView().down('#activeUsers');

        grid.setLoading('Logging Out User...'.t());
        Rpc.asyncData(this.getView().appManager, 'userAdminLogout', netaddr)
        .then( function(result){
            if(Util.isDestroyed(grid, me, view, vm)){
                return;
            }
            vm.set('activeUsers', result.list);

            grid.setLoading(false);
            setTimeout(function() {
                if(Util.isDestroyed(me, view)){
                    return;
                }
                me.getActiveUsers(view);
            },500);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },
});
