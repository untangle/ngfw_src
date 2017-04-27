Ext.define('Ung.config.about.view.LicenseAgreement', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-about-licenseagreement',
    itemId: 'license-agreement',
    helpSource: 'about_license_agreement',
    title: 'License Agreement'.t(),

    items: [{
        xtype: 'button',
        margin: 10,
        text: 'View License'.t(),
        iconCls: 'fa fa-file-text-o'
    }]

});
