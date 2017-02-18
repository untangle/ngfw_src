Ext.define('Ung.config.about.About', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.about',
    requires: [
        'Ung.config.about.AboutController'
    ],
    controller: 'config.about',
    viewModel: {
        kernelVersion: '',
        modificationState: '',
        rebootCount: '',
        activeSize: '',
        maxActiveSize: ''
    },
    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'About'.t() + '</strong>'
        }],
    }],
    items: [{
        xtype: 'config.about.server'
    }, {
        xtype: 'config.about.licenses'
    }, {
        xtype: 'config.about.licenseagreement'
    }]
});
Ext.define('Ung.config.about.AboutController', {    extend: 'Ext.app.ViewController',    alias: 'controller.config.about',    control: {        '#': {            beforerender: 'onBeforeRender',        },    },    onBeforeRender: function (view) {        try {            view.getViewModel().set({                kernelVersion: rpc.adminManager.getKernelVersion(),                modificationState: rpc.adminManager.getModificationState(),                rebootCount: rpc.adminManager.getRebootCount(),                activeSize: rpc.hostTable.getCurrentActiveSize(),                maxActiveSize: rpc.hostTable.getMaxActiveSize()            });            console.log(view.getViewModel());        } catch (ex) {        }    },    reloadLicenses: function () {        rpc.UvmContext.licenseManager().reloadLicenses(function (result, ex) {            // todo        });    }});
Ext.define('Ung.config.about.view.LicenseAgreement', {    extend: 'Ext.panel.Panel',    alias: 'widget.config.about.licenseagreement',    title: 'License Agreement'.t(),    items: [{        xtype: 'button',        margin: 10,        text: 'View License'.t(),        iconCls: 'fa fa-file-text-o'    }]});
Ext.define('Ung.config.about.view.Licenses', {    extend: 'Ext.panel.Panel',    alias: 'widget.config.about.licenses',    title: 'Licenses'.t(),    layout: 'fit',    tbar: [{        xtype: 'tbtext',        padding: '8 5',        style: { fontSize: '12px' },        html: Ext.String.format('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'.t(), '<b>', '</b>')    }],    items: [{        xtype: 'grid',        forceFit: true,        columns: [{            header: 'Name'.t(),            dataIndex: 'displayName',            width: 150        }, {            header: 'App'.t(),            dataIndex: 'name',            flex: 1        }, {            header: 'UID'.t(),            dataIndex: 'UID',            width: 150        }, {            header: 'Start Date'.t(),            dataIndex: 'start',            width: 240,            // renderer: function (value) { return i18n.timestampFormat(value*1000);}        }, {            header: 'End Date'.t(),            dataIndex: 'end',            width: 240,            // renderer: Ext.bind(function(value) { return i18n.timestampFormat(value*1000); }, this)        }, {            header: 'Seats'.t(),            dataIndex: 'seats',            width: 50        }, {            header: 'Valid'.t(),            dataIndex: 'valid',            width: 50        }, {            header: 'Status',            dataIndex: 'status',            width: 150        }],        bbar: [{            text: 'Refresh'.t(),            iconCls: 'fa fa-refresh',            handler: 'reloadLicenses'        }]    }]});
Ext.define('Ung.config.about.view.Server', {    extend: 'Ext.panel.Panel',    alias: 'widget.config.about.server',    title: 'Server'.t(),    scrollable: true,    bodyPadding: 10,    defaults: {        xtype: 'fieldset',        padding: 10    },    items: [{        title: 'About'.t(),        items: [{            xtype: 'component',            html: 'Do not publicly post or share the UID or account information.'.t() + '<br/>' +                'UID'.t() + ': ' + rpc.serverUID        }]    }, {        bind: {            html: 'Build'.t() + ': <strong>' + rpc.fullVersionAndRevision + '</strong> <br />' +                'Kernel'.t() + ': <strong>' + '{kernelVersion}' + '</strong> <br />' +                'History'.t() + ': <strong>' + '{modificationState}' + '</strong> <br />' +                'Reboots'.t() + ': <strong>' + '{rebootCount}' + '</strong> <br />' +                'Current active device count'.t() + ': <strong>' + '{activeSize}' + '</strong> <br />' +                'Highest active device count since reboot'.t() + ': <strong>' + '{maxActiveSize}' + '</strong>'        }    }]});