Ext.define('Ung.apps.firewall.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-firewall',
    controller: 'app-firewall',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' },
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/firewall',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-firewall-status' },
        { xtype: 'app-firewall-rules' }
    ]

});
