var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

function showDir(dir)
{
    var objDir = document.getElementById(dir).style;

    if (objDir.display == "block") {
        objDir.display = "none";
    } else {
        objDir.display = "block";
    }
    swapFolder('I' + dir);
}

function swapFolder(img)
{
    objImg = document.getElementById(img);

    if (objImg.src.indexOf('closed.gif') > -1) {
        objImg.src = openImg.src;
    } else {
        objImg.src = closedImg.src;
    }
}
