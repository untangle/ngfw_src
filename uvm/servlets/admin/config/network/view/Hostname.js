Ext.define('Ung.config.network.view.Hostname', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-network-hostname',
    itemId: 'hostname',
    scrollable: true,

    withValidation: true, // requires validation on save
    viewModel: true,

    title: 'Hostname'.t(),
    bodyPadding: 10,

    listeners: {
        afterrender: function () {
            if (Ung.AppIframe) {
                Ung.AppIframe.updateIframe('/console/settings/network/hostname', false);
                // Ung.AppIframe.setHidden(false);
            }
        }
    },

    // items: [{
    //     region: 'center',
    //     xtype: 'container',
    //     itemId: 'iframeWrapper',
    //     layout: 'fit',
    //     items: [Ung.AppIframe]
    // }]
});
