package `in`.i8n.notificationlogger.ui.privacy

enum class BlockType { TITLE, HEADING, BULLET, PARAGRAPH }

data class Block(val type: BlockType, val text: String)

/** Parses the small Markdown subset used by the privacy policy: `#`, `##`, `- ` and paragraphs. */
fun parseMarkdownBlocks(markdown: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotEmpty()) blocks += Block(BlockType.PARAGRAPH, paragraph.toString().trim())
        paragraph.clear()
    }
    for (raw in markdown.lines()) {
        val line = raw.trimEnd()
        when {
            line.isBlank() -> flush()
            line.startsWith("## ") -> { flush(); blocks += Block(BlockType.HEADING, line.drop(3)) }
            line.startsWith("# ") -> { flush(); blocks += Block(BlockType.TITLE, line.drop(2)) }
            line.startsWith("- ") -> { flush(); blocks += Block(BlockType.BULLET, line.drop(2)) }
            else -> paragraph.append(line.trim()).append(' ')
        }
    }
    flush()
    return blocks
}
