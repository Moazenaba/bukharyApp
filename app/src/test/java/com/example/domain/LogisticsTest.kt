package com.example.domain

import com.example.data.model.Committee
import com.example.data.model.CommitteeStaff
import com.example.data.model.CeremonyDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogisticsTest {
    private fun c(country: String = "", assoc: String = "", branch: String = "", staff: List<CommitteeStaff> = emptyList(), venue: String = "") =
        Committee(
            id = "1", committeeNumber = 1, committeeName = "n", sheikhCountry = country, associationName = assoc,
            branch = branch, staff = staff, ceremonyDetails = CeremonyDetails(venue = venue)
        )

    @Test fun placePrefersSheikhPlaceThenAssociationThenBranch() {
        assertEquals("مسجد النور", Logistics.placeOf(c("مسجد النور", "جمعية", "فرع")))
        assertEquals("جمعية", Logistics.placeOf(c("  ", "جمعية", "فرع")))
        assertEquals("فرع", Logistics.placeOf(c(branch = "فرع")))
        assertEquals("", Logistics.placeOf(c()))
    }

    @Test fun ceremonyPlaceFallsBackToCommitteePlace() {
        assertEquals("قاعة", Logistics.ceremonyPlaceOf(c("مسجد", venue = "قاعة")))
        assertEquals("مسجد", Logistics.ceremonyPlaceOf(c("مسجد")))
    }

    @Test fun headIsTheCommitteeHeadNotASheikh() {
        val sheikh = CommitteeStaff("s", "شيخ", "sheikh", "0100")
        val head = CommitteeStaff("h", "رئيس", "committee_head", "0111")
        assertEquals(head, Logistics.headOf(c(staff = listOf(sheikh, head))))
        assertNull(Logistics.headOf(c(staff = listOf(sheikh))))
    }

    @Test fun mapsUrlEncodesTheNameAndRefusesEmpty() {
        val url = Logistics.mapsUrl(" مسجد النور ")!!
        assertTrue(url.startsWith("https://www.google.com/maps/search/?api=1&query="))
        assertTrue(!url.contains(" ") && !url.contains("مسجد"))
        assertNull(Logistics.mapsUrl("  "))
    }

    @Test fun studentsTextUsesArabicNumberAgreement() {
        assertNull(Logistics.studentsText(0))
        assertEquals("طالب واحد", Logistics.studentsText(1))
        assertEquals("طالبان", Logistics.studentsText(2))
        assertEquals("5 طلاب", Logistics.studentsText(5))
        assertEquals("30 طالب", Logistics.studentsText(30))
    }
}
