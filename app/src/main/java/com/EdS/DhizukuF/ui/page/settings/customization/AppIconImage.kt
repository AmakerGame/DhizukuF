package com.EdS.DhizukuF.ui.page.settings.customization

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.customization.AppearanceManager
import com.EdS.DhizukuF.customization.IconChoice
import com.google.accompanist.drawablepainter.rememberDrawablePainter

/**
 * The picture of an icon choice: the DhizukuF icon, the original Dhizuku icon, or a picture of the user.
 * With [tvBanner] the two built-in choices show their TV banner (the wide picture that a TV launcher shows).
 */
@Composable
fun AppIconImage(choice: IconChoice, modifier: Modifier = Modifier, tvBanner: Boolean = false) {
    val context = LocalContext.current
    if (tvBanner && choice !is IconChoice.Custom) {
        Image(
            painter = painterResource(if (choice == IconChoice.Original) R.drawable.banner else R.drawable.banner_dhizukuf),
            contentDescription = null,
            modifier = modifier
        )
        return
    }
    if (choice is IconChoice.Custom) {
        val bitmap = remember(choice.id) { AppearanceManager.loadCustomIcon(context, choice.id)?.asImageBitmap() }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
            return
        }
    }
    // an adaptive icon is a Drawable, not a bitmap, so it goes through a drawable painter
    val drawable = remember(choice) {
        ContextCompat.getDrawable(
            context,
            if (choice == IconChoice.Original) R.mipmap.ic_original else R.mipmap.ic_launcher
        )
    }
    Image(painter = rememberDrawablePainter(drawable), contentDescription = null, modifier = modifier)
}
