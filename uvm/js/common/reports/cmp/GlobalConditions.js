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
                    //{ boxLabel: 'Client Port'.t(), name: 'col', inputValue: 'c_client_port' },
                    { boxLabel: 'Server Port'.t(), name: 'col', inputValue: 's_server_port' },
                    { boxLabel: 'Policy Id'.t(), name: 'col', inputValue: 'policy_id' }
                ]
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
                    itemId: 'add_value',
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

                Ext.Array.each(query.conditions, function (cond, idx) {

                    // condition button
                    conditionsButtons.push({
                        xtype: 'segmentedbutton',
                        allowToggle: false,
                        margin: '0 5',
                        items: [{
                            text: TableConfig.getColumnHumanReadableName(cond.column) + ' <span style="font-weight: bold; margin: 0 3px;">' + cond.operator + '</span> ' + cond.value,
                            menu: {
                                plain: true,
                                showSeparator: false,
                                mouseLeaveDelay: 0,
                                condition: cond,
                                items: [{
                                    xtype: 'container',
                                    layout: {
                                        type: 'hbox',
                                        align: 'stretch'
                                    },
                                    margin: '5 5',
                                    items: [{
                                        xtype: 'textfield',
                                        enableKeyEvents: true,
                                        flex: 1,
                                        value: cond.value,
                                        listeners: {
                                            keyup: function (el, e) {
                                                if (e.keyCode === 13) {
                                                    el.up('menu').hide();
                                                }
                                            }
                                        }
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
                                    // sets the condition operator
                                    xtype: 'radiogroup',
                                    simpleValue: true,
                                    publishes: 'value',
                                    // fieldLabel: '<strong>' + 'Operator'.t() + '</strong>',
                                    // labelAlign: 'top',
                                    columns: 1,
                                    vertical: true,
                                    value: cond.operator,
                                    items: [
                                        { boxLabel: 'equals [=]'.t(), name: 'op', inputValue: '=' },
                                        { boxLabel: 'not equals [!=]'.t(), name: 'op', inputValue: '!=' },
                                        { boxLabel: 'greater than [>]'.t(), name: 'op', inputValue: '>' },
                                        { boxLabel: 'less than [<]'.t(), name: 'op', inputValue: '<' },
                                        { boxLabel: 'greater or equal [>=]'.t(), name: 'op', inputValue: '>=' },
                                        { boxLabel: 'less or equal [<=]', name: 'op', inputValue: '<=' },
                                        { boxLabel: 'like'.t(), name: 'op', inputValue: 'like' },
                                        { boxLabel: 'not like'.t(), name: 'op', inputValue: 'not like' },
                                        { boxLabel: 'is'.t(), name: 'op', inputValue: 'is' },
                                        { boxLabel: 'is not'.t(), name: 'op', inputValue: 'is not' },
                                        { boxLabel: 'in'.t(), name: 'op', inputValue: 'in' },
                                        { boxLabel: 'not in'.t(), name: 'op', inputValue: 'not in' }
                                    ],
                                    listeners: {
                                        change: function (rg, val) {
                                            cond.operator = val;
                                            me.redirect();
                                        }
                                    }
                                }, '-', {
                                    xtype: 'checkbox',
                                    boxLabel: 'Autoformat Value'.t(),
                                    margin: 5,
                                    value: cond.autoFormatValue,
                                    listeners: {
                                        change: function (el, val) {
                                            cond.autoFormatValue = val;
                                            me.redirect();
                                        }
                                    }
                                }],
                                listeners: {
                                    beforehide: function (el) {
                                        el.condition.value = el.down('textfield').getValue();
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
                conds = vm.get('query.conditions'),
                col = menu.down('#add_column').getValue(),
                op = menu.down('#add_operator').getValue(),
                val = menu.down('#add_value').getValue(),
                fmt = menu.down('#add_fmt').getValue();

            menu.down('#add_column').reset();
            menu.down('#add_operator').setValue('=');
            menu.down('#add_value').setValue('');
            menu.down('#add_fmt').setValue(true);

            if (!col || !op || !val) {
                return;
            }

            conds.push({
                column: col,
                operator: op,
                value: val,
                autoFormatValue: fmt,
                javaClass: 'com.untangle.app.reports.SqlCondition'
            });
            me.redirect();
        },

        /**
         * Create new route based on new global conditions, and redirect to the new location
         */
        redirect: function () {
            var me = this, vm = me.getViewModel(),
                newQuery = '', route,
                conditions = vm.get('query.conditions');

            // view.context refers to dashboard or reports in admin servlet
            // app.context refers to admin or reports servlet
            if (Ung.app.conditionsContext === 'REPORTS') {
                newQuery = (Ung.app.context === 'REPORTS') ? '' : '#reports';
                route = vm.get('query.route');
                if (route.cat) {
                    newQuery += '?cat=' + route.cat;
                }

                if (route.rep) {
                    newQuery += '&rep=' + route.rep;
                }

                Ext.Array.each(conditions, function (cond, idx) {
                    newQuery += (idx === 0 && !route.cat) ? '?' : '&';
                    newQuery += cond.column + ':' + encodeURIComponent(cond.operator) + ':' + encodeURIComponent(cond.value) + ':' + (cond.autoFormatValue === true ? 1 : 0);
                });
            }

            if (Ung.app.conditionsContext === 'DASHBOARD') {
                newQuery = '#dashboard';
                Ext.Array.each(conditions, function (cond, idx) {
                    newQuery += (idx === 0) ? '?' : '&';
                    newQuery += cond.column + ':' + encodeURIComponent(cond.operator) + ':' + encodeURIComponent(cond.value) + ':' + (cond.autoFormatValue === true ? 1 : 0);
                });
            }

            Ung.app.redirectTo(newQuery);
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
                columnsComboStore.push([column.dataIndex, column.header + ' [' + column.dataIndex + ']']);
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
                                        store.push([column.dataIndex, column.header + ' [' + column.dataIndex + ']']);
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
                        dataIndex: 'value',
                        widget: {
                            xtype: 'textfield',
                            bind: '{record.value}'
                        }
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
                        vm.set('query.conditions', Ext.Array.pluck(store.getRange(), 'data'));
                        win.hide();
                        me.redirect();
                    }
                }]
            });

            dialog.show();
        },

        /**
         * Handles the addglobalcondition event
         * used when adding a new condition from a PIE_GRAPH Data View grid
         * */
        onAddGlobalCondition: function (col, val) {
            var me = this, vm = me.getViewModel(),
                conditions = vm.get('query.conditions'),
                readableColumn = TableConfig.getColumnHumanReadableName(col),
                msg = 'Add <strong>' + readableColumn + '</strong> column to the Global Conditions?'.t();

            if (!val) { return; } // val might be undefined in some cases (e.g. pie 'Others' value)

            // because this component is used in both dashboard and reports, event will fire twice
            // apply following logic to avoid exceptions
            if (!conditions) { return; }

            Ext.Msg.show({
                title: 'Add Global Condition'.t(),
                message: msg + '<br/><br/>' +
                            '<strong>' + readableColumn + ' [' + col + '] = ' + val + '</strong>',
                buttons: Ext.Msg.YESNO,
                icon: Ext.Msg.QUESTION,
                fn: function (btn) {
                    if (btn === 'yes') {
                        conditions.push({
                            column: col,
                            operator: '=',
                            value: val,
                            autoFormatValue: true,
                            javaClass: 'com.untangle.app.reports.SqlCondition'
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
                conds.push('<li><strong>' + TableConfig.getColumnHumanReadableName(c.column) + ' [' + c.column + '] ' + c.operator + ' ' + c.value + '</strong></li>');
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
