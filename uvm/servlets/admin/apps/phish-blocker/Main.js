Ext.define('Ung.apps.phishblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-phish-blocker',
    controller: 'app-phish-blocker',

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/phish-blocker',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-phish-blocker-status' },
        { xtype: 'app-phish-blocker-email' }
    ]

});
