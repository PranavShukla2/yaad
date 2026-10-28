package com.pranavshukla.yaad.ui.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

/**
 * A list with a detail pane. From 600 dp both panes show side by side; below that, [detail] replaces [list] while
 * [showDetail] is true.
 */
@Composable
fun ListDetailLayout(
    showDetail: Boolean,
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val listWidth = min(400.dp, maxWidth * 0.42f)
        if (WidthClass.of(maxWidth).isTwoPane) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
                VerticalDivider()
                Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
            }
        } else if (showDetail) {
            detail()
        } else {
            list()
        }
    }
}
