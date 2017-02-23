Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender',
            // close: 'onDestroy'
            // beforerender: 'onBeforeRender',
            // close: 'onClose',
        },
        'grid': {
            afterrender: 'onConditionsRender'
        }
    },

    conditionsGrid: {
        xtype: 'grid',
        trackMouseOver: false,
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        padding: '10 0',
        tbar: ['@addCondition'],
        bind: {
            store: {
                model: 'Ung.model.Condition',
                data: '{record.conditions.list}'
            }
        },
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Conditions! Add from the menu...</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Type'.t(),
            menuDisabled: true,
            dataIndex: 'conditionType',
            align: 'right',
            width: 200,
            renderer: 'conditionRenderer'
        }, {
            xtype: 'widgetcolumn',
            menuDisabled: true,
            width: 120,
            resizable: false,
            // widget: {
            //     xtype: 'combo',
            //     editable: false,
            //     bind: '{record.invert}',
            //     store: [[true, 'is NOT'.t()], [false, 'is'.t()]]
            // }
            widget: {
                xtype: 'segmentedbutton',
                bind: '{record.invert}',
                // bind: {
                //     value: '{record.invert}',
                // },
                items: [{
                    text: 'is'.t(),
                    // iconCls: 'fa fa-check fa-green',
                    value: false
                }, {
                    text: 'is NOT'.t(),
                    // iconCls: 'fa fa-ban fa-red',
                    value: true
                }]
            }
        }, {
            header: 'Value'.t(),
            xtype: 'widgetcolumn',
            menuDisabled: true,
            sortable: false,
            flex: 1,
            widget: {
                xtype: 'container',
                padding: '0 3'
                // layout: {
                //     type: 'hbox'
                // }
            },
            onWidgetAttach: 'onWidgetAttach'
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            sortable: false,
            width: 30,
            align: 'center',
            iconCls: 'fa fa-minus-circle fa-red',
            tdCls: 'action-cell-cond',
            handler: 'removeCondition'
        }]
    },


    onBeforeRender: function (v) {
        this.mainGrid = v.up('grid');

        if (!v.record) {
            v.record = Ext.create('Ung.model.Rule', Ext.clone(this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            this.getViewModel().set({
                record: v.record,
                windowTitle: 'Add'.t()
            });
        } else {
            this.getViewModel().set({
                record: v.record.copy(null),
                windowTitle: 'Edit'.t()
            });
        }


    },

    onAfterRender: function (view) {
        var fields = this.mainGrid.editorFields, form = view.down('form');
        // add editable column fields into the form
        for (var i = 0; i < fields.length; i++) {
            if (fields[i].dataIndex !== 'conditions') {
                form.add(fields[i]);
            } else {
                form.add({
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following conditions are met:'.t() + '</strong>'
                });
                form.add(this.conditionsGrid);
                form.add({
                    xtype: 'component',
                    padding: '0 0 10 0',
                    html: '<strong>' + (this.mainGrid.actionText || 'Perform the following action(s):'.t()) + '</strong>'
                });
            }
        }
        // form.isValid();
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            store;

        // if conditions grid
        if (!this.action) {
            if (v.down('grid')) {
                store = v.down('grid').getStore();

                if (store.getModifiedRecords().length > 0 || store.getRemovedRecords().length > 0 || store.getNewRecords().length > 0) {
                    v.record.set('conditions', {
                        javaClass: 'java.util.LinkedList',
                        list: Ext.Array.pluck(store.getRange(), 'data')
                    });
                }
            }

            for (var field in vm.get('record').modified) {
                if (field !== 'conditions') {
                    v.record.set(field, vm.get('record').get(field));
                }
            }
        }
        if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }
        v.close();
    },

    onCancel: function () {
        this.getView().close();
    },
    // conditions grid

    onConditionsRender: function (conditionsGrid) {
        var conds = this.mainGrid.conditions, menuConditions = [], i;

        // when record is modified update conditions menu
        this.recordBind = this.getViewModel().bind({
            bindTo: '{record}',
        }, this.setMenuConditions);

        // create and add conditions to the menu
        for (i = 0; i < conds.length; i += 1) {
            menuConditions.push({
                text: conds[i].displayName,
                conditionType: conds[i].name,
                index: i
            });
        }

        conditionsGrid.down('#addConditionBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menuConditions,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addCondition'
            }
        });
    },

    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuConditions: function () {
        var conditionsGrid = this.getView().down('grid'),
            menu = conditionsGrid.down('#addConditionBtn').getMenu(),
            store = conditionsGrid.getStore();
        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('conditionType', item.conditionType) ? true : false);
        });
    },

    /**
     * Adds a new condition for the edited rule
     */
    addCondition: function (menu, item) {
        var newCond = {
            conditionType: item.conditionType,
            invert: false,
            javaClass: this.getViewModel().get('ruleJavaClass'),
            value: ''
        };
        this.getView().down('grid').getStore().add(newCond);
        this.setMenuConditions();
    },

    /**
     * Removes a condition from the rule
     */
    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        // record.drop();
        this.getView().down('grid').getStore().remove(record);
        this.setMenuConditions();
    },

    /**
     * Renders the condition name in the grid
     */
    conditionRenderer: function (val) {
        return '<strong>' + this.mainGrid.conditionsMap[val].displayName + ':</strong>';
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    onWidgetAttach: function (column, container, record) {
        container.removeAll(true);

        var condition = this.mainGrid.conditionsMap[record.get('conditionType')], i, ckItems = [];

        // if (container.items.length >= 1) {
        //     return;
        // }

        switch (condition.type) {
        case 'boolean':
            container.add({
                xtype: 'component',
                padding: 3,
                html: 'True'.t()
            });
            break;
        case 'textfield':
            container.add({
                xtype: 'textfield',
                style: { margin: 0 },
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype
            });
            break;
        case 'numberfield':
            container.add({
                xtype: 'numberfield',
                style: { margin: 0 },
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype
            });
            break;
        case 'checkboxgroup':
            // console.log(condition.values);
            // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    inputValue: condition.values[i][0],
                    boxLabel: condition.values[i][1]
                });
            }
            container.add({
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}'
                },
                columns: 4,
                vertical: true,
                defaults: {
                    padding: '0 10 0 0'
                },
                items: ckItems
            });
        }
    },

    onDestroy: function () {
        console.log('destroy');
        this.recordBind.destroy();
        this.recordBind = null;
    }
});
