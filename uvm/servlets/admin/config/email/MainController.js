Ext.define('Ung.config.email.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.email',

    control: {
        '#': { afterrender: 'loadSettings' }
        // '#quarantine': { beforerender: 'loadQuarantine' }
    },

    // mailSender: rpc.UvmContext.mailSender(),
    originalMailSender: null,

    // smtpApp: rpc.appManager.app('smtp'),
    // safelistAdminView: null,

    loadSettings: function (view) {
        var vm = this.getViewModel(), me = this;
        rpc.mailSender = rpc.UvmContext.mailSender();
        rpc.smtpSettings = rpc.appManager.app('smtp');
        rpc.safelistAdminView = rpc.smtpSettings.getSafelistAdminView();
        rpc.quarantineMaintenenceView = rpc.smtpSettings.getQuarantineMaintenenceView();

        view.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise ('rpc.mailSender.getSettings'),
            Rpc.asyncPromise ('rpc.smtpSettings.getSmtpSettings'),
            Rpc.asyncPromise ('rpc.safelistAdminView.getSafelistContents', 'GLOBAL'),
            Rpc.directPromise('rpc.safelistAdminView.getUserSafelistCounts'),
            Rpc.asyncPromise ('rpc.quarantineMaintenenceView.listInboxes'),
            Rpc.directPromise('rpc.quarantineMaintenenceView.getInboxesTotalSize')
        ], this).then(function(result) {
            vm.set({
                mailSender: result[0],
                smtpSettings: result[1],
                globalSafeList: result[2],
                userSafeList: result[3],
                inboxesList: result[4],
                inboxesTotalSize: result[5]
            });
            me.originalMailSender = Ext.clone(result[0]);
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    },


    // using promise because of the testEmail need
    saveSettings: function () {
        var deferred = new Ext.Deferred();

        if (!Util.validateForms(this.getView())) {
            return;
        }

        var me = this, view = this.getView(), vm = this.getViewModel();
        view.setLoading('Saving ...');


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

                if (grid.getItemId() === 'safeListStore') { // this needs to be transformed back to array
                    var emails = [];
                    store.each(function(record) {
                        emails.push(record.get('emailAddress'));
                    });
                    vm.set('globalSafeList', emails);
                } else {
                    vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                }
                // store.commitChanges();
            }
        });


        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.mailSender.setSettings', me.getViewModel().get('mailSender')),
            Rpc.asyncPromise('rpc.smtpSettings.setSmtpSettingsWithoutSafelists', vm.get('smtpSettings')),
            Rpc.asyncPromise('rpc.safelistAdminView.replaceSafelist', 'GLOBAL', vm.get('globalSafeList'))
        ], this)
        .then(function() {
            Util.successToast('Email'.t() + ' settings saved!');
            // me.loadSettings();
            deferred.resolve();
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function () {
            view.setLoading(false);
        });
        return deferred.promise;
    },

    testEmail: function () {
        var me = this, vm = this.getViewModel(),
            modifiedVal = Ext.encode(vm.get('mailSender')),
            originalVal = Ext.encode(me.originalMailSender);

        if (originalVal !== modifiedVal) {
            Ext.Msg.show({
                title: 'Save Changes?'.t(),
                msg: Ext.String.format('Your current settings have not been saved yet.{0}Would you like to save your settings before executing the test?'.t(), '<br />'),
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function(btnId) {
                    if (btnId === 'yes') {
                        me.saveSettings().then(function () {
                            Ext.create('Ung.config.email.EmailTest');
                        });
                    }
                    if (btnId === 'no') {
                        Ext.create('Ung.config.email.EmailTest');
                    }
                },
                animEl: 'elId',
                icon: Ext.MessageBox.QUESTION
            });
        } else {
            Ext.create('Ung.config.email.EmailTest');
        }
    },

    purgeUserSafeList: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.safelistAdminView.deleteSafelists', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },


    purgeInboxes: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.deleteInboxes', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    releaseInboxes: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Releasing...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.rescueInboxes', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }
});
