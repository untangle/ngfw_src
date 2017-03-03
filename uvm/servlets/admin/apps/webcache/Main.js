Ext.define('Ung.apps.webcache.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-webcache',

    controller: 'app-webcache',

    items: [
        { xtype: 'app-webcache-status' },
        { xtype: 'app-webcache-cachebypass' }
    ]

});
