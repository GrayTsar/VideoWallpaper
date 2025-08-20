package com.graytsar.livewallpaper.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.graytsar.livewallpaper.R
import com.graytsar.livewallpaper.compose.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentSettingsScreen : Fragment() {
    val viewModel: SettingsScreenViewModel by viewModels()

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
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                SettingsCategoryTitle(title = "Theme")
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                SettingsCategoryBackground {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwitchWithText(
                            text = stringResource(R.string.nameDarkMode),
                            subText = "Enable dark mode",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(bottom = 16.dp))

                SettingsCategoryTitle(title = stringResource(R.string.image))
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                SettingsCategoryBackground {
                    Column {
                        Text(
                            text = stringResource(R.string.nameScaleType),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Fit to Screen",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(bottom = 16.dp))

                SettingsCategoryTitle(title = stringResource(R.string.video))
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                SettingsCategoryBackground {
                    Column {
                        SwitchWithText(
                            text = stringResource(R.string.nameEnableAudio),
                            subText = "Enable audio playback",
                            checked = false,
                            onCheckedChange = {}
                        )
                        Spacer(modifier = Modifier.padding(bottom = 16.dp))
                        SwitchWithText(
                            text = stringResource(R.string.nameCropVideo),
                            subText = "Fits the screen",
                            checked = false,
                            onCheckedChange = {}
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(bottom = 8.dp))

                SettingsCategoryTitle(title = "Other")
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
                SettingsCategoryBackground {
                    Column {
                        SwitchWithText(
                            text = stringResource(R.string.nameDoubleTapToPause),
                            subText = "Pause playback on double tap",
                            checked = false,
                            onCheckedChange = {}
                        )
                        Spacer(modifier = Modifier.padding(bottom = 16.dp))
                        SwitchWithText(
                            text = stringResource(R.string.namePlayOffscreen),
                            subText = "Playback is enabled when the screen is turned of",
                            checked = false,
                            onCheckedChange = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = Color.Gray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsCategoryTitle(
    title: String
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun SwitchWithText(
    text: String,
    subText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineBreak = LineBreak.Heading
                ),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private val gifScaleTypeTexts = arrayOf(
    R.string.scale_type_fit,
    R.string.scale_type_center,
    R.string.scale_type_original
)