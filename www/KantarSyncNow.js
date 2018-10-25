var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'KantarSyncNow', 'coolMethod', [arg0]);
};

exports.startDetection = function (success, error) {
    exec(success, error, 'KantarSyncNow', 'startDetection',[]);
};
