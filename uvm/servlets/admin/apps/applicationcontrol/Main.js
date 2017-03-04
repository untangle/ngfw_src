Ext.define('Ung.apps.applicationcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-applicationcontrol',
    controller: 'app-applicationcontrol',

    items: [
        { xtype: 'app-applicationcontrol-status' },
        { xtype: 'app-applicationcontrol-applications' },
        { xtype: 'app-applicationcontrol-rules' }
    ]

});
