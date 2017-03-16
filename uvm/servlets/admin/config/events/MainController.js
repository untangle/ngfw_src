Ext.define('Ung.config.events.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.events',

    control: {
        '#': {
            afterrender: 'loadEvents',
        },
    },

    loadEvents: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.eventManager.getSettings'),
        ], this).then(function(result) {
            vm.set({
                settings: result[0],
            });
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            v.setLoading(false);
        });
    },

    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();

        if (!Util.validateForms(view)) {
            return;
        }

        view.setLoading(true);

        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });

        Ext.Deferred.sequence([
            // !!! NOT RIGHT YET
            Rpc.asyncPromise('rpc.eventManager.setSettings', vm.get('settings')),
        ], this).then(function() {
            me.loadEvents();
            Util.successToast('Events'.t() + ' settings saved!');
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    }
});
