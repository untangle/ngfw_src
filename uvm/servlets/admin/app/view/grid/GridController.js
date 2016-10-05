Ext.define('Ung.view.grid.GridController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ung.grid',

    init: function (view) {
        // add toolbar buttons
        if (view.getToolbarFeatures()) {
            var features = view.getToolbarFeatures(),
                toolbar = Ext.create('Ext.toolbar.Toolbar');

            if (Ext.Array.contains(features, 'add')) {
                toolbar.add({
                    text: Ung.Util.iconTitle('Add', 'add-16'),
                    handler: 'addRecord'
                });
            }
            if (Ext.Array.contains(features, 'revert')) {
                toolbar.add({
                    text: Ung.Util.iconTitle('Revert', 'undo-16'),
                    handler: 'revertChanges'
                });
            }
            if (Ext.Array.contains(features, 'delete')) {
                toolbar.add({
                    text: Ung.Util.iconTitle('Delete', 'delete-16'),
                    handler: 'deleteRecords'
                });
            }
            if (Ext.Array.contains(features, 'importexport')) {
                toolbar.add('->', {
                    text: Ung.Util.iconTitle('Import', 'arrow_downward-16')
                }, {
                    text: Ung.Util.iconTitle('Export', 'arrow_upward-16')
                });
            }
            view.addDocked(toolbar);
        }

        // add celledit plugin
        if (view.getInlineEdit() === 'cell') {
            view.addPlugin({
                ptype: 'cellediting',
                clicksToEdit: 2
            });
        }

        // add row plugin (not used)
        if (view.getInlineEdit() === 'row') {
            view.addPlugin({
                ptype: 'rowediting',
                clicksToEdit: 2,
                clicksToMoveEditor: 1,
                //autoCancel: true,
                errorSummary: false,
                //removeUnmodified: true,
                pluginId: 'rowediting'
            });
        }
    },

    onBeforeDestory: function (view) {
        //console.log('on before destroy');
        //view.getPlugin('gridviewdragdrop').destroy();
    },


    checkChanges: function(store) {
        //console.log('checkchanges');
        this.getViewModel().set('isDirty', (store.getUpdatedRecords().length > 0 || store.getRemovedRecords().length > 0 || store.getModifiedRecords().length > 0));
    },

    /*
    onBeforeRender: function (view) {
        var vm = this.getViewModel();
        if (!view.getSettings().reorderColumn && view.getSettings().initialSortData) {
            vm.get('store').setSorters(view.getSettings().initialSortData);
        }
    },
    */
    addRecord: function () {
        var me = this;
        var vm = this.getViewModel();
        var win = Ext.create('Ung.view.grid.Editor', {
            title: 'Add'.t(),
            width: 500,
            y: 200,
            //height: 250,
            columns: me.getView().getColumns(),
            viewModel: {
                data: {
                    record: Ext.create('Ung.model.GenericRule')
                }
            }
        }).show();
        win.on('close', function () {
            if (win.getCloseAction() === 'save') {
                //console.log(win.getViewModel().get('record'));
                vm.get('store').add(win.getViewModel().get('record'));
            }
        });

    },

    deleteRecords: function () {
        this.getViewModel().get('store').remove(this.getView().getSelectionModel().getSelection());
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        var vm = this.getViewModel();
        var rec = record.copy(null);

        var win = Ext.create('Ung.view.grid.Editor', {
            title: 'Edit'.t(),
            width: 800,
            y: 200,
            columns: item.up('grid').getColumns(),
            //record: record

            viewModel: {
                data: {
                    record: rec
                }
            }
        }).show();
        win.on('close', function () {
            //record.copyFrom(win.getViewModel().get('record'));
            //record.beginEdit();
            record.copyFrom(win.getViewModel().get('record'));
            record.dirty = true;

            //console.log(win.getViewModel().get('record'));

            //record.endEdit();
            //record.set('string', 'hahahahah');
            //record.setDirty(true);
            record.commit();
            vm.get('store').update();

        });
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        record.drop();
        //console.log(record);
        //record.setConfig('markDelete', true);
        //record.markDelete = true;
        //console.log(record);
    },

    // applies changes into the settings object before pushing to server
    onSave: function () {
        var vm = this.getViewModel();
        if (vm.get('store')) {
            vm.set('settings.' + this.getView().getDataProperty() + '.list', Ext.Array.pluck(vm.get('store').getRange(), 'data'));
        }
    }


    /*
    onEdit: function (editor, e) {
        console.log(e);
    },

    revertChanges: function () {
        this.getViewModel().get('store').rejectChanges();
    },

    onSelectionChange: function (model, selected) {
        this.getViewModel().set('selectedRecords', selected.length);
    },

    onSave: function () {
        var vm = this.getViewModel();
        console.log('onsave');
        vm.set(this.getView().getSettings().dataPath, Ext.Array.pluck(vm.get('store').getRange(), 'data'));
        //vm.get('store').commitChanges();
    },

    onReloaded: function () {
        console.log('onreloaded');
        this.getViewModel().get('store').commitChanges();
        console.log(this.getViewModel().get('store'));
    }
    */
});