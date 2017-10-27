Ext.define('Ung.reports.cmp.GlobalConditionsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.globalconditions',


    updateConditions: function (store) {
        var me = this, entryView = me.getView().up('entry'), count = store.getCount();

        me.getView().setTitle('Global Conditions'.t() + ': ' + ((count > 0) ? (' <span style="color: red;">' + count + ' condition(s)</span>') : 'none'.t()));


        entryView.getViewModel().set('globalConditions', Ext.Array.pluck(store.getRange(), 'data'));
        entryView.getController().reload();
    },

    sqlColumnRenderer: function (val) {
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
    },

    onColumnChange: function (cmp, newValue) {
        var me = this, vm = me.getViewModel();

        if (!vm.get('f_tableconfig') || !newValue) { return; }

        var column = Ext.Array.findBy(vm.get('f_tableconfig.columns'), function (column) {
            return column.dataIndex === newValue;
        });

        cmp.up('toolbar').remove('sqlFilterValue');

        if (column.widgetField) {
            column.widgetField.itemId = 'sqlFilterValue';
            cmp.up('toolbar').insert(4, column.widgetField);
        } else {
            cmp.up('toolbar').insert(4, {
                xtype: 'textfield',
                itemId: 'sqlFilterValue',
                value: ''
            });
        }
    },

    // onFilterKeyup: function (cmp, e) {
    //     if (e.keyCode === 13) {
    //         this.addSqlFilter();
    //     }
    // },

    sqlFilterQuickItems: function (btn) {
        var menuItem, menuItems = [];
        Rpc.asyncData('rpc.reportsManager.getConditionQuickAddHints').then(function (result) {
            Ext.Object.each(result, function (key, vals) {
                menuItem = {
                    text: TableConfig.getColumnHumanReadableName(key),
                    disabled: vals.length === 0
                };
                if (vals.length > 0) {
                    menuItem.menu = {
                        plain: true,
                        items: Ext.Array.map(vals, function (val) {
                            return {
                                text: val,
                                column: key
                            };
                        }),
                        listeners: {
                            click: 'selectQuickFilter'
                        }
                    };
                }
                menuItems.push(menuItem);


            });
            btn.getMenu().removeAll();
            btn.getMenu().add(menuItems);
        });
    },

    addSqlFilter: function () {
        var me = this, grid = me.getView(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator'),
            _filterValueCmp = me.getView().down('#sqlFilterValue');

        // var gc = vm.get('globalConditions');

        grid.getStore().add({
            column: _filterComboCmp.getValue(),
            operator: _operatorCmp.getValue(),
            value: _filterValueCmp.getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');
        grid.down('#filtersToolbar').remove('sqlFilterValue');
    },

    selectQuickFilter: function (menu, item) {
        var me = this, grid = me.getView(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator');

        grid.getStore().add({
            column: item.column,
            operator: '=',
            value: item.text,
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        grid.down('#filtersToolbar').remove('sqlFilterValue');
    }
});
