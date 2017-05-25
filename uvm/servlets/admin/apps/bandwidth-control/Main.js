Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-bandwidth-control',
    controller: 'app-bandwidth-control',

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
                href: '#reports/bandwidth-control',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-bandwidth-control-status' },
        { xtype: 'app-bandwidth-control-rules' }
    ]

});
