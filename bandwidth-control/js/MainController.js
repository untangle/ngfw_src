Ext.define('Ung.apps.bandwidthcontrol.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-bandwidth-control',

    control: {
        '#': {
            afterrender: 'afterRender'
        }
    },

    afterRender: function () {
        this.getSettings();
    },

    // use a callback function needed for config wizard
    getSettings: function (cb) {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.directPromise('rpc.networkManager.getNetworkSettings.qosSettings.qosEnabled')
        ],this).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set({
                settings: result[0],
                isConfigured: result[0].configured,
                qosEnabled: result[1]
            });
            if (cb) {
                cb(vm.get('isConfigured'));
            }

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

    runWizard: function (btn) {
        var me = this;
        me.wizard = me.getView().add({
            xtype: 'app-bandwidth-control-wizard',
            appManager: me.getView().appManager,
            listeners: {
                // when wizard is finished, reload settings and try to start the app
                finish: function () {
                    me.getSettings(function (configured) {
                        if (configured && !me.getViewModel().get('state.on')) {
                            me.getView().down('appstate').down('button').click();
                        }
                    });
                }
            }
        });
        me.wizard.show();
    },

    statics: {
        actionRenderer: function (value, metaData, record){
            if (typeof value === 'undefined') {
                return 'Unknown action'.t();
            }
            switch(value.actionType) {
                case 'SET_PRIORITY':
                    var priostr;
                    switch(value.priority) {
                        case 1: priostr = 'Very High'.t(); break;
                        case 2: priostr = 'High'.t(); break;
                        case 3: priostr = 'Medium'.t(); break;
                        case 4: priostr = 'Low'.t(); break;
                        case 5: priostr = 'Limited'.t(); break;
                        case 6: priostr = 'Limited More'.t(); break;
                        case 7: priostr = 'Limited Severely'.t(); break;
                        default: priostr = 'Unknown Priority'.t() + ': ' + value.priority; break;
                    }
                    var result = 'Set Priority'.t() + ' [' + priostr + ']';
                    return result;
                case 'TAG_HOST': return 'Tag Host'.t();
                case 'APPLY_PENALTY_PRIORITY': return 'Apply Penalty Priority'.t(); // DEPRECATED
                case 'GIVE_CLIENT_HOST_QUOTA': return 'Give Client a Quota'.t();
                case 'GIVE_HOST_QUOTA': return 'Give Host a Quota'.t();
                case 'GIVE_USER_QUOTA': return 'Give User a Quota'.t();
            default: return 'Unknown Action'.t() + ': ' + value;
        }
    }
    }
});
