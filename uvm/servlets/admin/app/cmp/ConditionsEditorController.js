Ext.define('Ung.cmp.ConditionsEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.conditionseditorcontroller',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            validateField: 'validate',
            removed: 'onRemoved'
        },
        'grid': {
            afterlayout: 'onAfterGridLayout',
        }
    },

    /**
     * [onConditionsRender description]
     * @param  {[type]} grid [description]
     * @return {[type]}                [description]
     */
    onAfterRender: function (component) {
        var me = this;
        var view = this.getView();
        var vm = this.getViewModel();

        this.masterGrid = component.up('grid');
        this.recordeditor = component.up('window');

        this.recordBind = component.up('window').getViewModel().bind({
            bindTo: '{record}',
        }, this.setMenuConditions);

        me.buildConditions(component, view.conditionsOrder, view.conditions);

        me.getView().down('[name=conditionLabel]').update(me.getView());
        me.getView().down('[name=actionLabel]').update(me.getView());
    },

    buildConditions: function(component){
        var me = this;
        var view = me.getView();

        var menuConditions = [];

        var index = 0;
        var subMenus = {};
        view.conditionsOrder.forEach(function(name){
            index++;
            var condition = view.conditions[name];
            var nameFields = condition.name.split('.');
            if( condition.visible) {
                if( nameFields.length > 1){
                    var masterField = nameFields[0];
                    if( !subMenus[masterField] ){
                        subMenus[masterField] = {
                            showSeparator: false,
                            plain: true,
                            items: [],
                            mouseLeaveDelay: 0,
                            listeners: {
                                click: 'addCondition'
                            }
                        };
                    }
                    subMenus[masterField].items.push({
                        text: nameFields[1],
                        conditionType: condition.name,
                        index: index,
                        allowMultiple: false,
                        tooltip: condition.description ? condition.description : condition.displayName
                    });
                }
            }
        });
        view.conditionsOrder.forEach(function(name){
            index++;
            var condition = view.conditions[name];
            var nameFields = condition.name.split('.');
            if( condition.visible) {
                var masterField = nameFields.length > 1 ? nameFields[0] : null;
                if(masterField && !subMenus[masterField]){
                    return;
                }
                var menuConfig = {
                    text: condition.displayName,
                    conditionType: condition.name,
                    index: index,
                    allowMultiple: false,
                    tooltip: condition.description ? condition.description : condition.displayName
                    // allowMultiple: conditions.allowMultiple != undefined ? conditions.allowMultiple : false
                };
                if(masterField && subMenus[masterField]){
                    menuConfig.text = masterField;
                    menuConfig.menu = subMenus[masterField];
                    delete subMenus[masterField];
                }
                menuConditions.push(menuConfig);
            }
        });

        component.down('#addConditionBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menuConditions,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addCondition'
            }
        });

    },

    /**
     * Copy data to record editor conditions record.
     * @param  {[type]} me        [description]
     * @param  {[type]} container [description]
     * @return {[type]}           [description]
     */
    onRemoved: function(me, container){
        me = me.down ? me : me.getView();

        var recordEditorView = this.recordeditor;
        var store = me.down('grid').getStore();

        if(this.recordeditor.cancel){
            store.rejectChanges();
        }else{
            var recordName = me.recordName.split('.')[1];
            if (store.getModifiedRecords().length > 0 || store.getRemovedRecords().length > 0 || store.getNewRecords().length > 0) {
                var data = Ext.Array.pluck(store.getRange(), 'data');
                data.forEach( function(record){
                    if(typeof(record.value) == 'object' && record.value.length){
                        record.value = record.value.join(',');
                    }
                });
                recordEditorView.record.set( recordName, {
                    javaClass: 'java.util.LinkedList',
                    list: data
                });
            }
        }
    },

    /**
     * After initial grid layout, resize value column.  Subsequently, process comparator and widget queues.
     *
     * We should be able to use "flex:1" for the value column, and it does work except if you use
     * a large checkbox group (e.g.,IPS categories which has nearly 100 records).  This causes the layout
     * to eventually break.  It always works fine on a new record add, but would always break when
     * you tried to edit that record.  
     * To get around this, set the column width to a fixed value based on the grid - width of all other columns.
     *
     * A widget queue is also needed thanks to large chekcboxgroups.  Just using the onWidgetAttach listener
     * would cause a race condition with large checkboxgroups not being fully rendered before the next widget
     * came along to render itself.  By throwing them into this queue, layout event is fired upon successful
     * rendering so we can just walk the queues, FIFO style until empty.
     * 
     * @param  component ConditionEditor component.
     */
    valueColumnResized: false,
    lastGridWidth: 0,
    comparatorWidgetQueue: [],
    valueWidgetQueue: [],
    onAfterGridLayout: function (grid) {
        var me = this;
        var gridWidth = grid.body.getWidth();
        var resized = false;
        if(this.lastGridWidth != gridWidth){
            this.lastGridWidth = gridWidth;
            var availableWidth = gridWidth - 2;
            me.resizeColumn = null;
            grid.getColumns().forEach( function(column){
                if(column.widgetType == 'value'){
                    me.resizeColumn = column;
                    return;
                }
                availableWidth -= column.getWidth();
            });
            me.resizeColumn.setWidth(availableWidth);
        }
        var addedComparatorWidget = this.addComparatorWidget();
        if(!addedComparatorWidget){
            addedValueWidget = this.addValueWidget();
        }

        if(addedComparatorWidget || addedValueWidget){
            this.forceValidate();
        }
    },

    forceValidate: function(){
        this.validate();
        this.getView().up('form').getForm().checkValidity();
    },

    addValueWidget: function(){
        var me = this, 
            view = me.getView(),
            added = false;

        if(me.valueWidgetQueue.length){
            var widget = me.valueWidgetQueue.shift();
            widget.container.removeAll(true);
            widget.container.getBind().html.destroy();
            widget.container.setHtml('');

            var condition = view.conditions[widget.record.get(view.fields.type)];
            added = true;

            var valueBind = '{record.' + view.fields.value + '}';
            switch (condition.type) {
            case 'boolean':
                if(condition.values){
                    widget.container.add({
                        xtype: 'combo',
                        editable: false,
                        matchFieldWidth: false,
                        bind: {
                            value: valueBind
                        },
                        store: condition.values
                    });
                }else{
                    widget.container.add({
                        xtype: 'component',
                        padding: 3,
                        html: 'True'.t()
                    });
                }
                break;
            case 'textfield':
                widget.container.add({
                    xtype: 'textfield',
                    fieldLabel: condition.displayName,
                    hideLabel: true,
                    style: { margin: 0 },
                    bind: {
                        value: valueBind
                    },
                    vtype: condition.vtype,
                    allowBlank: false,
                    listeners:{
                        change: 'forceValidate'
                    }
                });
                break;
            case 'numberfield':
                widget.container.add({
                    xtype: 'numberfield',
                    fieldLabel: condition.displayName,
                    hideLabel: true,
                    style: { margin: 0 },
                    bind: {
                        value: valueBind
                    },
                    vtype: condition.vtype,
                    allowBlank: false,
                    listeners:{
                        change: 'forceValidate'
                    }
                });
                break;
            case 'sizefield':
                widget.container.add({
                    xtype: 'numberfield',
                    fieldLabel: condition.displayName,
                    hideLabel: true,
                    style: { margin: 0 },
                    bind: {
                        value: valueBind
                    },
                    vtype: condition.vtype,
                    allowBlank: false,
                    listeners:{
                        change: 'forceValidate'
                    }
                });
                break;
            case 'select':
                widget.container.add({
                    xtype: 'combo',
                    editable: false,
                    matchFieldWidth: false,
                    bind: {
                        value: valueBind
                    },
                    store: condition.values
                });
                break;
            case 'checkboxgroup':
                var ckItems = [];
                if(condition.store){
                    condition.store.each( function( record ){
                        ckItems.push({
                            inputValue: record.get(condition.storeValue),
                            boxLabel: record.get(condition.storeLabel),
                            autoEl: {
                                tag: 'div',
                                'data-qtip': condition.storeTip(record.get(condition.storeValue), null, record)
                            }
                        });
                    });
                }else{
                    for (i = 0; i < condition.values.length; i += 1) {
                        ckItems.push({
                            // name: 'ck',
                            inputValue: condition.values[i][0],
                            boxLabel: condition.values[i][1]
                            // !!! autoEl?
                        });
                    }
                }
                widget.container.add({
                    xtype: 'checkboxgroup',
                    bind: {
                        value: valueBind
                    },
                    columns: 3,
                    vertical: true,
                    defaults: {
                        padding: '0 10'
                    },
                    items: ckItems,
                    listeners:{
                        change: 'forceValidate'
                    }
                });
                break;
            case 'countryfield':
                widget.container.add({
                    xtype: 'tagfield',
                    flex: 1,
                    emptyText: 'Select countries or specify a custom value ...',
                    store: { type: 'countries' },
                    filterPickList: true,
                    forceSelection: false,
                    queryMode: 'local',
                    selectOnFocus: false,
                    growMax: 60,
                    createNewOnEnter: true,
                    createNewOnBlur: true,
                    bind: {
                        value: valueBind
                    },
                    displayField: 'name',
                    valueField: 'code',
                    listConfig: {
                        itemTpl: ['<div>{name} <strong>[{code}]</strong></div>']
                    },
                });
                break;
            case 'userfield':
                widget.container.add({
                    xtype: 'tagfield',
                    flex: 1,
                    emptyText: 'Select a user or specify a custom value ...',
                    store: { data: [] },
                    filterPickList: true,
                    forceSelection: false,
                    queryMode: 'local',
                    selectOnFocus: false,
                    growMax: 60,
                    createNewOnEnter: true,
                    createNewOnBlur: true,
                    displayField: 'uid',
                    valueField: 'uid',
                    listConfig: {
                        itemTpl: ['<div>{uid}</div>']
                    },
                    bind: {
                        value: valueBind
                    },
                    listeners: {
                        afterrender: function (field) {
                            var app, 
                                data = [{
                                firstName: '', lastName: null, uid: '[any]', displayName: 'Any User'
                            },{
                                firstName: '', lastName: null, uid: '[authenticated]', displayName: 'Any Authenticated User'
                            },{
                                firstName: '', lastName: null, uid: '[unauthenticated]', displayName: 'Any Unauthenticated/Unidentified User'
                            }];

                            field.getStore().loadData(data);

                            Rpc.asyncData('rpc.appManager.app', 'directory-connector')
                            .then(function(app){
                                if(Util.isDestroyed(field)){
                                    return;
                                }
                                Rpc.asyncData( app, 'getRuleConditonalUserEntries')
                                .then(function(result){
                                    if(Util.isDestroyed(field)){
                                        return;
                                    }
                                    Ext.Array.each( data.reverse(), function (record) {
                                        result.list.unshift(record);
                                    });
                                    field.getStore().loadData(result.list);
                                }, function(ex) {
                                    Util.handleException(ex);
                                });
                            }, function(ex) {
                                Util.handleException(ex);
                            });
                        }
                    }
                });
                break;
            case 'directorygroupfield':
                widget.container.add({
                    xtype: 'tagfield',
                    flex: 1,
                    emptyText: 'Select a group or specify a custom value ...',
                    store: { data: [] },
                    filterPickList: true,
                    forceSelection: false,
                    queryMode: 'local',
                    selectOnFocus: false,
                    growMax: 60,
                    createNewOnEnter: true,
                    createNewOnBlur: true,
                    displayField: 'CN',
                    valueField: 'SAMAccountName',
                    listConfig: {
                        itemTpl: ['<div>{CN} <strong>[{SAMAccountName}]</strong></div>']
                    },
                    bind: {
                        value: valueBind
                    },
                    listeners: {
                        afterrender: function (field) {
                            var app, 
                                data = [{
                                SAMAccountName: '*', CN: 'Any Group'
                            }];

                            field.getStore().loadData(data);

                            Rpc.asyncData('rpc.appManager.app', 'directory-connector')
                            .then(function(app){
                                if(Util.isDestroyed(field)){
                                    return;
                                }
                                Rpc.asyncData( app, 'getRuleConditionalGroupEntries')
                                .then(function(result){
                                    if(Util.isDestroyed(field)){
                                        return;
                                    }
                                    Ext.Array.each( data.reverse(), function (record) {
                                        result.list.unshift(record);
                                    });
                                    field.getStore().loadData(result.list);
                                }, function(ex) {
                                    Util.handleException(ex);
                                });
                            }, function(ex) {
                                Util.handleException(ex);
                            });
                        }
                    }
                });
                break;
            case 'directorydomainfield':
                widget.container.add({
                    xtype: 'tagfield',
                    flex: 1,
                    emptyText: 'Select a domain or specify a custom value ...',
                    store: { data: [] },
                    filterPickList: true,
                    forceSelection: false,
                    queryMode: 'local',
                    selectOnFocus: false,
                    growMax: 60,
                    createNewOnEnter: true,
                    createNewOnBlur: true,
                    displayField: 'description',
                    valueField: 'value',
                    listConfig: {
                        itemTpl: ['<div>{value} <strong>[{description}]</strong></div>']
                    },
                    bind: {
                        value: valueBind
                    },
                    listeners: {
                        afterrender: function (field) {
                            var app, 
                                data = [{
                                value: '*', description: 'Any Domain'
                            }];

                            field.getStore().loadData(data);

                            Rpc.asyncData('rpc.appManager.app', 'directory-connector')
                            .then(function(app){
                                if(Util.isDestroyed(field)){
                                    return;
                                }
                                Rpc.asyncData( app, 'getRuleConditionalDomainEntries')
                                .then(function(result){
                                    if(Util.isDestroyed(field)){
                                        return;
                                    }
                                    Ext.Array.each( result.list, function (record) {
                                        data.push({value: record, description: record});
                                    });
                                    field.getStore().loadData(data);
                                }, function(ex) {
                                    Util.handleException(ex);
                                });
                            }, function(ex) {
                                Util.handleException(ex);
                            });
                        }
                    }
                });
                break;
            case 'timefield':
                widget.container.add({
                    xtype: 'container',
                    layout: { type: 'hbox', align: 'middle' },
                    hoursStore: (function () {
                        var arr = [];
                        for (var i = 0; i <= 23; i += 1) {
                            arr.push(i < 10 ? '0' + i : i.toString());
                        }
                        return arr;
                    })(),
                    minutesStore: (function () {
                        var arr = [];
                        for (var i = 0; i <= 59; i += 1) {
                            arr.push(i < 10 ? '0' + i : i.toString());
                        }
                        return arr;
                    })(),
                    defaults: {
                        xtype: 'combo', store: [], width: 40, editable: false, queryMode: 'local', listeners: {
                            change: function (combo, newValue) {
                                var view = combo.up('container'), period = '';
                                view.record.set('value', view.down('#hours1').getValue() + ':' + view.down('#minutes1').getValue() + '-' + view.down('#hours2').getValue() + ':' + view.down('#minutes2').getValue());
                            }
                        }
                    },
                    items: [
                        { itemId: 'hours1', },
                        { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                        { itemId: 'minutes1' },
                        { xtype: 'component', html: 'to'.t(), width: 'auto', margin: '0 3' },
                        { itemId: 'hours2' },
                        { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                        { itemId: 'minutes2' }
                    ],
                    record: widget.record,
                    listeners: {
                        afterrender: function (view) {
                            var valueField = view.up('conditionseditor').fields.value;
                            view.down('#hours1').setStore(view.hoursStore);
                            view.down('#minutes1').setStore(view.minutesStore);
                            view.down('#hours2').setStore(view.hoursStore);
                            view.down('#minutes2').setStore(view.minutesStore);
                            if (!view.record.get(valueField)) {
                                view.down('#hours1').setValue('12');
                                view.down('#minutes1').setValue('00');
                                view.down('#hours2').setValue('13');
                                view.down('#minutes2').setValue('30');
                            } else {
                                var startTime = view.record.get(valueField).split('-')[0];
                                var endTime = view.record.get(valueField).split('-')[1];
                                view.down('#hours1').setValue(startTime.split(':')[0]);
                                view.down('#minutes1').setValue(startTime.split(':')[1]);
                                view.down('#hours2').setValue(endTime.split(':')[0]);
                                view.down('#minutes2').setValue(endTime.split(':')[1]);
                            }
                        }
                    }
                });
                break;
                default:
                    Util.handleException('Unknown condition value type: ' + condition.type);
            }
        }
        return added;
    },

    addComparatorWidget: function(){
        var me = this, 
            view = me.getView(),
            added = false;

        if(me.comparatorWidgetQueue.length){
            var widget = me.comparatorWidgetQueue.shift();

            widget.container.removeAll(true);
            widget.container.getBind().html.destroy();
            widget.container.setHtml('');

            var condition = view.conditions[widget.record.get(view.fields.type)];
            added = true;

            var comparatorBind = '{record.' + me.getView().fields.comparator + '}';

            var comparatorType = condition.comparator != undefined ? condition.comparator : 'invert';

            var comparator = null;
            view.comparators.forEach( function(c){
                if(c.name == comparatorType){
                    comparator = c;
                }
            });

            if(comparator == null){
                Util.handleException('Unknown comparator value type: ' + comparatorType);
            }else{
                widget.container.add({
                    xtype: 'combo',
                    editable: false,
                    matchFieldWidth: false,
                    bind: {
                        value: comparatorBind
                    },
                    store: comparator.store,
                    listeners: {
                        change: function(combo, newValue, oldValue){
                            combo.up('conditionseditor').getController().forceValidate();
                        }
                    }
                });
            }

        }
        return added;
    },

    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuConditions: function () {
        var view = this.getView(),
            conditionsEditorView = view.fields ? view : view.down('conditionseditor'),
            conditionsGrid = this.getView().down('grid'),
            menu = conditionsGrid.down('#addConditionBtn').getMenu(),
            store = conditionsGrid.getStore();

        menu.items.each(function (item) {
            if(item.allowMultiple){
                return;
            }
            item.setDisabled(store.findRecord(conditionsEditorView.fields.type, item.conditionType, 0, false, true, true) ? true : false);
        });
    },

    /**
     * Adds a new condition for the edited rule
     */
    addCondition: function (menu, item) {
        var me = this,
            view = this.getView();

        if( item === undefined){
            return;
        }
        var record = Ext.create(me.getView().model);

        record.set(view.fields.type, item.conditionType);
        record.set('javaClass', view.javaClassValue);

        // If default value not set in model, fill in empty values with first values from
        // associated comparator and value lists (if any).
        var conditionName = item.text;
        Ext.ClassManager.get(me.getView().model).fields.forEach( function(field){
            if( ( record.get(field.name) == undefined || record.get(field.name) == "" ) && field.defaultValue == undefined){
                for(var conditionField in view.fields){
                    if(view.fields[conditionField] == field.name){
                        switch(conditionField){
                            case 'comparator':
                                var comparator = null;
                                view.comparators.forEach(function(c){
                                    if(c.name == view.conditions[conditionName].comparator){
                                        comparator = c;
                                    }
                                });
                                record.set(field.name, comparator.store[0][0]);
                                break;
                            case 'value':
                                if(view.conditions[conditionName].values){
                                    record.set(field.name, view.conditions[conditionName].values[0]);
                                }
                                break;
                        }
                    }
                }
            }
        });

        me.getView().down('grid').getStore().add(record);
        me.setMenuConditions();
    },

    /**
     * Removes a condition from the rule
     */
    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        me.getView().down('grid').getStore().remove(record);
        me.setMenuConditions();
        me.forceValidate();
    },

    /**
     * Renders the condition name in the grid
     */
    conditionRenderer: function (value, column) {
        var view = this.getView();

        var displayName = view.conditions[value] ? view.conditions[value].displayName : value;
        var description = view.conditions[value] && view.conditions[value].description ? view.conditions[value].description : displayName;
        column.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(description) + '"';
        return '<strong>' + displayName + ':</strong>';
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    onWidgetAttach: function (column, container, record) {
        var me = this;

        var widgetType = column.widgetType;

        var found = false;
        me[widgetType + 'WidgetQueue'].forEach(function(value){
            if(value.container == container){
                found = true;
            }
        });
        if(!found){
            me[widgetType + 'WidgetQueue'].push({
                column: column,
                container: container,
                record: record,
                grid: column.up('grid')
            });
        }
    },


    /**
     * Check the widget fields for validity.
     * @return {[type]} [description]
     */
    validate: function(){
        var grid = this.getView().down('grid');

        var fields = grid.query('tableview')[0].query('field');

        if(!this.getView().allowEmpty && !fields.length){
            return false;
        }

        var valid = true;
        Ext.Array.each(fields, function (field) {
            if (!field.isValid()) {
                valid = false;
            }
            // console.log(field);
            // do a switch here for types.
            // If checkboxes, make sure at least one is checked.
        });
        return valid;
    }
});
