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
    ]
});

Ext.define('Ung.view.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main',

    // Quarantined Messages actions
    releaseMessages: function (btn) {
        var mids = [];
        Ext.Array.each(btn.up('grid').getSelection(), function (rec) {
            mids.push(rec.get('mailID'));
        });
        Ext.Msg.alert('To Do!', 'Release to Inbox');
        // rpc.releaseMessages(function () {
        // }, Ung.app.conf.token, mids);
    },


    // Safe List actions
    addSafeListAddress: function(btn) {
        var grid = btn.up('grid');
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
                    Ext.Msg.alert('To Do!', 'releaseAndSafelist()');
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
        var records = btn.up('grid').getSelection(), addresses = [];
        Ext.Array.each(records, function(record) {
            addresses.push(record.get('emailAddress'));
        });
        rpc.deleteRemaps(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            var message = Ext.String.format('Deleted {0} Remaps'.t(), addresses.length);
            Util.successToast(message);
            // drop removed records from grid
            Ext.Array.each(records, function (record) {
                record.drop();
            });
        }, Ung.app.conf.token, addresses);
    },

    // Forward / Receive actions
    setForwardAddress: function (btn) {
        var emailField = btn.up('panel').down('textfield');
        if (!emailField.isValid()) { return; }
        rpc.setRemap(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            Ung.app.conf.forwardAddress = emailField.getValue();
            Util.successToast('Updated forward address'.t());
        }, Ung.app.conf.token, emailField.getValue());
    },

    deleteForwardAddress: function(btn) {
        // delete forward address if exists
        var forwardAddress = Ung.app.conf.forwardAddress;
        if (forwardAddress) {
            rpc.deleteRemap(function(result, ex) {
                if (ex) { Util.handleException(ex); return; }
                Ung.app.conf.forwardAddress = '';
                this.getViewModel().set('forwardAddress', '');
                Util.successToast('Deleted forward address'.t());
            }, Ung.app.conf.token, forwardAddress);
        }
    }

});

/**
 * Quarantined Messages Tab
 */
Ext.define('Ung.view.Messages', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.messages',
    reference: 'messages',
    title: 'Quarantined Messages'.t(),
    viewModel: {
        formulas: {
            warning: function (get) {
                return Ext.String.format('The messages below were quarantined and will be deleted after {0} days.'.t(), get('quarantineDays'));
            }
        }
    },
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'component',
            padding: 5,
            bind: { html: '<i class="fa fa-exclamation-triangle fa-lg" style="color: orange;"></i> {warning}' }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        ui: 'footer',
        style: { background: '#EEE' },
        defaults: {
            disabled: true,
            bind: { disabled: '{!messages.selection}' }
        },
        items: [{
            text: 'Release to Inbox'.t(),
            iconCls: 'fa fa-inbox',
            handler: 'releaseMessages',
        }, {
            text: 'Release to Inbox & Add Senders to Safe List'.t(),
            iconCls: 'fa fa-user'
        }, {
            text: 'Delete'.t(),
            iconCls: 'fa fa-trash',
            handler: 'purgeMessages'
        }]
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        items: [ 'Filter:'.t(), {
            xtype: 'textfield'
        }]
    }],

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
            return value != 0 ? value : '';
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
    reference: 'safelist',

    title: 'Safe List'.t(),
    viewModel: {
        stores: {
            safeListStore: {
                data: '{safelistData}',
                fields: [{ name: 'emailAddress' }],
                proxy: {
                    type: 'memory',
                    reader: { type: 'array' }
                }
            }
        }
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            xtype: 'component',
            padding: 5,
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
                disabled: '{!safelist.selection}'
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
    bind: '{safeListStore}'
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
        reference: 'receivedQuarantinedMessages',
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
                bind: {
                    disabled: '{!receivedQuarantinedMessages.selection}'
                }
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
        this.getMainView().getViewModel().set(Ung.app.conf);
        // Ext.Deferred.parallel([
        //     Rpc.asyncPromise('rpc.reportsManager.getReportEntries'),
        //     Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications')
        // ]).then(function (result) {
        //     Ext.getStore('reports').loadData(result[0].list); // reports store is defined in Reports module
        //     Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result[1].list));
        //     Ext.getStore('reportstree').build(); // build the reports tree
        //     Ext.fireEvent('init');
        // });
    }
});
