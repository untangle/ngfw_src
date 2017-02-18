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
        },
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
            globalSL: {
                data: '{globalSafeListMap}'
            }
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
        '#': {
            beforerender: 'loadSettings'
        },
        '#quarantine': {
            beforerender: 'loadQuarantine'
        }
    },

    mailSender: rpc.UvmContext.mailSender(),
    originalMailSender: null,

    smtpNode: rpc.nodeManager.node('untangle-casing-smtp'),
    safelistAdminView: null,

    loadSettings: function (view) {
        this.safelistAdminView =  this.smtpNode.getSafelistAdminView();

        // load mail settings
        this.mailSettings();
        this.getSafeList();
    },

    saveSettings: function () {
        var deferred = new Ext.Deferred(),
            invalidFields = [];
        this.getView().query('form').forEach(function (form) {
            form.query('field{isValid()==false}').forEach(function (field) {
                invalidFields.push({ label: field.getFieldLabel(), error: field.getActiveError() });
            });
        });

        if (invalidFields.length > 0) {
            Ung.Util.invalidFormToast(invalidFields);
            deferred.reject('invalid fields');
        }

        var me = this, view = this.getView();
        view.setLoading('Saving ...');
        this.mailSender.setSettings(function(result, ex) {
            view.setLoading(false);
            if (ex) {
                console.error(ex);
                Ung.Util.exceptionToast(ex);
                deferred.reject(ex);
            }
            me.mailSettings();
            Ung.Util.successToast('Email'.t() + ' settings saved!');
            deferred.resolve();
        }, me.getViewModel().get('mailSender'));
        return deferred.promise;
    },

    mailSettings: function () {
        var me = this;
        this.mailSender.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('mailSender', result);
            me.originalMailSender = Ext.clone(result);
            console.log(me.getViewModel());
        });
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


    // Safe List
    getSafeList: function () {
        var me = this;
        me.safelistAdminView.getSafelistContents(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            me.getViewModel().set('globalSafeList', result);
        }, 'GLOBAL');
    },

    // Quarantine
    loadQuarantine: function () {
        var me = this;
        this.smtpNode.getSmtpNodeSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
             me.getViewModel().set('smtpNodeSettings', result);
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
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
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
                { boxLabel: 'Send email using the cloud hosted mail relay server'.t(), inputValue: 'RELAY' },
                { boxLabel: 'Send email directly'.t(), inputValue: 'DIRECT' },
                { boxLabel: 'Send email using the specified SMTP Server'.t(), inputValue: 'CUSTOM' }
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
Ext.define('Ung.config.email.view.SafeList', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.email.safelist',
    itemId: 'safelist',

    viewModel: true,
    title: 'Safe List'.t(),

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        region: 'center',

        title: 'Global Safe List'.t(),

        tbar: ['@add'],
        recordActions: ['@delete'],

        // listProperty: 'settings.dnsSettings.staticEntries.list',

        emptyRow: {
            emailAddress: 'email@' + rpc.hostname + '.com',
            // javaClass: 'com.untangle.uvm.network.DnsStaticEntry'
        },

        bind: '{globalSL}',

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
        region: 'south',

        height: '50%',
        split: true,

        title: 'Per User Safe Lists'.t(),

        // tbar: ['@add'],

        // bind: '{localServers}',

        columns: [{
            header: 'Account Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1
        }, {
            header: 'Safe List Size'.t(),
            width: 150,
            dataIndex: 'count',
            align: 'right'
        }],
    }]

});