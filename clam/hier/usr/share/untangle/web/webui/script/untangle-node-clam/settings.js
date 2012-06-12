Ung.NodeWin.dependency["untangle-node-clam"] = {
    name : "untangle-base-virus",
    fn : function() {
        if (!Ung.hasResource["Ung.Clam"]) {
            Ung.hasResource["Ung.Clam"] = true;
            Ung.NodeWin.registerClassName('untangle-node-clam', 'Ung.Clam');
			Ext.define('Ung.Clam', {
				extend:'Ung.Virus'
			});
        }
    }
};
//@ sourceURL=clam-settings.js
