package com.carife.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.profileAvatarColors
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CityPickerDialog
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSettingsScreen(
    viewModel: CarifeViewModel,
    onBack: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val profile = appState.profile
    val scope = rememberCoroutineScope()

    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var tagline by remember(profile.tagline) { mutableStateOf(profile.tagline) }
    var status by remember(profile.status) { mutableStateOf(profile.status) }
    var avatarColor by remember(profile.avatarColor) { mutableStateOf(profile.avatarColor) }
    var selectedCityId by remember(appState.selectedCity.id) { mutableStateOf(appState.selectedCity.id) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingPhotoUri = uri
            removePhoto = false
        }
    }

    val previewProfile = profile.copy(
        displayName = name,
        tagline = tagline,
        status = status,
        avatarColor = avatarColor,
        hasPhoto = when {
            pendingPhotoUri != null -> true
            removePhoto -> false
            else -> profile.hasPhoto
        },
    )
    val cityName = viewModel.cityRepository.byId(selectedCityId)?.name ?: appState.selectedCity.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Pengaturan Akun",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CarifeSurfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileAvatar(
                        profile = previewProfile,
                        size = 96.dp,
                        pendingPhotoUri = pendingPhotoUri.takeUnless { removePhoto },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { photoPicker.launch("image/*") }) {
                            Text("Pilih Foto")
                        }
                        if (previewProfile.hasPhoto) {
                            TextButton(
                                onClick = {
                                    pendingPhotoUri = null
                                    removePhoto = true
                                },
                            ) { Text("Hapus Foto") }
                        }
                    }
                    Text(
                        text = "Foto profil disimpan lokal di perangkat ini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CarifeSurfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Profil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        label = { Text("Nama tampilan") },
                        singleLine = true,
                        isError = name.isBlank(),
                        supportingText = {
                            if (name.isBlank()) {
                                Text("Nama wajib diisi")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it.take(70) },
                        label = { Text("Tagline") },
                        placeholder = { Text("Productive Workspace Seeker") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it.take(30) },
                        label = { Text("Status singkat") },
                        placeholder = { Text("Solo Deep Worker") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CarifeSurfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Warna avatar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Dipakai saat foto profil belum dipilih.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        profileAvatarColors.forEach { color ->
                            val selected = avatarColor == color
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = CarifePrimary,
                                        shape = CircleShape,
                                    )
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { avatarColor = color },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CarifeSurfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Kota utama",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            cityName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = { showCityPicker = true }) { Text("Ubah") }
                }
            }

            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val updated = previewProfile.copy(
                            displayName = name.trim(),
                            tagline = tagline.trim(),
                            status = status.trim(),
                        )
                        val saved = viewModel.saveAccount(
                            profile = updated,
                            cityId = selectedCityId,
                            photoUri = pendingPhotoUri.takeUnless { removePhoto },
                            removePhoto = removePhoto,
                        )
                        saving = false
                        if (saved) onBack()
                    }
                },
                enabled = !saving && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Simpan profil"
                    },
            ) {
                Text(if (saving) "Menyimpan..." else "Simpan Profil")
            }

            Text(
                text = "Profil, foto, dan kota disimpan lokal di perangkat ini.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            cities = viewModel.cityRepository.cities,
            selectedId = selectedCityId,
            onDismiss = { showCityPicker = false },
            onSelect = { cityId ->
                selectedCityId = cityId
                showCityPicker = false
            },
        )
    }
}
