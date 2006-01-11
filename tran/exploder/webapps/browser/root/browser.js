var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

function showDir(dir)
{
    var dirElem = $(dir);

    var dirStyle = $(dir).style;
    if (dirElem.display == "block") {
        dirElem.display = "none";
    } else {
        dirElem.display = "block";
    }
    swapFolder('I' + dir);
}

function swapFolder(img)
{
    objImg = $(img);

    if (objImg.src.indexOf('closed.gif') > -1) {
        objImg.src = openImg.src;
    } else {
        objImg.src = closedImg.src;
    }
}
