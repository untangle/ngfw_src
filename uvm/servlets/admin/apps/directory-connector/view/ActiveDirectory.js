Ext.define('Ung.apps.directoryconnector.view.ActiveDirectory', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-activedirectory',
    itemId: 'active_directory',
    title: 'Active Directory'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Active Directory Connector'.t(),
        items: [{
            xtype: 'component',
            html: Ext.String.format(
                "This allows the server to connect to an {0}Active Directory Server{1} for authentication for Captive Portal.".t(),
                '<b>','</b>')
        }, {
            xtype: "checkbox",
            bind: '{settings.activeDirectorySettings.enabled}',
            fieldLabel: 'Enable Active Directory Connector'.t(),
            labelWidth: 200,
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
                hidden: '{settings.activeDirectorySettings.enabled == false}',
                disabled: '{settings.activeDirectorySettings.enabled == false}'
            },
            defaults: {
                labelWidth: 190,
                width: 500
            },
            items: [{
                xtype: 'textfield',
                fieldLabel: 'AD Server IP or Hostname'.t(),
                bind: '{settings.activeDirectorySettings.LDAPHost}'
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Secure'.t(),
                bind: '{settings.activeDirectorySettings.LDAPSecure}',
                handler: 'activeDirectoryPortChanger'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Port'.t(),
                allowBlank: false,
                vtype: "port",
                bind: '{settings.activeDirectorySettings.LDAPPort}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Authentication Login'.t(),
                bind: '{settings.activeDirectorySettings.superuser}',
            }, {
                xtype: 'textfield',
                inputType: 'password',
                fieldLabel: 'Authentication Password'.t(),
                bind: '{settings.activeDirectorySettings.superuserPass}'
            },{
                xtype: 'container',
                layout: 'column',
                width: 800,
                items: [{
                    xtype:'textfield',
                    fieldLabel: 'Active Directory Domain'.t(),
                    labelWidth: 190,
                    width: 500,
                    bind: '{settings.activeDirectorySettings.domain}'
                },{
                    xtype: 'label',
                    html: '(FQDN such as mycompany.local)'.t(),
                    cls: 'boxlabel'
                }]
            }, {
                xtype: 'container',
                layout: 'column',
                width: 800,
                items: [{
                    xtype: 'textfield',
                    fieldLabel: 'Active Directory Organization'.t(),
                    labelWidth:190,
                    width: 500,
                    bind: '{settings.activeDirectorySettings.OUFilter}'
                }, {
                    xtype: 'label',
                    html: '(optional)'.t(),
                    cls: 'boxlabel'
                }]
            }],
        },{
            xtype: 'fieldset',
            title: 'Active Directory Test'.t(),
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.activeDirectorySettings.enabled == false}',
                disabled: '{settings.activeDirectorySettings.enabled == false}'
            },
            items:[{
                xtype: 'container',
                layout: 'column',
                items:[{
                    xtype: 'button',
                    text: 'Active Directory Test'.t(),
                    iconCls: 'test-icon',
                    handler: 'activeDirectoryTest'
                },{
                    xtype: 'component',
                    margin: '5 0 10 0',
                    html: Ext.String.format( 'The {0}Active Directory Test{1} verifies that the server can connect to the Active Directory Server.'.t(),'<b>','</b>')
                }]
            }]
        },{
            xtype: 'fieldset',
            title: 'Active Directory Users'.t(),
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.activeDirectorySettings.enabled == false}',
                disabled: '{settings.activeDirectorySettings.enabled == false}'
            },
            items: [{
                xtype: 'textarea',
                name: 'activeDirectoryUsersTextarea',
                hideLabel: true,
                readOnly: true,
                width: 500,
                height: 200,
                hidden: true
            },{
                xtype: 'button',
                text: 'Active Directory Users'.t(),
                iconCls: 'test-icon',
                handler: 'activeDirectoryUsers'
            }]
        },{
            xtype: 'fieldset',
            title: 'Active Directory Group Map'.t(),
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.activeDirectorySettings.enabled == false}',
                disabled: '{settings.activeDirectorySettings.enabled == false}'
            },
            items: [{
                xtype: 'ungrid',
                name: 'groupMapGrid',
                hidden: true,
                hasEdit: false,
                hasDelete: false,
                hasAdd: false,
                sortField: 'name',
                reserveScrollbar: true,
                store: [],
                height: 200,
                plugins:['gridfilters'],
                columnMenuDisabled: false,
                fields:[{
                    name: 'name'
                },{
                    name:'groups'
                }],
                columns: [{
                    header: 'Name'.t(),
                    dataIndex:'name',
                    sortable: true,
                    width: 180,
                    filter: {
                        type: 'string'
                    }
                },{
                    header: 'Groups'.t(),
                    dataIndex: 'groups',
                    sortable: true,
                    flex: 1,
                    filter: {
                        type: 'string'
                    }
                }]
            },{
                xtype: 'button',
                text: 'User Group Map'.t(),
                iconCls: 'test-icon',
                handler: 'activeDirectoryGroupMap'
            }, {
                xtype: 'button',
                text: 'Refresh group cache'.t(),
                iconCls: 'test-icon',
                handler: 'activeDirectoryGroupRefreshCache'
            }]
        }]
    }]
});
