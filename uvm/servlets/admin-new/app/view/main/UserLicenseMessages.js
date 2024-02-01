Ext.define('Ung.view.main.UserLicenseMessages', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.userLicenseMessages',

    dock: 'top',

    controller: 'userLicenseMessages',

    dockedItems: [{
        xtype: 'container',
        itemId: '_userLicenseMessages',
    }],
});