Ext.define('Ung.cmp.GridStatus', {
    extend: 'Ext.toolbar.TextItem',
    alias: 'widget.ungridstatus',

    controller: 'ungridstatus',

    tplFiltered: '{0} matched, {1} total entries'.t(),
    tplUnfiltered: '{0} entries'.t(),
                
    listeners: {
        update: 'onUpdateGridStatus'
    }

});
