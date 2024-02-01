Ext.define('Ung.config.email.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.email',

    control: {
        '#': { afterrender: 'loadSettings'}
    },

    originalMailSender: null,

    loadSettings: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        var sequence = [
            Rpc.asyncPromise('rpc.UvmContext.mailSender.getSettings')
        ];
        var dataNames = [
            'mailSender'
        ];

        if(Rpc.directData('rpc.appManager.app', 'smtp')){
            vm.set('smtp', true);
            sequence.push(
                Rpc.asyncPromise('rpc.appManager.app("smtp").getSmtpSettings'),
                Rpc.asyncPromise('rpc.appManager.app("smtp").getSafelistAdminView.getSafelistContents', 'GLOBAL'),
                Rpc.asyncPromise('rpc.appManager.app("smtp").getSafelistAdminView.getUserSafelistCounts'),
                Rpc.asyncPromise('rpc.appManager.app("smtp").getQuarantineMaintenenceView.listInboxes'),
                Rpc.asyncPromise('rpc.appManager.app("smtp").getQuarantineMaintenenceView.getInboxesTotalSize'),
                Rpc.directPromise('rpc.companyName')
            );

            dataNames.push(
                'smtpSettings',
                'globalSafeList',
                'userSafeList',
                'inboxesList',
                'inboxesTotalSize',
                'companyName'
            );
        }else{
            vm.set('smtp', false);
        }

        v.setLoading(true);
        Ext.Deferred.sequence(sequence, this).then(function(result) {
            if(Util.isDestroyed(me, v, vm)){
                return;
            }
            dataNames.forEach(function(name, index){
                vm.set(name, result[index]);
            });

            me.originalMailSender = Ext.clone(result[0]);
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },


    saveSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(this.getView())) {
            return null;
         }

        var fromAddressCmp = v.down('textfield[name="FromAddress"]');
        if (fromAddressCmp.rendered && !fromAddressCmp.isValid()) {
            Ung.app.redirectTo('#config/email/outgoing-server');
            Ext.MessageBox.alert('Warning'.t(), 'A From Address must be specified.'.t());
            fromAddressCmp.focus(true);
            return null;
        }

        v.setLoading(true);

        v.query('ungrid').forEach(function (grid) {
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

        var sequence = [
            Rpc.asyncPromise('rpc.UvmContext.mailSender.setSettings', vm.get('mailSender'))
        ];

        if(Rpc.directData('rpc.appManager.app', 'smtp')){
            sequence.push(Rpc.asyncPromise('rpc.appManager.app("smtp").setSmtpSettingsWithoutSafelists', vm.get('smtpSettings')));
            sequence.push(Rpc.asyncPromise('rpc.appManager.app("smtp").getSafelistAdminView.replaceSafelist', 'GLOBAL', vm.get('globalSafeList')));
        }
        Ext.Deferred.sequence(sequence, this).then(function(result) {
            if(Util.isDestroyed(me, v, vm)){
                return;
            }
            me.loadSettings();
            Util.successToast('Email settings saved!');
            Ext.fireEvent('resetfields', v);
        }, function (ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });

        // testEmail needs to handle this as a promise.
        return Ext.Deferred.resolved();
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
                    if(Util.isDestroyed(me)){
                        return;
                    }
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
            accounts.push(record.get('emailAddress'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.appManager.app("smtp").getSafelistAdminView.deleteSafelists', accounts)
        .then(function() {
            if(Util.isDestroyed(me)){
                return;
            }
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
        Rpc.asyncData('rpc.appManager.app("smtp").getQuarantineMaintenenceView.deleteInboxes', accounts)
        .then(function() {
            if(Util.isDestroyed(me)){
                return;
            }
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
        Rpc.asyncData('rpc.appManager.app("smtp").getQuarantineMaintenenceView.rescueInboxes', accounts)
        .then(function() {
            if(Util.isDestroyed(me)){
                return;
            }
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
            width: Ext.getBody().getViewSize().width - 20,
            maxHeight: Ext.getBody().getViewSize().height - 20,
            modal: true,
            layout: 'fit',
            items: [{
                xtype: 'ungrid',
                reference: 'mailsGrid',
                account: record.get('address'),
                border: false,
                bodyBorder: false,
                emptyText: 'No emails'.t(),
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
                    width: Renderer.timestampWidth,
                    dataIndex: 'quarantinedDate',
                    renderer: Renderer.timestamp
                }, {
                    header: 'Sender'.t(),
                    width: Renderer.emailWidth,
                    dataIndex: 'sender',
                    filter: { type: 'string' }
                }, {
                    header: 'Subject'.t(),
                    width: Renderer.messageWidth,
                    flex: 1,
                    dataIndex: 'subject',
                    filter: { type: 'string' }
                }, {
                    header: 'Size (KB)'.t(),
                    width: Renderer.sizeWidth,
                    dataIndex: 'size',
                    renderer: Renderer.datasize,
                    filter: { type: 'numeric' }
                }, {
                    header: 'Category'.t(),
                    width: Renderer.idWidth,
                    dataIndex: 'quarantineCategory',
                    filter: { type: 'string' }
                }, {
                    header: 'Detail'.t(),
                    width: Renderer.sizeWidth,
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
                    handler: 'externalAction',
                    action: 'purgeMails',
                    disabled: true,
                    bind: {
                        disabled: '{!mailsGrid.selection}'
                    }
                }, {
                    text: 'Release Selected'.t(),
                    iconCls: 'fa fa-circle fa-green',
                    handler: 'externalAction',
                    action: 'releaseMails',
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
            Rpc.asyncPromise ('rpc.appManager.app("smtp").getQuarantineMaintenenceView.listInboxes'),
            Rpc.asyncPromise('rpc.appManager.app("smtp").getQuarantineMaintenenceView.getInboxesTotalSize')
        ], this).then(function(result) {
            if(Util.isDestroyed(vm)){
                return;
            }
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
        Rpc.asyncData('rpc.appManager.app("smtp").getQuarantineMaintenenceView.getInboxRecords', grid.account)
        .then(function (result) {
            if(Util.isDestroyed(grid)){
                return;
            }
            if (result && result.list) {
                for (var i=0; i< result.list.length; i++) {
                    /* copy values from mailSummary to object */
                    result.list[i].subject = result.list[i].mailSummary.subject;
                    result.list[i].sender = result.list[i].mailSummary.sender;
                    result.list[i].quarantinedDate = result.list[i].internDate;
                    result.list[i].quarantineCategory = result.list[i].mailSummary.quarantineCategory;
                    result.list[i].quarantineDetail = result.list[i].mailSummary.quarantineDetail;
                    result.list[i].size = result.list[i].mailSummary.quarantineSize;
                }
            }
            grid.getStore().loadData(result.list);
        }).always(function () {
            if(Util.isDestroyed(me)){
                return;
            }
            me.dialog.setLoading(false);
        });
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
        Rpc.asyncData('rpc.appManager.app("smtp").getQuarantineMaintenenceView.purge', grid.account, emails)
        .then(function() {
            if(Util.isDestroyed(me)){
                return;
            }
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
        Rpc.asyncData('rpc.appManager.app("smtp").getQuarantineMaintenenceView.rescue', grid.account, emails)
        .then(function() {
            if(Util.isDestroyed(me)){
                return;
            }
            me.getAccountEmails();
        }).always(function () {
            Ext.MessageBox.hide();
        });
    }
});
