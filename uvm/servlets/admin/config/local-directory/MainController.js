Ext.define('Ung.config.local-directory.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-local-directory',

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

                // set the local record fields we use to deal with the expiration time and add vs edit logic
                for(var i = 0 ; i < result.list.length ; i++) {
                    result.list[i].localEmpty = false;

                    if (result.list[i].expirationTime == 0) {
                        result.list[i].localExpires = new Date();
                        result.list[i].localForever = true;
                    }
                    else {
                        result.list[i].localExpires = new Date(result.list[i].expirationTime);
                        result.list[i].localForever = false;
                    }
                }

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

        var userlist = me.getViewModel().get('usersData');
        var user;

        for(var i = 0 ; i < userlist.list.length ; i++) {
            user = userlist.list[i];

            if(user.password == null) user.password = "";

            // calculate the passwordBase64Hash for any changed passwords and remove cleartext
            if(user.password.length > 0) {
                user.passwordBase64Hash = Util.base64encode(user.password);
                user.password = "";
            }

            // use localForever and localExpires to set the correct expirationTime
            if (user.localForever == true) {
                user.expirationTime = 0;
            } else {
                user.expirationTime = user.localExpires.getTime();
            }
        }

        Rpc.asyncData('rpc.localDirectory.setUsers', userlist)
            .then(function (result) {
                Util.successToast('Local Directory'.t() + ' settings saved!');
            }).always(function () {
                me.loadSettings();
                view.setLoading(false);
                Ext.fireEvent('resetfields', view);
            });
    }

});
