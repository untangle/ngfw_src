Ext.define('Ung.reports.cmp.GlobalConditionsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.globalconditions',

    sqlColumnRenderer: function (val) {
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
    },

    addSqlFilter: function () {
        var me = this, vm = me.getViewModel(), grid = me.getView(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator'),
            _filterValueCmp = me.getView().down('#sqlFilterValue');

        vm.get('globalConditions').push({
            column: _filterComboCmp.getValue(),
            operator: _operatorCmp.getValue(),
            value: _filterValueCmp.getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        grid.down('#filtersToolbar').remove('sqlFilterValue');
        grid.setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('globalConditions').length));
        grid.getStore().reload();

        grid.up('entry').getController().reload();
    },

    removeSqlFilter: function (table, rowIndex) {
        var me = this, vm = me.getViewModel(), grid = me.getView();
        Ext.Array.removeAt(vm.get('globalConditions'), rowIndex);

        grid.down('#filtersToolbar').remove('sqlFilterValue');
        grid.setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('globalConditions').length));
        grid.getStore().reload();

        grid.up('entry').getController().reload();
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

    onFilterKeyup: function (cmp, e) {
        if (e.keyCode === 13) {
            this.addSqlFilter();
        }
    },

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

    selectQuickFilter: function (menu, item) {
        var me = this, vm = this.getViewModel(), grid = me.getView(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator');

        vm.get('globalConditions').push({
            column: item.column,
            operator: '=',
            value: item.text,
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        grid.down('#filtersToolbar').remove('sqlFilterValue');

        grid.setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('globalConditions').length));
        grid.getStore().reload();

        grid.up('entry').getController().reload();
    }
});
