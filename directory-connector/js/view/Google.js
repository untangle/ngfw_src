Ext.define('Ung.apps.directoryconnector.view.Google', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-google',
    itemId: 'google',
    title: 'Google'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Google Drive Connector'.t(),
        items: [{
            xtype: 'fieldset',
            name: 'fieldsetDriveDisabled',
            collapsible: false,
            border: 0,
            hidden: true,
            bind: {
                hidden: '{googleDriveIsConfigured == true}',
            },
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
            bind: {
                hidden: '{googleDriveIsConfigured == false}',
            },
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
