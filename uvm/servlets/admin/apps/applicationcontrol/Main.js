Ext.define('Ung.apps.applicationcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.applicationcontrol',

    viewModel: {
        data: {
            nodeName: 'untangle-node-application-control',
            appName: 'Application Control'
        }
    },

    items: [
        { xtype: 'app.applicationcontrol.status' },
        { xtype: 'app.applicationcontrol.applications' },
        { xtype: 'app.applicationcontrol.rules' }
    ]

});
