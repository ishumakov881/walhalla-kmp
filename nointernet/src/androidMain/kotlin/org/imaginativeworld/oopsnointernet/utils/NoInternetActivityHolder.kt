package org.imaginativeworld.oopsnointernet.utils

import android.annotation.SuppressLint
import android.app.Activity

@SuppressLint("StaticFieldLeak")
object NoInternetActivityHolder {

    var activity: Activity? = null

    fun init(activity: Activity) {
        NoInternetActivityHolder.activity = activity
    }

}