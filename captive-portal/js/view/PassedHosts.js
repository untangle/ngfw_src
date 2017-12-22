Ext.define('Ung.apps.captive-portal.view.PassedHosts', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-captive-portal-passedhosts',
    itemId: 'passed-hosts',
    title: 'Passed Hosts'.t(),
    scrollable: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'The pass lists provide a quick alternative way to allow access from specific clients, or to specific servers.'.t()
    }],

    layout: 'border',

    defaults: {
        xtype: 'ungrid',
        // border: false,
        split: true,
        tbar: ['@addInline', '->', '@import', '@export'],

        recordActions: ['delete'],

        emptyRow: {
            enabled: true,
            log: false,
            address: '0.0.0.0',
            description: '',
            javaClass: 'com.untangle.app.captive_portal.PassedAddress'
        },
        columns: [
            Column.enabled,{
                xtype: 'checkcolumn',
                header: 'Log'.t(),
                dataIndex: 'log',
                resizable: false,
                width: Renderer.booleanWidth
            }, {
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                dataIndex: 'address',
                editor: {
                    xtype: 'textfield',
                    emptyText: '[enter address]'.t(),
                    vtype: 'ipMatcher',
                    allowBlank: false
                }
            }, {
                header: 'Description'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                dataIndex: 'description',
                editor: {
                    xtype: 'textfield',
                    emptyText: '[no description]'.t()
                }
            }
        ]
    },

    items: [{
        region: 'center',
        title: 'Pass Listed Client Addresses'.t(),
        bind: '{passedClients}',
        listProperty: 'settings.passedClients.list',
        emptyText: 'No Pass Listed Client Addresses defined'.t(),
        bbar: [{
            xtype: 'component',
            padding: 5,
            style: {
                fontSize: '10px',
                color: '#777'
            },
            html: '<i class="fa fa-info-circle"></i> ' + 'Pass Listed Client Addresses is a list of Client IPs that are not subjected to the Captive Portal.'.t()
        }]
    }, {
        region: 'south',
        height: '50%',
        title: 'Pass Listed Server Addresses'.t(),
        bind: '{passedServers}',
        emptyText: 'No Pass Listed Server Addresses defined'.t(),
        listProperty: 'settings.passedServers.list',
        bbar: [{
            xtype: 'component',
            padding: 5,
            style: {
                fontSize: '10px',
                color: '#777'
            },
            html: '<i class="fa fa-info-circle"></i> ' + 'Pass Listed Server Addresses is a list of Server IPs that unauthenticated clients can access without authentication.'.t()
        }]
    }]
});
