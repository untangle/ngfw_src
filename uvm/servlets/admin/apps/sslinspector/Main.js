Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-sslinspector',
    controller: 'app-sslinspector',

    items: [
        { xtype: 'app-sslinspector-status' },
        { xtype: 'app-sslinspector-configuration' },
        { xtype: 'app-sslinspector-rules' }
    ]

});
