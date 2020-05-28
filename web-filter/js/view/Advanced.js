Ext.define('Ung.apps.webfilter.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-web-filter-advanced',
    itemId: 'advanced',
    title: 'Advanced'.t(),
    scrollable: true,

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Process HTTPS traffic by SNI (Server Name Indication) information if present'.t(),
        collapsible: true,
        checkboxToggle: true,
        checkbox: {
            bind: '{settings.enableHttpsSni}'
        },
        padding: 10,
        items: [{
            xtype: 'checkbox',
            margin: '0 0 0 20',
            boxLabel: 'Process HTTPS traffic by hostname in server certificate when SNI information not present'.t(),
            disabled: true,
            bind: {
                value: '{settings.enableHttpsSniCertFallback}',
                disabled: '{!settings.enableHttpsSni}'
            },
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'checkbox',
            margin: '0 0 0 20',
            boxLabel: 'Process HTTPS traffic by server IP if both SNI and certificate hostname information are not available'.t(),
            disabled: true,
            bind: {
                value: '{settings.enableHttpsSniIpFallback}',
                disabled: '{!settings.enableHttpsSniCertFallback}'
            },
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }]
    }, {
        xtype: 'fieldset',
        title: 'Safe browsing options'.t(),
        padding: '10 15',
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Enforce safe search on popular search engines'.t(),
            bind: '{settings.enforceSafeSearch}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Enforce Restricted Mode on YouTube'.t(),
            bind: '{settings.restrictYoutube}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Force searches through kid-friendly search engine'.t(),
            bind: '{settings.forceKidFriendly}'
        }]
    },{
        xtype: 'fieldset',
        title: 'Block options'.t(),
        padding: '10 15',
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: 'Block QUIC Sessions (UDP port 443)'.t(),
            bind: '{settings.blockQuic}'
        }, {
            xtype: 'checkbox',
            margin: '0 0 5 20',
            boxLabel: 'Log blocks'.t(),
            bind: {
                hidden: '{!isExpertMode || !settings.blockQuic}',
                value: '{settings.logQuic}'
            }
        }, {
            xtype: 'checkbox',
            boxLabel: 'Block pages from IP only hosts'.t(),
            bind: '{settings.blockAllIpHosts}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Pass if referrer matches any Pass Sites'.t(),
            bind: '{settings.passReferers}'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Close connection for blocked HTTPS sessions without redirecting to block page'.t(),
            bind: '{settings.closeHttpsBlockEnabled}'
        }]
    }, {
        xtype: 'fieldset',
        title: 'Restrict Google applications'.t(),
        checkboxToggle: true,
        checkbox: {
            bind: '{settings.restrictGoogleApps}'
        },
        collapsible: true,
        collapsed: true,
        padding: 10,
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'displayfield',
            value: 'NOTE:'.t() + ' ' + '<i>SSL Inspector</i> ' + 'must be installed and running with the Inspect Google Traffic configured to Inspect.'.t()
        }, {
            xtype: 'textfield',
            fieldLabel: 'Specify the comma separated list of domains allowed to access non-search Google applications'.t(),
            labelAlign: 'top',
            bind: '{settings.restrictGoogleAppsDomain}',
            validator: function(fieldValue) {
                if (fieldValue.length === 0) {
                    return true;
                }
                var domains = fieldValue.split(/,/);
                for (var i = 0; i < domains.length; i++){
                    var domain = domains[i];
                    if (domain.match(/^([^:]+):\/\//) !== null) {
                        return 'Domain cannot contain URL protocol.'.t();
                    }
                    if (domain.match( /^([^:]+):\d+\// ) !== null) {
                        return 'Domain cannot contain port.'.t();
                    }
                    if (domain.trim().length === 0) {
                        return 'Invalid domain specified'.t();
                    }
                }
                return true;
            }
        }],
        listeners: {
            collapse: function (el) {
                el.down('textfield').setValue('');
            }
        }
    }, {
        xtype: 'fieldset',
        title: 'Custom block page'.t(),
        checkboxToggle: true,
        checkbox: {
            bind: '{settings.customBlockPageEnabled}'
        },
        collapsible: true,
        collapsed: true,
        padding: 10,
        cls: 'app-section',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'displayfield',
            value: 'NOTE:'.t() + ' ' + 'Unblock operations are not available for custom block page'.t()
        },{
            xtype: 'textfield',
            fieldLabel: 'Custom block page URL'.t(),
            labelAlign: 'top',
            emptyText: 'http://example.com',
            bind: '{settings.customBlockPageUrl}',
            vtype: 'url'
        }]
    }, {
        xtype: 'fieldset',
        title: 'Unblock Options'.t(),
        padding: 10,
        cls: 'app-section',
        layout: {
            type: 'vbox'
        },
        bind:{
            hidden: '{settings.customBlockPageEnabled}',
            disabled: '{settings.customBlockPageEnabled}',
        },
        items: [{
            xtype: 'container',
            items: [{
                xtype: 'combo',
                width: 200,
                editable: false,
                queryMode: 'local',
                store: [
                    ['None', 'None'.t()],
                    ['Host', 'Temporary'.t()],
                    ['Global', 'Permanent and Global'.t()]
                ],
                bind: '{settings.unblockMode}'
            }, {
                xtype: 'container',
                margin: '0 0 0 10',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.unblockMode !== "Host"}',
                    disabled: '{settings.unblockMode !== "Host"}'
                },
                items: [{
                    xtype: 'container',
                    layout: 'column',
                    width: 500,
                    items:[{
                        xtype: 'textfield',
                        fieldLabel: 'Timeout'.t(),
                        bind: '{settings.unblockTimeout}'
                    },{
                        xtype: 'label',
                        margin: '5 0 0 5',
                        html: 'seconds'.t(),
                        cls: 'boxlabel'
                    }]
                }]
            }, {
                xtype: 'checkbox',
                margin: '0 0 0 10',
                boxLabel: 'Require Password'.t(),
                hidden: true,
                disabled: true,
                bind: {
                    value: '{settings.unblockPasswordEnabled}',
                    disabled: '{settings.unblockMode === "None"}',
                    hidden: '{settings.unblockMode === "None"}'
                },
                listeners: {
                    disable: function (el) {
                        el.setValue(false);
                    }
                }
            }]
        }, {
            xtype: 'container',
            layout: {
                type: 'hbox',
                align: 'center'
            },
            margin: '0 0 0 20',
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{!settings.unblockPasswordEnabled}',
                disabled: '{!settings.unblockPasswordEnabled}'
            },
            items: [{
                xtype: 'radiogroup',
                margin: '10 0',
                columns: 2,
                simpleValue: true,
                bind: '{settings.unblockPasswordAdmin}',
                items: [
                    { boxLabel: 'Administrator Password'.t(), inputValue: true, width: 200 },
                    { boxLabel: 'Custom Password'.t(), inputValue: false }
                ]
            }, {
                xtype: 'textfield',
                inputType: 'password',
                disabled: true,
                hidden: true,
                bind: {
                    value: '{settings.unblockPassword}',
                    disabled: '{settings.unblockPasswordAdmin}',
                    hidden: '{settings.unblockPasswordAdmin}'
                },
                margin: '0 0 0 10',
                listeners: {
                    disable: function (el) {
                        el.setValue('');
                    }
                }
            }],
            listeners: {
                disable: function (el) {
                    el.down('radiogroup').setValue(false);
                }
            }
        }]
    }, {
        xtype: 'button',
        text: 'Clear Category URL Cache.'.t(),
        iconCls: 'fa fa-trash-o fa-red',
        handler: 'clearHostCache'
    }]

});
