# kantar-sync-now

cordova plugin to provide access to the functionality provided by the Kantar audio watermarking SDK 

### start detection

```javascript

cordova.plugins.KantarSyncNow.invokeStartDetect( 
function(status) {
	console.log('sucess' + status);
}, function(err) {
	console.log(err);
});

```
