Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ssl-inspector',
    controller: 'app-ssl-inspector',

    viewModel: {

        data: {
            autoRefresh: false,
            trustedCertData: []
        },

        stores: {
            ignoreRules: { data: '{settings.ignoreRules.list}' },
            trustedCertList: { data: '{trustedCertData}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/ssl-inspector',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-ssl-inspector-status' },
        { xtype: 'app-ssl-inspector-configuration' },
        { xtype: 'app-ssl-inspector-rules' }
    ]

});
