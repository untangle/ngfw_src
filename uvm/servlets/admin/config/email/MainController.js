Ext.define('Ung.config.email.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.email',

    control: {
        '#': { afterrender: 'loadSettings', activate: 'loadSettings' }
    },

    // mailSender: rpc.UvmContext.mailSender(),
    originalMailSender: null,

    // smtpApp: rpc.appManager.app('smtp'),
    // safelistAdminView: null,

    loadSettings: function () {
        var vm = this.getViewModel(), me = this;
        rpc.mailSender = rpc.UvmContext.mailSender();
        rpc.smtpApp = rpc.appManager.app('smtp');

        if (!rpc.smtpApp) {
            me.getView().setLoading(true);
            Rpc.asyncData('rpc.mailSender.getSettings')
                .then(function (result) {
                    me.getView().setLoading(false);
                    vm.set({
                        mailSender: result,
                        smtp: false
                    });
                    me.originalMailSender = Ext.clone(result);
                }, function (ex) {
                    console.error(ex);
                    Util.handleException(ex);
                }).always(function() {
                    me.getView().setLoading(false);
                });
            return;
        }

        rpc.safelistAdminView = rpc.smtpApp.getSafelistAdminView();
        rpc.quarantineMaintenenceView = rpc.smtpApp.getQuarantineMaintenenceView();

        me.getView().setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise ('rpc.mailSender.getSettings'),
            Rpc.asyncPromise ('rpc.smtpApp.getSmtpSettings'),
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
                inboxesTotalSize: result[5],
                smtp: true
            });
            me.originalMailSender = Ext.clone(result[0]);
        }, function(ex) {
            console.error(ex);
            Util.handleException(ex);
        }).always(function() {
            me.getView().setLoading(false);
        });
    },


    // using promise because of the testEmail need
    saveSettings: function () {
        var deferred = new Ext.Deferred();
        var me = this, view = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(this.getView())) {
            return null;
         }

        var fromAddressCmp = view.down('textfield[name="FromAddress"]');
        if (fromAddressCmp.rendered && !fromAddressCmp.isValid()) {
            Ung.app.redirectTo('#config/email/outgoing-server');
            Ext.MessageBox.alert('Warning'.t(), 'A From Address must be specified.'.t());
            fromAddressCmp.focus(true);
            return null;
        }

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

        var promises =  [
            Rpc.asyncPromise('rpc.mailSender.setSettings', me.getViewModel().get('mailSender')),
        ];
        if (rpc.smtpApp) {
            promises.push(Rpc.asyncPromise('rpc.smtpApp.setSmtpSettingsWithoutSafelists', vm.get('smtpSettings')));
            promises.push(Rpc.asyncPromise('rpc.safelistAdminView.replaceSafelist', 'GLOBAL', vm.get('globalSafeList')));
        }
        Ext.Deferred.sequence(promises, this)
        .then(function() {
            Util.successToast('Email'.t() + ' settings saved!');
            // me.loadSettings();
            deferred.resolve();
        }, function(ex) {
            console.log(ex);
            Util.handleException(ex);
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
                me.refreshUserQuarantines();
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
                me.refreshUserQuarantines();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    showQuarantineDetails: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        me.dialog = me.getView().add({
            xtype: 'window',
            title: 'Email Quarantine Details for:'.t() + ' ' + record.get('address'),
            width: 1200,
            height: 600,
            modal: true,
            layout: 'fit',
            items: [{
                xtype: 'grid',
                reference: 'mailsGrid',
                account: record.get('address'),
                border: false,
                bodyBorder: false,
                viewConfig: {
                    enableTextSelection: true,
                    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Emails!</p>',
                },
                selModel: {
                    selType: 'checkboxmodel'
                },
                store: { data: [] },
                plugins: ['gridfilters'],
                sortField: 'quarantinedDate',
                fields: [{
                    name: 'mailID'
                }, {
                    name: 'quarantinedDate',
                    mapping: 'internDate'
                }, {
                    name: 'size'
                }, {
                    name: 'sender'
                }, {
                    name: 'subject'
                }, {
                    name: 'quarantineCategory'
                }, {
                    name: 'quarantineDetail'
                }],
                columns: [{
                    header: 'Date'.t(),
                    width: 140,
                    dataIndex: 'quarantinedDate',
                    renderer: function(value) {
                        if (!value) { return ''; }
                        var date = new Date();
                        date.setTime(value);
                        return Util.timestampFormat(value);
                    }
                }, {
                    header: 'Sender'.t(),
                    width: 180,
                    dataIndex: 'sender',
                    filter: { type: 'string' }
                }, {
                    header: 'Subject'.t(),
                    width: 150,
                    flex: 1,
                    dataIndex: 'subject',
                    filter: { type: 'string' }
                }, {
                    header: 'Size (KB)'.t(),
                    width: 85,
                    dataIndex: 'size',
                    renderer: function(value) {
                        return (value/1024.0).toFixed(3);
                    },
                    filter: { type: 'numeric' }
                }, {
                    header: 'Category'.t(),
                    width: 85,
                    dataIndex: 'quarantineCategory',
                    filter: { type: 'string' }
                }, {
                    header: 'Detail'.t(),
                    width: 85,
                    dataIndex: 'quarantineDetail',
                    renderer: function(value) {
                        var detail = value;
                        if (isNaN(parseFloat(detail))) {
                            if (detail === 'Message determined to be a fraud attempt') {
                                return 'Phish'.t();
                            }
                        } else {
                            return parseFloat(detail).toFixed(3);
                        }
                        return detail;
                    },
                    filter: { type: 'numeric' }
                }],
                tbar: [{
                    text: 'Purge Selected'.t(),
                    iconCls: 'fa fa-circle fa-red',
                    handler: 'purgeMails',
                    disabled: true,
                    bind: {
                        disabled: '{!mailsGrid.selection}'
                    }
                }, {
                    text: 'Release Selected'.t(),
                    iconCls: 'fa fa-circle fa-green',
                    handler: 'releaseMails',
                    disabled: true,
                    bind: {
                        disabled: '{!mailsGrid.selection}'
                    }
                }],
            }],
            fbar: [{
                text: 'Done'.t(),
                iconCls: 'fa fa-check',
                handler: function (btn) {
                    btn.up('window').close();
                }
            }],
            listeners: {
                afterrender: function (win) {
                    me.getAccountEmails();
                },
                close: function () {
                    me.refreshUserQuarantines();
                }
            }
        });
        me.dialog.show();
    },

    refreshUserQuarantines: function () {
        var me = this, vm = me.getViewModel();
        me.lookup('inboxesGrid').setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise ('rpc.quarantineMaintenenceView.listInboxes'),
            Rpc.directPromise('rpc.quarantineMaintenenceView.getInboxesTotalSize')
        ], this).then(function(result) {
            vm.set({
                inboxesList: result[0],
                inboxesTotalSize: result[1]
            });
        }, function(ex) {
            console.error(ex);
            Util.handleException(ex);
        }).always(function() {
            me.lookup('inboxesGrid').setLoading(false);
        });
    },


    getAccountEmails: function () {
        var me = this;
        if (!me.dialog) {
            return;
        }

        var grid = me.dialog.down('grid');
        me.dialog.setLoading(true);
        Rpc.asyncData('rpc.quarantineMaintenenceView.getInboxRecords', grid.account)
            .then(function (result) {
                if (result && result.list) {
                    for (var i=0; i< result.list.length; i++) {
                        /* copy values from mailSummary to object */
                        result.list[i].subject = result.list[i].mailSummary.subject;
                        result.list[i].sender = result.list[i].mailSummary.sender;
                        result.list[i].quarantineCategory = result.list[i].mailSummary.quarantineCategory;
                        result.list[i].quarantineDetail = result.list[i].mailSummary.quarantineDetail;
                        result.list[i].size = result.list[i].mailSummary.quarantineSize;
                    }
                }
                grid.getStore().loadData(result.list);
            }, function (ex) { console.log(ex); })
            .always(function () { me.dialog.setLoading(false); });
    },

    purgeMails: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            emails = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            emails.push(record.get('mailID'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.purge', grid.account, emails)
            .then(function() {
                me.getAccountEmails();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    releaseMails: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            emails = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            emails.push(record.get('mailID'));
        });

        Ext.MessageBox.wait('Releasing...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.rescue', grid.account, emails)
            .then(function() {
                me.getAccountEmails();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }
});
