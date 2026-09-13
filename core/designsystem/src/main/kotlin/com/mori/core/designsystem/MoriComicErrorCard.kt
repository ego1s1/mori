package com.mori.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mori.core.model.ComicError

/**
 * Error card for unreadable comics: one message source for every screen
 * (detail, reader) so the copy can never drift between them. Actions stay
 * per-screen: detail offers retry/remove, the reader offers none.
 */
@Composable
fun MoriComicErrorCard(
    error: ComicError,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    loading: Boolean = false,
) {
    MoriErrorCard(
        body = stringResource(
            when (error) {
                ComicError.CORRUPT -> R.string.mori_error_corrupt
                ComicError.PASSWORD_REQUIRED -> R.string.mori_error_password
                ComicError.EMPTY -> R.string.mori_error_empty
                ComicError.UNSUPPORTED -> R.string.mori_error_unsupported
            },
        ),
        primaryLabel = primaryLabel,
        onPrimary = onPrimary,
        secondaryLabel = secondaryLabel,
        onSecondary = onSecondary,
        loading = loading,
        modifier = modifier,
    )
}
