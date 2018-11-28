var exec = require('cordova/exec');

exports.invokeStartDetect = function (success, error) {
    exec(success, error, 'KantarSyncNow', 'invokeStartDetect',[]);
};
