package dev.walhalla.kmp.device

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.*
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
    val query = baseKeychainQuery() ?: return null
    CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
    CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query, result.ptr)
    if (status != errSecSuccess) return null

    val data = result.value as? NSData ?: return null
    NSString.create(data, NSUTF8StringEncoding)?.toString()?.takeIf { it.isNotEmpty() }
}

@OptIn(ExperimentalForeignApi::class)
private fun writeToKeychain(value: String) {
    deleteFromKeychain()
    val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
    val query = baseKeychainQuery() ?: return
    CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))
    CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
    SecItemAdd(query, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteFromKeychain() {
    val query = baseKeychainQuery() ?: return
    SecItemDelete(query)
}

@OptIn(ExperimentalForeignApi::class)
private fun baseKeychainQuery(): CFMutableDictionaryRef? {
    val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null) ?: return null
    CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
    CFDictionaryAddValue(query, kSecAttrService, SERVICE.toCFString())
    CFDictionaryAddValue(query, kSecAttrAccount, ACCOUNT.toCFString())
    return query
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toCFString(): CFStringRef? = memScoped {
    CFStringCreateWithCString(kCFAllocatorDefault, cstr, kCFStringEncodingUTF8)
}
