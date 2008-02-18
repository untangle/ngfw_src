I18N= function(map){
	//thousands_sep: ',',
	this.map=map;
}
I18N.prototype = {
	_: function (s) {
		if (typeof(this.map)!== null && this.map[s]) {
			return this.map[s];
		}
		return s;
	},
	
	pluralise: function (s, p, n) {
		if (n != 1) return this._(p);
		return this._(s);
	},
	
	sprintf: function (s) {
		var bits = s.split('%');
		var out = bits[0];
		var re = /^([ds])(.*)$/;
		for (var i=1; i<bits.length; i++) {
			p = re.exec(bits[i]);
			if (!p || arguments[i]==null) continue;
			if (p[1] == 'd') {
				out += parseInt(arguments[i], 10);
			} else if (p[1] == 's') {
				out += arguments[i];
			}
			out += p[2];
		}
		return out;
	}	
};

// TODO move i18n to a Node I18N superclass
I18N_Node = function(globalI18N,nodeMap) {
	this.globalI18N=globalI18N;
	this.nodeMap=nodeMap;
};
I18N_Node.prototype = { 
	// try to find a traslation in this node, then in the main rack translations
	_: function (s) {
		if (typeof(this.nodeMap)!== null && this.nodeMap[s]) {
			return this.nodeMap[s];
		}
		return this.globalI18N._(s);
	},
	
	// TODO: try to reuse the i18n functions
	pluralise:  function (s, p, n) {
		if (n != 1) return this._(p);
		return this._(s);
	},
	
	sprintf: function (s) {
		var bits = s.split('%');
		var out = bits[0];
		var re = /^([ds])(.*)$/;
		for (var i=1; i<bits.length; i++) {
			p = re.exec(bits[i]);
			if (!p || arguments[i]==null) continue;
			if (p[1] == 'd') {
				out += parseInt(arguments[i], 10);
			} else if (p[1] == 's') {
				out += arguments[i];
			}
			out += p[2];
		}
		return out;
	}	
};

