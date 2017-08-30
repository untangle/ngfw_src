Ext.define('Ung.apps.webcache.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-web-cache',
    controller: 'app-web-cache',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/web-cache',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-web-cache-status' },
        { xtype: 'app-web-cache-cachebypass' }
    ]

});
