Ext.define('Ung.apps.directoryconnector.view.UserNotificationApi', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-usernotificationapi',
    itemId: 'user-notification-api',
    title: 'User Notification API'.t(),
    scrollable: true,

    viewModel: true,
    bodyPadding: 10,

    items:[{
        xtype: 'fieldset',
        title: 'User Notification API'.t(),
        items: [{
            xtype: 'component',
            margin: '10 0 10 0',
            html: "The User Notification API provides a web-based app/API to allow scripts and agents to update the server's username-to-IP address mapping in order to properly identify users for Policy Manager, Reports, and other applications.".t()
       }, {
            xtype: "checkbox",
            bind: '{settings.apiEnabled}',
            fieldLabel: 'Enable User Notification API'.t(),
            labelWidth: 190,
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'fieldset',
            margin: '10 10 10 -10',
            border: 0,
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.apiEnabled == false}',
                disabled: '{settings.apiEnabled == false}'
            },
            items: [{
                xtype: 'fieldcontainer',
                layout: 'column',
                items: [{
                    xtype:'textfield',
                    name: 'secretKey',
                    fieldLabel: 'Secret Key'.t(),
                    labelWidth: 190,
                    bind: '{settings.apiSecretKey}',
                }]
        },{
            xtype: 'component',
            margin: '10 0 10 0',
            html: "If you use an Active Directory server, you should install the Active Directory Login Monitor on the server as described by the help documentation.  Otherwise, you will need to download and install the following script on all clients.".t()
        }, {
                xtype: 'button',
                text: 'Download User Notification Login Script'.t(),
                iconCls: 'fa fa-download',
                margin: '10 0 10 0',
                handler: 'downloadUserApiScript'
            }]
        }]
    }]
});
