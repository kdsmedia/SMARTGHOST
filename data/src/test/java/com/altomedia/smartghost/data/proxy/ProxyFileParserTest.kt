package com.altomedia.smartghost.data.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyFileParserTest {

    @Test
    fun `empty content yields empty result`() {
        val result = ProxyFileParser.parse("")
        assertTrue(result.endpoints.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `null content yields empty result`() {
        val result = ProxyFileParser.parse(null)
        assertTrue(result.endpoints.isEmpty())
    }

    @Test
    fun `utf8 byte order mark is stripped`() {
        val result = ProxyFileParser.parse("\uFEFF1.2.3.4:8080\n5.6.7.8:9090")
        assertEquals(2, result.endpoints.size)
        assertTrue(result.errors.isEmpty())
        assertEquals("1.2.3.4", result.endpoints.first().host)
    }

    @Test
    fun `carriage return line endings are handled`() {
        val result = ProxyFileParser.parse("1.2.3.4:8080\r\n5.6.7.8:9090\r\n")
        assertEquals(2, result.endpoints.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ip port user pass format keeps only host and port`() {
        val result = ProxyFileParser.parse("1.2.3.4:8080:alice:secret")
        assertEquals(1, result.endpoints.size)
        val endpoint = result.endpoints.first()
        assertEquals("1.2.3.4", endpoint.host)
        assertEquals(8080, endpoint.port)
    }

    @Test
    fun `userinfo in a scheme url is ignored`() {
        val result = ProxyFileParser.parse("http://alice:secret@10.0.0.1:3128")
        assertEquals(1, result.endpoints.size)
        assertEquals("10.0.0.1", result.endpoints.first().host)
        assertEquals(3128, result.endpoints.first().port)
    }

    @Test
    fun `host port lines are parsed with default scheme`() {
        val result = ProxyFileParser.parse("127.0.0.1:8080")
        assertEquals(1, result.endpoints.size)
        val endpoint = result.endpoints.first()
        assertEquals("127.0.0.1", endpoint.host)
        assertEquals(8080, endpoint.port)
        assertEquals("http", endpoint.scheme)
    }

    @Test
    fun `scheme qualified lines are parsed`() {
        val result = ProxyFileParser.parse("socks5://10.0.0.5:1080")
        assertEquals(1, result.endpoints.size)
        assertEquals("socks5", result.endpoints.first().scheme)
    }

    @Test
    fun `comments and blank lines are ignored`() {
        val content = """
            # this is a comment
            127.0.0.1:8080

            # another comment
            10.0.0.1:3128
        """.trimIndent()
        val result = ProxyFileParser.parse(content)
        assertEquals(2, result.endpoints.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `trailing comment is stripped`() {
        val result = ProxyFileParser.parse("127.0.0.1:8080 # local test server")
        assertEquals(1, result.endpoints.size)
        assertEquals(8080, result.endpoints.first().port)
    }

    @Test
    fun `invalid port is reported with line number`() {
        val result = ProxyFileParser.parse("127.0.0.1:99999")
        assertTrue(result.endpoints.isEmpty())
        assertEquals(1, result.errors.size)
        assertEquals(1, result.errors.first().line)
    }

    @Test
    fun `non numeric port is reported`() {
        val result = ProxyFileParser.parse("127.0.0.1:abc")
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `malformed line is reported`() {
        val result = ProxyFileParser.parse("this-is-not-a-proxy")
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `unsupported scheme is reported`() {
        val result = ProxyFileParser.parse("ftp://127.0.0.1:21")
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `duplicates are counted not duplicated`() {
        val content = """
            127.0.0.1:8080
            127.0.0.1:8080
            127.0.0.1:8081
        """.trimIndent()
        val result = ProxyFileParser.parse(content)
        assertEquals(2, result.endpoints.size)
        assertEquals(1, result.duplicateCount)
    }

    @Test
    fun `line numbers are correct with mixed content`() {
        val content = """
            # header
            127.0.0.1:8080
            bad-line
            10.0.0.1:3128
        """.trimIndent()
        val result = ProxyFileParser.parse(content)
        assertEquals(2, result.endpoints.size)
        assertEquals(1, result.errors.size)
        assertEquals(3, result.errors.first().line)
    }
}
