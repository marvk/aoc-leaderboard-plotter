package net.marvk.aocleaderboardplotter

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.coord.coordFlip
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.geom.geomBoxplot
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.pos.positionNudge
import org.jetbrains.letsPlot.scale.scaleColorManual
import org.jetbrains.letsPlot.scale.scaleFillDiscrete
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleXDiscrete
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.scale.scaleYDiscrete
import org.jetbrains.letsPlot.themes.flavorDarcula
import org.jetbrains.letsPlot.themes.flavorSolarizedLight
import org.jetbrains.letsPlot.themes.theme
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


data class Member(
    val id: Int,
    val localScore: Int,
    private val lastStarTs: Long,
    val globalScore: Int,
    val stars: Int,
    val name: String,
    val completionDayLevel: List<Day>,
) {
    val lastStar = lastStarTs.toLocalDate()

    companion object {
        fun parse(element: JsonElement, id: String): Member {
            val o = element.asJsonObject

            val days =
                o.get("completion_day_level")
                    .asJsonObject
                    .asMap()
                    .mapKeys { it.key.toInt() }
                    .map { (dayId, day) -> dayId to Day.parse(day, dayId) }
                    .sortedBy { it.first }
                    .map { it.second }

            return Member(
                id.toInt(),
                o.get("local_score").asInt,
                o.get("last_star_ts").asLong,
                o.get("global_score").asInt,
                o.get("stars").asInt,
                o.get("name").asString,
                days,
            )
        }
    }
}

data class Day(
    val index: Int,
    val parts: List<Part>,
) {
    companion object {
        fun parse(jsonElement: JsonElement, index: Int) =
            Day(
                index,
                listOf(1, 2).mapNotNull { i ->
                    jsonElement.asJsonObject
                        .get(i.toString())?.asJsonObject?.let { Part.parse(it, i) }
                }
            )
    }
}

data class Part(
    val index: Int,
    private val getStarTs: Long,
    val starIndex: Long,
) {
    val getStar = getStarTs.toLocalDate()

    companion object {
        fun parse(jsonObject: JsonObject, index: Int) =
            Part(
                index,
                jsonObject.get("get_star_ts").asLong,
                jsonObject.get("star_index").asLong,
            )
    }
}

private fun Long.toLocalDate(): LocalDateTime = LocalDateTime.ofEpochSecond(this, 0, ZoneOffset.UTC)

private class Plotter(
    private val members: List<Member>,
    private val date: ZonedDateTime?,
    private val year: Year,
) {
    private val numberOfDays = members.map { it.completionDayLevel }.maxOf { it.size }

    fun plotA() {
        val data: Map<String, List<Any>> = createData(members);

        val p =
            letsPlot(data) { x = "HoursToComplete"; y = "Y"; color = "Name" } +
                    ggsize(1000, 1000) +
                    scaleXContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                    scaleYContinuous(breaks = (0..numberOfDays).map { it + 0.25 }, labels = (0..numberOfDays).map { (1 + it).toString() }, name = "Day") +
                    geomPoint(size = 4) { shape = "Name" } +
                    ggtitle("IITS Advent of Code $year (Stand ${date})") +
                    flavorDarcula()

        ggsave(p, "plot.png", path = ".")
    }

    fun plotB() {
        val data = createData(members.reversed());

        val p =
            letsPlot(data) { x = "Name"; y = "CompletedAtHourOfDay" } +
                    scaleYContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                    ggsize(1000, 1000) +
                    geomBoxplot { fill = "Name"; } +
                    ggtitle("IITS Advent of Code $year (Stand ${date})") +
                    theme().legendPositionNone() +
                    flavorSolarizedLight() +
                    coordFlip()

        ggsave(p, "plot2.png", path = ".")
    }


    fun plotC() {
        val data = createData(members.reversed());

        val p =
            letsPlot(data) { x = "Day"; y = "CompletedAtHourOfDay" } +
                    scaleYContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                    scaleXContinuous(breaks = (0..numberOfDays).map { it }, labels = (0..numberOfDays).map { (it).toString() }, name = "Day") +
                    ggsize(1000, 1000) +
                    geomBoxplot { fill = "Day" } +
                    ggtitle("IITS Advent of Code $year (Stand ${date})") +
                    theme().legendPositionNone() +
                    flavorSolarizedLight() +
                    coordFlip()

        ggsave(p, "plot3.png", path = ".")
    }

    fun plotD() {
        val eachCount = countStarsPerDayAndPart(members)

        val n = 25
        val part1 = eachCount.map { (_, v) -> v[1]!! }
        val part2 = eachCount.map { (_, v) -> v[2]!! }
        val data = mapOf(
            "Day" to eachCount.map { (k, _) -> k },
            "Part1" to part1,
            "Part2" to part2,
            "Part 1" to List(n) { "Part 1" },
            "Part 2" to List(n) { "Part 2" },
        )

        val p = letsPlot(data) { x = "Day" } +
                ggsize(1000, 1000) +
                scaleXDiscrete(name = "Day", breaks = (1..n).toList(), labels = (1..n).map(Int::toString).toList()) +
                scaleColorManual(listOf("dark_green", "orange"), naValue = "gray", name = "Part") +
                scaleFillDiscrete(name = "") +
                scaleYDiscrete(name = "Stars") +
                geomBar(stat = Stat.identity, width = 0.4, position = positionNudge(-0.2), showLegend = false) { y = "Part1"; fill = "Part 1" } +
                geomBar(stat = Stat.identity, width = 0.4, position = positionNudge(0.2)) { y = "Part2"; fill = "Part 2" } +
                ggtitle("IITS Advent of Code $year (Stand ${date})") +
                theme().legendPositionTop() +
                flavorSolarizedLight()

        ggsave(p, "plot4.png", path = ".")
    }
}

private fun countStarsPerDayAndPart(members: List<Member>): Map<Int, Map<Int, Int>> {
    val eachCount = members.flatMap { it.completionDayLevel }.groupBy { it.index }.mapValues { (_, days) ->
        val eachCount = days.flatMap { it.parts }.groupingBy { it.index }.eachCount().toMutableMap()
        eachCount.putIfAbsent(1, 0)
        eachCount.putIfAbsent(2, 0)
        eachCount.toMap()
    }.toMutableMap()
    for (i in 1..25) {
        eachCount.putIfAbsent(i, mapOf(1 to 0, 2 to 0))
    }
    return eachCount.toMap()
}

private fun createData(members: List<Member>) =
    mapOf(
        "Name" to members.flatMap { member ->
            List(member.stars) { member.name }
        },

        "HoursToComplete" to members.flatMap { member ->
            member.completionDayLevel
                .map { it.index to it.parts.map(Part::getStar) }
                .flatMap { (index, list) -> list.map { 24 * (it.dayOfMonth - index) + ((it.hour + (it.minute / 60.0) + (it.second / 3600) - 5) % 24) } }
        },

        "CompletedAtHourOfDay" to members.flatMap { member ->
            member.completionDayLevel
                .map { it.index to it.parts.map(Part::getStar) }
                .flatMap { (index, list) -> list.map { 24 * (it.dayOfMonth - index) + ((it.hour + (it.minute / 60.0) + (it.second / 3600) - 5) % 24) } }
        },

        "Y" to members.flatMapIndexed { memberIndex, member ->
            member.completionDayLevel
                .flatMapIndexed { index, day -> List(day.parts.size) { index + (memberIndex.toDouble() / members.size.toDouble() / 2.0) } }
        },

        "Day" to members.flatMapIndexed { memberIndex, member ->
            member.completionDayLevel
                .flatMapIndexed { index, day -> List(day.parts.size) { day.index } }
        },

        "Part" to members.flatMap { member ->
            member.completionDayLevel.flatMap { it.parts }.map { it.index }
        }
    )

fun main() {
    val jsonObject = Gson().fromJson(Files.readString(Paths.get("aoc.json")), JsonObject::class.java)

    val year = jsonObject.get("event").asString.let(Year::parse)

    val members =
        jsonObject
            .get("members")
            .asJsonObject
            .entrySet()
            .map { (id, element) -> Member.parse(element, id) }
            .sortedBy { it.name.lowercase() }

    val date: ZonedDateTime = members.maxOf { it.lastStar }.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("+1"))

    Plotter(members, date, year).run {
        plotA()
        plotB()
        plotC()
        plotD()
    }
}
