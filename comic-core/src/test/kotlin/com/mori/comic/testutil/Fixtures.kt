package com.mori.comic.testutil

import java.io.File

/**
 * Loads committed fixture archives from the test resources tree. Because unit tests run on
 * the JVM, resources are read from the classpath but CBR/RAR fixtures are resolved from the
 * filesystem so [com.mori.comic.ComicFactory] can open them as [File]s.
 */
object Fixtures {
    /** Copies a classpath resource (named [resourcePath]) to a temp file and returns it. */
    fun resourceToTempFile(resourcePath: String): File {
        val bytes = javaClass.classLoader!!.getResourceAsStream(resourcePath)
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("Missing test resource: $resourcePath")
        val file = File.createTempFile("mori-fixture", resourcePath.substringAfterLast('/'))
        file.writeBytes(bytes)
        file.deleteOnExit()
        return file
    }

    fun sortedCbr(): File = resourceToTempFile("com/mori/comic/fixtures/sorted.cbr")

    fun solidCbr(): File = resourceToTempFile("com/mori/comic/fixtures/solid.cbr")

    fun lockedCbr(): File = resourceToTempFile("com/mori/comic/fixtures/locked.cbr")

    /** Real JPEG fixtures committed under `fixtures/images/`. */
    object Images {
        val extent: String = "com/mori/comic/fixtures/images"

        fun bytes(name: String): ByteArray =
            javaClass.classLoader!!.getResourceAsStream("$extent/$name")
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("Missing image resource: $name")

        fun landscapeJpg(): ByteArray = bytes("landscape.jpg")

        fun portraitJpg(): ByteArray = bytes("portrait.jpg")

        fun progressiveJpg(): ByteArray = bytes("progressive.jpg")

        fun comicWebp(): ByteArray = bytes("comic.webp")
    }
}
