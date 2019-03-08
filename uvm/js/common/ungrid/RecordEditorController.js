Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender',
            afterlayout: 'onAfterLayout',
        }
    },

    recordBind: null,
    actionBind: null,

    onBeforeRender: function (v) {
        var vm = this.getViewModel();
        this.mainGrid = v.up('grid');

        if (v.action) {
            this.action = v.action;
        }

        if (!v.record) {
            v.record = Ext.create(this.mainGrid.recordModel, Ung.util.Util.activeClone(v.template != null ? v.template : this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            vm.set({
                record: v.record,
                windowTitle: 'Add'.t()
            });
        } else {
            var rec = v.record.copy(null); // make a clean copy
            if (rec.get('simple') === true) { // if simple mode, make it false (Port Forward Rules)
                rec.set('simple', false);
            }
            vm.set({
                record: rec,
                windowTitle: v.action === 'add' ? 'Add'.t() : 'Edit'.t()
            });
        }

        /**
         * if record has action object
         * hard to explain but needed to keep dirty state (show as modified)
         */
        if (v.record.get('action') && (typeof v.record.get('action') === 'object')) {
            // console.log('do action bind');
            this.actionBind = vm.bind({
                bindTo: '{_action}',
                deep: true
            }, function (actionObj) {
                // if (!Ext.Object.equals(actionObj, vm.get('record.action'))) {
                vm.set('record.action', Ext.clone(actionObj));
                // }
            });
            vm.set('_action', v.record.get('action'));
        }
    },

    onAfterRender: function (view) {
        var fields = this.mainGrid.editorFields, form = view.down('form');

        for (var i = 0; i < fields.length; i++) {
            form.add(fields[i]);
        }
        form.isValid();
    },

    // ?? rework or is this ok?
    onAfterLayout: function( container, layout){
        var bodyWindowHeight = Ext.getBody().getViewSize().height;
        var windowY = container.getY();
        var windowHeight = container.getHeight();
        var windowBottom = windowY + windowHeight;
        if(windowBottom > bodyWindowHeight){
            container.setY(windowY - (windowBottom - bodyWindowHeight) );
        }
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            condStore, invalidConditionFields = [];

        if (!this.action || this.action === 'edit') {
            for (var field in vm.get('record').modified) {
                v.record.set(field, vm.get('record').get(field));
            }
        }
        if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }
        v.close();
    },

    onCancel: function () {
        this.getView().cancel = true;
        this.getView().close();
    },

    onDestroy: function () {
        this.recordBind.destroy();
        this.recordBind = null;
        this.actionBind.destroy();
        this.actionBind = null;
        this.callParent();
    }
});
