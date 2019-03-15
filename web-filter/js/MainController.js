Ext.define('Ung.apps.webfilter.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-web-filter',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
        '#site-lookup': {
            afterrender: 'initSiteLookup'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();

            var filters = store.getFilters().clone();
            store.clearFilter(true);

            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                }, this, true);
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
            filters.each( function(filter){
                store.addFilter(filter);
            });
        });

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    clearHostCache: function () {
        var v = this.getView();
        Ext.MessageBox.wait('Clearing Category URL Cache...'.t(), 'Please Wait'.t());

        Rpc.asyncData(v.appManager, 'clearCache', true)
        .then(function(result){
            Ext.MessageBox.hide();
            Util.successToast('The Category URL Cache was cleared succesfully.'.t());
        }, function(ex) {
            Util.handleException(ex);
            Ext.MessageBox.hide();
        });
    },

    initSiteLookup: function() {
        var vm = this.getViewModel();
        vm.set('siteLookupInput', '');
        vm.set('siteLookupAddress', '');
        vm.set('siteLookupCategory', '');
        vm.set('siteLookupCheckbox', false);
        vm.set('siteLookupSuggest', '');
    },

    handleSiteLookup: function() {
        var v = this.getView(), vm = this.getViewModel();

        var inputField = v.down("[fieldIndex='siteLookupInput']");

        if (inputField.getValue().length == 0) {
            Ext.MessageBox.alert('Site URL is empty'.t() , 'You must enter the Site URL for category search.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'lookupSite', inputField.getValue())
        .then(function(result){
            if(Util.isDestroyed(v, vm, inputField)){
                return;
            }
            v.setLoading(false);

            var showAddress = v.down("[fieldIndex='siteLookupAddress']");
            var showCategory = v.down("[fieldIndex='siteLookupCategory']");
            var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");
            var masterList = vm.get('settings.categories.list');
            showAddress.setValue(inputField.getValue());
            inputField.setValue('');

            var categoryList = [];
            var firstSuggest = null;
            for(var i = 0 ; i < result.list.length ; i++) {
                for(var j = 0 ; j < masterList.length ; j++) {
                    var category = masterList[j];
                    if (result.list[i] == category.string) {
                        categoryList.push(category.name);
                        if (firstSuggest == null) {
                            firstSuggest = category.string;
                            suggestBox.setValue(firstSuggest);
                        }
                    }
                }
            }
            showCategory.setValue(categoryList.join(","));
        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
            }
            Util.handleException(ex);
        });
    },

    handleCategorySuggest: function() {
        var v = this.getView(), vm = this.getViewModel();

        var showAddress = v.down("[fieldIndex='siteLookupAddress']");
        var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");

        if (showAddress.getValue().length == 0) {
            Ext.MessageBox.alert('Last Search URL is empty'.t() , 'You must search for a Site URL before you can suggest a different category.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'recategorizeSite', showAddress.getValue(), suggestBox.getValue())
        .then(function(result){
            if(Util.isDestroyed(v, vm, showAddress, suggestBox)){
                return;
            }
            v.setLoading(false);

            var showCategory = v.down("[fieldIndex='siteLookupCategory']");
            var checkBox = v.down("[fieldIndex='siteLookupCheckbox']");

            if (result == suggestBox.getValue()) {
                Ext.MessageBox.alert('Suggestion Submitted'.t(), showAddress.getValue() + ' - ' + vm.get('categories').findRecord('string', result).get('name'));
            } else {
                Ext.MessageBox.alert('Unable to submit suggestion.'.t(), 'Please try again later.'.t());
            }

            showAddress.setValue('');
            showCategory.setValue('');
            suggestBox.setValue('');
            checkBox.setValue(false);

        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
            }
            Util.handleException(ex);
        });
    }

});
