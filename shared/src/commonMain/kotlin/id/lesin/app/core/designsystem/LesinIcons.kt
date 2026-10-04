package id.lesin.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LesinIcons {
    val Add = icon("Add") {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }
    val Back = icon("Back") {
        moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f)
    }
    val Chevron = icon("Chevron") {
        moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
    }
    val Search = icon("Search") {
        moveTo(17f, 10f)
        arcTo(7f, 7f, 0f, true, true, 3f, 10f)
        arcTo(7f, 7f, 0f, true, true, 17f, 10f)
        moveTo(15f, 15f); lineTo(21f, 21f)
    }
    val Home = icon("Home") {
        moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f)
        moveTo(5f, 9f); lineTo(5f, 21f); lineTo(10f, 21f)
        lineTo(10f, 15f); lineTo(14f, 15f); lineTo(14f, 21f)
        lineTo(19f, 21f); lineTo(19f, 9f)
    }
    val Students = icon("Students") {
        moveTo(15f, 7f)
        arcTo(4f, 4f, 0f, true, true, 7f, 7f)
        arcTo(4f, 4f, 0f, true, true, 15f, 7f)
        moveTo(3f, 21f); lineTo(3f, 19f)
        curveTo(3f, 13f, 19f, 13f, 19f, 19f); lineTo(19f, 21f)
        moveTo(19f, 4f); curveTo(23f, 5f, 23f, 10f, 19f, 11f)
    }
    val Billing = icon("Billing") {
        moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f)
        lineTo(15f, 19f); lineTo(12f, 21f); lineTo(9f, 19f)
        lineTo(6f, 21f); close()
        moveTo(9f, 8f); lineTo(15f, 8f)
        moveTo(9f, 12f); lineTo(15f, 12f)
    }

    private fun icon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = draw,
            )
        }.build()
}
