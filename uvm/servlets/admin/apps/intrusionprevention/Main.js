Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-intrusionprevention',

    items: [
        { xtype: 'app-intrusionprevention-status' },
        { xtype: 'app-intrusionprevention-rules' },
        { xtype: 'app-intrusionprevention-variables' }
    ]

});
