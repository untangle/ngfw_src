Ext.define('Ung.config.administration.view.Google', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-administration-google',
    itemId: 'google',
    title: 'Google'.t(),
    withValidation: false,
    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Google Drive Connector'.t(),
        items: [{
            xtype: 'container',     // Remove this item and uncomment below code once Google connector issue is resolved
            margin: '5 0 15 0',
            html: '<p style="text-align: center; margin: 0; line-height: 2; font-size: 12px; color: black;"><i class="fa fa-info-circle fa-lg" style="color: dodgerblue"></i><br/>' + 'This feature is temporarily unavailable'.t() + '</p>'
        }, {
            xtype: 'fieldset',
            name: 'fieldsetDriveDisabled',
            collapsible: false,
            border: 0,
            hidden: true,
            // bind: {
            //     hidden: '{googleDriveIsConfigured == true}',
            // },
            items: [{
                xtype: 'component',
                html: 'The Google Drive is unconfigured.'.t(),
                margin: '10 0 0 0',
                style: {color:'red'},
                cls: 'warning'
            }, {
                xtype: "button",
                text: 'Configure Google Drive'.t(),
                iconCls: "fa fa-check-circle",
                margin: '10 0 10 0',
                handler: 'googleDriveConfigure'
            }]
        },{
            xtype: 'fieldset',
            name: 'fieldsetDriveEnabled',
            border: 0,
            collapsible: false,
            hidden: true,
            // bind: {
            //     hidden: '{googleDriveIsConfigured == false}',
            // },
            items: [{
                xtype: 'component',
                html: 'The Google Drive is configured.'.t(),
                margin: '10 0 0 0',
                style: {color:'green'}
            }, {
                xtype: "button",
                text: 'Reconfigure Google Drive'.t(),
                iconCls: "fa fa-refresh",
                margin: '10 0 10 0',
                handler: 'googleDriveConfigure'
            }, {
                xtype: "button",
                text: "Disconnect Google Drive".t(),
                margin: '10 0 10 0',
                iconCls: "fa fa-ban",
                handler: 'googleDriveDisconnect'
            }]
        }]
    }]
});
