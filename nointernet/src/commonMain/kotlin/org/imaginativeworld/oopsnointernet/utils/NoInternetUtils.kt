package org.imaginativeworld.oopsnointernet.utils

expect object NoInternetUtils {

    /**
     * Check if the device is connected with the Internet.
     */
    
    fun isConnectedToInternet(): Boolean

    /**
     * Check if the device is in airplane mode.
     */
    
    fun isAirplaneModeOn(): Boolean
    /**
     * Check if the device has an active VPN connection.
     */
    
    fun isVpnActive(): Boolean

    /**
     * Ping google.com to check if the internet connection is active.
     * It must be called from a background thread.
     */
    
    fun hasActiveInternetConnection(): Boolean

    /**
     * Open the system settings.
     */
    
    fun turnOnMobileData()


    /**
     * Open the airplane mode settings.
     */
    
    fun turnOffAirplaneMode()

    fun turnOnWifi()
}