Ext.define('Ung.view.dashboard.NewWidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.new-widget',

    control: {
        '#': { afterrender: 'onAfterRender', beforeclose: 'onClose' }
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();
        if (vm.get('entry')) {
            me.lookup('tree').selectPath('/reports/' + vm.get('entry.url'), 'slug', '/', me.selectNode, me);
            // me.lookup('tree').hide();
        } else {
            // me.lookup('tree').show();
        }
        vm.bind('{widget.displayColumns}', function (val) {
            if (me.getView().down('grid')) {
                var columns = me.getView().down('grid').getColumns();
                if (val) {
                    Ext.Array.each(columns, function (col) {
                        col.setHidden(Ext.Array.indexOf(val, col.dataIndex) < 0);
                    });
                } else {
                    Ext.Array.each(columns, function (col) {
                        col.setHidden(Ext.Array.indexOf(vm.get('entry.defaultColumns'), col.dataIndex) < 0);
                    });
                }
            }
        });
    },

    /**
     * the tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value, meta, record) {
        // if (!record.get('readOnly') && record.get('uniqueId')) {
        //     meta.tdCls = 'x-tree-custom-report';
        // }
        return this.rendererRegExp ? value.replace(this.rendererRegExp, '<span style="font-weight: bold; background: #EEE; color: #000; border-bottom: 1px #000 solid;">$1</span>') : value;
    },

    /**
     * filters reports tree
     */
    filterTree: function (field, value) {
        if (value.length === 1) { return; }


        var me = this, tree = me.lookup('tree');
        me.rendererRegExp = new RegExp('(' + value + ')', 'gi');

        if (!value) {
            tree.getStore().clearFilter();
            tree.collapseAll();
            field.getTrigger('clear').hide();
            return;
        }

        tree.getStore().getFilters().replaceAll({
            property: 'text',
            value: new RegExp(Ext.String.escape(value), 'i')
        });
        tree.expandAll();
        field.getTrigger('clear').show();
    },

    onTreeFilterClear: function () {
        this.lookup('tree').down('textfield').setValue();
    },

    selectNode: function (selModel, node) {
        var me = this, vm = this.getViewModel();
        if (!node.isLeaf()) {
            this.lookup('tree').collapseAll();
            node.expand();
            vm.set({
                entry: null,
                widget: null
            });
            me.lookup('report').remove('entry');
        } else {
            var xtype, onDashboard = true, newWidget;
            var entry = Ext.getStore('reports').findRecord('uniqueId', node.get('uniqueId'));
            me.widget = Ext.getStore('widgets').findRecord('entryId', node.get('uniqueId'));

            vm.set('entry', entry);

            if (me.widget) {
                if (entry.get('type') === 'EVENT_LIST') {
                    var tblCfg = TableConfig.getConfig(entry.get('table'));
                    var str = [];
                    Ext.Array.each(tblCfg.columns, function (col) {
                        str.push({name: col.dataIndex, text: col.header});
                    });
                    me.getView().down('tagfield').getStore().setData(str);
                }
                vm.set({
                    widget: me.widget.copy(null),
                    onDashboard: onDashboard
                });
            } else {
                newWidget = Ext.create('Ung.model.Widget', {
                    displayColumns: entry.get('defaultColumns'),
                    enabled: true,
                    entryId: entry.get('uniqueId'),
                    javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                    refreshIntervalSec: 60,
                    timeframe: '',
                    type: 'ReportEntry'
                });
                onDashboard = false;
                vm.set({
                    widget: newWidget.copy(null),
                    onDashboard: onDashboard
                });
            }

            switch (node.get('type')) {
            case 'EVENT_LIST':
                xtype = 'eventreport'; break;
            case 'TEXT':
                xtype = 'textreport'; break;
            default:
                xtype = 'graphreport';
            }

            me.lookup('report').remove('entry');

            me.lookup('report').add({
                xtype: xtype,
                title: entry.get('title'),
                itemId: 'entry',
                viewModel: { data: { entry: entry } }
            });
        }
    },

    refreshEntry: function () {
        this.lookup('report').down('#entry').getController().fetchData();
    },

    onClose: function () {
        var me = this, vm = this.getViewModel();
        me.lookup('tree').collapseAll();
        me.lookup('tree').getStore().clearFilter();
        vm.set({
            entry: null,
            widget: null,
            onDashboard: false
        });
        me.lookup('report').remove('entry');
    },

    onAdd: function () {
        var me = this, vm = this.getViewModel();
        Ext.getStore('widgets').add(vm.get('widget'));
        me.getView().close();

    },

    updateColumns: function(cmp, val) {
        var vm = this.getViewModel(), columns = this.getView().down('grid').getColumns();
        Ext.Array.each(columns, function (col) {
            col.setHidden(Ext.Array.indexOf(vm.get('widget.displayColumns'), col.dataIndex) < 0);
        });
    }

});
