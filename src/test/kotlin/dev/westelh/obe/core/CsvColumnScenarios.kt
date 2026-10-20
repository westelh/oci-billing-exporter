package dev.westelh.obe.core

internal data class CsvColumnScenario(val name: String, val columns: List<Pair<String, String>>) {
    val csv: String = columns.joinToString(",") { it.first } + "\n" +
        columns.joinToString(",") { it.second } + "\n"
    val hasTags: Boolean = columns.any { it.first.startsWith("tags/") }
}

// Share input layouts between parser and exposition tests; keep their assertions independent.
internal object CsvColumnScenarios {
    private val resource = javaClass.getResource("/reports_cost-extra-columns-large.csv")!!
    // This synthetic fixture has no quoted commas or multiline fields.
    private val lines = resource.readText().lineSequence().take(2).toList()
    private val columns = lines[0].split(',').zip(lines[1].split(','))
    private val extraColumns = columns.filter {
        it.first == "cost/attributedCost" || it.first == "usage/attributedUsage"
    }
    private val originalColumns = columns - extraColumns.toSet()
    // Place distinct unknown values next to known usage to detect positional mapping errors.
    private val columnsWithExtrasInMiddle = originalColumns.flatMap { column ->
        if (column.first == "usage/billedQuantity") extraColumns + column else listOf(column)
    }

    val cases = listOf(
        CsvColumnScenario("original columns", originalColumns),
        CsvColumnScenario("unknown columns in the middle", columnsWithExtrasInMiddle),
        CsvColumnScenario("reordered columns and tags", columns.reversed()),
        CsvColumnScenario("missing optional tags", columns.filterNot { it.first.startsWith("tags/") })
    )
}
