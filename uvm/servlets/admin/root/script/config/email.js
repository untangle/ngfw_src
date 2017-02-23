Ext.define('Ung.config.email.Email', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.email',

    requires: [
        'Ung.config.email.EmailController',
        'Ung.config.email.EmailTest',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],

    controller: 'config.email',

    viewModel: {
        data: {
            globalSafeList: null,
        }
    },

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'Email'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    items: [
        { xtype: 'config.email.outgoingserver' },
        { xtype: 'config.email.safelist' },
        { xtype: 'config.email.quarantine' }
    ]
});

Ext.define('Ung.config.email.EmailController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.email',

    control: {
        '#': { afterrender: 'loadSettings' }
        // '#quarantine': { beforerender: 'loadQuarantine' }
    },

    // mailSender: rpc.UvmContext.mailSender(),
    originalMailSender: null,

    // smtpNode: rpc.nodeManager.node('untangle-casing-smtp'),
    // safelistAdminView: null,

    loadSettings: function (view) {
        var vm = this.getViewModel(), me = this;
        rpc.mailSender = rpc.UvmContext.mailSender();
        rpc.smtpSettings = rpc.nodeManager.node('untangle-casing-smtp');
        rpc.safelistAdminView = rpc.smtpSettings.getSafelistAdminView();
        rpc.quarantineMaintenenceView = rpc.smtpSettings.getQuarantineMaintenenceView();

        view.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise ('rpc.mailSender.getSettings'),
            Rpc.asyncPromise ('rpc.smtpSettings.getSmtpNodeSettings'),
            Rpc.asyncPromise ('rpc.safelistAdminView.getSafelistContents', 'GLOBAL'),
            Rpc.directPromise('rpc.safelistAdminView.getUserSafelistCounts'),
            Rpc.asyncPromise ('rpc.quarantineMaintenenceView.listInboxes'),
            Rpc.directPromise('rpc.quarantineMaintenenceView.getInboxesTotalSize')
        ], this).then(function(result) {
            vm.set({
                mailSender: result[0],
                smtpSettings: result[1],
                globalSafeList: result[2],
                userSafeList: result[3],
                inboxesList: result[4],
                inboxesTotalSize: result[5]
            });
            me.originalMailSender = Ext.clone(result[0]);
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    },


    // using promise because of the testEmail need
    saveSettings: function () {
        var deferred = new Ext.Deferred();

        if (!Util.validateForms(this.getView())) {
            return;
        }

        var me = this, view = this.getView(), vm = this.getViewModel();
        view.setLoading('Saving ...');


        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();

            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {

                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;

                if (grid.getItemId() === 'safeListStore') { // this needs to be transformed back to array
                    var emails = [];
                    store.each(function(record) {
                        emails.push(record.get('emailAddress'));
                    });
                    vm.set('globalSafeList', emails);
                } else {
                    vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                }
                // store.commitChanges();
            }
        });


        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.mailSender.setSettings', me.getViewModel().get('mailSender')),
            Rpc.asyncPromise('rpc.smtpSettings.setSmtpNodeSettingsWithoutSafelists', vm.get('smtpSettings')),
            Rpc.asyncPromise('rpc.safelistAdminView.replaceSafelist', 'GLOBAL', vm.get('globalSafeList'))
        ], this)
        .then(function() {
            Util.successToast('Email'.t() + ' settings saved!');
            // me.loadSettings();
            deferred.resolve();
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function () {
            view.setLoading(false);
        });
        return deferred.promise;
    },

    testEmail: function () {
        var me = this, vm = this.getViewModel(),
            modifiedVal = Ext.encode(vm.get('mailSender')),
            originalVal = Ext.encode(me.originalMailSender);

        if (originalVal !== modifiedVal) {
            Ext.Msg.show({
                title: 'Save Changes?'.t(),
                msg: Ext.String.format('Your current settings have not been saved yet.{0}Would you like to save your settings before executing the test?'.t(), '<br />'),
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function(btnId) {
                    if (btnId === 'yes') {
                        me.saveSettings().then(function () {
                            Ext.create('Ung.config.email.EmailTest');
                        });
                    }
                    if (btnId === 'no') {
                        Ext.create('Ung.config.email.EmailTest');
                    }
                },
                animEl: 'elId',
                icon: Ext.MessageBox.QUESTION
            });
        } else {
            Ext.create('Ung.config.email.EmailTest');
        }
    },

    purgeUserSafeList: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.safelistAdminView.deleteSafelists', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },


    purgeInboxes: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Purging...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.deleteInboxes', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    releaseInboxes: function (btn) {
        var me = this,
            grid = btn.up('grid'),
            selected = grid.getSelectionModel().getSelected(),
            accounts = [];

        if (!selected || selected.length === 0) {
            return;
        }

        selected.each(function(record) {
            accounts.push(record.get('address'));
        });

        Ext.MessageBox.wait('Releasing...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.quarantineMaintenenceView.rescueInboxes', accounts)
            .then(function() {
                me.loadSettings();
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }
});

Ext.define('Ung.config.email.EmailTest', {
    extend: 'Ext.window.Window',
    width: 500,
    height: 300,
    modal: true,

    alias: 'widget.config.email.test',

    requires: [
        'Ung.config.email.EmailTestController'
    ],

    controller: 'config.email.test',

    title: 'Email Test'.t(),
    autoShow: true,

    layout: 'fit',
    plain: false,
    bodyBorder: false,
    border: false,

    items: [{
        xtype: 'form',
        // border: false,
        bodyPadding: 10,
        layout: 'anchor',
        items: [{
            xtype: 'component',
            html: '<strong style="margin-bottom:20px;font-size:11px;display:block;">' + 'Enter an email address to send a test message and then press Send. That email account should receive an email shortly after running the test. If not, the email settings may not be correct.<br/><br/>It is recommended to verify that the email settings work for sending to both internal (your domain) and external email addresses.'.t() + '</strong>'
        }, {
            xtype: 'textfield',
            anchor: '100%',
            vtype: 'email',
            validateOnBlur: true,
            allowBlank: false,
            fieldLabel: 'Email Address'.t(),
            emptyText: '[enter email]'.t(),
            bind: {
                disabled: '{processing === true}'
            }
        }, {
            xtype: 'component',
            bind: {
                html: '{emailRef}'
            }
        }, {
            xtype: 'component',
            padding: 10,
            style: {
                textAlign: 'center',
            },
            hidden: true,
            bind: {
                html: '{processingIcon}',
                hidden: '{processing === false}'
            }
        }],

        buttons: [{
            text: 'Cancel'.t(),
            handler: 'cancel'
        }, {
            text: 'Send'.t(),
            formBind: true,
            handler: 'sendMail'
        }]
    }],

    viewModel: {
        data: {
            processingIcon: null,
            processing: false
        },
    }

});

Ext.define('Ung.config.email.EmailTestController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.email.test',

    sendMail: function (btn) {
        var v = this.getView(), vm = this.getViewModel();
        btn.setDisabled(true);
        vm.set({
            processing: true,
            processingIcon: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i>'
        });
        rpc.UvmContext.mailSender().sendTestMessage(function (result, ex) {
            btn.setDisabled(false);
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
            vm.set({
                processing: null,
                processingIcon: '<i class="fa fa-check fa-3x fa-fw" style="color: green;"></i> <br/>' + 'Success'.t()
            });
        }, v.down('textfield').getValue());
    },
    cancel: function () {
        this.getView().close();
    }

});

Ext.define('Ung.config.email.view.OutgoingServer', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config.email.outgoingserver',
    itemId: 'outgoingserver',

    viewModel: true,
    title: 'Outgoing Server'.t(),

    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Outgoing Email Server'.t(),
        items: [{
            xtype: 'component',
            padding: 10,
            html: Ext.String.format('The Outgoing Email Server settings determine how the {0} Server sends emails such as reports, quarantine digests, etc. In most cases the cloud hosted mail relay server is preferred. Alternatively, you can configure mail to be sent directly to mail account servers. You can also specify a valid SMTP server that will relay mail for the {0} Server.'.t(), rpc.companyName)
        }, {
            xtype: 'radiogroup',
            margin: '0 0 10 10',
            simpleValue: true,
            bind: '{mailSender.sendMethod}',
            columns: 1,
            vertical: true,
            items: [
                { boxLabel: '<strong>' + 'Send email using the cloud hosted mail relay server'.t() + '</strong>', inputValue: 'RELAY' },
                { boxLabel: '<strong>' + 'Send email directly'.t() + '</strong>', inputValue: 'DIRECT' },
                { boxLabel: '<strong>' + 'Send email using the specified SMTP Server'.t() + '</strong>', inputValue: 'CUSTOM' }
            ]
        }, {
            xtype: 'fieldset',
            border: false,
            defaults: {
                labelWidth: 200,
                labelAlign: 'right'
            },
            disabled: true,
            bind: {
                disabled: '{mailSender.sendMethod !== "CUSTOM"}'
            },
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Server Address or Hostname'.t(),
            }, {
                xtype: 'textfield',
                width: 260,
                fieldLabel: 'Server Port'.t(),
                bind: '{mailSender.smtpPort}',
                vtype: 'port'
            }, {
                xtype: 'component',
                margin: '0 0 0 200',
                padding: 5,
                html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'Warning:'.t() + ' ' + 'SMTPS (465) is deprecated and not supported. Use STARTTLS (587).'.t(),
                hidden: true,
                bind: {
                    hidden: '{mailSender.smtpPort !== "465"}'
                }
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Use Authentication'.t(),
                reference: 'cb',
                bind: '{mailSender.authUser !== null}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Login'.t(),
                hidden: true,
                bind: {
                    value: '{mailSender.authUser}',
                    hidden: '{!cb.checked}'
                }
            }, {
                xtype: 'textfield',
                inputType: 'password',
                fieldLabel: 'Password'.t(),
                hidden: true,
                bind: {
                    value: '{mailSender.authPass}',
                    hidden: '{!cb.checked}'
                }
            }]
        } ]
    }, {
        xtype: 'fieldset',
        title: 'Email From Address'.t(),
        padding: 10,
        items: [{
            xtype: 'textfield',
            fieldLabel: Ext.String.format('The {0} Server will send email from this address.'.t(), rpc.companyName),
            labelAlign: 'top',
            emptyText: '[enter email]'.t(),
            vtype: 'email',
            allowBlank: false,
            bind: '{mailSender.fromAddress}'
        }]
    }, {
        xtype: 'fieldset',
        title: 'Email Test'.t(),
        padding: 10,
        items: [{
            xtype: 'component',
            html: 'The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.'.t()
        }, {
            xtype: 'button',
            margin: '10 0 0 0',
            text: 'Email Test'.t(),
            handler: 'testEmail'
        }]
    }]

});
Ext.define('Ung.config.email.view.Quarantine', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.email.quarantine',
    itemId: 'quarantine',

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

                tbar: ['@addInline'],
                recordActions: ['@delete'],
                listProperty: 'smtpSettings.quarantineSettings.allowedAddressPatterns.list',
                emptyRow: {
                    javaClass: 'com.untangle.node.smtp.EmailAddressRule',
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

                tbar: ['@addInline'],
                recordActions: ['@delete'],

                listProperty: 'smtpSettings.quarantineSettings.addressRemaps.list',
                emptyRow: {
                    javaClass: 'com.untangle.node.smtp.EmailAddressPairRule',
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

Ext.define('Ung.config.email.view.SafeList', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.email.safelist',
    itemId: 'safelist',

    title: 'Safe List'.t(),

    viewModel: {
        formulas: {
            globalSafeListMap: function (get) {
                if (get('globalSafeList')) {
                    return Ext.Array.map(get('globalSafeList'), function (email) {
                        return { emailAddress: email };
                    });
                }
                return {};
            }
        },
        stores: {
            globalSL: { data: '{globalSafeListMap}' },
            userSL: { data: '{userSafeList.list}' }
        }
    },

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        itemId: 'safeListStore',
        region: 'center',
        title: 'Global Safe List'.t(),

        bind: '{globalSL}',

        tbar: ['@addInline'],
        recordActions: ['@delete'],
        // listProperty: '',
        emptyRow: {
            emailAddress: 'email@' + rpc.hostname + '.com'
        },

        columns: [{
            header: 'Email Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.emailAddress}',
                emptyText: '[enter email]'.t(),
                vtype: 'email'
            }
        }]
    }, {
        xtype: 'grid',
        reference: 'userSafeList',
        region: 'east',

        width: '50%',
        split: true,

        title: 'Per User Safe Lists'.t(),

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
        },
        selModel: {
            selType: 'checkboxmodel'
        },

        bind: '{userSL}',

        tbar: [{
            text: 'Purge Selected'.t(),
            iconCls: 'fa fa-circle fa-red',
            handler: 'purgeUserSafeList',
            disabled: true,
            bind: {
                disabled: '{!userSafeList.selection}'
            }
        }],

        columns: [{
            header: 'Account Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1
        }, {
            header: 'Safe List Size'.t(),
            width: 150,
            dataIndex: 'count',
            align: 'right'
        }, {
            // todo: the show detail when available data
            header: 'Show Detail'.t()
        }],
    }]

});
