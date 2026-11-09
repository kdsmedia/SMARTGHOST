package com.altomedia.smartghost.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactorTest {

    @Test
    fun `github classic token is redacted`() {
        val input = "token=ghp_1234567890abcdefghijklmnopqrstuvwx"
        val output = SecretRedactor.redact(input)
        assertFalse(output.contains("ghp_1234567890"))
        assertTrue(output.contains("***REDACTED***"))
    }

    @Test
    fun `fine grained github token is redacted`() {
        val input = "github_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz0123456789"
        val output = SecretRedactor.redact(input)
        assertTrue(output.contains("***REDACTED***"))
        assertFalse(output.contains("abcdefghijklmnopqrstuvwxyz"))
    }

    @Test
    fun `bearer token is redacted`() {
        val output = SecretRedactor.redact("Authorization: Bearer abcdef1234567890")
        assertFalse(output.contains("abcdef1234567890"))
    }

    @Test
    fun `password assignment is redacted`() {
        val output = SecretRedactor.redact("password=SuperSecret123")
        assertFalse(output.contains("SuperSecret123"))
    }

    @Test
    fun `api key assignment is redacted`() {
        val output = SecretRedactor.redact("api_key: AIzaSyABCDEFGHIJKLMNOPQRSTUVWXYZ")
        assertFalse(output.contains("AIzaSyABCDEFGHIJKLMNOPQRSTUVWXYZ"))
    }

    @Test
    fun `credentials in url are redacted`() {
        val output = SecretRedactor.redact("https://user:pass1234@example.com/data")
        assertFalse(output.contains("pass1234"))
    }

    @Test
    fun `jwt is redacted`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        val output = SecretRedactor.redact("token=$jwt")
        assertFalse(output.contains("eyJhbGciOiJIUzI1NiJ9"))
    }

    @Test
    fun `plain text is untouched`() {
        val input = "SMARTGHOST diagnostics: endpoint 3 ok in 120 ms"
        assertEquals(input, SecretRedactor.redact(input))
    }

    @Test
    fun `containsSecret detects secrets`() {
        assertTrue(SecretRedactor.containsSecret("password=hunter2"))
        assertFalse(SecretRedactor.containsSecret("just a normal log line"))
    }

    @Test
    fun `null and empty are safe`() {
        assertEquals("", SecretRedactor.redact(null))
        assertEquals("", SecretRedactor.redact(""))
    }
}
