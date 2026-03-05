Ext.define('Ung.config.network.PortForwardSimple', {
    extend: 'Ext.window.Window',
    alias: 'widget.config-network-portforwardsimple',

    width: 800,
    minHeight: 400,
    maxHeight: Ext.getBody().getViewSize().height - 20,

    title: 'New Port Forward Rule'.t(),

    closeAction: 'destroy',
    closable: false,

    viewModel: {
        formulas: {
            _protocol: {
                get: function (get) {
                    var condition = Ext.Array.findBy(get('record.conditions.list'), function (cond) {
                        return cond.conditionType === 'PROTOCOL';
                    });

                    if(condition == null){
                        return -1;
                    }

                    return condition.value;

                },
                set: function (value) {
                    var condition = Ext.Array.findBy(this.get('record.conditions.list'), function (cond) {
                        return cond.conditionType === 'PROTOCOL';
                    });
                    if( condition != null ){
                        condition.value = value;
                    }
                }
            },
            _port: {
                get: function (get) {
                    var condition = Ext.Array.findBy(get('record.conditions.list'), function (cond) {
                        return cond.conditionType === 'DST_PORT';
                    });
                    if(condition == null){
                        return -1;
                    }

                    if (Ext.Array.indexOf([21, 25, 53, 80, 110, 143, 443, 1723], parseInt(condition.value, 10)) < 0) {
                        return -1;
                    } else {
                        return condition.value;
                    }
                },
                set: function (val) {
                    if (val !== -1) this.set('_portNo', val);
                }
            },
            _portNo: {
                get: function (get) {
                    var condition = Ext.Array.findBy(get('record.conditions.list'), function (cond) {
                        return cond.conditionType === 'DST_PORT';
                    });

                    if(condition == null){
                        return -1;
                    }

                    return condition.value;
                }
            }
        }
    },

    actions: {
        apply: {
            text: 'Done'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-ban',
            handler: 'onCancel'
        },
    },

    autoShow: true,
    modal: true,
    layout: 'fit',
    items: [{
        xtype: 'form',
        // region: 'center',
        scrollable: 'y',
        bodyPadding: 10,
        border: false,
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        items: [{
            xtype: 'checkbox',
            fieldLabel: 'Enable Port Forward Rule'.t(),
            name: 'enabled',
            bind: '{record.enabled}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Description'.t(),
            name: 'description',
            emptyText: '[no description]'.t(),
            bind: '{record.description}',
            allowBlank: false
        }, {
            xtype: 'fieldset',
            title: 'Forward the following traffic:'.t(),
            padding: 10,
            defaults: {
                labelWidth: 170,
                labelAlign : 'right',
                width: 300,
            },
            items: [{
                xtype: 'combo',
                fieldLabel: 'Protocol'.t(),
                name: 'protocol',
                editable: false,
                queryMode: 'local',
                bind: '{_protocol}',
                store: [['TCP,UDP', 'TCP & UDP'], ['TCP', 'TCP'], ['UDP', 'UDP']]
            }, {
                xtype: 'combo',
                reference: '_port',
                publishes: ['value'],
                name: 'port1',
                fieldLabel: 'Port'.t(),
                editable: false,
                queryMode: 'local',
                bind: {
                    value: '{_port}'
                },
                store: [[21, 'FTP (21)'], [25, 'SMTP (25)' ], [53, 'DNS (53)'], [80, 'HTTP (80)'], [110, 'POP3 (110)'], [143, 'IMAP (143)' ], [443, 'HTTPS (443)'], [1723, 'PPTP (1723)'], [-1, 'Other'.t()]],
                listeners: {
                    change: 'onPortChange'
                }
            }, {
                xtype: 'numberfield',
                fieldLabel: 'Port Number'.t(),
                name: 'port2',
                width: 250,
                minValue : 1,
                maxValue : 0xFFFF,
                vtype: 'port',
                allowBlank: false,
                disabled: true,
                hidden: true,
                bind: {
                    value: '{_portNo}',
                    disabled: '{_port.value !== -1}',
                    hidden: '{_port.value !== -1}'
                },
                listeners: {
                    change: 'onPortChange'
                }
            }]
        }, {
            xtype: 'fieldset',
            title: 'Traffic matching the above description destined to any Untangle IP will be forwarded to the new location:'.t(),
            padding: 10,
            items: [{
                xtype: 'textfield',
                allowBlank: false,
                fieldLabel: 'New Destination'.t(),
                name: 'newDestination',
                vtype: 'ipAddress',
                labelWidth: 170,
                labelAlign : 'right',
                bind: '{record.newDestination}'
            }]
        }],
        buttons: [{
            text: 'Switch to Advanced'.t(),
            iconCls: 'fa fa-exchange',
            handler: 'onSwitch'
        }, '->', '@cancel', '@apply']
    }],

    controller: {
        control: {
            '#': {
                afterrender: function (view) {
                    var vm = view.getViewModel(),
                        grid = view.up('ungrid');

                    if (view.record) {
                        // editing existing rule
                        vm.set('record', view.record.copy(null));
                        view.setTitle('Edit Port Forward Rule'.t());
                    } else {
                        // create new rule
                        vm.set('record', Ext.create('Ung.model.Rule', Ung.util.Util.activeClone(grid.emptyRow)));
                        view.setTitle('Add Port Forward Rule'.t());
                    }
                    view.down('form').isValid();
                }
            }
        },
        onPortChange: function (cmp, val) {
            var me = this, vm = me.getViewModel();
            var condition = Ext.Array.findBy(vm.get('record.conditions.list'), function (cond) {
                return cond.conditionType === 'DST_PORT';
            });
            if (val !== -1 && condition != null) {
                condition.value = val;
                vm.set('record.newPort', val);
            }
        },
        onApply: function () {
            var me = this, vm = me.getViewModel(),
                grid = me.getView().up('ungrid');

            if (me.getView().record) {
                // copy data into edited record
                me.getView().record.set(vm.get('record').getData());
            } else {
                // or add a new record
                vm.set('record.markedForNew', true);
                vm.set('record.simple', true); // when saving a simple rule (by default simple is set on false)
                grid.getStore().add(vm.get('record'));
            }
            me.getView().close();
        },

        onCancel: function () {
            this.getView().close();
        },

        onSwitch: function () {
            var me = this,
                grid = me.getView().up('ungrid'),
                record = me.getView().record; // pass record attached to the view not viewmodel

            // when editing an existing rule and switch to advanced
            if (record) {
                record.set('simple', false);
            }

            grid.getController().editorWin(record, me.getView().action);
            me.getView().close();
        }
    }
});
