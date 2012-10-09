/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

/**
 * Make all icons from fallacies.json
 */

function report(dunno, stdout, stderr) {
	console.log("stdout: " + stdout + " stderr:\n" + stderr);
}

/* prereqs */
var cp = require("child_process");
var fs = require("fs");

var raw = fs.readFileSync("../res/raw/fallacies.json");
var fal = JSON.parse(raw);

var text;
var filename;

for(var i = 0; i < fal.fallacies.length; i++) {
	filename = fal.fallacies[i].icon + ".png";
	text = '"' + fal.fallacies[i].acronym.replace('/', '\\n').replace('$$$$', '\\$\\$\\$\\$') + '"';
	console.log("LOG: " + filename + ' : ' + text );
	cp.exec("./makeIcon.sh " + text + ' ' + filename, report);
}

/* Generate placeholders */

cp.exec("./makeIcon.sh \" \" empty.png", report);
cp.exec("./makeIcon.sh \"+\" add_mru.png", report);

/* rsync --verbose --recursive res/* ../res */