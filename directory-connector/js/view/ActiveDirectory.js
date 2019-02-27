Ext.define('Ung.apps.directoryconnector.view.ActiveDirectory', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-activedirectory',
    itemId: 'active-directory',
    title: 'Active Directory'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Active Directory Connector'.t(),
        items: [{
            xtype: 'component',
            margin: '10 0 0 0',
            html: Ext.String.format(
                "This allows the server to connect to an {0}Active Directory Server{1} for authentication of username/passwords and building of a user-group map.".t(),
                '<b>','</b>')
        }, {
            xtype: 'component',
            margin: '10 0 0 20',
            html: Ext.String.format(
                "{0}NOTE:{1}  Additional steps are required to associate hosts and sessions with usernames, such as using Captive Portal, the Active Directory Server Login Monitor Agent, or the Login Script.".t() + "</br>" +
                    "See {0}Help{1} for more details.".t(),
                '<i>','</i>','<i>','</i>')
        }, {
                xtype: "checkbox",
                bind: '{settings.activeDirectorySettings.enabled}',
                fieldLabel: 'Enable Active Directory Connector'.t(),
                labelWidth: '100%',
                margin: '10 0 10 0',
                listeners: {
                    disable: function (ck) {
                        ck.setValue(false);
                    }
                }
            }, {
                xtype: 'ungrid',
                controller: 'unadserversgrid',
                split: true,
                title: 'Active Directory Servers'.t(),
                margin: '10 10 10 0',

                tbar: ['@add', '->', '@import', '@export'],
                recordActions: ['edit', 'delete', 'reorder'],

                listProperty: 'settings.activeDirectorySettings.servers.list',
                ruleJavaClass: 'com.untangle.app.directory_connector.ActiveDirectoryServer',

                emptyRow: {
                    LDAPHost: '',
                    LDAPPort: 636,
                    LDAPSecure: true,
                    OUFilters: {
                        javaClass: "java.util.LinkedList",
                        list: []
                    },
                    domain: '',
                    enabled: true,
                    javaClass: "com.untangle.app.directory_connector.ActiveDirectoryServer",
                    superuser: '',
                    superuserPass:''
                },

                disabled: true,
                hidden: true,

                bind: {
                    store: '{activeDirectoryServers}',
                    disabled: '{!settings.activeDirectorySettings.enabled}',
                    hidden: '{!settings.activeDirectorySettings.enabled}'
                },

                columns: [{
                    xtype: 'checkcolumn',
                    header: 'Enabled'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: Renderer.actionWidth
                }, {
                    header: 'Domain'.t(),
                    dataIndex: 'domain',
                    flex: 1,
                    editor: {
                        xtype: 'textfield',
                        bind: '{record.domain}'
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Azure'.t(),
                    dataIndex: 'azure',
                    width: Renderer.actionWidth,
                    listeners: {
                        beforecheckchange: 'azureChanger'
                    },
                }, {
                    header: 'OU Filters'.t(),
                    width: Renderer.messageWidth,
                    dataIndex: 'record.OUFilters.list',
                    renderer: 'ouFilterRenderer',
                    flex: 1
                }, {
                    header: 'Host'.t(),
                    dataIndex: 'LDAPHost',
                    flex: 1,
                    width: Renderer.hostnameWidth,
                    editor: {
                        xtype: 'textfield',
                        bind: '{record.LDAPHost}'
                    }
                }, {
                    header: 'Port'.t(),
                    width: Renderer.portWidth,
                    dataIndex: 'LDAPPort',
                    editor: {
                        xtype: 'textfield',
                        bind: '{record.LDAPPort}'
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Secure'.t(),
                    width: Renderer.actionWidth,
                    dataIndex: 'LDAPSecure',
                    listeners: {
                        beforecheckchange: 'portChanger'
                    },
                }, {
                    xtype: 'actioncolumn',
                    header: 'Test'.t(),
                    width: Renderer.actionWidth,
                    iconCls: 'fa fa-cogs',
                    align: 'center',
                    handler: 'serverTest',
                    isDisabled: 'isServerDisabled'
                }, {
                    xtype: 'actioncolumn',
                    header: 'Users'.t(),
                    width: Renderer.actionWidth,
                    iconCls: 'fa fa-user',
                    align: 'center',
                    handler: 'serverUsers',
                    isDisabled: 'isServerDisabled'
                }, {
                    xtype: 'actioncolumn',
                    header: 'Map'.t(),
                    width: Renderer.actionWidth,
                    iconCls: 'fa fa-user',
                    align: 'center',
                    handler: 'serverGroupMap',
                    isDisabled: 'isServerDisabled'
                }],

                editorXtype: 'ung.cmp.unactivedirectoryserverrecordeditor',
                editorFields: [
                    Field.enableRule(),
                {
                    xtype: 'textfield',
                    fieldLabel: 'Domain'.t(),
                    bind: '{record.domain}',
                    emptyText: '[no domain]'.t(),
                    allowBlank: false
                },{
                    xtype: 'checkbox',
                    fieldLabel: 'Azure'.t(),
                    bind: '{record.azure}',
                    handler: 'azureChanger',
                    listeners: {
                        beforecheckchange: 'azureChanger'
                    }
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    width: 800,
                    items: [{
                        xtype: 'label',
                        html: 'Active Directory Organizations'.t(),
                        width: 190,
                    },{
                        xtype: 'ungrid',
                        itemId: 'unoufiltergrid',
                        width: 305,
                        border: false,
                        titleCollapse: true,
                        tbar: ['@addInline'],
                        recordActions: ['delete'],
                        bind: '{record.OUFilters.list}',
                        maxHeight: 140,
                        emptyRow: {
                            field1: ''
                        },
                        columns: [{
                            header: 'Organizational Unit'.t(),
                            dataIndex: 'field1',
                            width: 200,
                            flex: 1,
                            editor : {
                                xtype: 'textfield',
                                vtype: 'x500attributes',
                                emptyText: '[enter OU]'.t(),
                                allowBlank: false
                            }
                        }]
                    },{
                        xtype: 'label',
                        html: '(optional)'.t(),
                        cls: 'boxlabel'
                    }]
                },{
                    xtype: 'textfield',
                    fieldLabel: 'Host'.t(),
                    bind: '{record.LDAPHost}',
                    emptyText: '[no host]'.t(),
                    allowBlank: false
                },{
                    xtype: 'textfield',
                    fieldLabel: 'Port'.t(),
                    bind: '{record.LDAPPort}',
                    emptyText: '[no port]'.t(),
                    allowBlank: false
                },{
                    xtype: 'checkbox',
                    fieldLabel: 'Secure'.t(),
                    bind: '{record.LDAPSecure}',
                    handler: 'portChanger',
                    listeners: {
                        beforecheckchange: 'portChanger'
                    }
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Authentication Login'.t(),
                    bind: '{record.superuser}',
                    emptyText: '[no authentication login]'.t(),
                    allowBlank: false
                }, {
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: 'Authentication Password'.t(),
                    bind: '{record.superuserPass}',
                    allowBlank: false
                },{
                    xtype: 'container',
                    layout: 'column',
                    items:[{
                        xtype: 'button',
                        text: 'Active Directory Test'.t(),
                        margin: '10 0 10 0',
                        iconCls: 'fa fa-cogs',
                        disabled: true,
                        bind: {
                            disabled: '{record.enabled == false || record.domain == "" || record.LDAPHost == "" || record.LDAPPort == "" || record.superuser == "" || record.password == ""}'
                        },
                        handler: 'serverTest'
                    },{
                        xtype: 'component',
                        margin: '12 0 10 10',
                        html: Ext.String.format( 'Verify the connection to the Active Directory Server.'.t(),'<b>','</b>')
                    }]
                }]
        },{
            xtype: 'panel',
            disabled: true,
            hidden: true,
            bind: {
                disabled: '{!settings.activeDirectorySettings.enabled}',
                hidden: '{!settings.activeDirectorySettings.enabled}'
            },
            border: 0,
            margin: '0 0 10 0',
            layout: {
                type: 'table',
                columns: 2
            },
            items:[{
                xtype: 'button',
                text: 'Users'.t(),
                iconCls: 'fa fa-user',
                handler: 'activeDirectoryUsers'
            },{
                xtype: 'label',
                margin: '0 0 0 10',
                html: "Verify all users from all servers.".t() + ' ' + 'In additional to authentication, some rules allow for user matching conditions.'.t(),
                cls: 'boxlabel'
            },{
                xtype: 'button',
                text: 'User Group Map'.t(),
                iconCls: 'fa fa-group',
                handler: 'activeDirectoryGroupMap'
            },{
                xtype: 'label',
                margin: '0 0 0 10',
                html: "Verify all user group mappings from all servers.".t() + ' ' + 'Some rules allow for group matching conditions.'.t(),
                cls: 'boxlabel'
            },{
                xtype: 'button',
                text: 'Refresh Group Cache'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'activeDirectoryGroupRefreshCache'
            },{
                xtype: 'label',
                margin: '0 0 0 10',
                html: "The group cache automatically refreshes every 20 minutes.".t() + ' ' + "Force an immediate refresh.".t(),
                cls: 'boxlabel'
            }]
        }]
    }]
});
