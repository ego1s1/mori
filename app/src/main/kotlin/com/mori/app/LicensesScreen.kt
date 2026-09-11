package com.mori.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** App-wide open-source licenses, generated at build time (no Soon badge). */
@Serializable
object LicensesRoute

fun NavController.navigateToLicenses() {
    navigate(LicensesRoute) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.licensesScreen(onBackClick: () -> Unit) {
    composable<LicensesRoute> {
        LicensesRouteContent(onBackClick = onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensesRouteContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<LicensesData?>(null) }
    var selected by remember { mutableStateOf<LibraryEntry?>(null) }
    LaunchedEffect(Unit) {
        data = withContext(Dispatchers.IO) { loadLicenses(context) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.licenses_title),
                        style = MoriEmphasized.headlineSmall,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = MoriIcons.Back,
                            contentDescription = stringResource(R.string.licenses_back),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) { padding ->
        val libraries = data?.libraries.orEmpty()
        if (data == null) {
            MoriLoading(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(libraries, key = { it.uniqueId }) { library ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = library.name.ifBlank { library.uniqueId },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            val sub = listOfNotNull(
                                library.artifactVersion,
                                library.organization?.name,
                            ).joinToString(" • ").ifBlank { null }
                            if (sub != null) {
                                Text(
                                    text = sub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = MoriIcons.Forward,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable(
                            onClick = { selected = library },
                            role = Role.Button,
                        ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
    val current = selected
    if (current != null && data != null) {
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = current.name.ifBlank { current.uniqueId },
                    style = MoriEmphasized.titleLarge,
                )
                val sub = listOfNotNull(
                    current.artifactVersion,
                    current.organization?.name,
                    current.website,
                ).joinToString("\n")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                data?.licensesFor(current)?.forEach { license ->
                    Text(
                        text = license.name.ifBlank { license.spdxId.orEmpty() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        text = license.content?.takeIf { it.isNotBlank() }
                            ?: license.url.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** Generated `aboutlibraries.json` model (unknown fields ignored). */
@Serializable
internal data class LicensesData(
    val libraries: List<LibraryEntry> = emptyList(),
    val licenses: Map<String, LicenseEntry> = emptyMap(),
)

@Serializable
internal data class LibraryEntry(
    val uniqueId: String,
    val name: String = "",
    val artifactVersion: String? = null,
    val description: String? = null,
    val website: String? = null,
    val organization: Organization? = null,
    val licenses: List<String> = emptyList(),
)

@Serializable
internal data class Organization(
    val name: String = "",
)

@Serializable
internal data class LicenseEntry(
    val name: String = "",
    val spdxId: String? = null,
    val url: String? = null,
    val content: String? = null,
)

internal fun LicensesData.licensesFor(library: LibraryEntry): List<LicenseEntry> =
    library.licenses.mapNotNull { licenses[it] }

private val licensesJson = Json { ignoreUnknownKeys = true }

internal fun parseLicenses(raw: String): LicensesData? =
    runCatching { licensesJson.decodeFromString<LicensesData>(raw) }.getOrNull()

internal fun loadLicenses(context: Context): LicensesData? {
    val raw = runCatching {
        context.resources.openRawResource(R.raw.aboutlibraries).use {
            it.readBytes().decodeToString()
        }
    }.getOrNull() ?: return null
    val parsed = parseLicenses(raw) ?: return null
    return parsed.copy(
        libraries = parsed.libraries.sortedBy { it.name.ifBlank { it.uniqueId }.lowercase() },
    )
}
