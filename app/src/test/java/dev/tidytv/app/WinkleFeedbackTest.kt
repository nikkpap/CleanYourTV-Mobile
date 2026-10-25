package dev.tidytv.app

import dev.tidytv.app.data.FeedbackDraft
import dev.tidytv.app.data.FeedbackKind
import dev.tidytv.app.data.ShareStats
import dev.tidytv.app.data.WinkleFeedback
import dev.tidytv.app.data.caption
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WinkleFeedbackTest {
    private class FakeHttp(val status: Int, val location: String? = null, val reply: String = "{\"success\":true}") : WinkleFeedback.Http {
        val posts = mutableListOf<Pair<String, String>>()
        override fun post(url: String, json: String): Pair<Int, String?> { posts += url to json; return status to location }
        override fun get(url: String) = reply
    }

    private fun feedback(http: WinkleFeedback.Http) = WinkleFeedback("0.2.0+2", "Google Pixel 9", "Android 16", "install-1", http)

    @Test fun payloadHasEveryRequiredField() {
        val p = feedback(FakeHttp(302)).payload(FeedbackDraft(FeedbackKind.BUG, "  It stopped at step 2  ", page = "tidy"))
        assertEquals("TidyTV", p.getString("app"))
        assertEquals("bug", p.getString("type"))
        assertEquals("It stopped at step 2", p.getString("feedback"))
        assertEquals("android", p.getString("platform"))
        assertEquals("0.2.0+2", p.getString("version"))
        assertEquals("Google Pixel 9", p.getString("device"))
        assertEquals("Android 16", p.getString("os"))
        assertEquals("in-app", p.getString("source"))
        assertEquals("tidy", p.getString("page"))
        assertEquals("install-1", p.getString("user_id"))
        assertEquals("", p.getString("email"))
        assertFalse(p.has("screenshot_base64"))
    }

    @Test fun logGoesIntoTheMessageAndScreenshotIsBase64() {
        val p = feedback(FakeHttp(302)).payload(FeedbackDraft(FeedbackKind.BUG, "hi", log = "connect 10.0.0.x", screenshot = byteArrayOf(1, 2, 3)))
        assertTrue(p.getString("feedback").startsWith("hi\n\n--- TidyTV log ---\nconnect"))
        assertEquals("AQID", p.getString("screenshot_base64"))
    }

    @Test fun redirectMeansDelivered() = runBlocking {
        val http = FakeHttp(302, "https://script.googleusercontent.com/r")
        assertTrue(feedback(http).send(FeedbackDraft(FeedbackKind.PRAISE, "great")))
        assertEquals(WinkleFeedback.ENDPOINT, http.posts.single().first)
        assertEquals("praise", JSONObject(http.posts.single().second).getString("type"))
    }

    @Test fun rateLimitedIsNotDelivered() = runBlocking {
        assertFalse(feedback(FakeHttp(302, "https://x/r", "{\"success\":false,\"error\":\"rate_limited\"}")).send(FeedbackDraft(FeedbackKind.BUG, "x")))
    }

    @Test fun serverErrorAndNetworkFailureAreNotDelivered() = runBlocking {
        assertFalse(feedback(FakeHttp(500)).send(FeedbackDraft(FeedbackKind.BUG, "x")))
        val broken = object : WinkleFeedback.Http {
            override fun post(url: String, json: String): Pair<Int, String?> = throw java.io.IOException("offline")
            override fun get(url: String) = ""
        }
        assertFalse(feedback(broken).send(FeedbackDraft(FeedbackKind.BUG, "x")))
    }

    @Test fun captionNamesTheSiteAndTheNumbers() {
        val c = ShareStats(6, "TCL 55C735", 123, true, emptyList()).caption()
        assertTrue(c, c.contains("6 junk apps turned off") && c.contains("123 MB") && c.contains("https://tidytv.winkle.dev"))
        assertTrue(ShareStats(1, null, null, false, emptyList()).caption().contains("1 junk app turned off"))
    }
}
