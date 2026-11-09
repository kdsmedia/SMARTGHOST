package com.altomedia.smartghost.data.session

import com.altomedia.smartghost.data.proxy.ProxyEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {

    private fun proxy(host: String, port: Int) = ProxyEndpoint(host, port, "http", 1)

    private fun pool(size: Int): List<ProxyEndpoint> =
        (1..size).map { proxy("10.0.0.$it", 8000 + it) }

    @Test
    fun `importing a pool exposes it for sampling`() {
        val repo = SessionRepository()
        repo.setProxyEndpoints("proxy.txt", pool(25))

        assertEquals(25, repo.allEndpoints.value.size)
        // The preview set is capped at ten until a session samples.
        assertEquals(10, repo.endpoints.value.size)
    }

    @Test
    fun `sample returns exactly ten endpoints`() {
        val repo = SessionRepository()
        repo.setProxyEndpoints("proxy.txt", pool(50))

        val sample = repo.sampleForSession(10)

        assertEquals(10, sample.size)
        assertEquals((1..10).toList(), sample.map { it.index })
        assertEquals(10, repo.endpoints.value.size)
    }

    @Test
    fun `sample is drawn from the imported pool without duplicates`() {
        val repo = SessionRepository()
        val pool = pool(30)
        repo.setProxyEndpoints("proxy.txt", pool)

        val sample = repo.sampleForSession(10)

        val poolLabels = pool.map { "${it.host}:${it.port}" }.toSet()
        val sampleLabels = sample.map { it.label }
        assertEquals(10, sampleLabels.toSet().size)
        assertTrue(poolLabels.containsAll(sampleLabels))
    }

    @Test
    fun `sampling a smaller pool returns everything available`() {
        val repo = SessionRepository()
        repo.setProxyEndpoints("proxy.txt", pool(4))

        val sample = repo.sampleForSession(10)

        assertEquals(4, sample.size)
    }

    @Test
    fun `empty pool yields an empty sample`() {
        val repo = SessionRepository()

        assertTrue(repo.sampleForSession(10).isEmpty())
        assertTrue(repo.endpoints.value.isEmpty())
    }

    @Test
    fun `clear removes both the pool and the session set`() {
        val repo = SessionRepository()
        repo.setProxyEndpoints("proxy.txt", pool(20))
        repo.sampleForSession(10)

        repo.clear()

        assertTrue(repo.allEndpoints.value.isEmpty())
        assertTrue(repo.endpoints.value.isEmpty())
    }
}
