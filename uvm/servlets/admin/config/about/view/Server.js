Ext.define('Ung.config.about.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-about-server',
    itemId: 'server',
    scrollable: true,

    title: 'Server'.t(),

    items:[{
        xtype: 'ungrid',
        itemId: 'protected_about',

        tbar: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px', fontWeight: 600, Color: 'red' },
            html: 'Do not publicly share this information.'.t()
        }],
        hideHeaders: true,

        bind: {
            store: '{protectedAbout}'
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            width: 250
        }, {
            header: 'Value'.t(),
            dataIndex: 'value',
            flex: 1
        }]
    },{
        xtype: 'ungrid',
        itemId: 'public_about',

        tbar: [{
            xtype: 'tbtext',
            padding: '8 5',
            html: 'Sharable information.'.t()
        }],
        hideHeaders: true,

        bind: {
            store: '{publicAbout}'
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            width: 250
        }, {
            header: 'Value'.t(),
            dataIndex: 'value',
            flex: 1
        }]
    }]
});
