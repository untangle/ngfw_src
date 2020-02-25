/**
 * Reports Global Conditions used in Reports or Dashboard
  */
Ext.define('Ung.reports.cmp.GlobalConditions', {
    extend: 'Ext.container.Container',
    alias: 'widget.globalconditions',

    // context: 'REPORTS', // can be 'REPORTS' or 'DASHBOARD'

    viewModel: true,

    layout: {
        type: 'hbox',
        align: 'middle'
    },

    items: [{
        xtype: 'component',
        margin: '0 5',
        style: {
            fontSize: '11px'
        },
        html: '<strong>' + 'Conditions:'.t() + '</strong>'
    }, {
        xtype: 'container',
        layout: 'hbox'
    }, {
        xtype: 'button',
        text: 'Add'.t(),
        iconCls: 'fa fa-plus-circle',
        focusable: false,
        menu: {
            plain: true,
            showSeparator: false,
            mouseLeaveDelay: 0,
            items: [{
                xtype: 'radiogroup',
                itemId: 'add_column',
                simpleValue: true,
                reference: 'rg',
                publishes: 'value',
                fieldLabel: '<strong>' + 'Choose Column'.t() + '</strong>',
                labelAlign: 'top',
                columns: 1,
                vertical: true,
                items: [
                    { boxLabel: 'Username'.t(), name: 'col', inputValue: 'username' },
                    { boxLabel: 'Protocol'.t(), name: 'col', inputValue: 'protocol' },
                    { boxLabel: 'Hostname'.t(), name: 'col', inputValue: 'hostname' },
                    { boxLabel: 'Client'.t(), name: 'col', inputValue: 'c_client_addr' },
                    { boxLabel: 'Server'.t(), name: 'col', inputValue: 's_server_addr' },
                    { boxLabel: 'Server Port'.t(), name: 'col', inputValue: 's_server_port' },
                    { boxLabel: 'Policy Id'.t(), name: 'col', inputValue: 'policy_id' }
                ],
                listeners: {
                    change: function(el, newValue, oldValue){
                        var valueText = el.up('menu').down('#add_value_text');
                        var valueCombo =  el.up('menu').down('#add_value_combo');

                        var table = TableConfig.getFirstTableFromField(newValue);
                        var values = TableConfig.getValues(table, newValue);
                        if(values.length){
                            valueCombo.setDisabled(false);
                            valueCombo.setHidden(false);
                            valueCombo.getStore().loadData(values);
                            valueCombo.setValue(valueCombo.getValue() !== null ? valueCombo.getValue() : values[0][0]);
                            valueText.setDisabled(true);
                            valueText.setHidden(true);
                        }else{
                            valueCombo.setDisabled(true);
                            valueCombo.setHidden(true);
                            valueText.setDisabled(false);
                            valueText.setHidden(false);
                        }
                    }
                }
            }, '-', {
                xtype: 'combobox',
                itemId: 'add_operator',
                fieldLabel: '<strong>' + 'Operator'.t() + '</strong>',
                labelAlign: 'top',
                editable: false,
                queryMode: 'local',
                disabled: true,
                bind: {
                    disabled: '{!rg.value}'
                },
                value: '=',
                store: [
                    ['=', 'equals [=]'.t()],
                    ['!=', 'not equals [!=]'.t()],
                    ['>', 'greater than [>]'.t()],
                    ['<', 'less than [<]'.t()],
                    ['>=', 'greater or equal [>=]'.t()],
                    ['<=', 'less or equal [<=]'.t()],
                    ['like', 'like'.t()],
                    ['not like', 'not like'.t()],
                    ['is', 'is'.t()],
                    ['is not', 'is not'.t()],
                    ['in', 'in'.t()],
                    ['not in', 'not in'.t()]
                ]
            }, {
                xtype: 'checkbox',
                itemId: 'add_fmt',
                boxLabel: 'AutoFormat Value'.t(),
                value: true,
                disabled: true,
                bind: {
                    disabled: '{!rg.value}'
                }
            }, '-', {
                xtype: 'container',
                layout: {
                    type: 'hbox',
                    align: 'stretch'
                },
                margin: '5 5',
                defaults: {
                    disabled: true,
                    bind: {
                        disabled: '{!rg.value}'
                    },
                },
                items: [{
                    xtype: 'textfield',
                    itemId: 'add_value_text',
                    enableKeyEvents: true,
                    flex: 1,
                    listeners: {
                        keyup: function (el, e) {
                            if (e.keyCode === 13) {
                                el.up('menu').hide();
                            }
                        }
                    }
                }, {
                    xtype: 'combo',
                    itemId: 'add_value_combo',
                    queryMode: 'local',
                    hidden: true,
                    disabled: true,
                    store: new Ext.data.ArrayStore({
                        fields: ['value', 'display'],
                        data: []
                    }),
                    emptyText: 'Select value'.t(),
                    displayField: 'display',
                    valueField: 'value',
                    allowBlank: false,
                    editable: false
                }, {
                    xtype: 'button',
                    text: 'OK'.t(),
                    iconCls: 'fa fa-check',
                    margin: '0 0 0 5',
                    listeners: {
                        click: function (el) {
                            el.up('menu').hide();
                        }
                    }
                }]
            }, '-', {
                text: '<strong>' + 'More conditions ...'.t() + '</strong>',
                handler: 'onMoreConditions'
            }],
            listeners: {
                hide: 'onAddConditionHide'
            }
        }
    }],

    controller: {

        listen: {
            global: {
                initialload: 'onInitialLoad',
                addglobalcondition: 'onAddGlobalCondition'
            }
        },

        /**
         * When all the app data is ready (e.g. reports store is populated)
         */
        onInitialLoad: function () {
            var me = this, view = me.getView(), vm = me.getViewModel(),
                conditionsHolder = view.down('container'), // container in which conditions are rendered
                conditionsButtons = []; // array of conditions components

            /**
             * When query is changed, update the conditions toolbar
             */
            vm.bind('{query}', function (query) {
                conditionsButtons = [];
                conditionsHolder.removeAll(); // remove all conditions buttons

                var data = {};
                if(query.conditions){
                    query.conditions.forEach( function(condition){
                        data[condition.get("column")] = condition.get("value");
                    });
                }
                var renderRecord = new Ext.data.Model(data);

                Ext.Array.each(query.conditions, function (condition, idx) {
                    // condition button
                    var firstTable = TableConfig.getFirstTableFromField(condition.get('column'));
                    conditionsButtons.push({
                        xtype: 'segmentedbutton',
                        allowToggle: false,
                        margin: '0 5',
                        items: [{
                            text: TableConfig.getColumnHumanReadableName(condition.get('column')) +
                                ' <span style="font-weight: bold; margin: 0 3px;">' +
                                    condition.get('operator') + '</span> ' +
                                    TableConfig.getDisplayValue(condition.get('value'), condition.get('table') ? condition.get('table') : firstTable, condition.get('column'), renderRecord),
                            menu: {
                                plain: true,
                                showSeparator: false,
                                mouseLeaveDelay: 0,
                                condition: condition,
                                items: [{
                                    xtype: 'combobox',
                                    itemId: 'add_operator',
                                    fieldLabel: '<strong>' + 'Operator'.t() + '</strong>',
                                    labelAlign: 'top',
                                    editable: false,
                                    queryMode: 'local',
                                    value: condition.get('operator'),
                                    store: [
                                        ['=', 'equals [=]'.t()],
                                        ['!=', 'not equals [!=]'.t()],
                                        ['>', 'greater than [>]'.t()],
                                        ['<', 'less than [<]'.t()],
                                        ['>=', 'greater or equal [>=]'.t()],
                                        ['<=', 'less or equal [<=]'.t()],
                                        ['like', 'like'.t()],
                                        ['not like', 'not like'.t()],
                                        ['is', 'is'.t()],
                                        ['is not', 'is not'.t()],
                                        ['in', 'in'.t()],
                                        ['not in', 'not in'.t()]
                                    ],
                                    listeners: {
                                        change: function (rg, val) {
                                            condition.set('operator', val);
                                            me.redirect();
                                        }
                                    }
                                }, '-', {
                                    xtype: 'checkbox',
                                    boxLabel: 'AutoFormat Value'.t(),
                                    margin: 5,
                                    value: condition.get('autoFormatValue'),
                                    listeners: {
                                        change: function (el, val) {
                                            condition.set('autoFormatValue', val);
                                            me.redirect();
                                        }
                                    }
                                }, '-', {
                                    xtype: 'container',
                                    name: 'values',
                                    layout: {
                                        type: 'hbox',
                                        align: 'stretch'
                                    },
                                    margin: '5 5',
                                    items: [{
                                        xtype: 'textfield',
                                        name: 'valueText',
                                        enableKeyEvents: true,
                                        flex: 1,
                                        value: condition.get('value'),
                                        listeners: {
                                            keyup: function (el, e) {
                                                if (e.keyCode === 13) {
                                                    el.up('menu').hide();
                                                }
                                            }
                                        }
                                    }, {
                                        xtype: 'combo',
                                        name: 'valueCombo',
                                        queryMode: 'local',
                                        store: new Ext.data.ArrayStore({
                                            fields: ['value', 'display'],
                                            data: []
                                        }),
                                        emptyText: 'Select value'.t(),
                                        displayField: 'display',
                                        valueField: 'value',
                                        allowBlank: false,
                                        editable: false,
                                        value: condition.get('value'),
                                    }, {
                                        xtype: 'button',
                                        name: 'okButton',
                                        text: 'OK'.t(),
                                        iconCls: 'fa fa-check',
                                        margin: '0 0 0 5',
                                        listeners: {
                                            click: function (el) {
                                                el.up('menu').hide();
                                            }
                                        }
                                    }]
                                }],
                                listeners: {
                                    beforeshow: function(menu){
                                        var valueText = menu.down('[name=valueText]');
                                        var valueCombo =  menu.down('[name=valueCombo]');
                                        var values = TableConfig.getValues(menu.condition.get('table') ? menu.condition.get('table') : firstTable, menu.condition.get('column'), renderRecord);
                                        if(values.length){
                                            valueCombo.setDisabled(false);
                                            valueCombo.setHidden(false);
                                            valueCombo.getStore().loadData(values);
                                            valueCombo.setValue(valueCombo.getValue());
                                            valueText.setDisabled(true);
                                            valueText.setHidden(true);
                                        }else{
                                            valueCombo.setDisabled(true);
                                            valueCombo.setHidden(true);
                                            valueText.setDisabled(false);
                                            valueText.setHidden(false);
                                        }
                                    },
                                    show: function(menu){
                                        menu.down("[name=valueCombo]").setWidth(menu.up().getWidth() - menu.down("[name=okButton]").getWidth());
                                    },
                                    beforehide: function (menu) {
                                        var valueText = menu.down('[name=valueText]');
                                        var valueCombo =  menu.down('[name=valueCombo]');

                                        if(valueCombo.isVisible() == true){
                                            menu.condition.set('value', valueCombo.getValue());
                                        }else{
                                            menu.condition.set('value', valueText.getValue());
                                        }
                                    },
                                    hide: function () {
                                        me.redirect();
                                    }
                                }
                            }
                        }, {
                            // removes a condition
                            iconCls: 'fa fa-times',
                            condIndex: idx,
                            handler: function (el) {
                                Ext.Array.removeAt(query.conditions, el.condIndex);
                                me.redirect();
                            }
                        }]
                    });
                });

                conditionsHolder.add(conditionsButtons); // updates the conditions buttons
            });
        },

        /**
         * Add a new global condition from menu when menu closes
         */
        onAddConditionHide: function (menu) {
            var me = this, vm = me.getViewModel(),
                conditions = vm.get('query.conditions'),
                column = menu.down('#add_column').getValue(),
                operator = menu.down('#add_operator').getValue(),
                value = menu.down('#add_value_text').getValue() || menu.down('#add_value_combo').getValue() ,
                autoFormatValue = menu.down('#add_fmt').getValue();

            menu.down('#add_column').reset();
            menu.down('#add_operator').setValue('=');
            menu.down('#add_value_text').setValue('');
            menu.down('#add_value_combo').setValue('');
            menu.down('#add_fmt').setValue(true);

            if (!column || !operator || !value) {
                return;
            }

            conditions.push( new Ung.model.ReportCondition({
                column: column,
                operator: operator,
                value: value,
                autoFormatValue: autoFormatValue,
            }));
            me.redirect();
        },

        /**
         * Create new route based on new global conditions, and redirect to the new location
         */
        redirect: function () {
            var me = this,
                vm = me.getViewModel(),
                queryContext = '',
                queryArguments = [],
                route,
                conditions = vm.get('query.conditions');

            // view.context refers to dashboard or reports in admin servlet
            // app.context refers to admin or reports servlet
            if (Ung.app.conditionsContext === 'REPORTS') {
                queryContext = (Ung.app.context === 'REPORTS') ? '' : '#reports';
                route = vm.get('query.route');
                if (route.cat) {
                    queryArguments.push('cat=' + route.cat);
                }

                if (route.rep) {
                    queryArguments.push('rep=' + route.rep);
                }
            }

            if (Ung.app.conditionsContext === 'DASHBOARD') {
                queryContext = '#dashboard';
            }

            Ung.app.redirectTo(
                queryContext + 
                ( queryArguments.length && conditions.length ? '?' : '' ) +
                queryArguments.join("&") + Ung.model.ReportCondition.getAllQueries(vm.get('query.conditions'), queryArguments.length ? '&' : '')
            );
        },

        /**
         * Manages all possible conditions, besides those predefined in the Add menu,
         * Shows a dialog with all available tables/columns from which to add/remove conditions
         */
        onMoreConditions: function () {
            var me = this, vm = me.getViewModel();
            var tablesComboStore = [], columnsComboStore = [];

            Ext.Object.each(TableConfig.tableConfig, function (table) {
                tablesComboStore.push([table, table]);
            });

            Ext.Array.each(TableConfig.tableConfig['sessions'].columns, function (column) {
                columnsComboStore.push([column.dataIndex, column.text + ' [' + column.dataIndex + ']']);
            });

            var dialog = me.getView().add({
                xtype: 'window',
                renderTo: Ext.getBody(),
                modal: true,
                draggable: false,
                resizable: false,
                width: 800,
                height: 400,
                title: 'Global Conditions'.t(),
                layout: 'fit',
                items: [{
                    xtype: 'grid',
                    sortableColumns: false,
                    enableColumnHide: false,
                    dockedItems: [{
                        xtype: 'toolbar',
                        dock: 'top',
                        ui: 'footer',
                        items: [{
                            xtype: 'combo',
                            width: 250,
                            fieldLabel: 'Select Table'.t(),
                            labelAlign: 'top',
                            queryMode: 'local',
                            store: tablesComboStore,
                            emptyText: 'Select Table'.t(),
                            allowBlank: false,
                            editable: false,
                            value: 'sessions',
                            listeners: {
                                change: function (el, table) {
                                    var columns = TableConfig.tableConfig[table].columns, store = [];
                                    Ext.Array.each(columns, function (column) {
                                        store.push([column.dataIndex, column.text + ' [' + column.dataIndex + ']']);
                                    });
                                    el.nextNode().setStore(store);
                                    el.nextNode().setValue(store[0]);
                                }
                            }
                        }, {
                            xtype: 'combo',
                            fieldLabel: 'Select Column'.t(),
                            emptyText: 'Select Column'.t(),
                            flex: 1,
                            labelAlign: 'top',
                            queryMode: 'local',
                            editable: false,
                            allowBlank: false,
                            store: columnsComboStore,
                            value: columnsComboStore[0]
                        }, {
                            text: 'Add Column'.t(),
                            scale: 'large',
                            handler: function (el) {
                                var tbar = el.up('toolbar'),
                                    col = tbar.down('combo').nextNode().getValue();
                                    // store = el.up('grid').getStore();

                                // if (store.find('column', col) >= 0) {
                                //     Ext.Msg.alert('Info ...', 'Column <strong>' + col + '</strong> is already added!');
                                //     return;
                                // }

                                el.up('grid').getStore().add({
                                    column: col,
                                    value: '',
                                    operator: '=',
                                    autoFormatValue: true,
                                    javaClass: 'com.untangle.app.reports.SqlCondition'
                                });
                            }
                        }]
                    }],
                    bind: {
                        store: {
                            data: '{query.conditions}'
                        }
                    },
                    border: false,
                    columns: [{
                        text: 'Column'.t(),
                        dataIndex: 'column',
                        flex: 1,
                        renderer: function (val) {
                            return TableConfig.getColumnHumanReadableName(val) +  ' [' + val + ']';
                        }
                    }, {
                        xtype: 'widgetcolumn',
                        text: 'Operator'.t(),
                        width: 200,
                        dataIndex: 'operator',
                        widget: {
                            xtype: 'combo',
                            editable: false,
                            queryMode: 'local',
                            bind: '{record.operator}',
                            store: [
                                ['=', 'equals [=]'.t()],
                                ['!=', 'not equals [!=]'.t()],
                                ['>', 'greater than [>]'.t()],
                                ['<', 'less than [<]'.t()],
                                ['>=', 'greater or equal [>=]'.t()],
                                ['<=', 'less or equal [<=]'.t()],
                                ['like', 'like'.t()],
                                ['not like', 'not like'.t()],
                                ['is', 'is'.t()],
                                ['is not', 'is not'.t()],
                                ['in', 'in'.t()],
                                ['not in', 'not in'.t()]
                            ]
                        }
                    }, {
                        xtype: 'widgetcolumn',
                        text: 'Value'.t(),
                        width: 200,
                        widget: {
                            xtype: 'container',
                            layout: 'fit'
                        },
                        onWidgetAttach: 'onValueWidgetAttach'
                    }, {
                        xtype: 'checkcolumn',
                        text: 'AutoFormat'.t(),
                        width: 70,
                        dataIndex: 'autoFormatValue'
                    }, {
                        xtype: 'actioncolumn',
                        width: 40,
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-trash-o',
                        menuDisabled: true,
                        hideable: false,
                        handler: function (view, rowIndex, colIndex, item, e, record) {
                            record.drop();
                        }
                    }]
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban',
                    handler: function (el) {
                        el.up('window').hide();
                    }
                }, {
                    text: 'Apply'.t(),
                    iconCls: 'fa fa-check',
                    handler: function (el) {
                        var win = el.up('window'), store = win.down('grid').getStore();
                        var conditions = [];
                        Ext.Array.pluck(store.getRange(), 'data').forEach( function(data){
                            conditions.push(new Ung.model.ReportCondition(data) );
                        });
                        vm.set('query.conditions', conditions);
                        win.hide();
                        me.redirect();
                    }
                }]
            });

            dialog.show();
        },

        onValueWidgetAttach: function (column, container, record) {
            var me = this, vm = me.getViewModel();

            var data = {};
            vm.get('query.conditions').forEach( function(condition){
                data[condition.get("column")] = condition.get("value");
            });
            var renderRecord = new Ext.data.Model(data);

            var firstTable = TableConfig.getFirstTableFromField(record.get('column'));
            var values = TableConfig.getValues(record.get('table') ? record.get('table') : firstTable, record.get('column'), renderRecord);

            container.removeAll(true);

            if(values.length > 0){
                container.add({
                    xtype: 'combo',
                    queryMode: 'local',
                    store: new Ext.data.ArrayStore({
                        fields: ['value', 'display'],
                        data: values
                    }),
                    emptyText: 'Select value'.t(),
                    displayField: 'display',
                    valueField: 'value',
                    allowBlank: false,
                    editable: false,
                    bind: {
                        value: '{record.value}'
                    },
                });

            }else{
                container.add({
                    xtype: 'textfield',
                    bind: {
                        value: '{record.value}'
                    },
                });
            }
        },

        /**
         * Handles the addglobalcondition event
         * used when adding a new condition from a PIE_GRAPH Data View grid
         */
        cTest: [],
        onAddGlobalCondition: function (table, record, value) {
            var me = this, vm = me.getViewModel(),
                conditions = vm.get('query.conditions');

            var data = {};
            if(typeof(record) != 'object'){
                data[record] = value;
                record = new Ext.data.Model(data);
            }

            var newConditions = [];
            var msgConditions = [];
            data = record.getData();
            for(var field in data){
                if( TableConfig.getTableField(table, field) ){
                    newConditions.push( new Ung.model.ReportCondition({
                        column: field,
                        operator: '=',
                        value: data[field],
                        autoFormatValue: true,
                        table: table
                    }));
                    msgConditions.push(
                        '<strong>' + TableConfig.getColumnHumanReadableName(field) + ' [' + field + '] = ' + TableConfig.getDisplayValue(data[field], table, field, record) + '</strong>'
                    );
                }
            }
            if (!conditions) { return; }

            Ext.Msg.show({
                title: 'Add Global Condition'.t(),
                message: 'Add the following the Global Conditions?'.t() +
                        '<br/><br/>' +
                        msgConditions.join('<br>'),
                buttons: Ext.Msg.YESNO,
                icon: Ext.Msg.QUESTION,
                fn: function (btn) {
                    if (btn === 'yes') {
                        newConditions.forEach( function(condition){
                            conditions.push(condition);
                        });
                        me.redirect();
                    }
                }
            });
        },

        /**
         * Checks if global conditions have effect on selected report,
         * and shows a dialog message, then redirect to Reports home
         */
        checkDisabledSelection: function () {
            var me = this, vm = me.getViewModel(),
                selection = vm.get('selection'), conds = [], msg;

            if (!selection || !selection.get('disabled')) {
                return;
            }

            Ext.Array.each(vm.get('query.conditions'), function (c) {
                conds.push('<li><strong>' + TableConfig.getColumnHumanReadableName(c.column) + ' [' + c.column + '] ' + c.operator + ' ' + TableConfig.getDisplayValue(c.value, c.table, c.column) + '</strong></li>');
            });

            if (selection.isLeaf()) {
                msg = '<strong>' + selection.get('text') + '</strong> report!';
            } else {
                msg = '<strong>' + selection.get('text') + '</strong> category!';
            }

            Ext.Msg.show({
                renderTo: Ext.getBody(),
                width: 500,
                height: 200,
                title: 'Info',
                message: 'Global Conditions: <ul>' + conds.join('') + '</ul>do not apply on ' + msg + '<br/><p>Redirecting to Reports home!</p>',
                buttons: Ext.Msg.OK,
                icon: Ext.Msg.INFO,
                fn: function () {
                    vm.set('query.route', {});
                    me.redirect();
                }
            });
        }
    }
});
