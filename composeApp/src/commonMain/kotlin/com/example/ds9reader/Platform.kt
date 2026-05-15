package com.example.ds9reader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
