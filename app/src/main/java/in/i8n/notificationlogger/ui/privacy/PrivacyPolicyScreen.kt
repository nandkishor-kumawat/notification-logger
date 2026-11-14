package `in`.i8n.notificationlogger.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import `in`.i8n.notificationlogger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows assets/privacy-policy.md, so the in-app text and the published policy are one file. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val blocks by produceState<List<Block>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("privacy-policy.md").bufferedReader().use { parseMarkdownBlocks(it.readText()) }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(blocks) { block ->
                when (block.type) {
                    BlockType.TITLE -> Text(styled(block.text), style = MaterialTheme.typography.headlineSmall)
                    BlockType.HEADING -> Text(
                        styled(block.text),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    BlockType.PARAGRAPH -> Text(styled(block.text), style = MaterialTheme.typography.bodyMedium)
                    BlockType.BULLET -> Row {
                        Text("•", Modifier.padding(end = 8.dp, start = 4.dp))
                        Text(styled(block.text), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val INLINE = Regex("""\*\*(.+?)\*\*|_(.+?)_|`(.+?)`""")

/** Applies **bold**, _italic_ and `code` spans. */
private fun styled(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE.findAll(text)) {
        append(text.substring(last, m.range.first))
        val (bold, italic, code) = m.destructured
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            else -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(code) }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
