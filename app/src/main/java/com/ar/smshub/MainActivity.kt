package com.ar.smshub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.v4.app.NotificationCompat.getExtras
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log


class MainActivity : AppCompatActivity() {

    var MY_PERMISSIONS_REQUEST_SEND_SMS = 1
    val MY_PERMISSIONS_REQUEST_SMS_RECEIVE = 10

    protected lateinit var settingsManager: SettingsManager
    lateinit var timerSend: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        settingsManager = SettingsManager(this)
        var mainFragment = MainFragment()
        mainFragment.arguments = intent.extras
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.main_view, mainFragment, "MAIN")
        transaction.commit()
        fragmentManager.executePendingTransactions()
        //initialize timer for the first time
        updateTimer()
        requestSMSSendPermission()
        requestSMSReadPermission()

        // Inside OnCreate Method
        registerReceiver(broadcastReceiver, IntentFilter("SMS_RECEIVED"))
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val b = intent.extras
            val number = b!!.getString("number")
            val message = b!!.getString("message")
            logMain("Message received and posted from: " + number + " - text: " + message)
        }
    }

    fun logMain(message: String, newline: Boolean = true) {
        val mainFragment = fragmentManager.findFragmentByTag("MAIN") as MainFragment
        if (newline) {
            mainFragment.textMainLog.setText(mainFragment.textMainLog.text.toString() + "\n" + message)
        } else {
            mainFragment.textMainLog.setText(mainFragment.textMainLog.text.toString() + message)
        }
        var scrollAmount =
            mainFragment.textMainLog.getLayout().getLineTop(mainFragment.textMainLog.getLineCount()) - mainFragment.textMainLog.getHeight()
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0) {
            mainFragment.textMainLog.scrollTo(0, scrollAmount)
        } else {
            mainFragment.textMainLog.scrollTo(0, 0)
        }
    }

    fun updateTimer() {
        if (settingsManager.isSendEnabled) {
            startTimer()
        } else {
            cancelTimer()
        }
    }

    fun cancelTimer() {
        if (::timerSend.isInitialized) {
            timerSend.cancel()
        }
        timerSend = Timer("SendSMS", true)
    }

    fun startTimer() {
        if (::timerSend.isInitialized) {
            timerSend.cancel()
        }
        timerSend = Timer("SendSMS", true)
        if (settingsManager.isSendEnabled) {
            val seconds = settingsManager.interval * 60
            val interval = (seconds * 1000).toLong()
            //this does not work
            //logMain("Timer started at " + minutes.toString())
            Log.d("---->", "Timer started at " + interval.toString())
            timerSend.schedule(SendTask(settingsManager, this), interval, interval)
        }
    }

    fun requestSMSSendPermission() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.SEND_SMS
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS),
                    MY_PERMISSIONS_REQUEST_SEND_SMS
                )

            }
        } else {
            // Permission has already been granted
        }
    }

    /**
     * check SMS read permission
     */
    fun isSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request runtime SMS permission
     */
    private fun requestSMSReadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS),
            MY_PERMISSIONS_REQUEST_SMS_RECEIVE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SEND_SMS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> {
                var settingsFragment = fragmentManager.findFragmentByTag("SETTINGS") as? SettingsFragment
                if (settingsFragment == null) {
                    settingsFragment = SettingsFragment()
                }
                val transaction = fragmentManager.beginTransaction()
                transaction.addToBackStack("MAIN")
                transaction.replace(R.id.main_view, settingsFragment, "SETTINGS")
                transaction.commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun msgShow(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

}
