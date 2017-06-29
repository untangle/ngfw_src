Ext.define('Ung.apps.directoryconnector.view.UserNotificationApi', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-usernotificationapi',
    itemId: 'user-notification-api',
    title: 'User Notification API'.t(),

    viewModel: true,
    bodyPadding: 10,

    items:[{
        xtype: 'fieldset',
        title: 'User Notification API'.t(),
        items: [{
            xtype: 'component',
            margin: '10 0 10 0',
            html: Ext.String.format(
                "The User Notification API provides a web-based app/API to allow scripts and agents to update the server's username-to-IP address mapping in order to properly identify users for Policy Manager, Reports, and other applications.".t(),
                '<b>','</b>')
        }, {
            xtype: "checkbox",
            bind: '{settings.apiEnabled}',
            fieldLabel: 'Enable User Notification API'.t(),
            labelWidth: 200,
            margin: '10 0 10 0',
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'fieldset',
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
                width: 600,
                items: [{
                    xtype:'textfield',
                    name: 'secretKey',
                    fieldLabel: 'Secret Key'.t(),
                    labelWidth: 190,
                    bind: '{settings.apiSecretKey}',
                },{
                    xtype: 'label',
                    html: '(blank means no secret key is required)'.t(),
                    margin: '5 0 0 10',
                    cls: 'boxlabel'
                }]
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
