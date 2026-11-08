package com.EdS.DhizukuF.ui.page.settings.oss

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.ui.theme.exclude
import com.EdS.DhizukuF.ui.widget.TvIconButton
import org.json.JSONObject

@Immutable
data class OssLicense(val name: String, val url: String)

@Immutable
data class OssLibrary(
    val name: String,
    val website: String,
    val licenses: List<OssLicense>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssLicensesPage(
    windowInsets: WindowInsets,
    navController: NavController
) {
    val context = LocalContext.current
    val libraries by produceState<List<OssLibrary>>(emptyList()) {
        value = runCatching {
            context.resources.openRawResource(R.raw.licenses).use { stream ->
                val root = JSONObject(stream.bufferedReader().readText())
                val array = root.optJSONArray("libraries") ?: return@use emptyList()
                buildList {
                    for (i in 0 until array.length()) {
                        val lib = array.optJSONObject(i) ?: continue
                        val licArr = lib.optJSONArray("licenses")
                        val licenses = buildList {
                            if (licArr != null) {
                                for (j in 0 until licArr.length()) {
                                    val l = licArr.optJSONObject(j) ?: continue
                                    add(OssLicense(l.optString("name"), l.optString("url")))
                                }
                            }
                        }
                        add(
                            OssLibrary(
                                name = lib.optString("name"),
                                website = lib.optString("website"),
                                licenses = licenses
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(windowInsets.exclude(WindowInsetsSides.Bottom))
            .fillMaxSize(),
        contentWindowInsets = windowInsets.only(WindowInsetsSides.Bottom),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TvIconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.TwoTone.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.oss_licenses)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(libraries, key = { it.name }) { lib ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = lib.website.isNotBlank()) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(lib.website))
                                )
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(lib.name, style = MaterialTheme.typography.titleMedium)
                    if (lib.licenses.isNotEmpty()) {
                        Text(
                            lib.licenses.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
