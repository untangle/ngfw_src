
var PageName = 'Platform-Server';
var PageId = 'pd9002080bf544f5581cfd9b30fae5c9d'
document.title = 'Platform-Server';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u16 = document.getElementById('u16');

var u7 = document.getElementById('u7');

var u28 = document.getElementById('u28');

var u30 = document.getElementById('u30');
gv_vAlignTable['u30'] = 'top';
var u15 = document.getElementById('u15');
gv_vAlignTable['u15'] = 'top';
var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'top';
var u19 = document.getElementById('u19');

var u13 = document.getElementById('u13');

var u22 = document.getElementById('u22');
gv_vAlignTable['u22'] = 'top';
var u12 = document.getElementById('u12');
gv_vAlignTable['u12'] = 'top';
var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u8 = document.getElementById('u8');

var u10 = document.getElementById('u10');
gv_vAlignTable['u10'] = 'top';
var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u26 = document.getElementById('u26');

var u25 = document.getElementById('u25');

var u21 = document.getElementById('u21');
gv_vAlignTable['u21'] = 'top';
var u17 = document.getElementById('u17');
gv_vAlignTable['u17'] = 'top';
var u3 = document.getElementById('u3');

var u29 = document.getElementById('u29');

var u23 = document.getElementById('u23');

var u14 = document.getElementById('u14');
gv_vAlignTable['u14'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u9 = document.getElementById('u9');
gv_vAlignTable['u9'] = 'top';
var u20 = document.getElementById('u20');

var u1 = document.getElementById('u1');

var u11 = document.getElementById('u11');
gv_vAlignTable['u11'] = 'top';
var u18 = document.getElementById('u18');
gv_vAlignTable['u18'] = 'top';
var u24 = document.getElementById('u24');

var u4 = document.getElementById('u4');

var u27 = document.getElementById('u27');

if (window.OnLoad) OnLoad();
