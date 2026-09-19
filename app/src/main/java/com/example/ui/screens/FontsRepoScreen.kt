package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.FontHelper
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun FontsRepoScreen(
    fontsList: List<FontItem>,
    onInstallFont: (name: String, category: String) -> Unit,
    onRemoveFont: (String) -> Unit,
    onClose: () -> Unit
) {
    var testSampleText by remember { mutableStateOf("GALLERY PRO 2026 // FAST & LIGHTWEIGHT") }
    var showInstallDialog by remember { mutableStateOf(false) }
    var customFontName by remember { mutableStateOf("") }
    var customFontCategory by remember { mutableStateOf("Custom User Font") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fonts_repo_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onClose,
                    backgroundColor = NeoWhite,
                    testTag = "fonts_btn_back"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FREE FONTS REPO",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                    Text(
                        text = "${fontsList.size} fonts active • Built-in Studio",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            NeoButton(
                text = "INSTALL FONT",
                onClick = { showInstallDialog = true },
                containerColor = NeoCyan,
                leadingIcon = Icons.Default.Add,
                testTag = "btn_install_font_top"
            )
        }

        // Live Test Text Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(NeoWhite, RoundedCornerShape(10.dp))
                .border(2.dp, NeoBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "LIVE TYPOGRAPHY TESTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = testSampleText,
                    onValueChange = { testSampleText = it },
                    placeholder = { Text("Type custom preview text...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeoDark,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = NeoBg,
                        unfocusedContainerColor = NeoBg
                    )
                )
            }
        }

        // Fonts List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(fontsList, key = { it.id }) { font ->
                NeoCard(
                    backgroundColor = NeoWhite,
                    shadowOffset = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = font.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = NeoDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                NeoBadge(
                                    text = font.category.uppercase(),
                                    backgroundColor = if (font.isCustomInstalled) NeoPink else NeoYellow,
                                    textColor = if (font.isCustomInstalled) NeoWhite else NeoDark
                                )
                            }

                            if (font.isCustomInstalled) {
                                NeoIconButton(
                                    icon = Icons.Default.Delete,
                                    contentDescription = "Remove Font",
                                    onClick = { onRemoveFont(font.id) },
                                    backgroundColor = NeoWhite,
                                    size = 32.dp,
                                    shadowOffset = 1.dp
                                )
                            } else {
                                NeoBadge(
                                    text = "READY",
                                    backgroundColor = NeoMint,
                                    textColor = NeoDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(NeoBorder.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Preview Rendered in the Font
                        Text(
                            text = if (testSampleText.isNotBlank()) testSampleText else font.previewText,
                            fontSize = 20.sp,
                            fontFamily = FontHelper.getFontFamilyForId(font.id),
                            fontWeight = FontHelper.getFontWeightForId(font.id),
                            fontStyle = FontHelper.getFontStyleForId(font.id),
                            color = NeoDark,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
        }

        // Install Dialog
        if (showInstallDialog) {
            AlertDialog(
                onDismissRequest = { showInstallDialog = false },
                title = {
                    Text(
                        text = "INSTALL CUSTOM FONT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Add any font of your choice into the local repository so it is instantly available in the photo editor:",
                            fontSize = 13.sp,
                            color = NeoDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customFontName,
                            onValueChange = { customFontName = it },
                            placeholder = { Text("e.g. Helvetica Neue Pro, Bebas Neue") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeoDark,
                                unfocusedBorderColor = NeoBorder,
                                focusedContainerColor = NeoWhite,
                                unfocusedContainerColor = NeoWhite
                            )
                        )
                    }
                },
                confirmButton = {
                    NeoButton(
                        text = "INSTALL TO STUDIO",
                        onClick = {
                            if (customFontName.isNotBlank()) {
                                onInstallFont(customFontName, customFontCategory)
                                customFontName = ""
                                showInstallDialog = false
                            }
                        },
                        containerColor = NeoMint
                    )
                },
                dismissButton = {
                    NeoButton(
                        text = "CANCEL",
                        onClick = { showInstallDialog = false },
                        containerColor = NeoWhite
                    )
                },
                containerColor = NeoBg,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
