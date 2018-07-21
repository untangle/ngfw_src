Ext.define('Ung.apps.intrusionprevention.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-intrusion-prevention',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getLastUpdateCheck'),
            Rpc.asyncPromise(v.appManager, 'getLastUpdate'),
            Rpc.directPromise('rpc.companyName'),
            function(){ return Ext.Ajax.request({
                url: "/admin/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "load",
                    arg2: vm.get('instance.id')
                },
                timeout: 600000});
            },function(){ return Ext.Ajax.request({
                url: "/admin/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "wizard",
                    arg2: vm.get('instance.id')
                },
                timeout: 600000});
        }]).then(function(result){
            if(Util.isDestroyed(me, vm)){
                return;
            }
            var settings = null;
            try{
                settings = Ext.decode( result[3].responseText);
            }catch(error){
                v.setLoading(false);
                Util.handleException({
                    message: 'Intrusion Prevention settings file is corrupt.'.t()
                });
                return;
            }
            vm.set({
                lastUpdateCheck: (result[0] !== null && result[0].time !== 0 ) ? Renderer.timestamp(result[0]) : "Never".t(),
                lastUpdate: (result[1] !== null && result[1].time !== 0 ) ? Renderer.timestamp(result[1]) : "Never".t(),
                companyName: result[2],
                settings: settings,
                wizardDefaults: Ext.decode( result[4].responseText ),
                profileStoreLoad: true,
                signaturesStoreLoad: true,
                variablesStoreLoad: true
            });
            v.getController().updateStatus();
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

        }, function (ex) {
            if(!Util.isDestroyed(me, v )){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getChangedDataRecords: function(target){
        var v = this.getView();
        var changed = {};
        v.query('app-intrusion-prevention-' + target).forEach(function(grid){
            var store = grid.getStore();
            store.getModifiedRecords().forEach( function(record){
                var data = {
                    op: 'modified',
                    recData: record.data
                };
                if(record.get('markedForDelete')){
                    data.op = 'deleted';
                }else if(record.get('markedForNew')){
                    data.op = 'added';
                }
                changed[record.get('_id')] = data;
            });
            store.commitChanges();
        });

        return changed;
    },

    getChangedData: function(){
        var me = this, vm = this.getViewModel();

        var settings = vm.get('settings');
        var changedDataSet = {};
        var keys = Object.keys(settings);
        for( var i = 0; i < keys.length; i++){
            if( ( keys[i] == "signatures" ) ||
                ( keys[i] == "variables" ) ||
                ( keys[i] == "rules" ) ||
                ( keys[i] == "profileId" ) ||
                ( keys[i] == "activeGroups") ){
                continue;
            }
            changedDataSet[keys[i]] = settings[keys[i]];
        }

        changedDataSet.rules = me.getChangedDataRecords('rules');
        changedDataSet.signatures = me.getChangedDataRecords('signatures');
        changedDataSet.variables = me.getChangedDataRecords('variables');

        return changedDataSet;
    },

    setSettings: function (additionalChanged) {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        var changedData = me.getChangedData();
        if(arguments.length == 1){
            if(additionalChanged){
                changedData= Ext.Object.merge(changedData,additionalChanged);
            }
        }

        Ext.Ajax.request({
            url: "/admin/download",
            jsonData: changedData,
            method: 'POST',
            params: {
                type: "IntrusionPreventionSettings",
                arg1: "save",
                arg2: vm.get('instance.id')
            },
            scope: this,
            timeout: 600000
        }).then(function(result){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }

            var response = Ext.decode( result.responseText );
            vm.set({
                profileStoreLoad: true,
                signaturesStoreLoad: true,
                variablesStoreLoad: true
            });

            if( !response.success) {
                Ext.MessageBox.alert("Error".t(), "Unable to save settings".t());
            } else {
                Rpc.asyncData(v.appManager, 'reconfigure')
                .then( function(result){
                    if(Util.isDestroyed(me, v, vm)){
                        return;
                    }
                    v.setLoading(false);
                    Util.successToast('Settings saved...');
                    v.down('appstate').getController().reload();
                    me.getSettings();
                    Ext.fireEvent('resetfields', v);
                }, function(response){
                    if(!Util.isDestroyed(me, v, vm)){
                        v.setLoading(false);
                        vm.set('panel.saveDisabled', true);
                    }
                    Util.handleException(response);
                });
            }
        }, function(response){
            Ext.MessageBox.alert("Error".t(), "Unable to save settings".t());
            if(!Util.isDestroyed(me, v, vm)){
                v.setLoading(false);
                Util.successToast('Unable to save settings...');
                return;
            }
            Util.handleException(response);
        });

    },

    regexSignatureVariable :  /^\$([A-Za-z0-9\_]+)/,
    regexSignature: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,
    isVariableUsed: function(variable) {
        var me = this, vm = this.getViewModel();

        if(Ext.isEmpty(variable)) {
            return false;
        }

        var signature, originalId, signatureMatches, variableMatches, j, internalId, d;
        var isUsed = false;
        vm.get('signatures').each(function(record){
            var signatureMatches = me.regexSignature.exec( record.get('signature') );
            if( signatureMatches ) {
                for( j = 1; j < signatureMatches.length; j++ ) {
                    variableMatches = me.regexSignatureVariable.exec( signatureMatches[j] );
                    if( variableMatches && variableMatches.shift().indexOf(variable)!= -1) {
                        isUsed = true;
                        return false;
                    }
                }
            }

        });
        return isUsed;
    },

    runWizard: function (btn) {
        this.wizard = this.getView().add({
            xtype: 'app-intrusion-prevention-wizard'
        });
        this.wizard.show();
    },

    storeDataChanged: function( store ){
        /*
         * Inexplicably, extjs does not see 'inline' data loads as "proper" store
         * reloads so it will never fire the 'load' event which logically sounds
         * like the correct event to listen to.
         *
         * The problem occurs on saves where data is "reloaded" but seen in
         * the store as a wholesale change.  Which is ridiculous and on the next
         * save in the same session, causes to see all records as modified
         * and therefore, send send ALL data back.
         *
         * To get around this, we have the inline loader routines set the
         * 'storeId'Load variable and if we see it here, cause all of those changes
         * to be "commited" since nothing has changed.
         *
         * Thanks, ExtJs.
         *
         */
        var vm = this.getViewModel();
        var storeId = store.getStoreId();
        if(vm.get( storeId + 'Load') == true){
            store.commitChanges();
            vm.set( storeId + 'Load', false);
        }
    },

    /*
     * Update bind variables for the Setup Wizard display on status screen.
     * On first blush we could do these as formulas but the code is basically the
     * same and would be duplicated.  So we do it here after the data has been
     * loaded.
     */
    updateStatus: function(){
        var me = this, vm = me.getViewModel();
        var activeGroups = vm.get('settings.activeGroups');
        var profileId= vm.get('settings.profileId');

        var types = ['classtypes', 'categories'];
        types.forEach(function(type){
            var current = activeGroups[type];
            var profile = {};

            var rawSelected = null;
            if(current === 'custom'){
                rawSelected = Ext.clone(activeGroups[type+'Selected']);
            }else{
                vm.get('wizardDefaults').profiles.forEach(function(wizardProfile){
                    if(wizardProfile.profileId === profileId){
                        rawSelected = Ext.clone( wizardProfile.activeGroups[type+'Selected'] );
                    }
                });
            }

            var selected = [];
            rawSelected.forEach(function(item){
                if(item[0] === '-'){
                    return;
                }
                if(item[0] === '+'){
                    item = item.substring(1);
                }
                selected.push(item);
            });
            profile.selected = selected;

            if(current === 'custom'){
                profile.value = 'Custom'.t() + Ext.String.format( ': {0}', selected.join(', ') );
            }else{
                profile.value = 'Recommended'.t();
            }
            profile.tip = selected.join(', ');
            vm.set(type+'Profile', profile);
        });
    },

    /**
     * ExtJs does not seem to allow tooltip binding, so to get around this, create
     * model variable with 'value' and 'tip' keys and when the value changes,
     * look at the bind parent value for that object and set the tooltip.
     */
    bindChange: function(cmp){
        cmp.getEl().set({
            'data-qtip': cmp.bind.value.stub.parentValue.tip
        });
    },

    statics:{
        ruleActionsRenderer: function(value, meta, record, x,y, z, table){
            var displayValue = table.up('grid').getController().getViewModel().get('ruleActionsStore').findRecord('value', value).get('display');
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( displayValue ) + '"';
            return displayValue;
        }
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.SignaturesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unintrusionsignaturesrecordeditor',

    controller: 'unintrusionsignaturesrecordeditorcontroller',
});

Ext.define('Ung.apps.intrusionprevention.cmp.SignaturesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unintrusionsignaturesrecordeditorcontroller',

    editorClasstypeChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel();
        if( newValue == null || Ung.apps.intrusionprevention.Main.classtypes.findExact('name', newValue) == null ){
            me.setValidation("Unknown classtype".t());
            return false;
        }
        me.setValidation(true);

        this.getView().up('grid').getController().updateSignature( this.getViewModel().get('record'), 'classtype', newValue );
    },

    editorMsgChange: function( me, newValue, oldValue, eOpts ){
        if( /[";]/.test( newValue ) ){
            me.setValidation( 'Msg contains invalid characters.'.t() );
            return false;
        }
        me.setValidation(true);
        this.getView().up('grid').getController().updateSignature( this.getViewModel().get('record'), 'msg', newValue );
    },

    sidRegex: /\s+sid:([^;]+);/,
    gidRegex: /\s+gid:\s*([^;]+);/,
    editorSidChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView();
        var vm = this.getViewModel();

        // Perform validation
        if( ! /^[0-9]+$/.test( newValue )){
            me.setValidation("Sid must be numeric".t());
            return false;
        }
        var record = vm.get('record');
        var originalRecord = v.record;

        var gid = '1';
        var signature = record.get('signature');
        if( this.gidRegex.test( signature )){
            gid = this.gidRegex.exec( signature )[1];
        }

        var match = false;
        vm.get('signatures').each( function( storeRecord ) {
            if( storeRecord != originalRecord && storeRecord.get('sid') == newValue) {
                var signatureGid = "1";
                signatureValue = storeRecord.get("signature");
                if( this.gidRegex.test( signatureValue ) ) {
                    signatureGid = this.gidRegex.exec( signatureValue )[1];
                    this.gidRegex.lastIndex = 0;
                }

                if( gid == signatureGid ){
                    match = true;
                    return false;
                }
            }
        }, this);

        if( match === true ){
            me.setValidation("Sid already in use.".t());
            return false;
        }

        me.setValidation(true);
        this.getView().up('grid').getController().updateSignature( this.getViewModel().get('record'), 'sid', newValue );
    },

    editorLogChange: function( me, newValue, oldValue, eOpts ) {
        // If log disabled, ensure that block is also disabled.
        var record = this.getViewModel().get('record');
        if( newValue === false) {
            record.set('block', false);
        }
        this.getView().up('grid').getController().updateSignature( record, 'log', newValue );
    },

    editorBlockChange: function( me, newValue, oldValue, eOpts ) {
        // If block enabled, that log is also enabled.
        var record = this.getViewModel().get('record');
        if( newValue === true ){
            record.set('log', true);
        }
        this.getView().up('grid').getController().updateSignature( record, 'block', newValue );
    },

    actionRegex: /^([#]+|)\s*(alert|log|pass|activate|dynamic|drop|sdrop|reject)/,
    editorSignatureChange: function( me, newValue, oldValue, eOpts ){
        var signature = new Ung.model.intrusionprevention.signature(newValue);

        if(!signature.get('valid')){
            return;
        }

        var record = this.getViewModel().get('record');

        record.set('sid', signature.getOption('sid'));
        record.set('gid', signature.getOption('gid'));
        record.set('msg', signature.getOption('msg'));

        var log = false;
        var block = false;
        if(signature.get('enabled')){
            var action = signature.get('action');
            if(action == 'alert'){
                log = true;
                block = false;
            }else if(action == 'drop'){
                log = true;
                block = true;
            }else if(action == 'sdrop'){
                log = false;
                block = true;
            }
        }
        record.set('log', log);
        record.set('block', block);
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.SignatureGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unintrusionsignaturesgrid',

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.unintrusionsignaturesrecordeditor',
            record: record
        });
        this.dialog.show();
    },

    regexSignatureGid: /\s+gid:\s*([^;]+);/,
    sidRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
        var sid = record.get('sid');
        var gid = '1';
        if( this.regexSignatureGid.test( record.get('signature') ) ){
            gid = this.regexSignatureGid.exec( record.get('signature') )[1];
        }
        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( "Sid".t() + ": " + sid + ", " + "Gid".t() + ":" + gid) + '"';
        return value;
    },

    classtypeRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
        var vm = this.getViewModel();
        var description = value;
        var classtypeRecord = Ung.apps.intrusionprevention.Main.classtypes.findRecord('name', value);
        if( classtypeRecord != null ){
            description = classtypeRecord.get('description');
        }
        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description ) + '"';
        return value;
    },

    categoryRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
        var vm = this.getViewModel();
        var description = value;
        var categoryRecord = Ung.apps.intrusionprevention.Main.categories.findRecord('name', value);
        if( categoryRecord != null ){
            description = categoryRecord.get('description');
        }
        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description  ) + '"';
        return value;
    },

    regexSignatureReference: /\s+reference:\s*([^\;]+)\;/g,
    referencesMap: {
        "bugtraq": "http://www.securityfocus.com/bid/",
        "cve": "http://cve.mitre.org/cgi-bin/cvename.cgi?name=",
        "nessus": "http://cgi.nessus.org/plugins/dump.php3?id=",
        "arachnids": "http://www.whitehats.com/info/IDS",
        "mcafee": "http://vil.nai.com/vil/content/v",
        "osvdb": "http://osvdb.org/show/osvdb/",
        "msb": "http://technet.microsoft.com/en-us/security/bulletin/",
        "url": "http://"
    },
    referenceRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
        var matches = null;
        matches = value.match(this.regexSignatureReference);
        if( matches == null ){
            return "";
        }
        var references = [];
        for( var i = 0; i < matches.length; i++ ){
            var rmatches = this.regexSignatureReference.exec( matches[i] );
            this.regexSignatureReference.lastIndex = 0;

            var url = "";
            var referenceFields = rmatches[1].split(",");
            var prefix = this.referencesMap[referenceFields[0]];
            if( prefix != null ){
                referenceFields[1] = referenceFields[1].trim();
                if((referenceFields[1].charAt(0) == '"') &&
                    (referenceFields[1].charAt(referenceFields[1].length - 1) == '"')){
                    referenceFields[1] = referenceFields[1].substr(1,referenceFields[1].length - 2);
                }
                url = prefix + referenceFields[1];
                references.push('<a href="'+ url + '" class="fa fa-search fa-black" style="text-decoration: none; color:black" target="_reference"></a>');
            }
        }
        return references.join("");
    },

    logBeforeCheckChange: function ( elem, rowIndex, checked ){
        var record = elem.getView().getRecord(rowIndex);
        if( !checked){
            record.set('log', false);
            record.set('block', false );
            this.updateSignature(record, 'block', false );
            this.updateSignature(record, 'log', false );
        }else{
            record.set('log', true);
            this.updateSignature(record, 'log', true );
        }
    },

    logCheckAll: function(checkbox, checked){
        Ext.MessageBox.wait(checked ? "Checking All ...".t() : "Unchecking All ...".t(), "Please wait".t());
        Ext.Function.defer(function() {
            var grid=checkbox.up("grid");
            var records=grid.getStore().getRange();
            grid.getStore().suspendEvents(true);
            var record;
            for(var i=0; i<records.length; i++) {
                record = records[i];
                if(checked == false ) {
                    record.set('block', false);
                    this.updateSignature(records[i], 'block', checked );
                }
                record.set('log', checked);
                this.updateSignature(records[i], 'log', checked );
            }
            grid.getStore().resumeEvents();
            grid.getStore().getFilters().notify('endupdate');
            Ext.MessageBox.hide();
        }, 100, this);

    },

    blockBeforeCheckChange: function ( elem, rowIndex, checked ){
        var record = elem.getView().getRecord(rowIndex);
        if(checked) {
            record.set('log', true );
            record.set('block', true);
            this.updateSignature(record, 'log', true );
            this.updateSignature(record, 'block', true );
        }else{
            record.set('block', false);
            this.updateSignature(record, 'block', false );
        }
    },

    blockCheckAll: function(checkbox, checked){
        Ext.MessageBox.wait(checked ? "Checking All ...".t() : "Unchecking All ...".t(), "Please wait".t());
        Ext.Function.defer(function() {
            var grid=checkbox.up("grid");
            var records=grid.getStore().getRange();
            grid.getStore().suspendEvents(true);
            var record;
            for(var i=0; i<records.length; i++) {
                record = records[i];
                if(checked) {
                    record.set('log', true);
                    this.updateSignature(records[i], 'log', checked );
                }
                record.set('block', checked);
                this.updateSignature(records[i], 'block', checked );
            }
            grid.getStore().resumeEvents();
            grid.getStore().getFilters().notify('endupdate');
            Ext.MessageBox.hide();
        }, 100, this);
    },

    updateSearchStatusBar: function(){
        var v = this.getView();
        var searchStatus = v.down("[name=searchStatus]");
        var hasLogOrBlockFilter = ( v.down("[name=searchLog]").getValue() === true ) || ( v.down("[name=searchBlock]").getValue() === true );
        var hasFilter = hasLogOrBlockFilter || ( v.down("[name=searchFilter]").getValue().length >= 2 );
        var statusText = "", logOrBlockText = "", totalEnabled = 0;
        if(!hasLogOrBlockFilter) {
            v.getStore().each(function( record ){
                if( ( record.get('log')) || ( record.get('block')) ) {
                    totalEnabled++;
                }
            });
            logOrBlockText = Ext.String.format( '{0} logging or blocking'.t(), totalEnabled);
        }
        if(hasFilter) {
            statusText = Ext.String.format( '{0} matching signature(s) found'.t(), v.getStore().getCount() );
            if(!hasLogOrBlockFilter) {
                statusText += ', ' + logOrBlockText;
            }
        } else {
            statusText = Ext.String.format( '{0} available signatures'.t(), v.getStore().getCount()) + ', ' + logOrBlockText;
        }
        searchStatus.update( statusText );
    },

    searchFilter: Ext.create('Ext.util.Filter', {
        filterFn: function(){}
    }),
    filterSearch: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            if( newValue.length > 1 ){
                var re = new RegExp(newValue, 'gi');
                this.searchFilter.setFilterFn( function(record){
                    return re.test(record.get('category')) ||
                        re.test(record.get('signature'));
                });
                store.addFilter( this.searchFilter );
            }
        }else{
            store.removeFilter( this.searchFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
    },

    logFilter: Ext.create('Ext.util.Filter', {
        property: 'log',
        value: true
    }),

    filterLog: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            store.addFilter( this.logFilter );
        }else{
            store.removeFilter( this.logFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
    },

    blockFilter: Ext.create('Ext.util.Filter', {
        property: 'block',
        value: true
    }),

    filterBlock: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            store.addFilter( this.blockFilter );
        }else{
            store.removeFilter( this.blockFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
    },

    exportData: function(){
        var grid = this.getView(),
            gridName = (grid.name !== null) ? grid.name : grid.recordJavaClass,
            vm = grid.up('app-intrusion-prevention').getController().getViewModel();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value = "IntrusionPreventionSettings";
        downloadForm["arg1"].value = "export";
        downloadForm["arg2"].value = vm.get('instance.id');
        downloadForm["arg3"].value = gridName.trim().replace(/ /g, '_');
        downloadForm["arg4"].value = Ext.encode({signatures: grid.up('app-intrusion-prevention').getController().getChangedDataRecords('signatures')});
        downloadForm.submit();

        Ext.MessageBox.hide();
    },

    importHandler: function(importMode, newData){
        var me = this;

        this.callParent(arguments);
        var store = this.getView().getStore();

        store.each( function(record){
            me.updateSignature(record, 'log', record.get('log') );
            me.updateSignature(record, 'block', record.get('block') );

        });
    },

    signaturesReconfigure: function( me , store , columns , oldStore , oldColumns , eOpts ){
        me.getController().updateSearchStatusBar();
    },

    updateSignature: function( record, updatedKey, updatedValue){
        var i, regex;
        var signatureValue = record.get('signature');
        var updatedSubkey = null;

        var signature = new Ung.model.intrusionprevention.signature(signatureValue);
        if(!signature.get('valid')){
            return;
        }

        // Action replacement
        if(updatedKey == 'log' || updatedKey == 'block'){
            var logValue = record.get('log');
            var blockValue = record.get('block');
            if(updatedKey == 'log'){
                logValue = updatedValue;
            }
            if(updatedKey == 'block'){
                blockValue = updatedValue;
            }

            signature.set('action', 'alert');
            signature.set('enabled', true);
            if( logValue === true && blockValue === true ) {
                signature.set('action', 'drop');
            } else if( logValue === false && blockValue === true ) {
                signature.set('action', 'sdrop');
            }else if( logValue === false && blockValue === false ) {
                signature.set('enabled', false);
            }

            // Update metadata with indicator that user has changed to preserve on signature updates.
            var date = new Date(Date.now() + Renderer.timestampOffset);
            signature.setMetadataOption('untangle_action', Ext.util.Format.date(date, "Y_m_d"));
        }else{
            signature.setOption(updatedKey, updatedValue);
        }
        record.set('signature', signature.build());
    },


});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unintrusionvariablesrecordeditor',

    controller: 'unintrusionvariablesrecordeditorcontroller',
});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unintrusionvariablesrecordeditorcontroller',

    editorVariableChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView();
        var vm = this.getViewModel();

        var match = false;
        vm.get('variables').each( function( storeRecord ) {
            if( ( storeRecord !== v.record ) && ( newValue == storeRecord.get('variable') ) ){
                match = true;
            }
        });
        if(match){
            me.setValidation("Variable name already in use.".t());
            return false;

        }
        me.setValidation(true);

        var activeVariable = v.up('app-intrusion-prevention').getController().isVariableUsed(newValue);
        me.setReadOnly(activeVariable);
        me.up("").down("[name=activeVariable]").setVisible(activeVariable);
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unintrusionvariablesgrid',

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.unintrusionvariablesrecordeditor',
            record: record
        });
        this.dialog.show();
    },

    exportData: function(){
        var grid = this.getView(),
            gridName = (grid.name !== null) ? grid.name : grid.recordJavaClass,
            vm = grid.up('app-intrusion-prevention').getController().getViewModel();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value = "IntrusionPreventionSettings";
        downloadForm["arg1"].value = "export";
        downloadForm["arg2"].value = vm.get('instance.id');
        downloadForm["arg3"].value = gridName.trim().replace(/ /g, '_');
        downloadForm["arg4"].value = Ext.encode({variables: grid.up('app-intrusion-prevention').getController().getChangedDataRecords('variables')});
        downloadForm.submit();

        Ext.MessageBox.hide();
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        if( this.getView().up('app-intrusion-prevention').getController().isVariableUsed( record.get('variable') ) ){
            Ext.MessageBox.alert( "Cannot Delete Variable".t(), "Variable is used by one or more signatures.".t() );
        }else{
            if (record.get('markedForNew')) {
                record.drop();
            } else {
                record.set('markedForDelete', true);
            }
        }
    }
});

Ext.define('Ung.model.intrusionprevention.signature',{
    extend: 'Ext.data.Model',
    fields:[{
        name: 'valid',
        type: 'boolean'
    },{
        name: 'enabled',
        type: 'boolean'
    },{
        name: 'action',
        type: 'string',
        defaultValue: 'alert'
    },{
        name: 'protocol',
        type: 'string'
    },{
        name: 'lnet',
        type: 'string'
    },{
        name: 'lport',
        type: 'string'
    },{
        name: 'direction',
        type: 'string'
    },{
        name: 'rnet',
        type: 'string'
    },{
        name: 'rport',
        type: 'string'
    },{
        name: 'options'
    },{
        name: 'optionsMetadata'
    }],

    signatureRegexMatch: /^([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+((tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|<>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)/,
    constructor: function(signature, session){
        var data = {
            valid: true,
            enabled: false,
            action: 'alert',
            protocol: '',
            lnet: '',
            lport: '',
            direction: '',
            rnet: '',
            rport: '',
            options: [],
            metadataOptions: []
        };

        if(!this.signatureRegexMatch.test(signature)){
            data['valid'] = false;
        }else{
            var matches = this.signatureRegexMatch.exec(signature);

            data['enabled'] = matches[1] == '#' ? false : true;
            data['action'] = matches[2].toLowerCase();
            if(matches[3] != ""){
                data['protocol'] = matches[4].toLowerCase();
                data['lnet'] = matches[5];
                data['lport'] = matches[6];
                data['direction'] = matches[7];
                data['rnet'] = matches[8];
                data['rport'] = matches[9];
            }
            data['options'] = matches[10].trim().split(';');
            data['options'].forEach( function(option, index, options){
                options[index] = option.trim();
                var kv = option.trim().split(':');
                if(kv[0] == 'metadata'){
                    data['optionsMetadata'] = kv[1].trim().split(',');
                    data['optionsMetadata'].forEach( function( moption, mindex, moptions){
                        moptions[mindex] = moption.trim();
                    });
                }
            });
        }

        this.callParent([data], session);
    },

    getOption: function(key){
        value = null;

        var options = this.get('options');
        var kv;
        options.forEach( function( option, index, optionsMetadata){
            kv = option.split(':');
            if(kv[0].trim() == key){
                kv[1] = kv[1].trim();
                if(kv[1][0] == '"' && kv[1][kv[1].length -1] == '"'){
                    kv[1] = kv[1].substring(1,kv[1].length - 1);
                }
                value = kv[1];
            }
        });
        return value;
    },

    setOption: function( key, value){
        var options = this.get('options');
        var found = false;
        var kv;
        options.forEach( function( option, index, optionsMetadata){
            kv = option.split(':');
            if(kv[0].trim() == key){
                kv[1] = kv[1].trim();
                var preserveQuotes = false;
                if(kv[1][0] == '"' && kv[1][kv[1].length -1] == '"'){
                    preserveQuotes = true;
                }
                kv[1] = (preserveQuotes ? '"' : '' ) + value + (preserveQuotes ? '"' : '' );
                found = true;
                options[index] = kv.join(': ');
            }
        });
        if(!found){
            options.push(key + ': ' + value);
        }
        this.set('options', options);
    },

    setMetadataOption: function( key, value){
        var optionsMetadata = this.get('optionsMetadata');
        if(optionsMetadata === undefined){
            optionsMetadata = [];
        }
        var found = false;
        var kv;
        optionsMetadata.forEach( function( option, index, optionsMetadata){
            kv = option.split(' ');
            if(kv[0].trim() == key){
                kv[1] = value;
                found = true;
                optionsMetadata[index] = kv.join(' ');
            }
        });
        if(!found){
            optionsMetadata.push(key + ' ' + value);
        }
        this.set('optionsMetadata', optionsMetadata);
    },

    build: function(){

        var options = this.get('options');
        var metadataOptions = this.get('optionsMetadata');
        if(metadataOptions){
            metadataOptions = metadataOptions.join(', ');
            var metadataFound = false;
            options.forEach( function(option, index, options){
                var kv = option.split(':');
                if(kv[0].trim() == 'metadata'){
                    kv[1] = metadataOptions;
                    options[index] = kv.join(': ');
                    metadataFound = true;
                }
            });
            if(!metadataFound){
                options.push('metadata: ' + metadataOptions);
            }
        }
        options = options.join('; ');

        return (this.get('enabled')  ? '' : '#') +
            this.get('action') + " " +
            (this.get('protocol') ? this.get('protocol') + ' ' : '') +
            (this.get('lnet') ? this.get('lnet') + ' ' : '') +
            (this.get('lport') ? this.get('lport') + ' ' : '') +
            (this.get('direction') ? this.get('direction') + ' ' : '') +
            (this.get('rnet') ? this.get('rnet') + ' ' : '') +
            (this.get('rport') ? this.get('rport') + ' ' : '') +
            "( " + options + " )";
    }
});

Ext.define ('Ung.apps.intrusionprevention.model.Condition', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'javaClass', type: 'string', defaultValue: ''},
        { name: 'type', type: 'string' },
        { name: 'comparator', type: 'string', defaultValue: '='},
        { name: 'value', type: 'auto', defaultValue: '' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
