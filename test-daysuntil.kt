import kotlinx.datetime.*

fun main() {
    val jan5 = LocalDate(2025, 1, 5)
    val jan1 = LocalDate(2025, 1, 1)

    println("jan5.daysUntil(jan1) = ${jan5.daysUntil(jan1)}")
    println("jan1.daysUntil(jan5) = ${jan1.daysUntil(jan5)}")

    // Test modulo with negative
    val gap = jan5.daysUntil(jan1)
    println("gap % 28 = ${gap % 28}")
}
