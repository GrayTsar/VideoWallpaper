package com.graytsar.livewallpaper.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.graytsar.livewallpaper.R
import com.graytsar.livewallpaper.compose.AppTheme

class FragmentSettingsScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SettingsScreen()
            }
        }
    }

    @Preview(
        showBackground = true
    )
    @Composable
    private fun SettingsScreen() {
        AppTheme {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Theme",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Row() {
                    Checkbox(
                        checked = true,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.nameDarkMode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = stringResource(R.string.image),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = stringResource(R.string.nameScaleType),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Fit to Screen",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = stringResource(R.string.video),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Row() {
                    Checkbox(
                        checked = false,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.nameEnableAudio),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Row() {
                    Checkbox(
                        checked = false,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.nameCropVideo),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = "Other",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row() {
                    Checkbox(
                        checked = false,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.nameDoubleTapToPause),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                Row() {
                    Checkbox(
                        checked = false,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.namePlayOffscreen),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private val gifScaleTypeTexts = arrayOf(
    R.string.scale_type_fit,
    R.string.scale_type_center,
    R.string.scale_type_original
)