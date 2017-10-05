Ext.define('Ung.view.reports.ReportWizardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.newreport',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function (view) {
        var me = this, vm = this.getViewModel();
        // me.tableConfig = TableConfig.generate(entry.get('table'));

        view.down('#categoryCombo').setValue('Hosts'); // preselect the first category which is always hosts



        vm.bind('{newEntry.table}', function (table) {
            me.tableConfig = TableConfig.generate(table);
            var tableColumns = me.tableConfig.comboItems;
            tableColumns.unshift({ text: 'No selection...', value: '' });
            vm.set('tableColumns', tableColumns);
            vm.set('newEntrySqlConditions', []);
            // view.down('#newEntrySqlConditionsCombo').getStore().setData(me.tableConfig.comboItems);
            // view.down('#newEntrySqlConditionsCombo').setValue(null);
        });
    },


    sqlColumnRenderer: function (val) {
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
    },


    addSqlCondition: function () {
        var me = this, vm = me.getViewModel(),
        conds = vm.get('newEntrySqlConditions') || [];

        conds.push({
            autoFormatValue: false,
            column: me.getView().down('#newEntrySqlConditionsCombo').getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition',
            operator: '=',
            value: ''
        });

        me.getView().down('#newEntrySqlConditionsCombo').setValue(null);

        vm.set('newEntrySqlConditions', conds);
        me.getView().down('#newEntrySqlConditions').getStore().reload();
    },

    removeSqlCondition: function (table, rowIndex) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('newEntrySqlConditions');
        Ext.Array.removeAt(conds, rowIndex);
        vm.set('newEntrySqlConditions', conds);
        me.getView().down('#newEntrySqlConditions').getStore().reload();
    },
});
