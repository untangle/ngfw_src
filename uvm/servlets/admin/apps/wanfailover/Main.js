Ext.define('Ung.apps.wanfailover.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wanfailover',
    controller: 'app-wanfailover',

    items: [
        { xtype: 'app-wanfailover-status' },
        { xtype: 'app-wanfailover-tests' }
    ]

});
