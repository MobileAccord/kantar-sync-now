module.exports = function(context) {
    var fs = context.requireCordovaModule('fs')
    var path = context.requireCordovaModule('path')
    var Q = context.requireCordovaModule('q')
    var xml = context.requireCordovaModule('cordova-common').xmlHelpers

    var deferred = Q.defer()

    var platformRoot = path.join(context.opts.projectRoot, './platforms/android')

    var filepaths = [
        path.join(platformRoot, './settings.gradle'),
        path.join(platformRoot, './settings.gradle'),
    ]

    var filepath = filepaths.find(function(filepath) {
        try {
            fs.accessSync(filepath, fs.constants.F_OK)
            return true
        } catch (err) {
            return false
        }
    })
	
	/**
	 * readTextFile read data from file
	 * @param  string   path   Path to file on hard drive
	 * @return string              String with file data
	 */
	var readTextFile = function readTextFile(path) {
		var str = "";
		var txtFile = new File(path);
		txtFile.open("r");
		while (!txtFile.eof) {
			// read each line of text
			str += txtFile.readln() + "\n";
		}
		return str;
	};
	
	var srcContents;

    if (filepath != null) {
		srcContents = readTextFile(filepath) + '\n include ":libSyncNowDetector"';
        fs.writeFileSync(filepath, srcContents)
        deferred.resolve()
    } else {
        deferred.reject(new Error("Can't find AndroidManifest.xml"))
    }

    return deferred.promise
}