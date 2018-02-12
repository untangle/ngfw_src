Ext.define('Ung.config.about.view.LicenseAgreement', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-about-licenseagreement',
    itemId: 'license-agreement',
    scrollable: true,

    title: 'License Agreement'.t(),

    items: [{
        xtype: 'button',
        margin: 10,
        text: 'View License'.t(),
        iconCls: 'fa fa-file-text-o',
        href: Rpc.directData('rpc.UvmContext.getLegalUrl') + '?' + Util.getAbout()
    }]

});
