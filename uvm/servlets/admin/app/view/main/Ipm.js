Ext.define('Ung.view.main.Ipm', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ipm',

    dock: 'top',

    controller: 'ipm',

    dockedItems: [{
        xtype: 'container',
        itemId: '_ipmMessages',
    }],
});