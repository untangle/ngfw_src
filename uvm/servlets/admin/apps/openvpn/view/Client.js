Ext.define('Ung.apps.openvpn.view.Client', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-openvpn-client',
    itemId: 'client',
    title: 'Client'.t(),
    viewModel: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The Client tab is used to configure servers to which OpenVPN will connect as a client.'.t()
    }],

    defaults: {
        padding: '0 0 0 10'
    },

    items: [{
        xtype: 'ungrid',
        region: 'center',
        tbar: ['@add'],
        recordActions: ['edit','delete'],
        listProperty: 'settings.remoteServers.list',

        bind: '{remoteServers}',

        columns: [{
            header: 'Enabled'.t(),
            dataIndex: 'enabled',
            xtype: 'checkcolumn',
            width: 80
        },{
            header: 'Server Name'.t(),
            dataIndex: 'name',
            width: 200,
            flex: 1
        }],
    },{
        xtype: 'displayfield',
        padding: '20 0 0 10',
        value: '<STRONG>' + 'Configure a new Remote Server connection'.t() + '</STRONG>'
    },{
        fieldLabel: 'Configuration File'.t(),
        labelWidth: 160,
        width: 400,
        xtype: 'textfield'
    },{
        xtype: 'button',
        align: 'center',
        width: 100,
        text: 'Submit'.t()
    }]
});
