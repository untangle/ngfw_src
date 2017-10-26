Ext.define('Ung.apps.bandwidthcontrol.ConfWizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-bandwidth-control-wizard',
    title: '<i class="fa fa-magic"></i> ' + 'Bandwidth Control Setup Wizard'.t(),
    modal: true,

    controller: 'app-bandwidth-control-wizard',
    viewModel: {
        type: 'app-bandwidth-control-wizard'
    },

    width: 800,
    height: 450,

    layout: 'card',

    defaults: {
        border: false,
        scrollable: 'y',
        bodyPadding: 10
    },

    items: [{
        title: 'Welcome'.t(),
        header: false,
        itemId: 'welcome',
        layout: {
            type: 'vbox'
        },
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Welcome to the Bandwidth Control Setup Wizard!'.t() + '</h2>',
        }, {
            xtype: 'component',
            html: '<p>' + 'This wizard will help guide you through your initial setup and configuration of Bandwidth Control.'.t() + '</p>',
        }, {
            xtype: 'component',
            html: '<p>' + 'Bandwidth Control leverages information provided by other applications in the rack.'.t() + '</p><ul>' +
                '<li>' + 'Web Filter (non-Lite) provides web site categorization.'.t() + '</li>' +
                '<li>' + 'Application Control provides protocol profiling categorization.'.t() + '</li>' +
                '<li>' + 'Application Control Lite provides protocol profiling categorization.'.t() + '</li>' +
                '<li>' + 'Directory Connector provides username/group information.'.t() + '</li></ul>' +
                '<p><strong>' + 'For optimal Bandwidth Control performance install these applications.'.t() + '</strong></p>'
        }, {
            xtype: 'component',
            html: '<i class="fa fa-exclamation-triangle fa-red"></i> ' + 'WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'.t()
        }]
    }, {
        title: 'WAN Bandwidth'.t(),
        header: false,
        itemId: 'wan',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Configure WANs download and upload bandwidths'.t() + '</h2>'
        }, {
            xtype: 'component',
            html: '<p>' + Ext.String.format('{0}Note:{1} When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.'.t(), '<font color="red">','</font>') + '<br/>' +
                'It is suggested to set these around 95% to 100% of the actual measured bandwidth available for each WAN.'.t() + '</p>',
        }, {
            xtype: 'component',
            bind: {
                html: '<p><i class="fa fa-exclamation-triangle fa-red"></i> <span style="color: red;">' + 'WARNING: These settings must be reasonably accurate for Bandwidth Control to operate properly!'.t() + '</span> </p>' +
                    '<p>{bandwidthLabel}</p>'
            }
        }, {
            xtype: 'ungrid',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,

            emptyText: 'No WAN Interfaces defined'.t(), 
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1
            },

            bind: {
                store: {
                    data: '{interfaces}',
                    filters: [
                        { property: 'configType', value: 'ADDRESSED' },
                        { property: 'isWan', value: true }
                    ]
                }
            },

            columns: [{
                header: 'Interface Id'.t(),
                align: 'right',
                width: Renderer.idWidth,
                resizable: false,
                dataIndex: 'interfaceId'
            }, {
                header: 'WAN'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                dataIndex: 'name'
            }, {
                header: 'Download Bandwidth'.t(),
                width: Renderer.sizeWidth,
                flex: 1,
                resizable: false,
                dataIndex: 'downloadBandwidthKbps',
                editor: {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: function (value) {
                    return Ext.isEmpty(value) ? 'Not set'.t() : (value + ' ' + 'kbps'.t());
                }
            }, {
                header: 'Upload Bandwidth'.t(),
                width: Renderer.sizeWidth,
                flex: 1,
                resizable: false,
                dataIndex: 'uploadBandwidthKbps',
                editor: {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: function (value) {
                    return Ext.isEmpty(value) ? 'Not set'.t() : (value + ' ' + 'kbps'.t());
                }
            }]
        }]
    }, {
        title: 'Configuration'.t(),
        header: false,
        itemId: 'configuration',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Choose a starting configuration'.t() + '</h2>'
        }, {
            xtype: 'component',
            html: '<p>' + 'Several initial default configurations are available for Bandwidth Control. Please select the environment most like yours below.'.t() + '</p>',
        }, {
            xtype: 'combo',
            fieldLabel: 'Configuration'.t(),
            margin: 10,
            editable: false,
            bind: '{selectedConf}',
            store: [
                ['business_business', 'Business'.t()],
                ['school_school', 'School'.t()],
                ['school_college', 'College/University'.t()],
                ['business_government', 'Government'.t()],
                ['business_nonprofit', 'Non-Profit'.t()],
                ['school_hotel', 'Hotel'.t()],
                ['home', 'Home'.t()],
                ['metered', 'Metered Internet'.t()],
                ['custom', 'Custom'.t()]
            ]
        }, {
            xtype: 'component',
            margin: 10,
            bind: {
                html: '{confDetails}'
            }
        }]
    }, {
        title: 'Quotas'.t(),
        header: false,
        itemId: 'quotas',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Configure Quotas'.t() + '</h2>'
        }, {
            xtype: 'component',
            html: '<p>' + 'Quotas for bandwidth can be set for hosts and users. This allows some hosts/users to be allocated high bandwidth, as long as it is remains within a certain usage quota; however, their bandwidth will be slowed if their quota is exceeded.'.t() + '</p>'
        }, {
            xtype: 'fieldset',
            checkboxToggle: true,
            checkbox: {
                bind: {
                    value: '{quota.enabled}'
                }
            },
            collapsible: true,
            title: 'Enable Quotas'.t(),
            margin: 10,
            padding: 10,
            items: [{
                xtype: "checkbox",
                bind: "{quota.hostEnabled}",
                fieldLabel: "Enable Quotas for Hosts (IP addresses)".t(),
                labelWidth: 250,
                checked: true
            }, {
                xtype: "checkbox",
                bind: "{quota.userEnabled}",
                fieldLabel: "Enable Quotas for Users (usernames)".t(),
                labelWidth: 250,
                checked: true
            }, {
                xtype: 'component',
                margin: '10 0 5 0',
                html: '<strong>' + 'Quota Expiration'.t() + '</strong><br/>' +
                    '<span style="font-size: 11px; color: #555;">' + 'controls how long a quota lasts (hourly, daily, weekly). The default is Daily.'.t() + '</span>'
            }, {
                xtype: 'combo',
                allowBlank: false,
                editable: false,
                forceSelection: false,
                store: [
                    [-4, 'End of Month'.t()], //END_OF_MONTH
                    [-3, 'End of Week'.t()], //END_OF_WEEK
                    [-2, 'End of Day'.t()], //END_OF_DAY
                    [-1, 'End of Hour'.t()] //END_OF_HOUR
                ],
                queryMode: 'local',
                bind: '{quota.expiration}'
            }, {
                xtype: 'component',
                margin: '10 0 5 0',
                html: '<strong>' + 'Quota Size'.t() + '</strong><br/>' +
                    '<span style="font-size: 11px; color: #555;">' + 'configures the size of the quota given to each host. The default is 1 Gb.'.t() + '</span>'
            }, {
                xtype: 'container',
                // width: 200,
                layout: {
                    type: 'hbox'
                },
                items: [{
                    xtype: 'numberfield',
                    allowBlank: false,
                    width: 80,
                    bind: '{quota.size}'
                }, {
                    xtype: 'combo',
                    margin: '0 0 0 5',
                    width: 100,
                    editable: false,
                    store: [
                        [1, 'bytes'.t()],
                        [1024, 'Kilobytes'.t()],
                        [1024*1024, 'Megabytes'.t()],
                        [1024*1024*1024, 'Gigabytes'.t()],
                        [1024*1024*1024*1024, 'Terrabytes'.t()]
                    ],
                    queryMode: 'local',
                    bind: '{quota.unit}'
                }]
            }, {
                xtype: 'component',
                margin: '10 0 5 0',
                html: '<strong>' + 'Quota Exceeded Priority'.t() + '</strong><br/>' +
                    '<span style="font-size: 11px; color: #555;">' + 'configures the priority given to hosts that have exceeded their quota.'.t() + '</span>'
            }, {
                xtype: 'combo',
                allowBlank: false,
                width: 200,
                editable: false,
                store: [
                    [1, 'Very High'.t()],
                    [2, 'High'.t()],
                    [3, 'Medium'.t()],
                    [4, 'Low'.t()],
                    [5, 'Limited'.t()],
                    [6, 'Limited More'.t()],
                    [7, 'Limited Severely'.t()]
                ],
                queryMode: 'local',
                bind: '{quota.priority}'
            }]
        }]
    }, {
        title: 'Finish'.t(),
        header: false,
        itemId: 'finish',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Congratulations'.t() + '</h2>'
        }, {
            xtype: 'component',
            html: '<p><strong>' + 'Bandwidth Control is now configured and enabled.'.t() + '</strong></p>'
        }]
    }],

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        defaults: {
            minWidth: 200
        },
        items: [{
            hidden: true,
            bind: {
                text: 'Previous'.t() + ' - <strong>' + '{prevBtnText}' + '</strong>',
                hidden: '{!prevBtn}'
            },
            iconCls: 'fa fa-chevron-circle-left',
            handler: 'onPrev'
        }, '->',  {
            hidden: true,
            bind: {
                text: 'Next'.t() + ' - <strong>' + '{nextBtnText}' + '</strong>',
                hidden: '{!nextBtn}'
            },
            iconCls: 'fa fa-chevron-circle-right',
            iconAlign: 'right',
            handler: 'onNext'
        }, {
            text: 'Close'.t(),
            hidden: true,
            bind: {
                hidden: '{nextBtn}'
            },
            iconCls: 'fa fa-check',
            handler: 'onFinish'
        }]
    }]


});
