package com.EdS.DhizukuF.ui.page.settings.customization

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.customization.AppName
import com.EdS.DhizukuF.customization.IconChoice
import com.google.accompanist.drawablepainter.rememberDrawablePainter

/** Launcher icon preview (adaptive icons are Drawables). */
@Composable
fun AppIconImage(choice: IconChoice, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable = remember(choice) {
        val res = when (choice) {
            IconChoice.Original -> R.mipmap.ic_original
            IconChoice.Material3 -> R.mipmap.ic_material3
            IconChoice.Default -> R.mipmap.ic_launcher
        }
        ContextCompat.getDrawable(context, res)
    }
    Image(painter = rememberDrawablePainter(drawable), contentDescription = null, modifier = modifier)
}

/** The wide TV banner of a launcher entry: what a TV launcher shows for this name and icon. */
@Composable
fun TvBannerImage(name: AppName, icon: IconChoice, modifier: Modifier = Modifier) {
    Image(painter = painterResource(bannerRes(name, icon)), contentDescription = null, modifier = modifier)
}

@DrawableRes
fun bannerRes(name: AppName, icon: IconChoice): Int {
    // Material3 reuses the "new" TV banners; Original keeps the classic ones.
    val original = icon == IconChoice.Original
    return when (name) {
        AppName.DhizukuF -> if (original) R.drawable.banner_dhizukuf_orig else R.drawable.banner_dhizukuf_new
        AppName.Dhizuku -> if (original) R.drawable.banner_dhizuku_orig else R.drawable.banner_dhizuku_new
        AppName.ProfileOwner -> if (original) R.drawable.banner_profile_owner_orig else R.drawable.banner_profile_owner_new
        AppName.DeviceOwner -> if (original) R.drawable.banner_device_owner_orig else R.drawable.banner_device_owner_new
    }
}
