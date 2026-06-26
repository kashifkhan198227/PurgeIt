package com.purgeit.android.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test fun `formats bytes`() = assertEquals("512 B", formatBytes(512))
    @Test fun `formats kilobytes`() = assertEquals("2 KB", formatBytes(2048))
    @Test fun `formats megabytes`() = assertEquals("1.5 MB", formatBytes(1_572_864))
    @Test fun `formats gigabytes`() = assertEquals("1.0 GB", formatBytes(1_073_741_824))
    @Test fun `handles zero`() = assertEquals("0 B", formatBytes(0))
    @Test fun `handles negative`() = assertEquals("0 B", formatBytes(-1))
}
