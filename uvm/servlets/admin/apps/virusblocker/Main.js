Ext.define('Ung.apps.virusblocker.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.virusblocker',

    viewModel: {
        data: {
            nodeName: 'untangle-node-virus-blocker',
            appName: 'Virus Blocker'
        }
    },

    items: [
        { xtype: 'app.virusblocker.status' },
        { xtype: 'app.virusblocker.web' },
        { xtype: 'app.virusblocker.email' },
        { xtype: 'app.virusblocker.ftp' },
        { xtype: 'app.virusblocker.passsites' },
        { xtype: 'app.virusblocker.advanced' }
    ]

});
