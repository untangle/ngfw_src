Ext.define('Ung.overrides.container.Container', {
    override: 'Ext.container.Container',

    /**
     * @protected
     * Used by {@link Ext.ComponentQuery ComponentQuery}, {@link #child} and {@link #down} to retrieve all of the items
     * which can potentially be considered a child of this Container.
     *
     * This may be overriden by Components which have ownership of Components
     * that are not contained in the {@link #property-items} collection.
     *
     * NOTE: IMPORTANT note for maintainers:
     * Items are returned in tree traversal order. Each item is appended to the result array
     * followed by the results of that child's getRefItems call.
     * Floating child items are appended after internal child items.
     */
    getRefItems: function(deep) {
        var me = this,
            items = me.items.items,
            len = items.length,
            i = 0,
            item,
            result = [];
        for (; i < len; i++) {
            item = items[i];
            result[result.length] = item;
            if (deep && item.getRefItems && item.items) {
                result.push.apply(result, item.getRefItems(true));
            }
        }
        // Append floating items to the list.
        if (me.floatingItems) {
            items = me.floatingItems.items;
            len = items.length;
            for (i = 0; i < len; i++) {
                item = items[i];
                result[result.length] = item;
                if (deep && item.getRefItems) {
                    result.push.apply(result, item.getRefItems(true));
                }
            }
        }
        return result;
    },

});