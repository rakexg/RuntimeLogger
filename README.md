# RuntimeLogger [ ![Download](https://api.bintray.com/packages/rakex/RuntimeLogger/runtimelogger/images/download.svg) ](https://bintray.com/rakex/RuntimeLogger/runtimelogger/_latestVersion)


RuntimeLogger is a library that enables you to get "logcat" logs from your app without the need of connecting the device to android studio. It executes the `logcat` command and saves the logs in a file. A helper activity is placed next to your launcher activity from which you can share the saved log files.

Check the demo code for an example of how it can be used.

# Setup

1. Add the main library dependency for debug build:  
`debugImplementation 'com.rakeshgurudu.android:runtimelogger:1.3.0'`  
Add the no-op version of the library for release builds. This will not add the helper activity and will not save logs for your release builds:  
`releaseImplementation 'com.rakeshgurudu.android:runtimelogger-no-op:1.3.0'`

2. Add `manifestPlaceholders` in `defaultConfig` that will be used to add a prefix to the name of the launcher activity.  
For eg. if your launcher activity name is "My App" the helper activity name will be "My App Runtime Logger". This keeps the helper activity next to your launcher activity for easy access.  
`manifestPlaceholders = [runtimeLoggerLauncherPrefix: "My App"]`
3. To start logging call the `startLogging` method passing a context:  
`RuntimeLogger.startLogging(context)`  
Saves the logs from the point when you call startlogging. It adds a notification from which you can start and stop saving logs. To automatically start saving logs turn on the option from settings of the helper activity. 
4. Call `endLogging` to stop saving logs. You can also manually stop by clicking **Stop** from the added notification:  
`RuntimeLogger.endLogging(context)`
5. It is recommended to increase the device log buffer size from device settings as shown below:  
`Settings -> Developer Options -> Logger Buffer sizes, select 4M or 16M`.  
