Ext.define('Ung.overrides.data.Store', {
    override: 'Ext.data.Store',

    loadRecords: function(records, options) {
        var me     = this,
            length = records.length,
            data   = me.getData(),
            addRecords, i, skipSort;

        if (options) {
            addRecords = options.addRecords;
        }

        if (!me.getRemoteSort() && !me.getSortOnLoad()) {
            skipSort = true;
            data.setAutoSort(false);
        }

        if (!addRecords) {
            me.clearData(true);
        }

        // Clear the flag AFTER the stores collection has been cleared down so that
        // observers of that collection know that it was due to a load, and a refresh is imminent.
        me.loading = false;

        me.ignoreCollectionAdd = true;
        me.callObservers('BeforePopulate');
        data.add(records);
        me.ignoreCollectionAdd = false;

        if (skipSort) {
            data.setAutoSort(true);
        }

        for (i = 0; i < length; i++) {
            records[i].join(me);
        }

        ++me.loadCount;
        me.complete = true;

        if (me.hasListeners.datachanged) {
            me.fireEvent('datachanged', me);
        }

        if (me.hasListeners.refresh) {
            me.fireEvent('refresh', me);
        }

        me.callObservers('AfterPopulate');

        me.commitChanges();
    }
});
