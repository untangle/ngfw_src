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
            // console.log(node.get('type'));
            var xtype, onDashboard = true, newWidget;
            var entry = Ext.getStore('reports').findRecord('uniqueId', node.get('uniqueId'));
            me.widget = Ext.getStore('widgets').findRecord('entryId', node.get('uniqueId'));

            if (!me.widget) {
                newWidget = Ext.create('Ung.model.Widget', {
                    displayColumns: entry.get('displayColumns'),
                    enabled: true,
                    entryId: entry.get('uniqueId'),
                    javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                    refreshIntervalSec: 60,
                    timeframe: '',
                    type: 'ReportEntry'
                });
                onDashboard = false;
            }
            vm.set({
                entry: entry,
                widget: me.widget ? me.widget.copy(null) : newWidget.copy(null),
                onDashboard: onDashboard
            });

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
        vm.set({
            entry: null,
            widget: null,
            onDashboard: false
        });
        me.lookup('report').remove('entry');
    },

    onAdd: function () {
        var me = this, vm = this.getViewModel();
        Ext.fireEvent('widgetaction', 'add', vm.get('widget'), vm.get('entry'), function () {
            me.getView().close();
        });
    },

    onSave: function () {
        var me = this, vm = this.getViewModel();
        Ext.fireEvent('widgetaction', 'save', vm.get('widget'), vm.get('entry'), function () {
            me.getView().close();
        });
    },

    onRemoveWidget: function () {
        var me = this, vm = this.getViewModel();
        Ext.fireEvent('widgetaction', 'remove', vm.get('widget'), null, function () {
            me.getView().close();
        });

    }

});
