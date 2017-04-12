Ext.define('Ung.config.events.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-events',

    control: {
        '#': {
            afterrender: 'loadEvents',
        },
    },

    loadEvents: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.eventManager.getSettings'),
            Rpc.asyncPromise('rpc.eventManager.getClassFields'),
        ], this).then(function(result) {
            vm.set({
                settings: result[0],
                classFields: result[1]
            });

            var classFields = vm.get('classFields');
            var classesStoreData = [];
            for( var className in classFields ){
                classesStoreData.push([className, classFields[className].description]);
            }

            vm.set('classes', Ext.create('Ext.data.ArrayStore', {
                fields: [ 'name', 'description' ],
                sorters: [{
                    property: 'name',
                    direction: 'ASC'
                }],
                data: classesStoreData
            }) );

            classesStoreData.unshift(['All', 'Match all classes (NOT RECOMMENDED!)'.t()]);
            vm.set('allClasses', Ext.create('Ext.data.ArrayStore', {
                fields: [ 'name', 'description' ],
                sorters: [{
                    property: 'name',
                    direction: 'ASC'
                }],
                data: classesStoreData
            }) );
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            v.setLoading(false);
        });


    },

    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();

        if (!Util.validateForms(view)) {
            return;
        }

        view.setLoading(true);

        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });

        Ext.Deferred.sequence([
            // !!! NOT RIGHT YET
            Rpc.asyncPromise('rpc.eventManager.setSettings', vm.get('settings')),
        ], this).then(function() {
            me.loadEvents();
            Util.successToast('Events'.t() + ' settings saved!');
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    },

});

Ext.define('Ung.config.events.cmp.EventsRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.uneventsrecordeditor',

    controller: 'uneventsrecordeditorcontroller',

    actions: {
        apply: {
            text: 'Done'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-ban',
            handler: 'onCancel'
        },
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Field'.t(),
            iconCls: 'fa fa-plus'
        },
    },

    /// !!! For listerner debugging.
    items: [{
        xtype: 'form',
        // region: 'center',
        scrollable: 'y',
        bodyPadding: 10,
        border: false,
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        items: [],
        buttons: ['@cancel', '@apply']
    }],
});

Ext.define('Ung.config.events.cmp.EventsRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.uneventsrecordeditorcontroller',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender',
        },
        'grid': {
            afterrender: 'onConditionsRender'
        }
    },

    conditionsGrid: {
        xtype: 'grid',
        trackMouseOver: false,
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        padding: '10 0',
        tbar: ['@addCondition'],
        disabled: true,
        hidden: true,
        bind: {
            store: {
                model: 'Ung.model.EventCondition',
                data: '{record.conditions.list}'
            },
            disabled: '{record.class == "All"}',
            hidden: '{record.class == "All"}'
        },
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Fields! Add from the menu...</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Field'.t(),
            menuDisabled: true,
            dataIndex: 'field',
            align: 'right',
            width: 200,
            renderer: 'fieldRenderer'
        }, {
            header: 'Comparator'.t(),
            xtype: 'widgetcolumn',
            menuDisabled: true,
            width: 120,
            resizable: false,
            widget: {
                xtype: 'combo',
                editable: false,
                queryMode: 'local',
                bind: '{record.comparator}',
                valueField: 'name',
                displayField: 'description',
                forceSelection: true
            },
            onWidgetAttach: 'comparatorWidgetAttach'
        }, {
            header: 'Value'.t(),
            xtype: 'widgetcolumn',
            menuDisabled: true,
            sortable: false,
            flex: 1,
            widget: {
                xtype: 'container',
                padding: '0 3'
            },
            onWidgetAttach: 'fieldValueWidgetAttach'
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            sortable: false,
            width: 30,
            align: 'center',
            iconCls: 'fa fa-minus-circle fa-red',
            tdCls: 'action-cell-cond',
            handler: 'removeCondition'
        }]
    },

    massageRecordIn: function(record){
        // breakout class record
        var conditions = record.get('conditions');
        conditions.list.forEach( function(condition){
            if(condition.field == 'class'){
                record.set( 'class', condition.fieldValue.substring(1, condition.fieldValue.length - 1) );
                record.set( '_classCondition', conditions.list.splice(conditions.list.indexOf(condition), 1)[0] );
            }
        });
        if(!record.get('class')){
            record.set('class', 'All');
        }
        record.set('conditions', conditions);
    },

    massageRecordOut: function(record, store){
        var conditionsList = Ext.Array.pluck( store.getRange(), 'data' );
        if(record.get('class') == 'All'){
            conditionsList = [];
        }else{
            var classCondition = record.get('_classCondition');
            classCondition.fieldValue = '*' + record.get('class') + '*';
            conditionsList.unshift(classCondition);
        }

        delete(record.data.class);
        delete(record.data._classCondition);

        record.set('conditions', {
            javaClass: 'java.util.LinkedList',
            list: conditionsList
        });
    },

    onBeforeRender: function (v) {
        var vm = this.getViewModel();

        this.mainGrid = v.up('grid');

        var windowTitle = '';
        if (!v.record) {
            v.record = Ext.create('Ung.model.Rule', Ext.clone(this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            windowTitle = 'Add'.t();
        } else {
            windowTitle = 'Edit'.t();
        }

        this.massageRecordIn(v.record);

        this.getViewModel().set({
            record: ( this.action == 'add' ) ? v.record : v.record.copy(null),
            windowTitle: windowTitle
        });

        /**
         * if record has action object
         * hard to explain but needed to keep dirty state (show as modified)
         */
        if (v.record.get('action') && (typeof v.record.get('action') === 'object')) {
            this.actionBind = vm.bind({
                bindTo: '{_action}',
                deep: true
            }, function (actionObj) {
                vm.set('record.action', Ext.clone(actionObj));
            });
            vm.set('_action', v.record.get('action'));
        }

    },

    onAfterRender: function (view) {
        var fields = this.mainGrid.editorFields,
            form = view.down('form');
        // add editable column fields into the form

        for (var i = 0; i < fields.length; i++) {
            if (fields[i].dataIndex !== 'conditions') {
                form.add(fields[i]);
            } else {
                form.add([{
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following field conditions are met:'.t() + '</strong>'
                },{
                    xtype: 'combo',
                    fieldLabel: 'Class'.t(),
                    editable: false,
                    queryMode: 'local',
                    bind:{
                        value: '{record.class}',
                        store: ((fields[i].allowAllClasses === true) ? '{allClasses}' : '{classes}' ),
                    },
                    valueField: 'name',
                    displayField: 'name',
                    forceSelection: true,
                    listConfig:   {
                        itemTpl: '<div data-qtip="{description}">{name}</div>'
                    },
                    listeners: {
                        change: 'classChange'
                    }
                },
                this.conditionsGrid
                ]);
            }
        }
        form.isValid();
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            condStore;

        if (v.down('grid')) {
            condStore = v.down('grid').getStore();
            /* Regardless of what changeed, we need to re-integrate the record. */
            this.massageRecordOut(vm.get('record'), condStore);
        }

        if (!this.action) {
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
        var v = this.getView(),
            vm = this.getViewModel();
        if (v.down('grid')) {
            condStore = v.down('grid').getStore();
            this.massageRecordOut(vm.get('record'), condStore);
        }
        this.getView().close();
    },

    onConditionsRender: function () {
        var vm = this.getViewModel();

        // when record is modified update conditions menu
        this.recordBind = this.getViewModel().bind({
            bindTo: '{record}',
        }, this.setMenuConditions, this);

        var menu = [];
        if( vm.get('classFields')[vm.get('record.class')] ){
            vm.get('classFields')[vm.get('record.class')].fields.forEach(function(fieldCondition){
                if(fieldCondition.name == 'class' || fieldCondition.name == 'timeStamp' ){
                    return;
                }
                menu.push({
                    text: fieldCondition.name,
                    tooltip: fieldCondition.description,
                });
            });
        }

        var conditionsGrid = this.getView().down('grid');
        conditionsGrid.down('#addConditionBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menu,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addCondition'
            }
        });
    },
    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuConditions: function () {
        var conditionsGrid = this.getView().down('grid'),
            menu = conditionsGrid.down('#addConditionBtn').getMenu(),
            store = conditionsGrid.getStore();

        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('field', item.text) ? true : false);
        });
    },

    /**
     * Adds a new condition for the edited rule
     */
    addCondition: function (menu, item) {
        if(item){
            var condition = {
                field: item.text,
                comparator: '=',
                javaClass: this.mainGrid.ruleJavaClass,
                // ?? default values for enums and such?
                fieldValue: ''
            };

            this.getView().down('grid').getStore().add( condition );
            this.setMenuConditions();
        }
    },

    /**
     * Removes a condition from the rule
     */
    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        // record.drop();
        this.getView().down('grid').getStore().remove(record);
        this.setMenuConditions();
    },

    classChange: function( combo, newValue, oldValue ){
        if(oldValue == null){
            combo.resetOriginalValue();
            return;
        }
        if(newValue == combo.originalValue){
            return;
        }
        if( newValue != oldValue ){
            var record = this.getViewModel().get('record');
            Ext.MessageBox.confirm(
                    'Change Class'.t(),
                    'If you change the class, fields may be removed. Are you sure?'.t(),
                    Ext.bind(function(button){
                        if (button == 'no') {
                            record.set('class', oldValue );
                        } else {
                            this.classChangeFields(oldValue);
                        }
                    },this)
            );
        }
    },

    classChangeFields: function( oldClassName ){
        var vm = this.getViewModel();

        // Re-render add field menu
        this.onConditionsRender();

        // Remove fields if they do not exist in new class.
        var oldFields = vm.get('classFields')[oldClassName].fields;
        var newFields = vm.get('classFields')[vm.get('record.class')].fields;

        var store = this.getView().down('grid').getStore();
        var match;
        store.each(Ext.bind( function( record ){
            match = false;
            newFields.forEach( Ext.bind( function( field ){
                if(record.get('field') == field.name){
                    // Provisionally match!
                    match = true;

                    // But look for other reasons why it may not be true.
                    oldFields.forEach( Ext.bind( function( oldField ){
                        if(field.name == oldField.name){
                            if( this.getRecordFieldType(field.type) != this.getRecordFieldType(oldField.type) ){
                                // Form display is not the same.
                                match = false;
                            }
                            if( field.values && oldField.values ){
                                // Enumerated, but field value list is not the same.
                                if(field.values.length != oldField.values.length){
                                    match = false;
                                }
                                var i = field.values.length;
                                while( i-- ){
                                    if(field.values[i] != oldField.values[i]){
                                        match = false;
                                        break;
                                    }
                                }   
                            }
                        }
                    }, this ) );
                }
            }, this ) );
            if(match === false){
                store.remove(record);
            }
        }, this) );

    },

    /*
     * Change the store for the widget to the appropriate class fieldset
     */
    fieldWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        container.setStore( this.buildClassFieldStore( vm.get('record').get('class') ) );
    },

    /**
     * Renders the condition name in the grid
     */
    fieldRenderer: function (value) {
        return '<strong>' + value + ':</strong>';
    },

    getRecordFieldType: function(recordType){
        var fieldType = 'string';
        switch(recordType.toLowerCase()){
            case 'double':
            case 'int':
            case 'integer':
            case 'long':
            case 'short':
                fieldType = 'numeric';
                break;

            case 'boolean':
                fieldType = 'boolean';
                break;                
        }
        return fieldType;
    },

    comparatorFieldStores: {},
    buildComperatorFieldStore: function( type ){

        var storeName = this.getRecordFieldType(type);

        if( !(storeName in this.comparatorFieldStores) ){
            var vm = this.getViewModel();

            var fields = [];
            switch(storeName){
                case 'numeric':
                    fields.push([ '>', 'Less (<)'.t() ]);
                    fields.push([ '>=', 'Less or equal (<=)'.t() ]);
                    fields.push([ '=', 'Equals (=)'.t() ]);
                    fields.push([ '<=', 'Greater or equal (>=)'.t() ]);
                    fields.push([ '<', 'Greater (>)'.t() ]);
                    fields.push([ '!=', 'Does not equal (!=)'.t() ]);
                    break;

                case 'boolean':
                    fields.push([ '=', 'Is'.t() ]);
                    fields.push([ '!=', 'Is not'.t() ]);
                    break;

                default:
                    fields.push([ '=', 'Equals (=)'.t() ]);
                    fields.push([ '!=', 'Does not equal (!=)'.t() ]);
                    break;
            }

            this.comparatorFieldStores[storeName] = Ext.create('Ext.data.ArrayStore', {
                fields: [ 'name', 'description' ],
                data: fields
            });
        }
        return this.comparatorFieldStores[storeName];
    },
    comparatorWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        var className = vm.get('record').get('class');
        var classes = vm.get('classFields');

        var type = null;
        for( var classesClassName in  classes ){
            if( classesClassName == className ){
                classes[classesClassName].fields.forEach( function(field){
                    if(field.name == record.get('field')){
                        if( field.values ){
                            type = 'enum';
                        }else{
                            type = field.type;
                        }
                    }
                });
            }
        }

        container.setStore( this.buildComperatorFieldStore( type ) );
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    fieldValueWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        var className = vm.get('record').get('class');
        var classes = vm.get('classFields');

        var type = null;
        var enumValues = null;
        for( var classesClassName in classes ){
            if( classesClassName == className ){
                classes[classesClassName].fields.forEach( function(field){
                    if(field.name == record.get('field')){
                        if( field.values ){
                            type = 'enum';
                            enumValues = [];
                            field.values.forEach( function(value){
                                enumValues.push([ value ]);
                            });
                            if(!record.get('fieldValue')){
                                record.set('fieldValue', enumValues[0]);
                            }
                        }else{
                            type = field.type;
                        }
                    }
                });
            }
        }

        container.removeAll(true);

        var fieldType = this.getRecordFieldType(type);
        switch(fieldType){
            case 'enum':
                break;

            case 'boolean':
                container.add({
                    xtype: 'component',
                    padding: 3,
                    html: 'True'.t()
                });
                break;

            case 'numeric':
                var allowDecimals = (type == 'float' ? true : false);
                container.add({
                    xtype: 'numberfield',
                    style: { margin: 0 },
                    allowDecimals: allowDecimals,
                    bind: {
                        value: '{record.fieldValue}'
                    },
                    // vtype: condition.vtype
                });
                break;

            default:
                if( type == 'enum'){
                    container.add({
                        xtype: 'combo',
                        editable: true,
                        queryMode: 'local',
                        bind: '{record.fieldValue}',
                        valueField: 'name',
                        displayField: 'name',
                        store: {
                            fields: [ 'name' ],
                            sorters: [{
                                property: 'name',
                                direction: 'ASC'
                            }],
                            data: enumValues
                        }
                    });
                }else{
                    container.add({
                        xtype: 'textfield',
                        style: { margin: 0 },
                        bind: {
                            value: '{record.fieldValue}'
                        },
                        // vtype: condition.vtype
                    });
                }
        }

        // determine appropriate type
        // build combo based on type:
        // type: "boolean",
        // type: "Boolean",
            // true

        // type: "Class",

        // type: "double",
        // type: "Double",
        // type: "int",
        // type: "Integer",
        // type: "long",
        // type: "Long",
        // type: "short",
        // type: "Short",
        //  numeric with java limits

        // type: "float",
        //  numeric with decimal vtype

        // type: "InetAddress",
        //  string with IP vtype

        // type: "String",
        //  string

        // type: "Timestamp",
        //  ??

        // type: "URI",
        //  string with url vtype

        // type: "Action",
        // type: "AddressKind",
        // type: "CaptivePortalSettings$AuthenticationType",
        // type: "CaptivePortalUserEvent$EventType",
        // type: "DeviceTableEntry",
        // type: "EventRule",
        // type: "File",
        // type: "HttpMethod",
        // type: "HttpRequestEvent",
        // type: "LogEvent",
        // type: "OpenVpnEvent$EventType",
        // type: "Reason",
        // type: "RequestLine",
        // type: "SessionEvent",
        // type: "Set",
        // type: "SmtpMessageEvent",
        // type: "SpamMessageAction",
        // type: "WanFailoverEvent$Action",


        // container.removeAll(true);

        // var condition = this.mainGrid.conditionsMap[record.get('conditionType')], i, ckItems = [];

        // switch (condition.type) {
        // case 'boolean':
        //     container.add({
        //         xtype: 'component',
        //         padding: 3,
        //         html: 'True'.t()
        //     });
        //     break;
        // case 'textfield':
        //     container.add({
        //         xtype: 'textfield',
        //         style: { margin: 0 },
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         vtype: condition.vtype
        //     });
        //     break;
        // case 'numberfield':
        //     container.add({
        //         xtype: 'numberfield',
        //         style: { margin: 0 },
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         vtype: condition.vtype
        //     });
        //     break;
        // case 'checkboxgroup':
        //     // console.log(condition.values);
        //     // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
        //     for (i = 0; i < condition.values.length; i += 1) {
        //         ckItems.push({
        //             inputValue: condition.values[i][0],
        //             boxLabel: condition.values[i][1]
        //         });
        //     }
        //     container.add({
        //         xtype: 'checkboxgroup',
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         columns: 4,
        //         vertical: true,
        //         defaults: {
        //             padding: '0 10 0 0'
        //         },
        //         items: ckItems
        //     });
        // }
    },
});

Ext.define('Ung.config.events.cmp.EventGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.uneventsgrid',

    onBeforeRender: function (view) {
        // // create conditionsMap for later use
        // if (view.conditions) {
        //     view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        // }
    },

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.uneventsrecordeditor',
            record: record
        });
        this.dialog.show();
    },

    conditionsRenderer: function (value) {
        var view = this.getView(),
            conds = value.list,
            resp = [], i, valueRenderer = [];

        console.log('conditionsRenderer');


        // for (i = 0; i < conds.length; i += 1) {
        //     valueRenderer = [];

        //     switch (conds[i].conditionType) {
        //     case 'SRC_INTF':
        //     case 'DST_INTF':
        //         conds[i].value.toString().split(',').forEach(function (intfff) {
        //             valueRenderer.push(Util.interfacesListNamesMap()[intfff]);
        //         });
        //         break;
        //     case 'DST_LOCAL':
        //     case 'WEB_FILTER_FLAGGED':
        //         valueRenderer.push('true'.t());
        //         break;
        //     default:
        //         valueRenderer = conds[i].value.toString().split(',');
        //     }
        //     resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + valueRenderer.join(', ') + '</span>' + '</strong>');
        // }
        // return resp.length > 0 ? resp.join(' &nbsp;&bull;&nbsp; ') : '<em>' + 'No conditions' + '</em>';
    },
    conditionRenderer: function (val) {
        // return this.getView().conditionsMap[val].displayName;
        return 'conditionRenderer';
    },

});

Ext.define('Ung.config.events.cmp.Grid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.uneventgrid',

    setConditions: function( conditions ){
        this.conditions = conditions;
    },
    getConditions: function(){
        return this.conditions;
    }

});

Ext.define ('Ung.model.EventCondition', {
    extend: 'Ext.data.Model' ,
    fields: [
        // { name: 'conditionType', type: 'string' },
        // { name: 'invert', type: 'boolean', defaultValue: false },
        { name: 'field', type: 'string', defaultValue: '' },
        { name: 'comparator', type: 'string', defaultValue: '=' },
        { name: 'fieldValue', type: 'auto', defaultValue: '' },
        { name: 'javaClass', type: 'string' },
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
