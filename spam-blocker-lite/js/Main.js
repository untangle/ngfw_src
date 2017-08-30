Ext.define('Ung.apps.spamblockerlite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spam-blocker-lite',
    controller: 'app-spam-blocker-lite',

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/spam-blocker-lite',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-spam-blocker-lite-status' },
        { xtype: 'app-spam-blocker-lite-email' }
    ]
});
