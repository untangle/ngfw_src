Ext.define('Ung.config.about.view.Licenses', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-about-licenses',
    itemId: 'licenses',
    scrollable: true,

    title: 'Licenses'.t(),
    layout: 'fit',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: Ext.String.format('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'.t(), '<b>', '</b>')
    }],

    items: [{
        xtype: 'ungrid',
        itemId: 'licenses',

        emptyText: 'No Licenses defined'.t(),

        bind: {
            store: {
                data: '{licenses}'
            }
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'displayName',
            width: Renderer.messageWidth
        }, {
            header: 'App'.t(),
            dataIndex: 'currentName',
            width: Renderer.messageWidth,
            flex: 1
        }, {
            header: 'UID'.t(),
            dataIndex: 'UID',
            width: Renderer.messageWidth
        }, {
            header: 'Start Date'.t(),
            dataIndex: 'start',
            width: Renderer.dateWidth,
            renderer: Renderer.timestamp
        }, {
            header: 'End Date'.t(),
            dataIndex: 'end',
            width: Renderer.dateWidth,
            renderer: Renderer.timestamp
        }, {
            header: 'Seats'.t(),
            dataIndex: 'seatsDisplay',
            width: Renderer.idWidth
        }, {
            header: 'Valid'.t(),
            dataIndex: 'valid',
            width: Renderer.booleanWidth
        }, {
            header: 'Status',
            dataIndex: 'status',
            width: Renderer.messageWidth
        }],

    }],

    bbar: [{
        text: 'Refresh'.t(),
        iconCls: 'fa fa-refresh',
        handler: 'reloadLicenses'
    }]

});
