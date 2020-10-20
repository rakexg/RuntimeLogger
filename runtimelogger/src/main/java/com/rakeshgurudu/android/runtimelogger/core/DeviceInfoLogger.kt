package com.rakeshgurudu.android.runtimelogger.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.drm.DrmInfoRequest
import android.drm.DrmManagerClient
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaDrm
import android.os.Build
import android.os.Debug
import android.os.StatFs
import android.provider.Settings.Secure
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.io.File
import java.util.*

const val LINE_SEPARATOR = "\n"

class DeviceInfoLogger {

    fun getLogData(context: Context): StringBuilder {
        val builder = StringBuilder()
        builder.append("======== Device and App Info ========").append(LINE_SEPARATOR)
        builder.append(LINE_SEPARATOR)
        //Device detail
        builder.append("Device Manufacturer: ").append(Build.MANUFACTURER).append(LINE_SEPARATOR)
        builder.append("Device             : ").append(Build.DEVICE).append(LINE_SEPARATOR)
        builder.append("Model              : ").append(Build.MODEL).append(LINE_SEPARATOR)
        builder.append("Product            : ").append(Build.PRODUCT).append(LINE_SEPARATOR)

        //OS details
        builder.append("OS version         : ").append(Build.VERSION.RELEASE)
        builder.append(" SDK: ").append("v").append(Build.VERSION.SDK_INT)
        //.append(" ").append(Build.VERSION.INCREMENTAL)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            builder.append(" ").append("Security Patch: ").append(Build.VERSION.SECURITY_PATCH)
        }
        builder.append(LINE_SEPARATOR)
        builder.append("FINGERPRINT        : ").append(Build.FINGERPRINT)
        builder.append(LINE_SEPARATOR)

        //Supported ABI's version
        builder.append("ARM ABI version's  : ")
        getARMABIVersion(builder)
        builder.append(LINE_SEPARATOR)

        builder.append("Display            : ").append(Build.DISPLAY).append(LINE_SEPARATOR)
        //context.resources.configuration.orientation
        builder.append("Screen Resolution  : ").append(getScreenResolution(context))
        builder.append(LINE_SEPARATOR)
        builder.append("Screen Density     : ").append(getDisplayDensity(context))
        builder.append(LINE_SEPARATOR)
        builder.append("Screen Refresh Rate: ").append(getDisplayRefreshRate(context))
        builder.append(LINE_SEPARATOR)

        //memory details
        getMemoryUsage(builder)
        builder.append(LINE_SEPARATOR)

        builder.append("App version        : ")
        builder.append(context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        @Suppress("DEPRECATION")
        builder.append(
                " (${context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                ).versionCode})"
        )

        builder.append(LINE_SEPARATOR)
        builder.append("WideVine Info      : ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            builder.append("WideVine Properties")
            builder.append(LINE_SEPARATOR)
            wideVineModularDrmInfo(builder)
        } else {
            builder.append("Info not available. Requires SDK ${Build.VERSION_CODES.JELLY_BEAN_MR2} and above")
        }
        builder.append("Classic DRM info   : ")
        classicDrmInfo(context, builder)

        /* TODO: Media codec to log based on user's need
        builder.append(LINE_SEPARATOR)
        builder.append("Media Codec Info   : ")
        mediaCodecInfo(builder)*/

        builder.append(LINE_SEPARATOR)
        builder.append(LINE_SEPARATOR)
        builder.append("================ END ================")
        builder.append(LINE_SEPARATOR)
        builder.append(LINE_SEPARATOR)
        return builder
    }

    private fun getARMABIVersion(builder: StringBuilder) {
        @Suppress("DEPRECATION") val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            listOf(Build.SUPPORTED_ABIS)
        } else {
            val abis = LinkedList<String?>()
            abis.add(Build.CPU_ABI)
            if (Build.CPU_ABI2 != null && "unknown" != Build.CPU_ABI2) {
                abis.add(Build.CPU_ABI2)
            }
            abis
        }
        val iterator = version.iterator()
        builder.append(iterator.next())
        while (iterator.hasNext()) {
            builder.append(", ")
            builder.append(iterator.next())
        }
    }

    private fun getMemoryUsage(builder: StringBuilder) {
        val info = Runtime.getRuntime()
        val totalMemory = info.totalMemory()
        builder.append("Total Memory GB    : ")
        builder.append(totalMemory / 1024 / 1024)
        builder.append(LINE_SEPARATOR)
        builder.append("Free Memory MB     : ")
        builder.append(info.freeMemory().toFloat() / 1024 / 1024)
        builder.append(LINE_SEPARATOR)
        builder.append("Max Memory         : ")
        builder.append(info.maxMemory() / 1024 / 1024)
    }

    @Suppress("unused")
    @SuppressLint("HardwareIds")
    private fun isEmulator(context: Context): Boolean {
        val androidId =
                Secure.getString(context.contentResolver, "android_id")
        return "sdk" == Build.PRODUCT || "google_sdk" == Build.PRODUCT || androidId == null
    }

    /**
     * Need other solution to find root
     */
    private fun isRooted(context: Context): Boolean {
        val isEmulator: Boolean = isEmulator(context)
        val buildTags = Build.TAGS
        return if (!isEmulator && buildTags != null && buildTags.contains("test-keys")) {
            true
        } else {
            var file = File("/system/app/Superuser.apk")
            if (file.exists()) {
                true
            } else {
                file = File("/system/xbin/su")
                !isEmulator && file.exists()
            }
        }
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    @Suppress("unused")
    private fun getDeviceState(context: Context): Int {
        var deviceState = 0
        if (isEmulator(context)) {
            deviceState = deviceState or 1
        }
        if (isRooted(context)) {
            deviceState = deviceState or 2
        }
        if (isDebuggerAttached()) {
            deviceState = deviceState or 4
        }
        return deviceState
    }

    private fun getScreenResolution(context: Context): String {
        val displayMetrics = DisplayMetrics()
        val windowManager: WindowManager =
                (context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager)
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels.toString() + "x" + displayMetrics.heightPixels
    }

    private fun getDisplayDensity(context: Context): String {
        val density = context.resources.displayMetrics.densityDpi
        val levels: LinkedHashMap<Int, String> = object : LinkedHashMap<Int, String>() {
            init {
                put(DisplayMetrics.DENSITY_LOW, "ldpi")
                put(DisplayMetrics.DENSITY_MEDIUM, "mdpi")
                put(DisplayMetrics.DENSITY_HIGH, "hdpi")
                put(DisplayMetrics.DENSITY_XHIGH, "xhdpi")
                put(DisplayMetrics.DENSITY_XXHIGH, "xxhdpi")
                put(DisplayMetrics.DENSITY_XXXHIGH, "xxxhdpi")
            }
        }
        var densityString = "unknown"
        for ((key, value) in levels) {
            densityString = value
            if (key > density) {
                break
            }
        }
        return "$densityString ($density)"
    }

    private fun getDisplayRefreshRate(context: Context): String {
        return String.format(
                Locale.ENGLISH,
                "%.2f hz",
                (context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
        )
    }

    @Suppress("unused", "DEPRECATION")
    private fun calculateUsedDiskSpaceInBytes(path: String?): Long {
        //path =Environment.getDataDirectory().getPath()
        val statFs = StatFs(path)
        val blockSizeBytes = statFs.blockSize.toLong()
        val totalSpaceBytes = blockSizeBytes * statFs.blockCount.toLong()
        val availableSpaceBytes = blockSizeBytes * statFs.availableBlocks.toLong()
        return totalSpaceBytes - availableSpaceBytes
    }

    @Suppress("unused")
    private fun getBatteryLevel(context: Context): Float? {
        val ifilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
        val battery = context.registerReceiver(null as BroadcastReceiver?, ifilter)
        return if (battery == null) {
            null
        } else {
            val level = battery.getIntExtra("level", -1)
            val scale = battery.getIntExtra("scale", -1)
            level.toFloat() / scale.toFloat()
        }
    }

    @Suppress("unused")
    private fun getAppProcessInfo(
            packageName: String,
            context: Context
    ): RunningAppProcessInfo? {
        val actman = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = actman.runningAppProcesses
        var procInfo: RunningAppProcessInfo? = null
        if (processes != null) {
            val var5: Iterator<*> = processes.iterator()
            while (var5.hasNext()) {
                val info =
                        var5.next() as RunningAppProcessInfo
                if (info.processName == packageName) {
                    procInfo = info
                    break
                }
            }
        }
        return procInfo
    }

    @Suppress("unused")
    private fun isAppDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and 2 != 0
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun wideVineModularDrmInfo(builder: StringBuilder): StringBuilder? {
        @Suppress("LocalVariableName") val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
            return builder.append("MediaDrm crypto scheme not supported WIDEVINE_UUID: ")
                    .append(WIDEVINE_UUID)
        }
        val mediaDrm: MediaDrm
        mediaDrm = try {
            MediaDrm(WIDEVINE_UUID)
        } catch (e: Exception) {
            return builder.append("MediaDrm UnsupportedSchemeException WIDEVINE_UUID: ")
                    .append(WIDEVINE_UUID)
        }

        /*val mediaDrmEvents = JSONArray()
        mediaDrm.setOnEventListener { md: MediaDrm?, sessionId: ByteArray?, event: Int, extra: Int, data: ByteArray? ->
            try {
                val encodedData =
                    if (data == null) null else Base64.encodeToString(
                        data,
                        Base64.NO_WRAP
                    )
                mediaDrmEvents.put(
                    JSONObject().put("event", event).put("extra", extra).put("data", encodedData)
                )
            } catch (e: JSONException) {
                Log.e(com.kaltura.kalturadeviceinfo.Collector.TAG, "JSONError", e)
            }
        }
        try {
            val session: ByteArray
            session = mediaDrm.openSession()
            mediaDrm.closeSession(session)
        } catch (e: Exception) {
            mediaDrmEvents.put(JSONObject().put("Exception(openSession)", e.toString()))
        }*/

        val stringProps = arrayOf(
                MediaDrm.PROPERTY_VENDOR,
                MediaDrm.PROPERTY_VERSION,
                MediaDrm.PROPERTY_DESCRIPTION,
                MediaDrm.PROPERTY_ALGORITHMS,
                "securityLevel",
                "systemId",
                "privacyMode",
                "sessionSharing",
                "usageReportingSupport",
                "appId",
                "origin",
                "hdcpLevel",
                "maxHdcpLevel",
                "maxNumberOfSessions",
                "numberOfOpenSessions"
        )
        for (prop in stringProps) {
            val value: String = try {
                mediaDrm.getPropertyString(prop)
            } catch (e: IllegalStateException) {
                "<unknown>"
            }
            builder.append("                   : $prop").append(" - ").append(value)
            builder.append(LINE_SEPARATOR)
        }
        val byteArrayProps = arrayOf(
                MediaDrm.PROPERTY_DEVICE_UNIQUE_ID,
                "provisioningUniqueId",
                "serviceCertificate"
        )
        for (prop in byteArrayProps) {
            val value: String? = try {
                Base64.encodeToString(
                        mediaDrm.getPropertyByteArray(prop),
                        Base64.NO_WRAP
                )
            } catch (e: IllegalStateException) {
                "<unknown>"
            } catch (e: NullPointerException) {
                "<unknown>"
            }
            builder.append("                   : $prop").append(" - ").append(value)
            builder.append(LINE_SEPARATOR)
        }


        //response.put("events", mediaDrmEvents);
        return builder
    }

    @Suppress("DEPRECATION")
    private fun classicDrmInfo(mContext: Context, builder: StringBuilder): StringBuilder? {
        val drmManagerClient = DrmManagerClient(mContext)
        val availableDrmEngines = drmManagerClient.availableDrmEngines
        builder.append("engines - ").append(availableDrmEngines.joinToString(":"))
        try {
            if (drmManagerClient.canHandle("", "video/wvm")) {
                val request = DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO, "video/wvm")
                request.put("WVPortalKey", "OEM")
                val response = drmManagerClient.acquireDrmInfo(request)
                var status: String? = response["WVDrmInfoRequestStatusKey"] as String
                status = arrayOf("HD_SD", null, "SD")[status!!.toInt()]
                builder.append("widevine")
                builder.append("version - ")
                builder.append(response["WVDrmInfoRequestVersionKey"])
                builder.append("status - ")
                builder.append(status)
            }
        } catch (e: Exception) {
            builder.append("error")
            builder.append(e.message)
        }
        drmManagerClient.release()
        return builder
    }

    @Suppress("unused", "DEPRECATION")
    private fun mediaCodecInfo(builder: StringBuilder): StringBuilder? {
        val mediaCodecs = ArrayList<MediaCodecInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val codecInfos = mediaCodecList.codecInfos
            Collections.addAll(mediaCodecs, *codecInfos)
        } else {
            var i = 0
            val n = MediaCodecList.getCodecCount()
            while (i < n) {
                mediaCodecs.add(MediaCodecList.getCodecInfoAt(i))
                i++
            }
        }
        val encoders = ArrayList<MediaCodecInfo>()
        val decoders = ArrayList<MediaCodecInfo>()
        for (mediaCodec in mediaCodecs) {
            if (mediaCodec.isEncoder) {
                encoders.add(mediaCodec)
            } else {
                decoders.add(mediaCodec)
            }
        }
        builder.append("Decoders")
        for (mediaCodec in decoders) {
            builder.append(LINE_SEPARATOR)
            builder.append("                   : ")
            builder.append(mediaCodec.name)
            builder.append(" :: ")
            mediaCodecInfo(mediaCodec, builder)
        }
        return builder
    }

    private fun mediaCodecInfo(mediaCodec: MediaCodecInfo, builder: StringBuilder): StringBuilder? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.append(" isVendor: ")
            builder.append(mediaCodec.isVendor)
            builder.append(", isSoftwareOnly: ")
            builder.append(mediaCodec.isSoftwareOnly)
            builder.append(", isHardwareAccelerated: ")
            builder.append(mediaCodec.isHardwareAccelerated)
        }
        builder.append(", supportedTypes")
        builder.append(" - ")
        builder.append(mediaCodec.supportedTypes.joinToString(":"))
        return builder
    }

    /*fun getAppIconHashOrNull(context: Context): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.openRawResource(getAppIconResourceId(context))
            val sha1: String = sha1(inputStream)
            return if (sha1.isNullOrEmpty()) null else sha1
        } catch (var7: Exception) {
            Log.w("Fabric", "Could not calculate hash for app icon:" + var7.message)
        } finally {
            closeOrLog(
                inputStream,
                "Failed to close icon input stream."
            )
        }
        return null
    }

    fun getAppIconResourceId(context: Context): Int {
        return context.applicationContext.applicationInfo.icon
    }*/

    //CrashlyticsController:writeSessionEvent 858
    @Suppress("unused")
    fun getAllThreadStackTrace() {

        /*
        * List<StackTraceElement[]> stacks = new LinkedList();
        StackTraceElement[] exceptionStack = trimmedEx.stacktrace;
        String buildId = this.appData.buildId;
        String appIdentifier = this.idManager.getAppIdentifier();
        Thread[] threads;
        if (includeAllThreads) {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            threads = new Thread[allStackTraces.size()];
            int i = 0;

            for(Iterator var27 = allStackTraces.entrySet().iterator(); var27.hasNext(); ++i) {
                Entry<Thread, StackTraceElement[]> entry = (Entry)var27.next();
                threads[i] = (Thread)entry.getKey();
                stacks.add(this.stackTraceTrimmingStrategy.getTrimmedStackTrace((StackTraceElement[])entry.getValue()));
            }
        } else {
            threads = new Thread[0];
        }
        * */
        /*val stacks: MutableList<Array<StackTraceElement>?> = LinkedList<Any?>()
        val exceptionStack: Array<StackTraceElement> = trimmedEx.stacktrace
        val buildId: String = this.appData.buildId
        val appIdentifier: String = this.idManager.getAppIdentifier()
        val threads: Array<Thread?>
        if (includeAllThreads) {
            val allStackTraces =
                Thread.getAllStackTraces()
            threads = arrayOfNulls(allStackTraces.size)
            var i = 0
            val var27: Iterator<*> = allStackTraces.entries.iterator()
            while (var27.hasNext()) {
                val entry: Map.Entry<Thread, Array<StackTraceElement?>> =
                    var27.next() as Map.Entry<*, *>
                threads[i] = entry.key
                stacks.add(this.stackTraceTrimmingStrategy.getTrimmedStackTrace(entry.value as Array<StackTraceElement?>))
                ++i
            }
        } else {
            threads = arrayOfNulls(0)
        }*/
    }

}