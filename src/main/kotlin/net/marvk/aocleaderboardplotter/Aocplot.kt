package net.marvk.aocleaderboardplotter

import com.google.gson.Gson
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.min


data class Member(
    val id: Int,
    val localScore: Int,
    val lastStarTs: Long,
    val globalScore: Int,
    val stars: Int,
    val name: String,
    val completionDayLevel: List<Day>,
) {
    val lastStar by lazy { lastStarTs.toLocalDate() }
}

data class Day(val index: Int, val parts: List<Part>)
data class Part(val index: Int, val getStarTs: Long, val starIndex: Long) {
    val getStar by lazy { getStarTs.toLocalDate() }
}

fun main() {
    val fromJson = Gson().fromJson(Files.readString(Paths.get("aoc.json")), JsonObject::class.java)

    val members = fromJson.get("members").asJsonObject.entrySet().map { (id, element) ->

        val o = element.asJsonObject


        val Days = o.get("completion_day_level").asJsonObject.entrySet().map { (dayId, day) ->
            val i = dayId.toInt()

            i to Day(i,
                listOf(1, 2).mapNotNull {
                    day.asJsonObject.get(it.toString())?.asJsonObject?.toPart(it)
                }
            )
        }.sortedBy { it.first }
            .map { it.second }


        Member(
            id.toInt(),
            o.get("local_score").asInt,
            o.get("last_star_ts").asLong,
            o.get("global_score").asInt,
            o.get("stars").asInt,
            o.get("name").asString,
            Days,
        )
    }.sortedBy { it.name.lowercase() }

//    members.first { it.name.contains("ilia", ignoreCase = true) }.also {
//        it.completionDayLevel.forEach {
//            println(it.index)
//            it.parts.forEach { part ->
//                println(part)
//                println(part.getStar)
//                println()
//            }
//            println("---".repeat(100))
//        }
//    }

    val date: ZonedDateTime = members.maxOf { it.lastStar }.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("+1"))

    plotA(members, date)
    plotB(members, date)
    plotC(members, date)
    plotD(members, date)
//    plotSus(members)
}

private fun plotSus(members: List<Member>) {
    val n = 25

    val data: MutableMap<Any, Any> = mutableMapOf(
        "Day" to (1..n).toList()
    )

    fun generate(name: String): List<Long> {
        val r = (1..n)
            .map { members.single { it.name == name }.completionDayLevel.getOrNull(it - 1) }
            .map {
                it?.takeIf { it.parts.size == 2 }?.let {
                    min(Duration.between(it.parts[0].getStar, it.parts[1].getStar).seconds, 25000)
                } ?: 0
            }

        data += name to r
        data += name.uppercase() to List(n) { name }

        return r
    }

    generate("krankkk").map(Duration::ofSeconds).withIndex().forEach(::println)

    val names = listOf("krankkk", "jtheegarten-iits", "marvk", "sschellhoff", "kmees", "Andreas Schröder", "Igor")

    for (name in names) generate(name)

    var p = letsPlot(data) { x = "Day" } +
            ggsize(1200, 500) +
//            scaleXDiscrete(name = "Day", breaks = (1..n).toList(), labels = (1..n).map(Int::toString).toList()) +
            scaleColorManual(listOf("dark_green", "orange"), naValue = "gray", name = "Part") +
            scaleFillDiscrete(name = "") +
            scaleYDiscrete(name = "Seconds")

    for ((i, name) in names.withIndex()) {
        p = p + geomBar(stat = Stat.identity, width = 0.8 / names.size, position = positionNudge(-0.4 + (0.8 / names.size) * i)) { y = name; fill = name.uppercase() }
    }

    p = p + theme().legendPositionTop() +
            flavorSolarizedLight()


    ggsave(p, "sus.png", path = ".")
}

private fun plotD(members: List<Member>, date: ZonedDateTime?) {
    val eachCount = countStarsPerDayAndPart(members)

    for (e in eachCount) {
        println(e)
    }

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

    for (datum in data) {
        println(datum)
    }

    val p = letsPlot(data) { x = "Day" } +
            ggsize(1000, 1000) +
            scaleXDiscrete(name = "Day", breaks = (1..n).toList(), labels = (1..n).map(Int::toString).toList()) +
            scaleColorManual(listOf("dark_green", "orange"), naValue = "gray", name = "Part") +
            scaleFillDiscrete(name = "") +
            scaleYDiscrete(name = "Stars") +
            geomBar(stat = Stat.identity, width = 0.4, position = positionNudge(-0.2), showLegend = false) { y = "Part1"; fill = "Part 1" } +
            geomBar(stat = Stat.identity, width = 0.4, position = positionNudge(0.2)) { y = "Part2"; fill = "Part 2" } +
            theme().legendPositionTop() +
            flavorSolarizedLight()

    ggsave(p, "plot4.png", path = ".")
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

private fun plotA(members: List<Member>, date: ZonedDateTime?) {
    val data: Map<String, List<Any>> = createData(members);

    (0 until data.entries.first().value.size).map { i ->
        data.entries.map { it.key + ": " + it.value[i] }
    }.forEach(::println)

    val numberOfDays = members.map { it.completionDayLevel }.maxOf { it.size }

    var p =
        letsPlot(data) { x = "HoursToComplete"; y = "Y"; color = "Name" } +
                ggsize(1000, 1000) +
                scaleXContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                scaleYContinuous(breaks = (0..numberOfDays).map { it + 0.25 }, labels = (0..numberOfDays).map { (1 + it).toString() }, name = "Day") +
                geomPoint(size = 4) { shape = "Name" } +
                ggtitle("IITS Advent of Code 2022 (Stand ${date})") +
                flavorDarcula()

    ggsave(p, "plot.png", path = ".")
}

private fun plotB(members: List<Member>, date: ZonedDateTime) {
    val data = createData(members.reversed());

    var p2 =
        letsPlot(data) { x = "Name"; y = "CompletedAtHourOfDay" } +
                scaleYContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                ggsize(1000, 1000) +
                geomBoxplot { fill = "Name"; } +
                ggtitle("IITS Advent of Code 2022 (Stand ${date})") +
                theme().legendPositionNone() +
                flavorSolarizedLight() +
                coordFlip()

    ggsave(p2, "plot2.png", path = ".")
}


private fun plotC(members: List<Member>, date: ZonedDateTime) {
    val data = createData(members.reversed());
    val numberOfDays = members.map { it.completionDayLevel }.maxOf { it.size }

    var p2 =
        letsPlot(data) { x = "Day"; y = "CompletedAtHourOfDay" } +
                scaleYContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
                scaleXContinuous(breaks = (0..numberOfDays).map { it }, labels = (0..numberOfDays).map { (it).toString() }, name = "Day") +
                ggsize(1000, 1000) +
                geomBoxplot { fill = "Day" } +
                ggtitle("IITS Advent of Code 2022 (Stand ${date})") +
                theme().legendPositionNone() +
                flavorSolarizedLight() +
                coordFlip()

    ggsave(p2, "plot3.png", path = ".")
}

//private fun plotD(members: List<Member>, date: ZonedDateTime) {
//    val data = createData(members.reversed());
//    val numberOfDays = members.map { it.completionDayLevel }.maxOf { it.size }
//
//    var p2 =
//        letsPlot(data) { x = "Day"; y = "CompletedAtHourOfDay" } +
//                scaleYContinuous(breaks = listOf(0, 6, 12, 18, 24), labels = listOf("6", "12", "18", "24", "6"), limits = 0 to 24, name = "Hour") +
//                scaleXContinuous(breaks = (0..numberOfDays).map { it }, labels = (0..numberOfDays).map { (it).toString() }, name = "Day") +
//                ggsize(1000, 1000) +
//                geomBoxplot { fill = "Day" } +
//                ggtitle("IITS Advent of Code 2022 (Stand ${date})") +
//                theme().legendPositionNone() +
//                flavorSolarizedLight() +
//                coordFlip()
//
//    ggsave(p2, "plot3.png", path = ".")
//}


private fun createData(members: List<Member>) =
    mapOf(
        "Name" to members.flatMap { member ->
            List(member.stars) { member.name }
        },

        "HoursToComplete" to members.flatMap { member ->
            member.completionDayLevel
                .map { it.index to it.parts.map { it.getStar } }
                .flatMap { (index, list) -> list.map { 24 * (it.dayOfMonth - index) + ((it.hour + (it.minute / 60.0) + (it.second / 3600) - 5) % 24) } }
        },

        "CompletedAtHourOfDay" to members.flatMap { member ->
            member.completionDayLevel
                .map { it.index to it.parts.map { it.getStar } }
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

private fun Long.toLocalDate(): LocalDateTime = LocalDateTime.ofEpochSecond(this, 0, ZoneOffset.UTC)

private fun JsonObject.toPart(index: Int) =
    Part(
        index,
        get("get_star_ts").asLong,
        get("star_index").asLong,
    )

