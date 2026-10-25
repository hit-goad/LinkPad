package com.linkpad.controller

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var hid: HidGamepadService
    private val pad = PadState()
    private var sensitivity = 1f
    private var gyroEnabled = false
    private var gyroSensitivity = 1f
    private var status by mutableStateOf("请允许附近设备权限")
    private var refresh by mutableIntStateOf(0)
    override fun onCreate(state: Bundle?) { super.onCreate(state); enableEdgeToEdge()
        hid = HidGamepadService(this) { status = it }
        if (android.os.Build.VERSION.SDK_INT >= 31) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 10)
        else hid.register()
        setContent { LinkPadScreen() }
    }
    override fun onRequestPermissionsResult(r: Int, p: Array<String>, g: IntArray) { super.onRequestPermissionsResult(r,p,g); if (g.all { it == PackageManager.PERMISSION_GRANTED }) hid.register() else status = "需要“附近设备”权限才能作为蓝牙手柄" }
    @Composable private fun LinkPadScreen() {
        val tick = refresh // makes sensor / touch state redraw
        var viewMode by remember { mutableStateOf(false) }
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF83B8FF))) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
                Row(Modifier.align(Alignment.TopCenter).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(status, color = Color.White, modifier = Modifier.weight(1f)); Text("灵敏度", color=Color.LightGray)
                    Slider(sensitivity, { sensitivity = it }, Modifier.width(120.dp)); TextButton({ gyroEnabled = !gyroEnabled }) { Text(if(gyroEnabled) "体感：开" else "体感：关") }
                    Slider(gyroSensitivity, { gyroSensitivity = it }, Modifier.width(100.dp)); TextButton({ viewMode = !viewMode }) { Text(if(viewMode) "自由视角" else "右摇杆") }
                }
                Stick(Modifier.align(Alignment.BottomStart).padding(48.dp), left = true) { x,y -> pad.lx=x; pad.ly=y; transmit() }
                if (viewMode) FreeLook(Modifier.align(Alignment.CenterEnd).padding(end=70.dp)) { x,y -> pad.rx=x; pad.ry=y; transmit() }
                else Stick(Modifier.align(Alignment.BottomEnd).padding(48.dp), left = false) { x,y -> pad.rx=x; pad.ry=y; transmit() }
                FaceButtons(Modifier.align(Alignment.CenterEnd).padding(end=35.dp)) { mask, down -> pad.buttons = if(down) pad.buttons or mask else pad.buttons and mask.inv(); transmit() }
                Row(Modifier.align(Alignment.BottomCenter).padding(22.dp)) { GameButton("SELECT", 1 shl 8) { m,d -> pad.buttons=if(d)pad.buttons or m else pad.buttons and m.inv();transmit() }; Spacer(Modifier.width(20.dp)); GameButton("START", 1 shl 9) {m,d->pad.buttons=if(d)pad.buttons or m else pad.buttons and m.inv();transmit()} }
            }
        }
    }
    private fun transmit() { hid.send(pad); refresh++ }
    @Composable private fun Stick(modifier: Modifier, left: Boolean, changed:(Int,Int)->Unit) {
        var dot by remember { mutableStateOf(Offset.Zero) }
        Canvas(modifier.size(180.dp).pointerInput(Unit) { detectDragGestures(onDragStart={dot=it; updateStick(it, changed)}, onDragEnd={dot=Offset.Zero;changed(0,0)}) { c,d -> dot+=d; updateStick(dot,changed) } }) {
            drawCircle(Color(0x333B82F6), size.minDimension/2); drawCircle(Color(0xBB83B8FF), 35.dp.toPx(), center + dot.coerce(65.dp.toPx()))
        }
    }
    private fun Offset.coerce(limit: Float): Offset { val l=getDistance(); return if(l>limit) this*(limit/l) else this }
    private fun updateStick(o: Offset, changed:(Int,Int)->Unit) { val p=o.coerce(65f*resources.displayMetrics.density); changed((p.x/(65f*resources.displayMetrics.density)*127*sensitivity).roundToInt().coerceIn(-127,127), (p.y/(65f*resources.displayMetrics.density)*127*sensitivity).roundToInt().coerceIn(-127,127)) }
    @Composable private fun FreeLook(modifier: Modifier, changed:(Int,Int)->Unit) { Canvas(modifier.size(250.dp).pointerInput(Unit) { detectDragGestures(onDragEnd={changed(0,0)}) { _,d -> changed((d.x*2*sensitivity).roundToInt().coerceIn(-127,127),(d.y*2*sensitivity).roundToInt().coerceIn(-127,127)) } }) { drawCircle(Color(0x223B82F6), size.minDimension/2) } }
    @Composable private fun FaceButtons(modifier: Modifier, action:(Int,Boolean)->Unit) { Row(modifier) { GameButton("X",1 shl 2,action); GameButton("Y",1 shl 3,action); GameButton("A",1 shl 0,action); GameButton("B",1 shl 1,action) } }
    @Composable private fun GameButton(label:String, mask:Int, action:(Int,Boolean)->Unit) { Button(onClick={}, modifier=Modifier.padding(4.dp).pointerInput(mask) { detectDragGestures(onDragStart={action(mask,true)},onDragEnd={action(mask,false)},onDragCancel={action(mask,false)}) {_,_->} }) { Text(label) } }
    override fun onResume() { super.onResume(); (getSystemService(SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { (getSystemService(SENSOR_SERVICE) as SensorManager).registerListener(this,it,SensorManager.SENSOR_DELAY_GAME) } }
    override fun onPause() { (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(this); super.onPause() }
    override fun onSensorChanged(e: SensorEvent) { if(gyroEnabled) { pad.rx=(e.values[1]*gyroSensitivity*45).roundToInt().coerceIn(-127,127); pad.ry=(e.values[0]*gyroSensitivity*45).roundToInt().coerceIn(-127,127); transmit() } }
    override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
    override fun onDestroy() { hid.stop(); super.onDestroy() }
}
