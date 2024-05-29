Ext.define('Ung.cmp.GridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungrid',

    addCount: 0,

    control: {
        '#': {
            drop: 'onDropRecord',
            beforeedit: 'onBeforeEdit'
        },
        'checkcolumn':{
            beforecheckchange: 'onBeforeCheckChange'
        }
    },

    addRecord: function () {
        this.editorWin(null, null, arguments.length > 2 ? arguments[2] : null);
    },

    addRecordInline: function () {
        var v = this.getView(),
            newRecord = Ext.create(v.recordModel, Ung.util.Util.activeClone(v.emptyRow)), rowIndex;

        newRecord.set('markedForNew', true);
        if (v.topInsert) {
            v.getStore().insert(0, newRecord);
        } else {
            v.getStore().add(newRecord);
        }

        // start edit the record right after it was added
        var cellediting = Ext.Array.findBy(v.getPlugins(), function (plugin) {
            return plugin.ptype === 'cellediting';
        });

        if (cellediting) {
            rowIndex = v.getStore().indexOf(newRecord);
            cellediting.startEditByPosition({ row: rowIndex, column: 0 });
        }
    },

    /** used for Port Forward Rules */
    addSimpleRecord: function () {
        this.simpleEditorWin(null, 'add');
    },

    /**
     * Copy a record and mark it as new.
     *
     * If specified in the view, copyId specifies a unique identifier to update.
     * This identifier defaults to the value of -1 unless copyIdPreserve is specified
     * where the current value is preserved (e.g.,-1 is bad valuefor snort rules).
     *
     * If specified, the copyAppendField is a text field that will have thhe " (copy)"
     * identifier appended.
     */
    copyRecord: function (view, rowIndex, colIndex, item, e, record) {
        var me = this,
            v = me.getView(),
            newRecord = record.copy(null);

        var newRecordData_Id = newRecord.data._id;
        var data = Ext.clone(record.data);
        var referenceFields = {};
        if(v.copyReferenceFields){
            v.copyReferenceFields.forEach( function(field){
                if(data[field]){
                    referenceFields[field] = data[field];
                    delete data[field];
                }
            });
        }
        newRecord.data = JSON.parse(JSON.stringify(data));
        if(v.copyReferenceFields){
            for(var field in referenceFields){
                newRecord.data[field] = referenceFields[field];
            }
        }
        newRecord.data._id = newRecordData_Id;
        newRecord.set('markedForNew', true);

        if( v.copyId ){
            var value = -1;
            if( v.copyIdPreserve ){
                value = newRecord.get( v.copyId );
            }
            newRecord.set( v.copyId, value );
        }

        if( v.copyAppendField ){
            newRecord.set( v.copyAppendField, newRecord.get(v.copyAppendField) + ' ' + '(copy)'.t() );
        }

        if( newRecord.get('readOnly') == true){
            delete newRecord.data['readOnly'];
        }

        var restrict = v.restrictedRecords;
        if(restrict && ( restrict.keyMatch != v.copyId ) ){
            delete newRecord.data[restrict.keyMatch];
        }

        if(v.copyModify){
            v.copyModify.forEach(function(kv){
                if(kv.value instanceof Function) 
                    newRecord.set(kv['key'], kv['value']());
                else
                    newRecord.set(kv['key'], kv['value']);
            });
        }

        if (v.topInsert) {
            v.getStore().insert(0, newRecord);
        } else {
            v.getStore().add(newRecord);
        }
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record) {
        if (!record.get('simple')) {
            this.editorWin(record);
        } else {
            this.simpleEditorWin(record, 'edit'); // action to be passed to main editor on Switch
        }
    },

    editorWin: function (record, action, template) {
        this.dialog = this.getView().add({
            xtype: this.getView().editorXtype,
            renderTo: Ext.getBody(),
            record: record,
            action: action, // add or edit
            template: template
        });

        // look for window overrides in the parent grid
        if (this.dialog.ownerCt.editorWidth) this.dialog.width = this.dialog.ownerCt.editorWidth;
        if (this.dialog.ownerCt.editorHeight) this.dialog.height = this.dialog.ownerCt.editorHeight;

        this.dialog.show();
    },

    isRecordRestricted: function(record){
        var restrict = this.getView().restrictedRecords;

        if( restrict ){
            var value = record.get(restrict.keyMatch);
            if(typeof(restrict.valueMatch) == 'object' &&
                restrict.valueMatch.test( value )){
                return true;
            }else if(restrict.valueMatch == value){
                return true;
            }
        }
        return false;
    },

    isRecordRestrictedField: function(record, dataIndex){
        var restrict = this.getView().restrictedRecords;

        if(!this.getView().getController().isRecordRestricted(record)){
            return false;
        }

        var result = true;
        restrict.editableFields.forEach( function(field){
            if(field == dataIndex){
                result = false;
            }
        });
        return result;
    },

    simpleEditorWin: function (record, action) {
        this.simpledialog = this.getView().add({
            xtype: this.getView().simpleEditorAlias,
            record: record,
            action: action
        });

        // look for window overrides in the parent grid
        if (this.simpledialog.ownerCt.editorWidth) this.simpledialog.width = this.simpledialog.ownerCt.editorWidth;
        if (this.simpledialog.ownerCt.editorHeight) this.simpledialog.height = this.simpledialog.ownerCt.editorHeight;

        this.simpledialog.show();
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        if (record.get('markedForNew')) {
            record.drop();
        } else {
            record.set('markedForDelete', true);
        }
    },

    moveUp: function (view, rowIndex, colIndex, item, e, record) {
        var store = this.getView().down('grid').getStore();
        store.remove(record, true); // just moving
        store.insert(rowIndex + item.direction, record);
    },

    // onCellClick: function (table, td, cellIndex, record) {
    //     var me = this;
    //     if (td.dataset.columnid === 'conditions') {
    //         Ext.widget('ung.cmp.ruleeditor', {
    //             conditions: me.getView().conditions,
    //             conditionsMap: me.getView().conditionsMap,
    //             viewModel: {
    //                 data: {
    //                     rule: record
    //                 },
    //                 formulas: {
    //                     conditionsData: {
    //                         bind: '{rule.conditions.list}',
    //                         get: function (coll) {
    //                             return coll || [];
    //                         }
    //                     },
    //                 },
    //             }
    //         });
    //     }
    // },

    priorityRenderer: function(value, meta) {
        var result = '';
        if (!Ext.isEmpty(value)) {
            switch (value) {
                case 0:
                    result = '';
                    break;
                case 1:
                    result = 'Very High'.t();
                    break;
                case 2:
                    result = 'High'.t();
                    break;
                case 3:
                    result = 'Medium'.t();
                    break;
                case 4:
                    result = 'Low'.t();
                    break;
                case 5:
                    result = 'Limited'.t();
                    break;
                case 6:
                    result = 'Limited More'.t();
                    break;
                case 7:
                    result = 'Limited Severely'.t();
                    break;
                default:
                    result = Ext.String.format('Unknown Priority: {0}'.t(), value);
            }
        }
        meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( result ) + '"';
        return result;
    },

    onDropRecord: function (node, data ) {
        this.getView().getStore().isReordered = true;
        data.records.forEach(function(record){
            record.set('markedForMove', true);
        });
    },

    onBeforeEdit: function(plugin, edit){
        if(edit.grid.getController().isRecordRestrictedField(edit.record, edit.column.dataIndex)){
            return false;
        }
        return true;
    },
    onBeforeCheckChange: function(column, rowIndex, checked, record){
        if(column.up('grid').getController().isRecordRestrictedField(record, column.dataIndex)){
            return false;
        }
        return true;
    },

    // import/export features
    importData: function () {
        var me = this,
            v = me.getView();

        this.importDialog = this.getView().add({
            xtype: 'window',
            title: 'Import Settings'.t(),
            renderTo: Ext.getBody(),
            modal: true,
            layout: 'fit',
            width: 450,
            items: [{
                xtype: 'form',
                border: false,
                url: 'gridSettings',
                bodyPadding: 10,
                layout: 'anchor',
                items: Ext.Array.merge( v.import && v.import.items ? v.import.items : [], [{
                    fieldLabel: 'How to process'.t(),
                    name: 'importMode',
                    editable: false,
                    xtype: 'combo',
                    queryMode: 'local',
                    grow: true,
                    value: 'replace',
                    store: [[
                        'replace', 'Replace current settings'.t()
                    ],[
                        'prepend', 'Prepend to current settings'.t()
                    ],[
                        'append', 'Append to current settings'.t()
                    ]],
                    forceSelection: true
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    fieldLabel: 'Settings file'.t(),
                    emtpyText: 'Select a file',
                    allowBlank: false,
                    validateOnBlur: false,
                    listeners:{
                        change: function( field, value){
                            // Remove "fakepath" leaving just the filename
                            field.setRawValue(value.replace(/(^.*(\\|\/))?/, ""));
                        }
                    }
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'import'
                }]
                ),
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban fa-red',
                    handler: function () {
                        me.importDialog.close();
                    }
                }, {
                    text: 'Import'.t(),
                    iconCls: 'fa fa-check',
                    formBind: true,
                    handler: v.import && v.import.handler ? v.import.handler : function (btn) {
                        btn.up('form').submit({
                            waitMsg: 'Please wait while the settings are uploaded...'.t(),
                            success: function(form, action) {
                                if (!action.result) {
                                    Ext.MessageBox.alert('Warning'.t(), 'Import failed.'.t());
                                    return;
                                }
                                if (!action.result.success) {
                                    Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                                    return;
                                }
                                me.importHandler(form.getValues().importMode, action.result.msg);
                                me.importDialog.close();
                            },
                            failure: function(form, action) {
                                Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                            }
                        });
                    }
                }]
            }],
        });
        this.importDialog.show();
    },

    // same as import but without prepend or append options
    replaceData: function () {
        var me = this;
        this.importDialog = this.getView().add({
            xtype: 'window',
            title: 'Import Settings'.t(),
            modal: true,
            layout: 'fit',
            width: 450,
            items: [{
                xtype: 'form',
                border: false,
                url: 'gridSettings',
                bodyPadding: 10,
                layout: 'anchor',
                items: [{
                    xtype: 'component',
                    margin: 10,
                    html: 'Replace current settings with settings from:'.t()
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    fieldLabel: 'File'.t(),
                    labelAlign: 'right',
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'import'
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban fa-red',
                    handler: function () {
                        me.importDialog.close();
                    }
                }, {
                    text: 'Import'.t(),
                    iconCls: 'fa fa-check',
                    formBind: true,
                    handler: function (btn) {
                        btn.up('form').submit({
                            waitMsg: 'Please wait while the settings are uploaded...'.t(),
                            success: function(form, action) {
                                if (!action.result) {
                                    Ext.MessageBox.alert('Warning'.t(), 'Import failed.'.t());
                                    return;
                                }
                                if (!action.result.success) {
                                    Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                                    return;
                                }
                                me.importHandler('replace', action.result.msg);
                                me.importDialog.close();
                            },
                            failure: function(form, action) {
                                Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                            }
                        });
                    }
                }]
            }],
        });
        this.importDialog.show();
    },

    getNestedEditorFieldConfig: function(){
        var editorFields = this.getView().editorFields;
        var mappedObject={};

        if(editorFields){
            editorFields.forEach(function(currFieldConfigObj){
                var currFieldConfig = {};
                if(currFieldConfigObj.xtype === 'container'){
                    currFieldConfig = currFieldConfigObj;
                }else if(currFieldConfigObj.xtype === 'ungrid' && currFieldConfigObj.columns){
                    currFieldConfig = {
                        xtype : 'container',
                        items : [currFieldConfigObj]
                    };
                }
                if(currFieldConfig.xtype === 'container'){
                    if(currFieldConfig.items){
                        currFieldConfig.items.forEach(function(currItem){
                            var fieldObj = {};
                            if(currItem.xtype === 'ungrid'){
                                if(currItem.columns){
                                    currItem.columns.forEach(function(currColumn){
                                        if(currColumn.dataIndex && currColumn.editor && Object.keys(currColumn.editor).length > 0){
                                            fieldObj[currColumn.dataIndex] = currColumn.editor;
                                        }
                                    });
                                }
                            }
                            if(Object.keys(fieldObj).length > 0){
                                var fieldName = currItem.bind;
                                var mapKey = null;
                                if(typeof currItem.bind === "object"){
                                    for(var key in currItem.bind){
                                        if(key === "store"){
                                            fieldName = currItem.bind[key];
                                        }
                                    }
                                }
                                if(fieldName.split(".").length > 1){
                                    mapKey = fieldName.split('.')[1].split('}')[0];
                                }else{
                                    mapKey = fieldName.split('.')[0].split('}')[0].split('{')[1];
                                }
                                if(mapKey){
                                    mappedObject[mapKey]=fieldObj;
                                }
                            }
                        });
                    }
                }
            });

            // adjusting key values of objects if in nested grid only one column field1 is present
            Ext.Object.each(mappedObject, function(currObjKey, currObjValue) {
                if(Object.keys(currObjValue).length === 1 && currObjValue["field1"]){
                    currObjValue[currObjKey] = currObjValue["field1"];
                }
                if(currObjValue["field1"]){
                    delete currObjValue["field1"];
                }
            });
        }

        return mappedObject;
    },

    getFieldConditions: function(condition){
        if(condition){
            var newConditions = null;
            switch (condition.type) {
            case 'textfield':
            case 'numberfield':
            case 'sizefield':
                newConditions = {
                    allowBlank: false,
                };
                break;
            default:
                newConditions={};
            }
            return Ext.merge(condition, newConditions); 
        }
    },

    importHandlerValidator: function(record, fieldValueFn, fieldConfigFn, isNestedEditor, mappedObject, validationErrors, errorObj, nestedObject, grid, me){
        for (var fieldName in record) {
            if(!isNestedEditor && mappedObject[fieldName]){
                continue;
            }else if(isNestedEditor && !nestedObject[fieldName]){
                continue;
            }
            if (record.hasOwnProperty(fieldName)) {
                var fieldValue = fieldValueFn(fieldName);
                var fieldConfig = fieldConfigFn(fieldName);

                if (fieldConfig !== undefined && (fieldConfig.validator || fieldConfig.vtype || !fieldConfig.allowBlank)) {
                    var validationErrorMsg = null;

                    if(fieldConfig.hasOwnProperty("minValue")){
                        if(fieldValue < fieldConfig.minValue){
                            validationErrorMsg = Ext.String.format('The minimum value for this field is {0}.'.t(), fieldConfig.minValue);
                        }
                    }
                    
                    if(fieldConfig.hasOwnProperty("maxValue")){
                        if(fieldValue > fieldConfig.maxValue){
                            validationErrorMsg = Ext.String.format('The maximum value for this field is {0}.'.t(), fieldConfig.maxValue);
                        }
                    }

                    if (!fieldConfig.allowBlank && (!fieldConfig.bind || (fieldConfig.bind && !fieldConfig.bind.disabled)) && Ext.isEmpty(fieldValue)) {
                        validationErrorMsg = Ext.String.format('This field is required.'.t()); 
                    } else if (fieldConfig.allowBlank && Ext.isEmpty(fieldValue)) {
                        continue; // Skip validation if allowBlank is true and field value is empty
                    }

                    // Check vtype validation
                    if (fieldConfig.vtype) {
                        var vtype = fieldConfig.vtype;
                       if(fieldValue !== null){
                            if (!Ext.form.field.VTypes[vtype](fieldValue)) {
                                validationErrorMsg = Ext.form.field.VTypes[vtype + 'Text'];
                            }
                       }
                    }
                    
                    // Currently, custom validator is tailored exclusively for the WG App.
                    // To extend its functionality to other apps, ensure custom validators across each app retrieve stored values during import operations.
                    if(grid.viewConfig.importValidationJavaClass){
                        if (fieldConfig.validator && fieldConfig.validator(fieldValue, this) != true) {
                            validationErrorMsg = fieldConfig.validator(fieldValue, this);
                        }
                    }
                    
                    if (validationErrorMsg !== null) {
                        errorObj.isValidRecord = false;
                        validationErrors.push(Ext.String.format('Validation failed for field: {0}, value: {1}, error: {2}'.t(), fieldName, fieldValue, validationErrorMsg)); 
                        break; // Stop validation for this record if any field fails
                    }
                }

                // check allowblank and vtype validation for conditions applied

                if(fieldConfig && fieldConfig.conditionsOrder){
                    if(fieldConfig && fieldConfig.conditions){
                        var errorMsgForConditions = null;
                        var currentValue = null;
                        for (var i=0; i < fieldConfig.conditionsOrder.length; i++){
                            var conditionName = fieldConfig.conditionsOrder[i];
                            if(Object.entries(fieldConfig.conditions).length > 0){
                                var currentCondition = fieldConfig.conditions[conditionName];
                                if(currentCondition && fieldValue && fieldValue.list){
                                    for(var j=0; j < fieldValue.list.length; j++){
                                        var currentRow = fieldValue.list[j];
                                        currentValue = currentRow.value;
                                        if(currentRow.conditionType && currentRow.conditionType === conditionName){
                                            if (currentCondition.hasOwnProperty("allowBlank") && !currentCondition.allowBlank && Ext.isEmpty(currentValue)) {
                                                errorMsgForConditions = Ext.String.format('This field is required.'.t());
                                                break; 
                                            } else if (currentCondition.allowBlank && Ext.isEmpty(currentValue)) {
                                                continue; // Skip validation if allowBlank is true and field value is empty
                                            }

                                            if (currentCondition.vtype) {
                                                var currVtype = currentCondition.vtype;
                                                if(currentValue){
                                                    if (!Ext.form.field.VTypes[currVtype](currentValue)) {
                                                        errorMsgForConditions = Ext.form.field.VTypes[currVtype + 'Text'];
                                                        break;
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            if(errorMsgForConditions){
                                break;
                            }
                        }

                        if (errorMsgForConditions !== null) {
                            errorObj.isValidRecord = false;
                            validationErrors.push(Ext.String.format('Validation failed for field: {0}, value: {1}, error: {2}'.t(), fieldName, currentValue, errorMsgForConditions)); 
                            break; // Stop validation for this record if any field fails
                        }
                    }
                }else if(grid.viewConfig.importValidationForAlertEvents && fieldConfig && !fieldConfig.conditionsOrder && fieldConfig.conditions){
                    var classFieldConditions = {};
                    var appVm = this.getView().up("config-events");
                    if(appVm){
                        appVm = appVm.getViewModel();
                        for(var listIndex = 0; listIndex < fieldValue.list.length; listIndex++){
                            if(fieldValue.list[listIndex] && fieldValue.list[listIndex].field === "class"){
                                var currCls = fieldValue.list[listIndex].fieldValue;
                                currCls = currCls.split("*");
                                if(currCls.length === 0){
                                    currCls = currCls[0];
                                }else{
                                    currCls = currCls[1];
                                }
                                if(appVm.get("classFields")){
                                    classFieldConditions = appVm.get("classFields")[currCls];
                                }
                            }
                        }
                    }
                    
                    // check allowblank and vtype validation for conditions applied for events
                    if(Object.keys(classFieldConditions).length > 0 && fieldConfig && fieldConfig.conditions && fieldValue && fieldValue.list){
                        var errorMsgForCurrCondn = null;
                        var currentValueObj = null;
                        for (var k=0; k < fieldValue.list.length; k++){
                            currentValueObj = fieldValue.list[k];
                            if(currentValueObj.field !== "class"){
                                var currentCondn = me.getFieldConditions(classFieldConditions.conditions[currentValueObj.field]);
                                if(currentCondn && Object.keys(currentCondn).length > 0 && fieldValue && fieldValue.list){
                                    if (currentCondn.hasOwnProperty("allowBlank") && !currentCondn.allowBlank && Ext.isEmpty(currentValueObj.fieldValue)) {
                                        errorMsgForCurrCondn = Ext.String.format('This field is required.'.t());
                                        break; 
                                    } else if ((!currentCondn.hasOwnProperty("allowBlank") || currentCondn.allowBlank) && Ext.isEmpty(currentValueObj.fieldValue)) {
                                        continue; // Skip validation if allowBlank is true and field value is empty
                                    }
                                    if (currentCondn.vtype) {
                                        var currCondnVtype = currentCondn.vtype;
                                        if(currentValueObj.fieldValue){
                                            if (!Ext.form.field.VTypes[currCondnVtype](currentValueObj.fieldValue)) {
                                                errorMsgForCurrCondn = Ext.form.field.VTypes[currCondnVtype + 'Text'];
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (errorMsgForCurrCondn !== null) {
                            errorObj.isValidRecord = false;
                            validationErrors.push(Ext.String.format('Validation failed for field: {0}, value: {1}, error: {2}'.t(), currentValueObj.field, currentValueObj.fieldValue, errorMsgForCurrCondn)); 
                            break; // Stop validation for this record if any field fails
                        }
                    }
                }
            }
        }
    },

    importHandler: function (importMode, newData) {
        var grid = this.getView(),
            existingData = Ext.Array.pluck(grid.getStore().getRange(), 'data'),
            me = this;

        var validData = [];
        var validationErrors = [];

        //check and if nested editor is present get the list
        var mappedObject = this.getNestedEditorFieldConfig();

        newData.forEach(function(record) {
                var errorObj = {isValidRecord:true};
                this.importHandlerValidator(record,
                function(fieldNm){
                    return record[fieldNm];
                },
                function(fieldNm){
                    return me.getFieldConfig(fieldNm);
                },
                false, mappedObject, validationErrors, errorObj, {}, grid, me);
                if(errorObj.isValidRecord){
                    for(var fieldName in record){
                        if(!errorObj.isValidRecord){
                            break;
                        }
                        if(mappedObject[fieldName]){
                            for(var i=0; i<record[fieldName].list.length; i++ ){
                                if(!errorObj.isValidRecord){
                                    break;
                                }
                                var currListElement = {};
                                if(typeof record[fieldName].list[i] === "object"){
                                    currListElement = record[fieldName].list[i];
                                }else{
                                    currListElement[fieldName] = record[fieldName].list[i];
                                }
                                this.importHandlerValidator(currListElement,
                                function(fieldNm){
                                    return currListElement[fieldNm];
                                },
                                function(fieldNm){
                                    return mappedObject[fieldName][fieldNm];
                                },
                                true,mappedObject, validationErrors, errorObj, mappedObject[fieldName], grid, me);
                            }
                        }
                    }
                }
                if (errorObj.isValidRecord) {
                    validData.push(record);
                }
        }, this);
        

        // Show validation errors as alert
        if (validationErrors.length > 0) {
            var errorMessage = Ext.String.format("Import record validation error:\n\n{0}".t(), validationErrors.join("\n"));
            alert(errorMessage);  // Do not proceed with loading data if there are validation errors for record
        }

        //To import all the record for another app
        if (validData.length === 0 && validationErrors.length === 0) {
            validData = newData;
        }
    
        Ext.Array.forEach(existingData, function (rec, index) {
            delete rec._id;
        });

        if (importMode === 'replace') {
            grid.getStore().removeAll();
        }
        if (importMode === 'append') {
            Ext.Array.insert(existingData, existingData.length, validData);
            validData = existingData;
        }
        if (importMode === 'prepend') {
            Ext.Array.insert(existingData, 0, validData);
            validData = existingData;
        }
    
        grid.getStore().loadData(validData);
        grid.getStore().each(function(record){
            record.set('markedForNew', true);
        });
    },
    
    getFieldConfig: function(fieldName) {
        // Retrieve the field configuration from editorFields array based on field name extracted from bind property
        if(this.getView().editorFields !== null){
            // generating the editorFieldsConfig Array for normal fields and fields with fieldcontainer type
            // as fieldContainer again has items in it which we need to take as well.
            var editorFieldsConfig = [];
            this.getView().editorFields.forEach(function(currObj){
                if(currObj.xtype === "fieldcontainer" && currObj.items){
                    currObj.items.forEach(function(currItem){
                       editorFieldsConfig.push(currItem); 
                    });
                }else{
                    editorFieldsConfig.push(currObj);
                }
            });

            return editorFieldsConfig.find(function(fieldConfig) {
                if (fieldConfig.bind) {
                    // Extract the field name from bind property in various formats
                    var bindValue = fieldConfig.bind.value || fieldConfig.bind;
                    if (typeof bindValue === 'string') {
                        // If bindValue is a string, check if it's in the specified format
                        var fieldNameFromBind = bindValue.split('.')[1].split('}')[0];
                        if (fieldNameFromBind === fieldName) {
                            return fieldConfig;
                        }
                    } else if (typeof bindValue === 'object') {
                        // If bindValue is an object, check each key for the specified format
                        for (var key in bindValue) {
                            if (bindValue.hasOwnProperty(key) && key === 'value') {
                                var value = bindValue[key];
                                if (typeof value === 'string' && value.includes('{record.')) {
                                    var fieldNameFromBindValue = value.split('.')[1].split('}')[0];
                                    if (fieldNameFromBindValue === fieldName) {
                                        return fieldConfig;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            });
        }else if(this.getView().config && this.getView().config.columns){
            var editorConfig = null;
            editorConfig = this.getView().config.columns.find(function (fieldConfig){
                if(fieldConfig.dataIndex){
                    if(fieldConfig.dataIndex === fieldName && fieldConfig.editor){
                        return fieldConfig.editor;
                    }
                }
                return false;
            });
            if(editorConfig && editorConfig.editor){
                return editorConfig.editor;
            }
        }
       
    },
    
    getExportData: function (useId) {
        var data = Ext.Array.pluck(this.getView().getStore().getRange(), 'data');
        Ext.Array.forEach(data, function (rec, index) {
            delete rec._id;
            if (useId) {
                rec.id = index + 1;
            }
            Ext.Object.each(rec,function(objKey, ObjValue){
                if(ObjValue && ObjValue.list && typeof ObjValue === "object"){
                    Ext.Array.forEach(ObjValue.list, function (innerRec) {
                        delete innerRec._id;
                    });
                }
            });
        });
        return Ext.encode(data);
    },

    exportData: function () {
        var grid = this.getView();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = grid.getXType();
        exportForm.gridData.value = this.getExportData(false);
        exportForm.submit();
        Ext.MessageBox.hide();
    },

    changePassword: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        this.pswdDialog = this.getView().add({
            xtype: 'window',
            title: 'Change Password'.t(),
            modal: true,
            resizable: false,
            layout: 'fit',
            width: 350,
            items: [{
                xtype: 'form',
                layout: 'anchor',
                border: false,
                bodyPadding: 10,
                defaults: {
                    xtype: 'textfield',
                    labelWidth: 120,
                    labelAlign: 'right',
                    anchor: '100%',
                    inputType: 'password',
                    allowBlank: false,
                    listeners: {
                        keyup: function (el) {
                            var form = el.up('form'),
                                vals = form.getForm().getValues();
                            if (vals.pass1.length < 3 || vals.pass2.length < 3 || vals.pass1 !== vals.pass2) {
                                form.down('#done').setDisabled(true);
                            } else {
                                form.down('#done').setDisabled(false);
                            }
                        }
                    }
                },
                items: [{
                    fieldLabel: 'Password'.t(),
                    name: 'pass1',
                    enableKeyEvents: true,
                    // minLength: 3,
                    // minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3),
                }, {
                    fieldLabel: 'Confirm Password'.t(),
                    name: 'pass2',
                    enableKeyEvents: true
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    handler: function () {
                        me.pswdDialog.close();
                    }
                }, {
                    text: 'Done'.t(),
                    itemId: 'done',
                    disabled: true,
                    // formBind: true
                    handler: function (btn)  {
                        record.set('password', btn.up('form').getForm().getValues().pass1);
                        var changePasswordOptions = btn.up('grid').changePasswordOptions;

                        if (changePasswordOptions &&
                            changePasswordOptions.setRecordFields &&
                            ( typeof changePasswordOptions.setRecordFields === 'object') ){
                            // With optional changePasswordOptions.setRecordFields set these record
                            // values which may be neccessar to properly force password change in backend.
                            var key;
                            for ( key in changePasswordOptions.setRecordFields){
                                if(changePasswordOptions.setRecordFields.hasOwnProperty(key)){
                                    record.set(key, changePasswordOptions.setRecordFields[key]);
                                }
                            }
                        }
                        me.pswdDialog.close();
                    }
                }]
            }]
        });
        this.pswdDialog.show();
    },

    beforeEdit: function( editor, context ){
        if( context &&
            context.record &&
            context.record.get('readOnly') === true ){
            return false;
        }
        return true;
    },

    refresh: function(){
        var view = this.getView(),
            parentController = null;

        // In parent controller, look for method as get<itemId> in camel case  ( e.g.,getWanStatus() ).
        // If itemId not found, look for getRefresh()
        var method = Ext.String.camelCase(['get', view.itemId ? view.itemId : 'refresh']);

        while( view != null){
            parentController = view.getController();

            if( parentController && parentController[method]){
                break;
            }
            view = view.up();
        }
        if (!parentController) {
            console.log('Unable to get the extra controller for method ' + method);
            return;
        }

        parentController[method].apply(parentController, arguments);

    },

    resetView: function(){
        var view = this.getView(),
            store = view.getStore();

        Ext.state.Manager.clear(view.stateId);
        store.getSorters().removeAll();

        // Look for first non-hidden column and make that the default sort.
        var defaultDataIndex = null;
        view.initialConfig.columns.forEach( Ext.bind(function( column ){
            if(!column.hidden && defaultDataIndex == null){
                defaultDataIndex = column.dataIndex;
            }
        }));
        if(defaultDataIndex != null){
            store.sort(defaultDataIndex, 'ASC');
        }

        store.clearFilter();
        view.reconfigure(null, view.initialConfig.columns);
    },

    /**
     * Used for extra column actions which can be added to the grid but are very specific to that context
     * The grid requires to have defined a parentView tied to the controller on which action method is implemented
     * action - is an extra configuration set on actioncolumn and represents the name of the method to be called
     * see Users/UsersController implementation
     */
    externalAction: function (v, rowIndex, colIndex, item, e, record) {
        var view = this.getView(),
            parentController = null,
            action = item && item.action ? item.action : v.action;

        while( view != null){
            parentController = view.getController();

            if( parentController && parentController[action]){
                break;
            }
            view = view.up();
        }

        if (!parentController) {
            console.log('Unable to get the extra controller');
            return;
        }

        // call the action from the extra controller in extra controller scope, and pass all the actioncolumn arguments
        if (action) {
            parentController[action].apply(parentController, arguments);
        } else {
            console.log('External action not defined!');
        }
    }
});
