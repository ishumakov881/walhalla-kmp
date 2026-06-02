package dev.walhalla.kmp.device

import kotlinx.cinterop.*
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSUserDefaults
import platform.Security.*
import platform.darwin.NSCopyingProtocol
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SERVICE = "dev.walhalla.online.device_id"
private const val ACCOUNT = "installation"
private const val INSTALL_ID_KEY = "dev.walhalla.online.install_id"

@OptIn(ExperimentalForeignApi::class, ExperimentalUuidApi::class)
actual class Installation {

    actual fun deviceId(): String {
        readFromKeychain()?.let { return it }
        val id = Uuid.random().toString()
        writeToKeychain(id)
        return id
    }

    actual fun installId(): String? {
        val defaults = NSUserDefaults.standardUserDefaults
        val existing = defaults.stringForKey(INSTALL_ID_KEY)?.toString()?.trim()
        if (!existing.isNullOrEmpty()) return existing

        val id = Uuid.random().toString()
        defaults.setObject(id, forKey = INSTALL_ID_KEY)
        return id
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readFromKeychain(): String? = memScoped {
    val query = baseKeychainQuery().apply {
        setKeychainObject(kCFBooleanTrue, kSecReturnData)
        setKeychainObject(kSecMatchLimitOne, kSecMatchLimit)
    }
    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query.asCFDictionary(), result.ptr)
    if (status != errSecSuccess) return null
    val data = result.value as? NSData ?: return null
    NSString.create(data, NSUTF8StringEncoding)?.toString()?.takeIf { it.isNotEmpty() }
}

@OptIn(ExperimentalForeignApi::class)
private fun writeToKeychain(value: String) {
    deleteFromKeychain()
    val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
    val query = baseKeychainQuery().apply {
        setKeychainObject(data, kSecValueData)
        setKeychainObject(kSecAttrAccessibleAfterFirstUnlock, kSecAttrAccessible)
    }
    SecItemAdd(query.asCFDictionary(), null)
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteFromKeychain() {
    SecItemDelete(baseKeychainQuery().asCFDictionary())
}

@OptIn(ExperimentalForeignApi::class)
private fun baseKeychainQuery(): NSMutableDictionary =
    NSMutableDictionary().apply {
        setKeychainObject(kSecClassGenericPassword, kSecClass)
        setKeychainObject(SERVICE, kSecAttrService)
        setKeychainObject(ACCOUNT, kSecAttrAccount)
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSMutableDictionary.setKeychainObject(value: Any?, key: CPointer<*>?) {
    setObject(value as NSCopyingProtocol, forKey = key as NSCopyingProtocol)
}

@Suppress("UNCHECKED_CAST")
private fun NSMutableDictionary.asCFDictionary(): CFDictionaryRef? = this as CFDictionaryRef
