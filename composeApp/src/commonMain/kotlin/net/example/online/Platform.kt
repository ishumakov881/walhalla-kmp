package net.example.online

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform