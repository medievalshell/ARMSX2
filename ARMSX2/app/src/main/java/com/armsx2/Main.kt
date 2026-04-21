package com.armsx2

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.armsx2.events.TestResult
import com.armsx2.ui.Colors
import com.armsx2.ui.LauncherScreen
import com.armsx2.ui.WindowImpl
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.libsdl.app.HIDDeviceManager
import kr.co.iefriends.pcsx2.MainActivity
import kr.co.iefriends.pcsx2.NativeApp
import org.libsdl.app.SDLControllerManager
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.min

class SurfaceCallbacks(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
    }
    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        NativeApp.onNativeSurfaceChanged(holder.surface, width, height)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        NativeApp.onNativeSurfaceChanged(null, 0, 0)
    }

}

private const val STICK_DEAD = 0.15f

val codeGenTests = mutableStateOf("")
val patchTests = mutableStateOf("")
val vuJitTests = mutableStateOf("")
val eeJitTests = mutableStateOf("")
val vifTests = mutableStateOf("")
val eeSeqTests = mutableStateOf("")

class Main: ComponentActivity() {
    companion object {
        var instance : Main? = null
        private val eDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        private val eScope = CoroutineScope(eDispatcher)

        val surface = mutableStateOf<SurfaceView?>(null)

        @JvmField
        val eState = mutableStateOf(EmuState.STOPPED)

        val focusRequester = FocusRequester()

        private var m_szGamefile = ""

        fun onTestResults(result: TestResult) {
            when (result.name) {
                "VuJitTests" -> vuJitTests.value = "${result.passed}/${result.total}"
                "PatchTests" -> patchTests.value = "${result.passed}/${result.total}"
                "CodegenTests" -> codeGenTests.value = "${result.passed}/${result.total}"
                "EeJitTests" -> eeJitTests.value = "${result.passed}/${result.total}"
                "VifTests" -> vifTests.value = "${result.passed}/${result.total}"
                "EeSeqTests" -> eeSeqTests.value = "${result.passed}/${result.total}"
                else -> println("Test:${result.name}: ${result.passed}/${result.total}")
            }
        }

        fun invoke(task: suspend () -> Unit) {
            eScope.launch {
                task()
            }
        }

        fun start() {
            invoke {
                eState.value = EmuState.RUNNING
                NativeApp.runVMThread(m_szGamefile)
            }
            WindowImpl.toolbarVisible.value = false
        }

        fun startWithGamePath(path: String) {
            if (path.isEmpty()) return
            m_szGamefile = path
            start()
        }

        fun pause() {
            NativeApp.pause()
            eState.value = EmuState.PAUSED
        }

        fun resume() {
            NativeApp.resume()
            eState.value = EmuState.RUNNING
        }

        fun stop() {
            NativeApp.shutdown()
            eState.value = EmuState.STOPPED
        }

        fun restart() {
            stop()
            start()
        }

        fun renderOpenGL() {
            NativeApp.renderOpenGL()
        }

        fun renderVulkan() {
            NativeApp.renderVulkan()
        }

        fun renderSoftware() {
            NativeApp.renderSoftware()
        }

        fun copyAssetAll(p_context: Context, srcPath: String) {
            val assetMgr = p_context.assets
            try {
                val destPath =
                    p_context.getExternalFilesDir(null).toString() + File.separator + srcPath
                assetMgr.list(srcPath)?.let {
                    if (it.isEmpty()) {
                        MainActivity.copyFile(p_context, srcPath, destPath)
                    } else {
                        val dir = File(destPath)
                        if (!dir.exists()) dir.mkdir()
                        for (element in it) {
                            copyAssetAll(p_context, srcPath + File.separator + element)
                        }
                    }
                }
            } catch (ignored: IOException) {
            }
        }

        fun getSupportedGLESVersion(context: Context): Double {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = am.deviceConfigurationInfo
            return info.glEsVersion.toDouble()
        }

        fun isAndroidEmulator(): Boolean {
            return Build.MODEL.startsWith("sdk_")
        }
    }

    val openFileAction = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            try {
                val intent = result.data
                if (intent != null) {
                    m_szGamefile = intent.dataString ?: ""
                    if (m_szGamefile.isNotEmpty()) {
                        println(m_szGamefile)
                        restart()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    init {
        instance = this
    }

    var tested = false

    fun sendKeyAction(p_action: KeyEventType, p_keycode: Int) {
        if (p_action == KeyEventType.KeyDown) {
            var pad_force = 0
            if (p_keycode >= 110) {
                var _abs = 90f // Joystic test value
                _abs = min(_abs, 100f)
                pad_force = (_abs * 32766.0f / 100).toInt()
            }
            NativeApp.setPadButton(p_keycode, pad_force, true)
        } else if (p_action == KeyEventType.KeyUp || p_action == KeyEventType.Unknown) {
            NativeApp.setPadButton(p_keycode, 0, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Launcher.init(this)
        surface.value = SurfaceCallbacks(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

       // Default resources
        copyAssetAll(applicationContext, "bios");
        copyAssetAll(applicationContext, "resources");

        invoke {
            NativeApp.initializeOnce(applicationContext)

            // Set up JNI
            SDLControllerManager.nativeSetupJNI()

            // Initialize state
            SDLControllerManager.initialize()

            HIDDeviceManager(applicationContext)

            println("PCSX2_INIT")

            // Tests that need VTLB/eeMem — run after init
            NativeApp.runEeJitTests()
            NativeApp.runEeSeqTests()
            NativeApp.runVifTests()
        }

        val glVersion = getSupportedGLESVersion(this)

        if (glVersion < 3.1) {
            eState.value = EmuState.RENDER_UNSUPPORTED
            println("RENDER_UNSUPPORTED")
        }

        if (isAndroidEmulator()) {
            eState.value = EmuState.EMULATOR_UNSUPPORTED
            println("DEVICE_UNSUPPORTED")
        }


        setContent {
            WindowImpl.Window {
                if (!tested) {
                    NativeApp.runCodegenTests()
                    NativeApp.runPatchTests()
                    NativeApp.runVuJitTests()
                    tested = true
                }
                if (surface.value != null) {
                    AndroidView(factory = { surface.value!! }, modifier = Modifier
                        .focusable()
                        .focusRequester(focusRequester)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                WindowImpl.toolbarVisible.value = !WindowImpl.toolbarVisible.value
                            })
                        }
                        .onKeyEvent { event ->
                            if (eState.value != EmuState.RUNNING)
                                return@onKeyEvent false
                            when (event.key) {
/*                                Key.DirectionUp -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_DPAD_UP)
                                    true
                                }

                                Key.DirectionDown -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_DPAD_DOWN)
                                    true
                                }

                                Key.DirectionLeft -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_DPAD_LEFT)
                                    true
                                }

                                Key.DirectionRight -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_DPAD_RIGHT)
                                    true
                                }*/

                                Key.ButtonA -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_A)
                                    true
                                }

                                Key.ButtonB -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_B)
                                    true
                                }

                                Key.ButtonX -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_X)
                                    true
                                }

                                Key.ButtonY -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_Y)
                                    true
                                }

                                Key.ButtonSelect -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_SELECT)
                                    true
                                }

                                Key.ButtonStart -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_START)
                                    true
                                }

                                Key.ButtonL1 -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_L1)
                                    true
                                }

                                Key.ButtonR1 -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_R1)
                                    true
                                }

                                Key.ButtonL2 -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_L2)
                                    true
                                }

                                Key.ButtonR2 -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_R2)
                                    true
                                }

                                Key.ButtonThumbLeft -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_THUMBL)
                                    true
                                }

                                Key.ButtonThumbRight -> {
                                    sendKeyAction(event.type, KeyEvent.KEYCODE_BUTTON_THUMBR)
                                    true
                                }

                                else -> false
                            }
                        })
                }

                if (eState.value == EmuState.STOPPED || eState.value == EmuState.RENDER_UNSUPPORTED || eState.value == EmuState.EMULATOR_UNSUPPORTED) {
                    Box(Modifier
                        .fillMaxSize()
                        .background(Colors.surface.value)) {
                        if (eState.value == EmuState.EMULATOR_UNSUPPORTED) {
                            Box(Modifier.align(Alignment.Center)) {
                                Column {
                                    Image(LineAwesomeIcons.Android, "",
                                        colorFilter = ColorFilter.tint(Colors.pasx2_blue),
                                        modifier = Modifier
                                            .size(150.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        "Android Emulator is not supported", fontSize = 22.sp, color = Colors.pasx2_blue,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        "Please use a physical device", fontSize = 22.sp, color = Colors.pasx2_blue,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        } else if (eState.value == EmuState.RENDER_UNSUPPORTED) {
                            Box(Modifier.align(Alignment.Center)) {
                                Text(
                                    "GLES 3.1+ required",
                                    fontSize = 22.sp, color = Colors.pasx2_blue
                                )
                            }
                        } else {
                            LauncherScreen()
                        }
                    }
                }
/*                if (eState.value == EmuState.RUNNING) {
                    Text("Hello Overlay!", color = Color.Blue)
                }*/
            }
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (eState.value == EmuState.RUNNING) {
            sendAxis(ev, MotionEvent.AXIS_X,     posCode = 111, negCode = 113) // L right / left
            sendAxis(ev, MotionEvent.AXIS_Y,     posCode = 112, negCode = 110) // L down  / up
            sendAxis(ev, MotionEvent.AXIS_Z,     posCode = 121, negCode = 123) // R right / left
            sendAxis(ev, MotionEvent.AXIS_RZ,    posCode = 122, negCode = 120) // R down  / up
            sendAxis(ev, MotionEvent.AXIS_HAT_X, posCode = 22,  negCode = 21)  // D right / left
            sendAxis(ev, MotionEvent.AXIS_HAT_Y, posCode = 20,  negCode = 19)  // D down  / up
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    private fun sendAxis(event: MotionEvent, axis: Int, posCode: Int, negCode: Int) {
        val v = event.getAxisValue(axis)
        val posVal = if (v > STICK_DEAD) v else 0f
        val negVal = if (v < -STICK_DEAD) -v else 0f
        NativeApp.setPadButton(posCode, (posVal * 32767).toInt(), posVal > 0f)
        NativeApp.setPadButton(negCode, (negVal * 32767).toInt(), negVal > 0f)
    }

    override fun onPause() {
        if (eState.value == EmuState.RUNNING)
            NativeApp.pause()
        super.onPause()
    }

    override fun onResume() {
        if (eState.value == EmuState.PAUSED)
            NativeApp.resume()
        super.onResume()
    }

    override fun onDestroy() {
        NativeApp.shutdown()
        super.onDestroy()

        val appPid = Process.myPid()
        Process.killProcess(appPid)
    }
}