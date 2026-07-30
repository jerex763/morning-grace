package com.morninggrace.alarm

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmServiceReadinessTest {

    @Test
    fun `system voice allows playback without recordings`() {
        assertNull(
            AlarmService.unavailablePlaybackMessage(
                ttsAvailable = true,
                bibleEnabled = true,
                preferRecordedBible = true,
                recordedChapterCount = 0
            )
        )
    }

    @Test
    fun `recordings allow playback without system voice`() {
        assertNull(
            AlarmService.unavailablePlaybackMessage(
                ttsAvailable = false,
                bibleEnabled = true,
                preferRecordedBible = true,
                recordedChapterCount = 3
            )
        )
    }

    @Test
    fun `missing system voice and recordings explains why playback cannot start`() {
        assertNotNull(
            AlarmService.unavailablePlaybackMessage(
                ttsAvailable = false,
                bibleEnabled = true,
                preferRecordedBible = true,
                recordedChapterCount = 0
            )
        )
    }
}
