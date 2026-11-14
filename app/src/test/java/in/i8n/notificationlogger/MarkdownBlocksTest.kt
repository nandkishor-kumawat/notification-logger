package `in`.i8n.notificationlogger

import `in`.i8n.notificationlogger.ui.privacy.Block
import `in`.i8n.notificationlogger.ui.privacy.BlockType
import `in`.i8n.notificationlogger.ui.privacy.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownBlocksTest {
    @Test
    fun parsesTitleHeadingsBulletsAndJoinsParagraphLines() {
        val md = "# Title\n\n_Updated_\n\n## Section\n\nline one\nline two\n\n- a\n- b\n"
        assertEquals(
            listOf(
                Block(BlockType.TITLE, "Title"),
                Block(BlockType.PARAGRAPH, "_Updated_"),
                Block(BlockType.HEADING, "Section"),
                Block(BlockType.PARAGRAPH, "line one line two"),
                Block(BlockType.BULLET, "a"),
                Block(BlockType.BULLET, "b"),
            ),
            parseMarkdownBlocks(md),
        )
    }
}
