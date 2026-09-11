package com.mori.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LicensesDataTest {

    @Test
    fun parsesLibrariesAndLicenses() {
        val data = parseLicenses(FIXTURE)

        assertEquals(2, data?.libraries?.size)
        assertEquals("Activity", data?.libraries?.get(0)?.name)
        assertEquals(listOf("Apache-2.0"), data?.libraries?.get(0)?.licenses)
        val licenses = data?.licensesFor(data.libraries[0]).orEmpty()
        assertEquals(1, licenses.size)
        assertTrue(licenses[0].content.orEmpty().startsWith("Apache License"))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val data = parseLicenses("""{"libraries":[{"uniqueId":"x","futureField":1}],"unknownRoot":true}""")

        assertEquals(1, data?.libraries?.size)
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(parseLicenses("not json"))
    }

    @Test
    fun missingLicenseHashYieldsNoDetail() {
        val data = parseLicenses(
            """{"libraries":[{"uniqueId":"x","name":"X","licenses":["nope"]}],"licenses":{}}""",
        )

        assertTrue(data?.licensesFor(data.libraries[0]).isNullOrEmpty() == true)
    }

    private companion object {
        const val FIXTURE = """{
            "libraries": [
                {
                    "uniqueId": "androidx.activity:activity",
                    "name": "Activity",
                    "artifactVersion": "1.9.3",
                    "organization": {"name": "The Android Open Source Project"},
                    "licenses": ["Apache-2.0"],
                    "website": "https://example.com"
                },
                {
                    "uniqueId": "com.example:other",
                    "name": "",
                    "licenses": []
                }
            ],
            "licenses": {
                "Apache-2.0": {
                    "name": "Apache License 2.0",
                    "spdxId": "Apache-2.0",
                    "url": "https://example.com/license",
                    "content": "Apache License..."
                }
            }
        }"""
    }
}
