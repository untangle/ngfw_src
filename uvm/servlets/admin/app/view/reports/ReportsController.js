Ext.define('Ung.view.reports.ReportsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': { afterrender: 'onAfterRender', deactivate: 'resetView' }
    },

    listen: {
        global: {
            init: 'onInit'
        }
    },

    onAfterRender: function () {
        this.getView().setLoading(true);
    },

    onInit: function () {
        var me = this, vm = me.getViewModel();
        me.getView().setLoading(false);

        vm.bind('{hash}', function (hash) {
            if (!hash) { me.resetView(); return; }
            me.lookup('tree').collapseAll();
            me.lookup('tree').selectPath(window.location.hash.replace('#', '/'), 'slug', '/', me.selectPath, me);
        });
        me.buildStats();
    },

    /**
     * selects a tree node (report or category) based on location hash
     * and updates viewmodel with the report entry
     */
    selectPath: function (success, node) {
        var me = this, vm = me.getViewModel(), record;

        if (!success) { console.log('error'); return; }

        if (node.isLeaf()) {
            // report node
            record = Ext.getStore('reports').findRecord('url', window.location.hash.replace('#reports/', ''));
            if (record) {
                vm.set({
                    report: record, // main reference from the store
                    entry: record.copy(null) // report reference copy on which modifications are made
                });
            }
            me.lookup('cards').setActiveItem('report');
        } else {
            me.lookup('cards').setActiveItem('category');
            me.buildStats();
            node.expand();
        }

        // me.lookup('breadcrumb').setSelection(node);
    },

    /**
     * the tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value, meta, record) {
        // if (!record.isLeaf()) {
        //     meta.tdCls = 'x-tree-category';
        // }
        // if (!record.get('readOnly') && record.get('uniqueId')) {
        //     meta.tdCls = 'x-tree-custom-report';
        // }
        return this.rendererRegExp ? value.replace(this.rendererRegExp, '<span style="font-weight: bold; background: #EEE; color: #000; border-bottom: 1px #000 solid;">$1</span>') : value;
    },

    /**
     * filters reports tree
     */
    filterTree: function (field, value) {
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

    /**
     * resets the view to an initial state
     */
    resetView: function () {
        var me = this, tree = me.lookup('tree');
        tree.collapseAll();
        tree.getSelectionModel().deselectAll();
        tree.getStore().clearFilter();
        tree.down('textfield').setValue('');

        me.buildStats();
        me.lookup('cards').setActiveItem('category');
        me.getViewModel().set('hash', null);
    },

    /**
     * builds statistics for categories
     */
    buildStats: function () {
        var me = this, vm = me.getViewModel(), tree = me.lookup('tree'), selection,
        stats = {
            set: false,
            reports: {
                total: 0,
                custom: 0,
                chart: 0,
                event: 0,
                info: 0
            },
            categories: {
                total: 0,
                app: 0
            }
        };

        if (tree.getSelection().length === 0) {
            selection = tree.getRootNode();
        } else {
            selection = tree.getSelection()[0];
        }

        selection.cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            if (node.isLeaf()) {
                stats.reports.total += 1;
                if (!node.get('readOnly')) { stats.reports.custom += 1; }
                switch(node.get('type')) {
                    case 'TIME_GRAPH':
                    case 'TIME_GRAPH_DYNAMIC':
                    case 'PIE_GRAPH':
                        stats.reports.chart += 1; break;
                    case 'EVENT_LIST':
                        stats.reports.event += 1; break;
                    case 'TEXT':
                        stats.reports.info += 1; break;
                }
            } else {
                stats.categories.total += 1;
                if (node.get('type') === 'app') {
                    stats.categories.app += 1;
                }
            }
        });
        vm.set('stats', stats);
        // vm.notify();
    },

    // breadcrumbSelection: function (el, node) {
    //     console.log(node);
    // }
});
