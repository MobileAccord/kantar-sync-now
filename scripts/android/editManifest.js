module.exports = function(context) {
    var fs = context.requireCordovaModule('fs')
    var path = context.requireCordovaModule('path')
    var Q = context.requireCordovaModule('q')
    var xml = context.requireCordovaModule('cordova-common').xmlHelpers

    var deferred = Q.defer()

    var platformRoot = path.join(context.opts.projectRoot, './platforms/android')

    var filepaths = [
        path.join(platformRoot, './AndroidManifest.xml'),
        path.join(platformRoot, './app/src/main/AndroidManifest.xml'),
    ]

    var filepath = filepaths.find(function(filepath) {
        try {
            fs.accessSync(filepath, fs.constants.F_OK)
            return true
        } catch (err) {
            return false
        }
    })

    var doc

    if (filepath != null) {
        doc = xml.parseElementtreeSync(filepath)
        doc.getroot().find('./application').attrib['tools:replace'] =
            'android:allowBackup'
        fs.writeFileSync(filepath, doc.write({ indent: 4 }))
        deferred.resolve()
    } else {
        deferred.reject(new Error("Can't find AndroidManifest.xml"))
    }

    var filepath = filepaths.find(function(filepath) {
        try {
            fs.accessSync(filepath, fs.constants.F_OK)
            return true
        } catch (err) {
            return false
        }
    })
	
	var settingsPath = './platforms/android/settings.gradle';

    if (filepath != null) {
		srcContents = fs.readFile(settingsPath, 'utf8') + '\ninclude ":libSyncNowDetector"';
        fs.writeFileSync(settingsPath, srcContents)
		console.log(srcContents);
		console.log('\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n');
        deferred.resolve()
    } else {
        deferred.reject(new Error("Can't find settings.gradle.xml"))
    }

    return deferred.promise
}
