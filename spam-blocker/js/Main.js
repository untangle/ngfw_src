Ext.define('Ung.apps.spamblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-spam-blocker',
    controller: 'app-spam-blocker',

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/spam-blocker',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-spam-blocker-status' },
        { xtype: 'app-spam-blocker-email' }
    ]
});
