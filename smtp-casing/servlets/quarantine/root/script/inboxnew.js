Ext.define('Ung.view.Main', {
    extend: 'Ext.tab.Panel',
    controller: 'main',
    viewModel: {
        formulas: {
            title: function(get) { return Ext.String.format('Quarantine Digest for: {0}'.t(), '<strong>' + get('currentAddress') + '</strong>'); }
        }
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        border: false,
        style: {
            background: '#1b1e26'
        },
        items: [{
            xtype: 'component',
            width: 80,
            html: '<img src="' + '/images/BrandingLogo.png" style="display: block; height: 40px; margin: 0 auto;"/>',
        }, {
            xtype: 'component',
            bind: { html: '{title}' },
            style: { color: '#CCC', fontSize: '16px' }
        }]
    }],
    items: [
        { xtype: 'messages' },
        { xtype: 'safelist' },
        { xtype: 'forwardreceive' }
    ],
    listeners: {
        afterrender: 'onAfterRender'
    }
});

Ext.define('Ung.view.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main',

    onAfterRender: function (view) {
        this.token = Ung.app.conf.token;
        this.refreshQuarantineGrid();
    },

    refreshQuarantineGrid: function () {
        var me = this, vm = me.getViewModel();
        rpc.getInboxRecords(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            var mails = [];
            Ext.Array.each(result, function (mail) {
                mail.time = mail.internDate; // preserve time in a different prop
                Ext.apply(mail, mail.mailSummary);
                mails.push(mail);
            });
            vm.set('mails', mails);
        }, me.token);

        // for testing
        var mails = [], getTestRecord = function(index) {
            return {
                recipients : 'recipients' + index,
                sender : 'sender' + (index % 10) + '@test.com',
                mailID : 'mailID' + index,
                internDate : 10000 * index,
                quarantineSize : 500 * index,
                attachmentCount : 1000 - index,
                quarantineDetail : parseFloat(index) / 100,
                truncatedSubject : 'subject spam' + index
            };
        };
        var length = Math.floor((Math.random() * 50));
        for (var i = parseInt(length / 3, 10); i < length; i++) {
            mails.push(getTestRecord(i));
        }
        vm.set('mails', mails);
    },

    // Quarantined Messages actions
    releaseMessages: function (btn) {
        var me = this, mids = [];
        Ext.Array.each(btn.up('grid').getSelection(), function (rec) {
            mids.push(rec.get('mailID'));
        });
        rpc.releaseMessages(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            if (result.releaseCount > 0) {
                Util.successToast(Ext.String.format('Released {0} Messages'.t(), result.releaseCount));
            }
        }, me.token, mids);
    },

    releaseAndSafeList: function (btn) {
        var me = this, vm = me.getViewModel(), addresses = [];
        Ext.Array.each(btn.up('grid').getSelection(), function (rec) {
            addresses.push(rec.get('sender'));
        });
        rpc.safelist(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            if (result.safelistCount > 0) {
                Util.successToast(Ext.String.format('Safelisted {0} Addresses'.t(), result.safelistCount));
            }
            // refresh safelist grid
            if (result.safelist) {
                Ext.Array.each(result.safelist, function (sl) {
                    sl = [ sl ];
                });
                vm.set('safelistData', result.safelist);
            }
        }, me.token, addresses);
    },

    purgeMessages: function (btn) {
        var me = this, mids = [];
        Ext.Array.each(btn.up('grid').getSelection(), function (rec) {
            mids.push(rec.get('mailID'));
        });
        rpc.purgeMessages(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            if (result.purgeCount > 0) {
                Util.successToast(Ext.String.format('Deleted {0} Messages'.t(), result.purgeCount));
            }
        }, me.token, mids);
    },

    filterMessages: function (field, value) {
        if (!value) {
            field.getTrigger('clear').hide();
        } else {
            field.getTrigger('clear').show();
        }
        // to do the rest of the filtering
    },


    // Safe List actions
    addSafeListAddress: function(btn) {
        var me = this, vm = me.getViewModel(), grid = btn.up('grid');
        var addWin = grid.add({
            xtype: 'window',
            modal: 'true',
            width: 300,
            height: 170,
            title: 'Add an Email Address to Safelist'.t(),
            layout: 'fit',
            items: [{
                xtype: 'form',
                border: false,
                layout: 'anchor',
                bodyPadding: 10,
                items: [{
                    xtype: 'textfield',
                    fieldLabel: 'Email Address'.t(),
                    name: 'email',
                    anchor: '100%',
                    labelAlign: 'top',
                    allowBlank: false,
                    blankText: 'Please enter a valid email address'.t(),
                    enableKeyEvents: true,
                    vtype: 'email',
                    validateOnChange: false,
                    validateOnBlur: false,
                    msgTarget: 'under',
                    listeners: {
                        // specialkey: 'onEnter'
                    }
                }]
            }],
            buttons: [{
                text: 'Save'.t(),
                iconCls: 'fa fa-floppy-o',
                handler: function (btn) {
                    var emailField = btn.up('window').down('textfield');
                    if (!emailField.isValid()) { return; }

                    rpc.safelist(function (result, ex) {
                        if (ex) { Util.handleException(ex); btn.up('window').close(); return; }
                        if (result.safelistCount > 0) {
                            Util.successToast(Ext.String.format('Safelisted {0} Addresses'.t(), result.safelistCount));
                        }
                        // refresh safelist grid
                        if (result.safelist) {
                            Ext.Array.each(result.safelist, function (sl) {
                                sl = [ sl ];
                            });
                            vm.set('safelistData', result.safelist);
                        }
                        btn.up('window').close();
                    }, me.token, [emailField.getValue()]);
                }
            }, {
                text: 'Cancel'.t(),
                iconCls: 'fa fa-ban',
                handler: function(btn) {
                    btn.up('window').close();
                }
            }]
        });
        addWin.show();
    },

    deleteSafeListAddresses: function (btn) {
        var me = this, records = btn.up('grid').getSelection(), addresses = [];
        Ext.Array.each(records, function(record) {
            addresses.push(record.get('emailAddress'));
        });
        rpc.deleteAddressesFromSafelist(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            Util.successToast(Ext.String.format('Deleted {0} addresses'.t(), result.safelistCount));
            // drop removed records from grid
            Ext.Array.each(records, function (record) {
                record.drop();
            });
        }, me.token, addresses);
    },

    // Forward / Receive actions
    setForwardAddress: function (btn) {
        var me = this, emailField = btn.up('panel').down('textfield');
        if (!emailField.isValid()) { return; }
        rpc.setRemap(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            Ung.app.conf.forwardAddress = emailField.getValue();
            Util.successToast('Updated forward address'.t());
        }, me.token, emailField.getValue());
    },

    deleteForwardAddress: function(btn) {
        // delete forward address if exists
        var me = this, forwardAddress = Ung.app.conf.forwardAddress;
        if (forwardAddress) {
            rpc.deleteRemap(function(result, ex) {
                if (ex) { Util.handleException(ex); return; }
                Ung.app.conf.forwardAddress = '';
                this.getViewModel().set('forwardAddress', '');
                Util.successToast('Deleted forward address'.t());
            }, me.token, forwardAddress);
        }
    },

    deleteReceived: function(btn) {
        var me = this, records = btn.up('grid').getSelection(), addresses = [];
        Ext.Array.each(records, function(record) {
            addresses.push(record.get('emailAddress'));
        });
        rpc.deleteRemaps(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            Util.successToast(Ext.String.format('Deleted {0} Remaps'.t(), addresses.length));
            // drop removed records from grid
            Ext.Array.each(records, function (record) {
                record.drop();
            });
        }, me.token, addresses);
    }

});

/**
 * Quarantined Messages Tab
 */
Ext.define('Ung.view.Messages', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.messages',
    reference: 'messagesGrid',
    title: 'Quarantined Messages'.t(),
    viewModel: {
        formulas: {
            warning: function (get) {
                return Ext.String.format('The messages below were quarantined and will be deleted after {0} days.'.t(), get('quarantineDays'));
            },
        }
    },
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        style: { background: '#FEFEDD' },
        items: [{
            xtype: 'component',
            padding: '10 5',
            bind: { html: '<i class="fa fa-exclamation-triangle fa-lg" style="color: orange;"></i> {warning}' }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        ui: 'footer',
        style: { background: '#EEE' },
        items: [{
            text: 'Release to Inbox'.t(),
            iconCls: 'fa fa-inbox',
            disabled: true,
            bind: { disabled: '{!messagesGrid.selection}' },
            handler: 'releaseMessages'
        }, {
            text: 'Release to Inbox & Add Senders to Safe List'.t(),
            iconCls: 'fa fa-user',
            disabled: true,
            bind: { disabled: '{!messagesGrid.selection}' },
            handler: 'releaseAndSafeList'
        }, {
            text: 'Delete'.t(),
            iconCls: 'fa fa-trash',
            disabled: true,
            bind: { disabled: '{!messagesGrid.selection}' },
            handler: 'purgeMessages'
        }, '->', 'Filter:'.t(), {
            xtype: 'textfield',
            enableKeyEvents: true,
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
                    handler: function (field) {
                        field.setValue('');
                    }
                }
            },
            listeners: {
                change: 'filterMessages',
                buffer: 100
            }
        }, {
            xtype: 'checkbox',
            boxLabel: 'Case sensitive'.t()
        }]
    }],
    bind: {
        store: {
            data: '{mails}',
            sortOnLoad: true,
            sorters: { property : 'internDate', direction : 'DESC' },
            fields: [
                { name: 'recipients' },
                { name: 'mailID' },
                { name: 'attachmentCount' },
                { name: 'truncatedSender' },
                { name: 'truncatedSubject' },
                { name: 'subject' },
                { name: 'quarantineCategory' },
                { name: 'quarantineDetail', sortType : 'asFloat' },
                { name: 'quarantineSize' },
                { name : 'sender', sortType : Ext.data.SortTypes.asUCString },
                {
                    name: 'internDate',
                    convert: function(value) {
                        var date = new Date(), d, t;
                        date.setTime(value);
                        d = Ext.util.Format.date(date, 'm/d/Y');
                        t = Ext.util.Format.date(date, 'g:i a');
                        return d + ' ' + t;
                    }
                }
            ]
        }
    },

    enableColumnHide : false,
    enableColumnMove : false,
    plugins : [ 'gridfilters' ],
    selModel: { selType: 'checkboxmodel' },
    columns: [{
        header: 'From'.t(),
        dataIndex: 'sender',
        width: 250,
        filter: { type: 'string' }
    }, {
        header: 'Attachments'.t(),
        dataIndex: 'attachmentCount',
        width: 90,
        tooltip: 'Number of Attachments in the email.'.t(),
        tooltipType: 'title',
        align: 'center',
        renderer: function(value) {
            return value !== 0 ? value : '';
        },
        filter: { type : 'numeric' }
    }, {
        header: 'Score'.t(),
        dataIndex: 'quarantineDetail',
        width: 65,
        align: 'center',
        filter: { type: 'numeric' }
    }, {
        header: 'Subject'.t(),
        dataIndex: 'truncatedSubject',
        flex: 1,
        width: 250,
        filter: { type : 'string' }
    }, {
        header: 'Date'.t(),
        dataIndex: 'internDate',
        width: 140,
        filter: { type : 'string' },
        sorter: function (rec1, rec2) {
            var t1 = rec1.getData().time, t2 = rec2.getData().time;
            return (t1 > t2) ? 1 : (t1 === t2) ? 0 : -1;
        }
    }, {
        header: 'Size (KB)'.t(),
        dataIndex: 'quarantineSize',
        renderer: function(value) {
            return Math.round(((value + 0.0) / 1024) * 10) / 10;
        },
        width: 70,
        filter: { type : 'numeric' }
    }]
});

/**
 * Safe List Tab
 */
Ext.define('Ung.view.SafeList', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.safelist',
    reference: 'safelistGrid',

    title: 'Safe List'.t(),
    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        style: { background: '#FEFEDD' },
        items: [{
            xtype: 'component',
            padding: '10 5',
            html: '<i class="fa fa-exclamation-triangle fa-lg" style="color: orange;"></i> ' + 'You can use the Safe List to make sure that messages from these senders are never quarantined.'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        ui: 'footer',
        style: { background: '#EEE' },
        items: [{
            text: 'Add'.t(),
            iconCls: 'fa fa-plus-circle',
            handler: 'addSafeListAddress'
        }, {
            text: 'Delete Addresses'.t(),
            iconCls: 'fa fa-trash',
            disabled: true,
            bind: {
                disabled: '{!safelistGrid.selection}'
            },
            handler: 'deleteSafeListAddresses'
        }]
    }],

    enableColumnHide : false,
    enableColumnMove : false,
    plugins : [ 'gridfilters' ],
    selModel: { selType: 'checkboxmodel' },
    columns: [{
        header: 'Email Address'.t(),
        dataIndex: 'emailAddress',
        menuDisabled: true,
        flex: 1
    }],
    bind: {
        store: {
            data: '{safelistData}',
            fields: [{ name: 'emailAddress' }],
            proxy: {
                type: 'memory',
                reader: { type: 'array' }
            }
        }
    }
});

/**
 * Safe List Tab
 */
Ext.define('Ung.view.ForwardReceive', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.forwardreceive',
    title: 'Forward or Receive Quarantines'.t(),
    layout: 'border',
    items: [{
        title: 'Forward Quarantined Messages To:'.t(),
        region: 'north',
        border: false,
        height: 'auto',
        layout: { type: 'hbox' },
        bodyPadding: '20 0',
        defaults: { margin: '0 5' },
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Email Address'.t(),
            labelAlign: 'right',
            width: 400,
            bind: '{forwardAddress}',
            allowBlank: false,
            blankText: 'Please enter a valid email address'.t(),
            enableKeyEvents: true,
            vtype: 'email',
            validateOnChange: false,
            validateOnBlur: false,
            msgTarget: 'under'
        }, {
            xtype: 'button',
            text: 'Set forward address'.t(),
            iconCls: 'fa fa-floppy-o',
            handler: 'setForwardAddress'
        }, {
            xtype: 'button',
            text: 'Delete forward address'.t(),
            iconCls: 'fa fa-trash',
            handler: 'deleteForwardAddress'
        }]
    }, {
        xtype: 'grid',
        reference: 'receivedGrid',
        title: 'Received Quarantined Messages From:'.t(),
        region: 'center',
        flex: 1,
        enableColumnHide : false,
        enableColumnMove : false,
        selModel: { selType: 'checkboxmodel' },
        columns: [{
            header: 'Email Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1,
            menuDisabled: true
        }],
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            ui: 'footer',
            style: { background: '#EEE' },
            items: [{
                text: 'Delete Addresses'.t(),
                iconCls: 'fa fa-trash',
                disabled: true,
                bind: { disabled: '{!receivedGrid.selection}' },
                handler: 'deleteReceived'
            }]
        }],
    }]
});

Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',

    // stores: [
    //     'Categories',
    //     'Reports',
    //     'ReportsTree'
    // ],

    // refs: {
    //     reportsView: '#reports',
    // },
    // config: {
    //     routes: {
    //         '': 'onMain',
    //         ':category': 'onMain',
    //         ':category/:entry': 'onMain'
    //     }
    // },

    // onMain: function (categoryName, reportName) {
    //     var reportsVm = this.getReportsView().getViewModel();
    //     var hash = ''; // used to handle reports tree selection

    //     if (categoryName) {
    //         hash += categoryName;
    //     }
    //     if (reportName) {
    //         hash += '/' + reportName;
    //     }
    //     reportsVm.set('hash', hash);
    // }
});

Ext.define('Ung.Inbox', {
    extend: 'Ext.app.Application',
    name: 'Ung',
    namespace: 'Ung',
    controllers: ['Global'],
    defaultToken : '',
    mainView: 'Ung.view.Main',
    launch: function () {
        // add initial confs in main viewmodel
        this.getMainView().getViewModel().set(this.conf);
        document.title = Ext.String.format('{0} | Quarantine Digest for: {1}'.t(), this.conf.companyName, this.conf.currentAddress);
    }
});
