Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            afterrender: 'onBeforeRender',
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
        // store: {
        //     data: view.record.get('conditions').list
        //     // data: '{record.conditions.list}'
        // },

        columns: [{
            header: 'Type'.t(),
            menuDisabled: true,
            // sortable: false,
            dataIndex: 'conditionType',
            width: 200,
            renderer: 'conditionRenderer'
        }, {
            xtype: 'widgetcolumn',
            menuDisabled: true,
            sortable: false,
            width: 70,
            widget: {
                xtype: 'combo',
                editable: false,
                bind: '{record.invert}',
                store: [[true, 'is NOT'.t()], [false, 'is'.t()]]
            }
            // widget: {
            //     xtype: 'segmentedbutton',
            //     bind: '{record.invert}',
            //     // bind: {
            //     //     value: '{record.invert}',
            //     // },
            //     items: [{
            //         text: 'IS',
            //         value: true
            //     }, {
            //         text: 'IS NOT',
            //         value: false
            //     }]
            // }
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
            iconCls: 'fa fa-trash',
            handler: 'removeCondition'
        }]
    },


    onBeforeRender: function (view) {
        var fields = view.fields, form = view.down('form');
        console.log(fields);
        // add editable column fields into the form
        for (var i = 0; i < fields.length; i++) {
            if (fields[i].editor) {
                form.add(fields[i].editor);
            }
            if (fields[i].getItemId() === 'conditions') {
                form.add({
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following conditions are met:'.t() + '</strong>'
                });
                form.add(this.conditionsGrid);
                form.add({
                    xtype: 'component',
                    padding: '0 0 10 0',
                    html: '<strong>' + view.actionDescription + '</strong>'
                });
            }
        }
        form.isValid();
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            store;

        // if conditions grid
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

        if (v.action === 'add') {
            console.log(v.record);
            v.store.add(v.record);
        }
        v.close();
    },

    onCancel: function () {
        this.getView().close();
    },
    // conditions grid

    onConditionsRender: function (conditionsGrid) {
        var conds = this.getView().conditions, menuConditions = [], i;

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
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    onWidgetAttach: function (column, container, record) {
        container.removeAll(true);

        var condition = this.getView().conditionsMap[record.get('conditionType')], i, ckItems = [];

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
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype
            });
            break;
        case 'numberfield':
            container.add({
                xtype: 'numberfield',
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
