package com.linkpad.controller

import android.bluetooth.*
import android.content.Context
import java.util.concurrent.Executor

class HidGamepadService(private val context: Context, private val onStatus: (String) -> Unit) {
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private val adapter get() = BluetoothAdapter.getDefaultAdapter()
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) { onStatus(if (registered) "手柄已就绪：请在接收设备蓝牙设置中配对" else "HID 注册失败") }
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) { host = if (state == BluetoothProfile.STATE_CONNECTED) device else null; onStatus(if (host != null) "已连接：${device.name ?: device.address}" else "等待连接") }
    }
    fun register() {
        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                hid = proxy as BluetoothHidDevice
                val sdp = BluetoothHidDeviceAppSdpSettings("LinkPad Gamepad", "Virtual Bluetooth controller", "LinkPad", BluetoothHidDevice.SUBCLASS2_GAMEPAD, GamepadReport.descriptor)
                hid?.registerApp(sdp, null, null, Executor { it.run() }, callback)
            }
            override fun onServiceDisconnected(profile: Int) { hid = null; onStatus("蓝牙服务断开") }
        }, BluetoothProfile.HID_DEVICE)
    }
    fun send(state: PadState) { host?.let { hid?.sendReport(it, GamepadReport.ID.toInt(), GamepadReport.encode(state)) } }
    fun stop() { hid?.unregisterApp(); hid = null }
}
