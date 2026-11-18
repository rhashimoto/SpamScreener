package com.shoestringresearch.spamscreener

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shoestringresearch.spamscreener.notification.ScreeningNotificationHelper
import com.shoestringresearch.spamscreener.ui.MainUiState
import com.shoestringresearch.spamscreener.ui.MainViewModel
import com.shoestringresearch.spamscreener.ui.PatternItemUiModel
import com.shoestringresearch.spamscreener.ui.theme.SpamScreenerTheme

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    ScreeningNotificationHelper.ensureNotificationChannel(this)

    setContent {
      SpamScreenerTheme {
        val uiState by viewModel.uiState.collectAsState()

        // Permissions and Role requesting
        val contactPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { /* Result handled, permission granted/denied */ }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { /* Result handled, permission granted/denied */ }

        val roleLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()
        ) { /* Role request handled */ }

        LaunchedEffect(Unit) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
          }

          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }

          val roleManager = getSystemService(RoleManager::class.java)
          if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
              val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
              roleLauncher.launch(intent)
            }
          }
        }

        ScreeningApp(
          uiState = uiState,
          onScreeningEnabledChanged = viewModel::onScreeningEnabledChanged,
          onAddPattern = viewModel::onAddPattern,
          onUpdatePattern = viewModel::onUpdatePattern,
          onRemovePattern = viewModel::onRemovePattern
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreeningApp(
  uiState: MainUiState,
  onScreeningEnabledChanged: (Boolean) -> Unit,
  onAddPattern: () -> Unit,
  onUpdatePattern: (Int, String) -> Unit,
  onRemovePattern: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Spam Screener") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Enable Screening Checkbox
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = uiState.isScreeningEnabled,
            onCheckedChange = onScreeningEnabledChanged
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Enable Screening",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Intercept incoming calls and screen against patterns",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      HorizontalDivider()

      // Screening Patterns Header & Add Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Screening Patterns",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Regular expressions for numbers to screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Button(onClick = onAddPattern) {
          Text("Add Pattern")
        }
      }

      if (uiState.patterns.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = "No screening patterns defined.\nClick \"Add Pattern\" above to add a regex pattern.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(
            items = uiState.patterns,
            key = { _, item -> item.id }
          ) { index, item ->
            PatternItemRow(
              index = index,
              item = item,
              onUpdate = { newText -> onUpdatePattern(index, newText) },
              onRemove = { onRemovePattern(index) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun PatternItemRow(
  index: Int,
  item: PatternItemUiModel,
  onUpdate: (String) -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  var textFieldValue by remember {
    mutableStateOf(
      TextFieldValue(
        text = item.pattern,
        selection = TextRange(item.pattern.length)
      )
    )
  }

  // Keep internal text in sync if pattern was changed externally (e.g. initial load or reset)
  if (textFieldValue.text != item.pattern) {
    textFieldValue = textFieldValue.copy(
      text = item.pattern,
      selection = TextRange(item.pattern.length)
    )
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = textFieldValue,
          onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != item.pattern) {
              onUpdate(newValue.text)
            }
          },
          label = { Text("Pattern ${index + 1}") },
          placeholder = { Text("e.g. ^\\+?1?800.*") },
          modifier = Modifier.weight(1f),
          isError = !item.validationResult.isValid,
          visualTransformation = remember(item.validationResult) {
            RegexErrorVisualTransformation(
              isValid = item.validationResult.isValid,
              errorIndex = item.validationResult.errorIndex
            )
          },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace
          )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(48.dp)
        ) {
          Text(
            text = "✕",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
          )
        }
      }

      if (!item.validationResult.isValid) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.validationResult.errorMessage?.let { "Error: $it" } ?: "Invalid regular expression",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}

/**
 * VisualTransformation that underlines the character at errorIndex in red.
 * If errorIndex points to or beyond the length of the string, it underlines the last character
 * (or the entire string if empty/single).
 */
class RegexErrorVisualTransformation(
  private val isValid: Boolean,
  private val errorIndex: Int
) : VisualTransformation {

  override fun filter(text: AnnotatedString): TransformedText {
    if (isValid || text.isEmpty()) {
      return TransformedText(text, OffsetMapping.Identity)
    }

    val annotatedString = buildAnnotatedString {
      append(text.text)

      val targetIndex = when {
        errorIndex in 0 until text.length -> errorIndex
        errorIndex >= text.length -> text.length - 1
        else -> 0 // If index is negative / unknown, underline whole string or from 0
      }

      val endIndex = if (errorIndex < 0) text.length else (targetIndex + 1).coerceAtMost(text.length)
      val startIndex = if (errorIndex < 0) 0 else targetIndex

      addStyle(
        style = SpanStyle(
          textDecoration = TextDecoration.Underline,
          color = Color.Red
        ),
        start = startIndex,
        end = endIndex
      )
    }

    return TransformedText(annotatedString, OffsetMapping.Identity)
  }
}
