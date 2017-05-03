Ext.define('Ung.config.email.view.Quarantine', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-email-quarantine',
    itemId: 'quarantine',
    helpSource: 'email_quarantine',
    title: 'Quarantine'.t(),

    viewModel: {
        formulas: {
            maxHoldTime: {
                get: function (get) { return get('smtpSettings.quarantineSettings.maxMailIntern') / (1440*60*1000); },
                set: function (value) { this.set('smtpSettings.quarantineSettings.maxMailIntern', value * (1440*60*1000)); }
            },
            digestHour: {
                get: function (get) {
                    var date = new Date();
                    date.setHours(get('smtpSettings.quarantineSettings.digestHourOfDay'));
                    date.setMinutes(get('smtpSettings.quarantineSettings.digestMinuteOfDay'));
                    return date;
                },
                set: function (value) {
                    this.set('smtpSettings.quarantineSettings.digestHourOfDay', value.getHours());
                    this.set('smtpSettings.quarantineSettings.digestMinuteOfDay', value.getMinutes());
                }
            },
            quarantineTotalDisk: function (get) {
                return Ext.String.format('Total Disk Space Used: {0} MB'.t(), get('inboxesTotalSize')/(1024 * 1024));
            }
        },
        stores: {
            qInboxes: { data: '{inboxesList.list}' },
            qAddresses: { data: '{smtpSettings.quarantineSettings.allowedAddressPatterns.list}' },
            qForwards: { data: '{smtpSettings.quarantineSettings.addressRemaps.list}' }
        }
    },

    layout: 'border',

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
                labelWidth: 250
            },
            items: [{
                xtype: 'numberfield',
                fieldLabel: 'Maximum Holding Time (days)'.t(),
                allowBlank: false,
                minValue: 0,
                maxValue: 99,
                regex: /^([0-9]|[0-9][0-9])$/,
                regexText: 'Maximum Holding Time must be a number in range 0-99'.t(),
                bind: '{maxHoldTime}'
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Send Daily Quarantine Digest Emails'.t(),
                bind: '{smtpSettings.quarantineSettings.sendDailyDigests}'
            }, {
                xtype: 'timefield',
                fieldLabel: 'Quarantine Digest Sending Time'.t(),
                allowBlank: false,
                editable: false,
                increment: 30,
                bind: '{digestHour}'
            }, {
                xtype: 'component',
                margin: '10 0 0 0',
                html: Ext.String.format('Users can also request Quarantine Digest Emails manually at this link: <b>https://{0}/quarantine/</b>'.t(), rpc.networkManager.getPublicUrl())
            }]
        }, {
            xtype: 'grid',
            reference: 'inboxesGrid',
            title: 'User Quarantines'.t(),
            flex: 1,

            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
            },
            selModel: {
                selType: 'checkboxmodel'
            },

            bind: '{qInboxes}',

            tbar: [{
                text: 'Purge Selected'.t(),
                iconCls: 'fa fa-circle fa-red',
                handler: 'purgeInboxes',
                disabled: true,
                bind: {
                    disabled: '{!inboxesGrid.selection}'
                }
            }, {
                text: 'Release Selected'.t(),
                iconCls: 'fa fa-circle fa-green',
                handler: 'releaseInboxes',
                disabled: true,
                bind: {
                    disabled: '{!inboxesGrid.selection}'
                }
            }, '->', {
                xtype: 'tbtext',
                bind: {
                    text: '{quarantineTotalDisk}'
                }
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
            region: 'center',
            title: 'Quarantinable Addresses'.t(),

            dockedItems: [{
                xtype: 'component',
                padding: 10,
                style: {
                    fontSize: '11px'
                },
                html: 'Email addresses on this list will have quarantines automatically created. All other emails will be marked and not quarantined.'.t(),
                dock: 'top'
            }],

            layout: 'fit',

            items: [{
                xtype: 'ungrid',
                border: false,

                bind: '{qAddresses}',

                tbar: ['@addInline', '->', '@import', '@export'],
                recordActions: ['delete'],
                listProperty: 'smtpSettings.quarantineSettings.allowedAddressPatterns.list',
                emptyRow: {
                    javaClass: 'com.untangle.app.smtp.EmailAddressRule',
                    address: ''
                },

                columns: [{
                    header: 'Quarantinable Address'.t(),
                    flex: 1,
                    dataIndex: 'address',
                    renderer: function (value) {
                        return value || '<em>click to edit</em>';
                    },
                    editor: {
                        xtype: 'textfield',
                        emptyText: '[enter email address rule]'.t(),
                        allowBlank: false,
                        vtype: 'email'
                    }
                }],
            }]
        }, {
            region: 'south',
            height: '50%',
            split: true,
            title: 'Quarantine Forwards'.t(),

            dockedItems: [{
                xtype: 'component',
                padding: 10,
                style: {
                    fontSize: '11px'
                },
                html: 'This is a list of email addresses whose quarantine digest gets forwarded to another account. This is common for distribution lists where the whole list should not receive the digest.'.t(),
                dock: 'top'
            }],

            layout: 'fit',

            items: [{
                xtype: 'ungrid',
                border: false,
                forceFit: true,

                bind: '{qForwards}',

                tbar: ['@addInline', '->', '@import', '@export'],
                recordActions: ['delete'],

                listProperty: 'smtpSettings.quarantineSettings.addressRemaps.list',
                emptyRow: {
                    javaClass: 'com.untangle.app.smtp.EmailAddressPairRule',
                    address1: '',
                    address2: '',
                },

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
    }]

});
