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

    recordBind: null,
    actionBind: null,

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
            width: 80,
            resizable: false,
            widget: {
                xtype: 'combo',
                // margin: 3,
                editable: false,
                bind: '{record.invert}',
                store: [[true, 'is NOT'.t()], [false, 'is'.t()]]
            }
        }, {
            header: 'Value'.t(),
            xtype: 'widgetcolumn',
            cellWrap: true,
            menuDisabled: true,
            sortable: false,
            flex: 1,
            widget: {
                xtype: 'container',
                padding: '1 3',
                layout: 'fit'
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
        var vm = this.getViewModel();
        this.mainGrid = v.up('grid');

        if (!v.record) {
            v.record = Ext.create('Ung.model.Rule', Ext.clone(this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            vm.set({
                record: v.record,
                windowTitle: 'Add'.t()
            });
        } else {
            this.getViewModel().set({
                record: v.record.copy(null),
                windowTitle: 'Edit'.t()
            });
        }

        /**
         * if record has action object
         * hard to explain but needed to keep dirty state (show as modified)
         */
        if (v.record.get('action') && (typeof v.record.get('action') === 'object')) {
            this.actionBind = vm.bind({
                bindTo: '{_action}',
                deep: true
            }, function (actionObj) {
                // console.log(actionObj);
                // console.log(vm.get('record.action'));
                // console.log(Ext.Object.equals(Ext.clone(actionObj), vm.get('record.action')));
                // if (!Ext.Object.equals(actionObj, vm.get('record.action'))) {
                vm.set('record.action', Ext.clone(actionObj));
                // }
            });
            vm.set('_action', v.record.get('action'));
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
                    html: '<strong>' + this.mainGrid.actionText + '</strong>'
                });
            }
        }
        form.isValid();
        // setTimeout(view.center();
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            condStore;

        // if conditions
        if (v.down('grid')) {
            condStore = v.down('grid').getStore();
            if (condStore.getModifiedRecords().length > 0 || condStore.getRemovedRecords().length > 0 || condStore.getNewRecords().length > 0) {
                v.record.set('conditions', {
                    javaClass: 'java.util.LinkedList',
                    list: Ext.Array.pluck(condStore.getRange(), 'data')
                });
            }
        }

        if (!this.action) {
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
            javaClass: this.mainGrid.ruleJavaClass,
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
        var me = this;
        container.removeAll(true);

        var condition = this.mainGrid.conditionsMap[record.get('conditionType')], i;

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
            var ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    // name: 'ck',
                    inputValue: condition.values[i][0],
                    boxLabel: condition.values[i][1]
                });
            }
            container.add({
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}'
                },
                columns: 3,
                vertical: true,
                defaults: {
                    padding: '0 10 0 0'
                },
                items: ckItems
            });
            break;
        case 'countryfield':
            // container.layout = { type: 'hbox', align: 'stretch'};
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select countries or specify a custom value ...',
                store: { type: 'countries' },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                value: record.get('value'),
                displayField: 'name',
                valueField: 'code',
                listConfig: {
                    itemTpl: ['<div>{name} <strong>[{code}]</strong></div>']
                },
                listeners: {
                    change: function (field, newValue) {
                        // transform array into comma separated values string
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }

                    }
                }
            });
            break;
        case 'userfield':
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select a user or specify a custom value ...',
                store: { data: [] },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                // value: record.get('value'),
                displayField: 'uid',
                valueField: 'uid',
                listConfig: {
                    itemTpl: ['<div>{uid}</div>']
                },
                listeners: {
                    afterrender: function (field) {
                        var app, data = [];
                        try {
                            app = rpc.appManager.app('directory-connector');
                        } catch (e) {
                            Util.exceptionToast(e);
                        }
                        if (app) {
                            data = app.getUserEntries().list;
                        } else {
                            data.push({ firstName: '', lastName: null, uid: '[any]', displayName: 'Any User'});
                            data.push({ firstName: '', lastName: null, uid: '[authenticated]', displayName: 'Any Authenticated User'});
                            data.push({ firstName: '', lastName: null, uid: '[unauthenticated]', displayName: 'Any Unauthenticated/Unidentified User'});
                        }
                        field.getStore().loadData(data);
                        field.setValue(record.get('value'));
                    },
                    change: function (field, newValue) {
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }
                    }
                }
            });
            break;
        case 'directorygroupfield':
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select a group or specify a custom value ...',
                store: { data: [] },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                // value: record.get('value'),
                displayField: 'displayName',
                valueField: 'SAMAccountName',
                listConfig: {
                    itemTpl: ['<div>{displayName} <strong>[{SAMAccountName}]</strong></div>']
                },
                listeners: {
                    afterrender: function (field) {
                        var app, data = [];
                        try {
                            app = rpc.appManager.app('directory-connector');
                        } catch (e) {
                            Util.exceptionToast(e);
                        }
                        if (app) {
                            data = app.getGroupEntries().list;
                        }
                        data.push({ SAMAccountName: '*', displayName: 'Any Group'});

                        field.getStore().loadData(data);
                        field.setValue(record.get('value'));
                    },
                    change: function (field, newValue) {
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }
                    }
                }
            });
            break;
        case 'timefield':
            container.add({
                xtype: 'container',
                layout: { type: 'hbox', align: 'middle' },
                hoursStore: (function () {
                    var arr = [];
                    for (var i = 0; i <= 23; i += 1) {
                        arr.push(i < 10 ? '0' + i : i.toString());
                    }
                    return arr;
                })(),
                minutesStore: (function () {
                    var arr = [];
                    for (var i = 0; i <= 59; i += 1) {
                        arr.push(i < 10 ? '0' + i : i.toString());
                    }
                    return arr;
                })(),
                defaults: {
                    xtype: 'combo', store: [], width: 40, editable: false, queryMode: 'local', listeners: {
                        change: function (combo, newValue) {
                            var view = combo.up('container'), period = '';
                            record.set('value', view.down('#hours1').getValue() + ':' + view.down('#minutes1').getValue() + '-' + view.down('#hours2').getValue() + ':' + view.down('#minutes2').getValue());
                        }
                    }
                },
                items: [
                    { itemId: 'hours1', },
                    { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                    { itemId: 'minutes1' },
                    { xtype: 'component', html: 'to'.t(), width: 'auto', margin: '0 3' },
                    { itemId: 'hours2' },
                    { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                    { itemId: 'minutes2' }
                ],
                listeners: {
                    afterrender: function (view) {
                        view.down('#hours1').setStore(view.hoursStore);
                        view.down('#minutes1').setStore(view.minutesStore);
                        view.down('#hours2').setStore(view.hoursStore);
                        view.down('#minutes2').setStore(view.minutesStore);
                        if (!record.get('value')) {
                            view.down('#hours1').setValue('12');
                            view.down('#minutes1').setValue('00');
                            view.down('#hours2').setValue('13');
                            view.down('#minutes2').setValue('30');
                        } else {
                            startTime = record.get('value').split('-')[0];
                            endTime = record.get('value').split('-')[1];
                            view.down('#hours1').setValue(startTime.split(':')[0]);
                            view.down('#minutes1').setValue(startTime.split(':')[1]);
                            view.down('#hours2').setValue(endTime.split(':')[0]);
                            view.down('#minutes2').setValue(endTime.split(':')[1]);
                        }
                    }
                }
            });
            break;
        }
    },

    onDestroy: function () {
        this.recordBind.destroy();
        this.recordBind = null;
        this.actionBind.destroy();
        this.actionBind = null;
    }
});
