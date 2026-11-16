package com.example.domain

import com.example.data.model.Committee
import com.example.data.model.CommitteeStaff
import java.net.URLEncoder

/**
 * What a sheikh needs to know before going to a committee: where, whom to call there, how many students.
 * Pure functions, so the screens only show what these return.
 */
object Logistics {
    /** The place name: the sheikh's country/place field, else the association, else the branch. Empty when none is set. */
    fun placeOf(c: Committee): String =
        listOf(c.sheikhCountry, c.associationName, c.branch).map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()

    /** The place a ceremony happens in, falling back to the committee's own place. */
    fun ceremonyPlaceOf(c: Committee): String = c.ceremonyDetails.venue.trim().ifEmpty { placeOf(c) }

    /** The committee head assigned to this committee («رئيس المكان»), or null when none is assigned. */
    fun headOf(c: Committee): CommitteeStaff? = c.staff.firstOrNull { it.role == "committee_head" }

    /** A Google Maps search link by name. Opens the Maps app when installed, else the browser. Null when the name is empty. */
    fun mapsUrl(place: String): String? {
        val q = place.trim()
        if (q.isEmpty()) return null
        return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(q, "UTF-8")
    }

    /** «١٢ طالب» in the way a person says it. Null when the count is unknown. */
    fun studentsText(count: Int): String? = when {
        count <= 0 -> null
        count == 1 -> "طالب واحد"
        count == 2 -> "طالبان"
        count in 3..10 -> "$count طلاب"
        else -> "$count طالب"
    }
}
