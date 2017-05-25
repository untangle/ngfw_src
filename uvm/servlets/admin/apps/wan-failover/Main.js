Ext.define('Ung.apps.wanfailover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wan-failover',
    controller: 'app-wan-failover',

    viewModel: {

        data: {
            autoReload: false,
            wanStatusData: [],
            pingListData: []
        },

        stores: {
            tests: {
                data: '{settings.tests.list}'
            },
            wanStatusStore: {
                data: '{wanStatusData}'
            },
            pingListStore: {
                fields: [ 'name' , 'addr' ],
                data: '{pingListData}'
            },
        },

    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/wan-failover',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-wan-failover-status' },
        { xtype: 'app-wan-failover-tests' }
    ]

});
