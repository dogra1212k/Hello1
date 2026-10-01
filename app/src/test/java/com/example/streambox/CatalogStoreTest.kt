package com.example.streambox

import android.content.SharedPreferences
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CatalogStoreTest {
    private val defaults = MovieJson.decode(File("src/main/assets/open_movies.json").readText())
    private val oldUrls = listOf("BigBuckBunny", "ElephantsDream", "Sintel", "TearsOfSteel").map {
        "https://storage.googleapis.com/gtv-videos-bucket/sample/$it.mp4"
    }
    private val custom = Movie("My film", "Movies", "My credits", "https://media.example/my-film.mp4", "")

    @Test fun freshInstallLoadsTheCurrentFullFilmCatalog() {
        assertEquals(defaults, CatalogStore(MemoryPreferences(), defaults).getMovies())
    }

    @Test fun savedBuiltinIdsReceiveNewSourcesAndPostersWithoutLosingTheirMapping() {
        val old = defaults.mapIndexed { index, movie ->
            movie.copy(videoUrl = oldUrls[index], posterUrl = "https://old.example/poster.jpg",
                tmdbId = 100 + index, mediaType = "tv")
        }
        val prefs = MemoryPreferences()
        prefs.edit().putString("movies", MovieJson.encode(old)).apply()
        val expected = defaults.mapIndexed { index, movie -> movie.copy(tmdbId = 100 + index, mediaType = "tv") }
        assertEquals(expected, CatalogStore(prefs, defaults).getLocalMovies())
    }

    @Test fun pre170BuiltinsWithoutIdsAlsoReceiveNewSourcesAndCredits() {
        val old = oldUrls.map { Movie("Old demo title", "Movies", "Old description", it, "") }
        val prefs = MemoryPreferences()
        prefs.edit().putString("movies", MovieJson.encode(old)).apply()
        assertEquals(defaults, CatalogStore(prefs, defaults).getLocalMovies())
    }

    @Test fun customEntriesAreNotReplacedJustBecauseTheyShareALegacyUrl() {
        val cloudCopy = custom.copy(id = "user-created", videoUrl = oldUrls.first(), tmdbId = 42)
        val prefs = MemoryPreferences()
        prefs.edit().putString("movies", MovieJson.encode(listOf(custom, cloudCopy))).apply()
        assertEquals(listOf(custom, cloudCopy), CatalogStore(prefs, defaults).getLocalMovies())
    }

    @Test fun anUpgradeDoesNotRestoreFilmsTheUserDeleted() {
        val prefs = MemoryPreferences()
        val remaining = defaults.first().copy(videoUrl = oldUrls.first())
        prefs.edit().putString("movies", MovieJson.encode(listOf(remaining, custom))).apply()
        assertEquals(listOf(defaults.first(), custom), CatalogStore(prefs, defaults).getLocalMovies())
        prefs.edit().putString("movies", "[]").apply()
        assertTrue(CatalogStore(prefs, defaults).getLocalMovies().isEmpty())
    }

    @Test fun onlyTheRetiredBuiltinPromosAreRemovedDuringAnUpgrade() {
        val promo = custom.copy(videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
        val userEntry = promo.copy(id = "user-created")
        val prefs = MemoryPreferences()
        prefs.edit().putString("movies", MovieJson.encode(listOf(promo, userEntry, custom))).apply()
        assertEquals(listOf(userEntry, custom), CatalogStore(prefs, defaults).getLocalMovies())
    }

    @Test fun addingAVideoPersistsMigratedSourcesForTheNextLaunch() {
        val prefs = MemoryPreferences()
        prefs.edit().putString("movies", MovieJson.encode(listOf(defaults.first().copy(videoUrl = oldUrls.first())))).apply()
        CatalogStore(prefs, defaults).add(custom)
        assertEquals(listOf(defaults.first(), custom), CatalogStore(prefs, defaults).getMovies())
        assertEquals(listOf(defaults.first(), custom), MovieJson.decode(prefs.getString("movies", "[]")!!))
    }

    @Test fun removingOneDuplicateDoesNotDeleteEveryMatchingEntry() {
        val prefs = MemoryPreferences()
        val store = CatalogStore(prefs, defaults)
        store.saveMovies(listOf(custom, custom))
        store.removeAt(1)
        assertEquals(listOf(custom), CatalogStore(prefs, defaults).getLocalMovies())
    }

    @Test fun localAdminIndicesIgnoreCloudRowsAndCloudOverrides() {
        val prefs = MemoryPreferences()
        val store = CatalogStore(prefs, defaults)
        val cloudOnly = custom.copy(title = "Cloud only", videoUrl = "https://media.example/cloud.mp4", id = "cloud-1")
        val cloudOverride = custom.copy(id = "cloud-2")
        store.saveMovies(listOf(custom, defaults.first()))
        store.saveCloudMovies(listOf(cloudOnly, cloudOverride))
        assertEquals(listOf(custom, defaults.first()), store.getLocalMovies())
        store.removeAt(0)
        val reloaded = CatalogStore(prefs, defaults)
        assertEquals(listOf(defaults.first()), reloaded.getLocalMovies())
        assertEquals(listOf(cloudOnly, cloudOverride, defaults.first()), reloaded.getMovies())
    }

    @Test fun invalidLocalIndicesLeaveTheSavedCatalogIntact() {
        val prefs = MemoryPreferences()
        val store = CatalogStore(prefs, defaults)
        store.saveMovies(listOf(custom))
        val raw = prefs.getString("movies", null)
        store.removeAt(-1)
        store.removeAt(1)
        assertEquals(raw, prefs.getString("movies", null))
    }

    @Test fun emptyCloudSyncAndDefaultResetPreserveTheirSeparateCatalogs() {
        val store = CatalogStore(MemoryPreferences(), defaults)
        store.saveMovies(listOf(custom))
        store.saveCloudMovies(listOf(custom.copy(id = "cloud", videoUrl = "https://media.example/cloud.mp4")))
        store.saveCloudMovies(emptyList())
        assertEquals(listOf(custom), store.getMovies())
        store.resetDefaults()
        assertEquals(defaults, store.getMovies())
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?) =
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String?) = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clear = false
            private fun put(key: String?, value: Any?): SharedPreferences.Editor = apply { changes[key!!] = value }
            override fun putString(key: String?, value: String?) = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
            override fun putInt(key: String?, value: Int) = put(key, value)
            override fun putLong(key: String?, value: Long) = put(key, value)
            override fun putFloat(key: String?, value: Float) = put(key, value)
            override fun putBoolean(key: String?, value: Boolean) = put(key, value)
            override fun remove(key: String?) = put(key, null)
            override fun clear(): SharedPreferences.Editor = apply { clear = true }
            override fun apply() {
                if (clear) values.clear()
                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            }
            override fun commit(): Boolean { apply(); return true }
        }
    }
}
