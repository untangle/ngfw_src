Ext.define('Ung.apps.webfilter.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-web-filter',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
        '#sitelookup': {
            afterrender: 'initSiteLookup'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set('settings', result);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    clearHostCache: function () {
        Ext.MessageBox.wait('Clearing Host Cache...'.t(), 'Please Wait'.t());
        this.getView().appManager.clearCache(function (result, ex) {
            Ext.MessageBox.hide();
            if (ex) { Util.handleException('There was an error clearing the host cache, please try again.'.t()); return; }
            Util.successToast('The Host Cache was cleared succesfully.'.t());
        }, true);
    },

    initSiteLookup: function() {
        var v = this.getView(), vm = this.getViewModel();
        vm.set('siteLookupInput', '');
        vm.set('siteLookupAddress', '');
        vm.set('siteLookpuCategory', '');
        vm.set('siteLookupCheckbox', false);
        vm.set('siteLookupSuggest', '');
    },

    handleSiteLookup: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        var inputField = v.down("[fieldIndex='siteLookupInput']");
        var showAddress = v.down("[fieldIndex='siteLookupAddress']");
        var showCategory = v.down("[fieldIndex='siteLookupCategory']");
        var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");
        var masterList = vm.get('settings.categories.list');

        if (inputField.getValue().length == 0) {
            Ext.MessageBox.alert('Site URL is empty'.t() , 'You must enter the Site URL for category search.'.t());
            return;
        }

        v.setLoading(true);

        v.appManager.lookupSite(Ext.bind(function(result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            showAddress.setValue(inputField.getValue());
            inputField.setValue('');

            var categoryList = [];
            var firstSuggest = null;
            for(var i = 0 ; i < result.list.length ; i++) {
                for(j = 0 ; j < masterList.length ; j++) {
                    category = masterList[j];
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

        }, this), inputField.getValue());
    },

    handleCategorySuggest: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        var showAddress = v.down("[fieldIndex='siteLookupAddress']");
        var showCategory = v.down("[fieldIndex='siteLookupCategory']");
        var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");
        var checkBox = v.down("[fieldIndex='siteLookupCheckbox']");

        if (showAddress.getValue().length == 0) {
            Ext.MessageBox.alert('Last Search URL is empty'.t() , 'You must search for a Site URL before you can suggest a different category.'.t());
            return;
        }

        v.setLoading(true);

        v.appManager.recategorizeSite(Ext.bind(function(result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }

            if (result == suggestBox.getValue()) {
                Ext.MessageBox.alert('Suggestion Submitted'.t(), showAddress.getValue());
            } else {
                Ext.MessageBox.alert('Unable to submit suggestion.'.t(), 'Please try again later.'.t());
            }

            showAddress.setValue('');
            showCategory.setValue('');
            suggestBox.setValue('');
            checkBox.setValue(false);

        }, this), showAddress.getValue(), suggestBox.getValue());
    }

});
