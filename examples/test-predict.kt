import com.bomcomes.calculator.helpers.CycleCalculator
import kotlinx.datetime.LocalDate

fun main() {
    val lastTheDayStart = LocalDate(2025, 1, 5)
    val fromDate = LocalDate(2025, 1, 1)
    val toDate = LocalDate(2025, 2, 28)
    val period = 28
    val days = 5
    
    println("Testing predictInRange:")
    println("  lastTheDayStart: $lastTheDayStart")
    println("  fromDate: $fromDate")
    println("  toDate: $toDate")
    println("  period: $period")
    println("  days: $days")
    
    val predictDays = CycleCalculator.predictInRange(
        isPredict = true,
        lastTheDayStart = lastTheDayStart,
        fromDate = fromDate,
        toDate = toDate,
        period = period,
        rangeStart = 0,
        rangeEnd = days - 1,
        delayTheDays = 0
    )
    
    println("\nResult: $predictDays")
}
