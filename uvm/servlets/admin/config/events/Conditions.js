/**
 * Extend the condition editor to support events..
 *
 * The biggest change is to handle the first record in the condition list which is the "class"
 * field.  This controls which class fields are selectable and targets (tags).
 * 
 */
Ext.define('Ung.config.events.ConditionsEditor', {
    extend: 'Ung.cmp.ConditionsEditor',
    alias: 'widget.eventconditionseditor',

    controller: 'eventconditionseditorcontroller',
    model: 'Ung.model.EventCondition',

    javaClassValue: 'com.untangle.uvm.event.EventRuleCondition',

    allowAllClasses: false,

    constructor: function(config) {
        var me = this;

        var items = Ext.Array.clone(me.items);

        var pos = -1;
        Ext.Array.findBy(items, function(item, index){
            if(item.name == 'conditionLabel'){
                pos = index;
                return true;
            }
        });
        Ext.Array.insert(items, pos+1, [{
            xtype: 'combo',
            name: 'classSelect',
            fieldLabel: 'Class'.t(),
            editable: false,
            queryMode: 'local',
            width: '100%',
            bind:{
                value: '{record.class}',
                store: (config.allowAllClasses === true) ? '{allClasses}' : '{classes}'
            },
            valueField: 'name',
            displayField: 'name',
            forceSelection: true,
            listConfig:   {
                itemTpl: '<div data-qtip="{description}">{name}</div>'
            },
            listeners: {
                change: 'onClassChange'
            }
        }]);

        config.items = items;

        me.callParent(arguments);
    },

    statics:{
        classRenderer: function(value, meta){
            var me = this, 
                view = me.getView().up('grid'),
                conditionsEditorField = Ext.Array.findBy(view.editorFields, function(item){
                    if(item.xtype.indexOf('conditionseditor') > -1){
                        return true;
                    }
                }),
                classFields = me.up('config-events').getViewModel().get(conditionsEditorField.allowAllClasses ? 'allClassFields' : 'classFields'),
                className = '';

            if(!value.list.length){
                if(conditionsEditorField && conditionsEditorField.allowAllClasses){
                    className = 'All';
                }else{
                    className = 'UNKNOWN';
                }
            }else{
                className = value.list[0]['fieldValue'];
                className = className.substring(1, className.length -1);
            }
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( classFields[className] ? classFields[className].description : 'Unknown'.t() ) + '"';
            return className;
        },
        conditionsRenderer: function(value, meta){
            var me = this, 
                view = me.getView().up('grid'),
                conditionsEditorField = Ext.Array.findBy(view.editorFields, function(item){
                    if(item.xtype.indexOf('conditionseditor') > -1){
                        return true;
                    }
                }),
                classFields = me.up('config-events').getViewModel().get(conditionsEditorField.allowAllClasses ? 'allClassFields' : 'classFields'),
                className = '',
                valueList = Ext.Array.clone(value.list);

            if(!value.list.length){
                var valueString = '<i>' + 'Match all fields'.t() + '</i>';
                meta.tdAttr = 'data-qtip="' + valueString + '"';
                return valueString;
            }else{
                className = value.list[0]['fieldValue'];
                className = className.substring(1, className.length -1);
                if(classFields[className] && classFields[className].conditions){
                    conditionsEditorField.conditions = classFields[className].conditions;
                }
                valueList.shift();
            }

            return Ung.cmp.ConditionsEditor.renderer.call(this, { list: valueList}, meta);
        }
    }

});

Ext.define('Ung.config.events.ConditionsEditorController', {
    extend: 'Ung.cmp.ConditionsEditorController',
    alias: 'controller.eventconditionseditorcontroller',

    onAfterRender: function (component) {
        var me = this,
            view = me.getView(),
            vm = this.getViewModel(),
            record = vm.get('record');

        var className = '';

        if(!record.get('conditions').list.length){
            className = 'All';
        }else{
            me.classCondition = record.get('conditions').list.shift();
            className =  me.classCondition.fieldValue;
            if(className.length && className[0] == '*'){
                className = className.substring(1, className.length -1);
            }
        }
        record.set('class', className);
        record.commit();

        var classFields = vm.get('allClassFields');
        if(classFields[className].conditionsOrder){
            view.conditionsOrder = classFields[className].conditionsOrder;
        }
        if(classFields[className].conditions){
            view.conditions = classFields[className].conditions;
        }

        me.callParent([component]);
    },

    onRemoved: function(me, container){
        var recordEditorView = this.recordeditor,
            store = me.down('grid').getStore(),
            vm = this.getViewModel(),
            record = vm.get('record'),
            recordName = me.recordName.split('.')[1],
            data = [],
            modified = store.getModifiedRecords().length || store.getRemovedRecords().length || store.getNewRecords().length;

        me = me.down ? me : me.getView();
        var classValue = record.get('class');

        if(this.recordeditor.cancel){
            if(record.previousValues && record.previousValues.class){
                classValue = record.previousValues.class;
            }
            store.rejectChanges();
        }

        data = Ext.Array.pluck(store.getRange(), 'data');
        data.forEach( function(record){
            if(typeof(record.value) == 'object' && record.value.length){
                record.value = record.value.join(',');
            }
        });

        var className = "";
        if(classValue == 'All'){
            className = 'All';
        }else{
            this.classCondition.fieldValue = '*' + classValue + '*';
            data.unshift(this.classCondition);
        }

        recordEditorView.record.set( recordName, {
            javaClass: 'java.util.LinkedList',
            list: data
        });
        if(this.recordeditor.cancel || !modified && !recordEditorView.record.modified){
            recordEditorView.record.commit();
        }
    },

    onClassChange: function( combo, newValue, oldValue ){
        var me = this,
            view = me.getView(),
            appVm = view.up('config-events').getViewModel(),
            classFields = appVm.get('allClassFields'),
            classValue = newValue;

        view.down('grid').setVisible( newValue == 'All' ? false : true);

        view.conditionsOrder = classFields[classValue]['conditionsOrder'];
        view.conditions = classFields[classValue]['conditions'];
        view.comparators = Ext.clone(Ung.cmp.ConditionsEditor.comparators);
        me.buildConditions(view);
        me.updateTargetFields();
        
        if(oldValue == null){
            return;
        }

        var record = this.getViewModel().get('record');
        if(record.previousValues && 
            record.previousValues.class && 
            oldValue == record.previousValues.class){
            // Set back to previous and cancelled.
            return;
        }

        if( newValue != oldValue ) {
            if ( view.down('grid').getStore().getData().length > 0 ) {
                Ext.MessageBox.confirm(
                    'Change Class'.t(),
                    'If you change the class, fields may be removed. Are you sure?'.t(),
                    Ext.bind(function(button){
                        if (button == 'no') {
                            record.set('class', oldValue);
                        } else {
                            this.classChangeFields(newValue, oldValue);
                        }
                    },this)
                );
            }
        }
    },

    classChangeFields: function( newClassName, oldClassName ){
        var view = this.getView(),
            appVm = view.up('config-events').getViewModel();

        // Remove fields if they do not exist in new class.
        // console.log('classChangeFields');
        var oldFields = appVm.get('classFields')[oldClassName].conditions;
        var newFields = appVm.get('classFields')[newClassName].conditions;

        var store = this.getView().down('grid').getStore();
        var match;
        store.each(Ext.bind( function( record ){
            match = false;
            for( var field in newFields){
                if(record.get('field') == field){
                    // Provisionally match!
                    match = true;

                    // But look for other reasons why it may not be true.
                    for(var oldField in oldFields){
                        if(field == oldField){
                            if( field.type != oldField.type){
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
                    }
                }
            }
            if(match === false){
                store.remove(record);
            }
        }, this) );

        // While slightly out of place, this is the best place to check for trigger target field
        // since it also depends on class fields.
        // var targetCombo = this.getView().down('#target');
        var targetCombo = this.getView().up('window').down('#target');
        if(targetCombo){
            var comboValue = targetCombo.getValue();
            match = false;
            // newFields.forEach( function( field ){
            for( var field in newFields){
                if( field.name == comboValue){
                    match = true;
                }
            }
            if( match == false ){
                targetCombo.setValue( newFields[Object.keys(newFields)[0]].name );
            }
        }

    },

    updateTargetFields: function(){
        var me = this,
            view = me.getView(),
            appVm = view.up('config-events').getViewModel(),
            targetFields = [];

        for(var conditionName in view.conditions){
            targetFields.push([conditionName, conditionName]);
        }
        appVm.get( 'targetFields' ).loadData( targetFields );
    }

});