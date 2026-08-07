package com.example.securekeep.sync

import java.security.MessageDigest

object ChecksumUtil {

    fun generate(title: String, content: String): String {
        val input = "$title|$content"

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}