Ext.define('Ung.config.email.view.Quarantine', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.email.quarantine',
    itemId: 'quarantine',

    title: 'Quarantine'.t(),

    viewModel: {
        formulas: {
            maxHoldTime: function (get) {
                return get('smtpNodeSettings.quarantineSettings.maxMailIntern') / (1440*60*1000);
            }
        },
        stores: {
            qAddresses: {
                data: '{smtpNodeSettings.quarantineSettings.allowedAddressPatterns.list}'
            },
            qForwards: {
                data: '{smtpNodeSettings.quarantineSettings.addressRemaps.list}'
            }
        }
    },


    layout: 'border',

    actions: {
        purge: {
            text: 'Purge Selected'.t()
        },
        release: {
            text: 'Release Selected'.t()
        }
    },

    items: [{
        region: 'center',
        border: false,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'form',
            bodyPadding: 20,
            defaults: {
                labelWidth: 250,
                labelAlign: 'right'
            },
            items: [{
                xtype: 'numberfield',
                fieldLabel: 'Maximum Holding Time (days)'.t(),
                allowBlank: false,
                minValue: 0,
                maxValue: 99,
                // regex: /^([0-9]|[0-9][0-9])$/,
                // regexText: 'Maximum Holding Time must be a number in range 0-99'.t(),
                bind: '{maxHoldTime}'
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Send Daily Quarantine Digest Emails'.t(),
                bind: '{smtpNodeSettings.quarantineSettings.sendDailyDigests}'
            }, {
                xtype: 'timefield',
                fieldLabel: 'Quarantine Digest Sending Time'.t(),
                allowBlank: false,
                increment: 1,
                bind: '{smtpNodeSettings.quarantineSettings.digestHourOfDay}'
            }, {
                xtype: 'component',
                margin: '10 0 0 0',
                bind: {
                    html: '{smtpNodeSettings.quarantineSettings.maxMailIntern}'
                }
                //html: Ext.String.format('Users can also request Quarantine Digest Emails manually at this link: <b>https://{0}/quarantine/</b>'.t(), rpc.networkManager.getPublicUrl())
            }]
        }, {
            xtype: 'grid',
            title: 'User Quarantines'.t(),
            flex: 1,
            tbar: ['@purge', '@release', '->', {
                xtype: 'tbtext',
                html: 'to see'
                // html: Ext.String.format('Total Disk Space Used: {0} MB'.t(), i18n.numberFormat((this.getQuarantineMaintenenceView().getInboxesTotalSize()/(1024 * 1024)).toFixed(3)))
            }],
            columns: [{
                header: 'Account Address'.t(),
                dataIndex: 'address',
                flex: 1
            }, {
                header: 'Message Count'.t(),
                width: 185,
                align: 'right',
                dataIndex: 'totalMails'
            }, {
                header: 'Data Size (kB)'.t(),
                width: 185,
                align: 'right',
                dataIndex: 'totalSz'
            }]
        }]
    }, {
        region: 'east',
        width: '40%',
        split: true,
        border: false,
        layout: 'border',

        items: [{
            xtype: 'ungrid',
            region: 'center',
            title: 'Quarantinable Addresses'.t(),

            tbar: ['@add'],
            recordActions: ['@delete'],

            emptyRow: {
                address: ''
            },

            bind: '{qAddresses}',

            columns: [{
                header: 'Quarantinable Address'.t(),
                flex: 1,
                dataIndex: 'address',
                renderer: function (value) {
                    return value || '<em>click to edit</em>';
                },
                editor: {
                    xtype: 'textfield',
                    emptyText: "[enter email address rule]".t(),
                    allowBlank: false,
                    vtype: 'email'
                }
            }]

        }, {
            xtype: 'ungrid',
            region: 'south',
            height: '50%',
            split: true,
            forceFit: true,

            title: 'Quarantine Forwards'.t(),

            tbar: ['@add'],
            recordActions: ['@delete'],

            emptyRow: {
                address1: '',
                address2: ''
            },

            bind: '{qForwards}',

            columns: [{
                header: 'Distribution List Address'.t(),
                dataIndex: 'address1',
                width: 200,
                renderer: function (value) {
                    return value || '<em>click to edit</em>';
                },
                editor: {
                    xtype: 'textfield',
                    emptyText: 'distributionlistrecipient@example.com'.t(),
                    vtype: 'email',
                    allowBlank: false
                }
            }, {
                header: 'Send to Address'.t(),
                dataIndex: 'address2',
                flex: 1,
                renderer: function (value) {
                    return value || '<em>click to edit</em>';
                },
                editor: {
                    xtype: 'textfield',
                    emptyText: 'quarantinelistowner@example.com'.t(),
                    vtype: 'email',
                    allowBlank: false
                }
            }]

        }]
    }]

});