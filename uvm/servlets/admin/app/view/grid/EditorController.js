Ext.define('Ung.view.grid.EditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ung.grideditor',

    onBeforeRender: function (view) {
        var form = view.lookupReference('form');

        var vm = view.getViewModel();

        view.columns.forEach(function (column) {
            if (column.editorField === 'textfield' || column.editorField === 'textarea') {
                form.add({
                    xtype: column.editorField,
                    fieldLabel: column.text,
                    labelAlign: 'right',
                    labelWidth: 150,
                    width: '100%',
                    disabled: column.editDisabled,
                    vtype: column.getEditor().vtype,
                    allowBlank: column.getEditor() ? column.getEditor().allowBlank : true,
                    emptyText  : column.text,
                    //maskRe     : /[a-z]/i,
                    msgTarget : 'under',
                    bind: {
                        value: '{record.' + column.dataIndex + '}'
                    }
                });
            }

            if (column.editorField === 'checkbox') {
                form.add({
                    xtype: column.editorField,
                    fieldLabel: column.text,
                    labelAlign: 'right',
                    labelWidth: 150,
                    disabled: column.editDisabled,
                    msgTarget : 'under',
                    bind: {
                        value: '{record.' + column.dataIndex + '}'
                    }
                });
            }

            if (column.editorField === 'action') {
                var formulas = {
                    showPriority: {
                        get: function (get) {
                            return get('record.actionType') === 'SET_PRIORITY';
                        }
                    },
                    showPenalty: {
                        get: function (get) {
                            return get('record.actionType') === 'PENALTY_BOX_CLIENT_HOST';
                        }
                    },
                    showQuota: {
                        get: function (get) {
                            return get('record.actionType') === 'GIVE_CLIENT_HOST_QUOTA';
                        }
                    }
                };
                vm.setFormulas(formulas);

                vm.bind({
                    bindTo: '{record.actionType}'
                }, function (val) {
                    if (val === 'SET_PRIORITY') {
                        vm.set('record.actionPriority', 1);
                    }
                    if (val === 'GIVE_CLIENT_HOST_QUOTA') {
                        vm.set('record.actionQuotaTime', -3);
                    }
                });

                form.add([{
                    xtype: 'component',
                    padding: '10 0 5 0',
                    margin: '0 0 0 155',
                    style: {
                        fontWeight: 'bold'
                    },
                    html: 'Perform the following action:'.t()
                }, {
                    xtype: 'combo',
                    fieldLabel: 'Action Type'.t(),
                    labelAlign: 'right',
                    labelWidth: 150,
                    width: 400,
                    displayField: 'displayName',
                    valueField: 'name',
                    msgTarget : 'under',
                    editable: false,
                    bind: '{record.actionType}',
                    queryMode: 'local',
                    store: Ext.create('Ext.data.Store', {
                        fields: ['name', 'displayName'],
                        data: [
                            { name: 'SET_PRIORITY', displayName: 'Set Priority'.t() },
                            { name: 'PENALTY_BOX_CLIENT_HOST', displayName: 'Send Client to Penalty Box'.t() },
                            { name: 'GIVE_CLIENT_HOST_QUOTA', displayName: 'Give Client a Quota'.t() }
                        ]
                    })
                }, {
                    xtype: 'combo',
                    fieldLabel: 'Priority'.t(),
                    labelAlign: 'right',
                    labelWidth: 150,
                    width: 300,
                    displayField: 'displayName',
                    valueField: 'value',
                    msgTarget : 'under',
                    editable: false,
                    hidden: true,
                    bind: {
                        value: '{record.actionPriority}',
                        hidden: '{!showPriority}',
                        disabled: '{!showPriority}'
                    },
                    store: Ext.create('Ext.data.Store', {
                        fields: ['value', 'displayName'],
                        data: [
                            //{ value: 0, displayName: '' },
                            { value: 1, displayName: 'Very High'.t() },
                            { value: 2, displayName: 'High'.t() },
                            { value: 3, displayName: 'Medium'.t() },
                            { value: 4, displayName: 'Low'.t() },
                            { value: 5, displayName: 'Limited'.t() },
                            { value: 6, displayName: 'Limited More'.t() },
                            { value: 7, displayName: 'Limited Severely'.t() }
                        ]
                    })
                }, {
                    xtype: 'numberfield',
                    fieldLabel: 'Penalty Time'.t(),
                    labelAlign: 'right',
                    labelWidth: 150,
                    width: 250,
                    hidden: true,
                    bind: {
                        hidden: '{!showPenalty}',
                        disabled: '{!showPenalty}',
                        value: '{record.actionPenaltyTime}'
                    }
                }, {
                    xtype: 'combo',
                    fieldLabel: 'Quota Expiration'.t(),
                    labelAlign: 'right',
                    labelWidth: 150,
                    displayField: 'displayName',
                    valueField: 'value',
                    msgTarget : 'under',
                    editable: false,
                    hidden: true,
                    bind: {
                        hidden: '{!showQuota}',
                        disabled: '{!showQuota}',
                        value: '{record.actionQuotaTime}'
                    },
                    store: Ext.create('Ext.data.Store', {
                        fields: ['value', 'displayName'],
                        data: [
                            //{ value: 0, displayName: '' },
                            { value: -3, displayName: 'End of Week'.t() },
                            { value: -2, displayName: 'End of Day'.t() },
                            { value: -1, displayName: 'End of Hour'.t() }
                        ]
                    })
                }, {
                    xtype: 'container',
                    hidden: true,
                    layout: {
                        type: 'hbox'
                    },
                    bind: {
                        hidden: '{!showQuota}',
                        disabled: '{!showQuota}'
                    },
                    items: [{
                        xtype: 'numberfield',
                        fieldLabel: 'Quota Size'.t(),
                        labelAlign: 'right',
                        labelWidth: 150,
                        width: 250,
                        bind: {
                            value: '{record.actionQuotaBytes}'
                        }
                    }, {
                        xtype: 'combo',
                        displayField: 'unit',
                        valueField: 'value',
                        editable: false,
                        value: 1,
                        width: 100,
                        margin: '0 0 0 5',
                        store: Ext.create('Ext.data.Store', {
                            fields: ['value', 'unit'],
                            data: [
                                { value: 1, unit: 'bytes'.t() },
                                { value: 1000, unit: 'Kilobytes'.t() },
                                { value: 1000000, unit: 'Megabytes'.t() },
                                { value: 1000000000, unit: 'Gigabytes'.t() },
                                { value: 1000000000000, unit: 'Terrabytes'.t() }
                            ]
                        })
                    }]
                }]);
            }

            if (column.editorField === 'conditions') {
                console.log(vm.get('record.conditions'));
                form.add([{
                    xtype: 'component',
                    padding: '10 0 5 0',
                    margin: '0 0 0 155',
                    style: {
                        fontWeight: 'bold'
                    },
                    html: 'If all of the following conditions are met:'.t()
                }, {
                    xtype: 'ung.gridconditions',
                    margin: '0 0 0 155',
                    viewModel: {
                        stores: {
                            store: {
                                model: 'Ung.model.Condition',
                                data: '{record.' + column.dataIndex + '}'
                            }
                        }
                    }

                }]);
            }


            /*
            if (column.dataIndex && column.editorField) {
                console.log(column.dataIndex);
                if (column.editorField === 'ung.gridconditions') {
                    form.add({
                        xtype: 'ung.gridconditions',
                        bind: {
                            //record: '{record}',
                            conditions: '{record.' + column.dataIndex + '}'
                        },
                        /*
                        viewModel: {
                            data: {
                                conditions: '{record.' + column.dataIndex + '}'
                            }
                        }


                        viewModel: {
                            stores: {
                                store: {
                                    model: 'Ung.model.Rule',
                                    data: '{record.' + column.dataIndex + '}'
                                }
                            }
                        }

                    });
                } else {

                }
            }
            */
        });
    },

    /*
    onBeforeRender: function () {
        var vm = this.getViewModel();
        vm.bind({
            bindTo: '{record}',
            single: true
        }, function () {});
    },
    */

    /*
    onAfterRender: function (form) {
        form.keyNav = Ext.create('Ext.util.KeyNav', form.el, {
            enter: function () {
                console.log('enter');
            }
        });
    },
    */

    onSave: function () {
        this.getView().setCloseAction('save');
        this.getView().close();
    },

    onCancel: function () {
        this.getView().setCloseAction('cancel');
        this.getView().close();
    }

});