Ext.define('Ung.config.localdirectory.LocalDirectoryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.localdirectory',

    control: {
        '#': {
            beforerender: 'loadSettings'
        }
    },

    localDirectory: rpc.UvmContext.localDirectory(),

    loadSettings: function () {
        var me = this;
        rpc.localDirectory = rpc.UvmContext.localDirectory();
        Rpc.asyncData('rpc.localDirectory.getUsers')
            .then(function (result) {
                me.getViewModel().set('usersData', result);
            });
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        if (!Util.validateForms(view)) {
            return;
        }


        view.setLoading('Saving ...');
        // used to update all tabs data
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

        Rpc.asyncData('rpc.localDirectory.setUsers', me.getViewModel().get('usersData'))
            .then(function (result) {
                me.getViewModel().set('usersData', result);
                Util.successToast('Local Directory'.t() + ' settings saved!');
            }).always(function () {
                view.setLoading(false);
            });
    }

});
