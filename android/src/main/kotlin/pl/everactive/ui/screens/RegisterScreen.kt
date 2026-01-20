package pl.everactive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String, String) -> Unit,
    onBackToLoginClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Role dropdown
    var selectedRole by remember { mutableStateOf("") }
    var isRoleExpanded by remember { mutableStateOf(false) }
    val roles = listOf("Employee", "Supervisor")

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Custom colors (same as Login)
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.outline,
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.outline,
        unfocusedLabelColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Dropdown
            ExposedDropdownMenuBox(
                expanded = isRoleExpanded,
                onExpandedChange = { isRoleExpanded = !isRoleExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Role") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = inputColors
                )

                ExposedDropdownMenu(
                    expanded = isRoleExpanded,
                    onDismissRequest = { isRoleExpanded = false }
                ) {
                    roles.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(text = item) },
                            onClick = {
                                selectedRole = item
                                isRoleExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = inputColors,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Register Button
            Button(
                onClick = {
                    if (password == confirmPassword) {
                        onRegisterSuccess(username, selectedRole)
                    } else {
                        errorMessage = "Passwords do not match"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = username.isNotBlank() &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    selectedRole.isNotBlank()
            ) {
                Text("REGISTER")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login Link
            TextButton(onClick = onBackToLoginClick) {
                Text("Already have an account? Log in")
            }
        }
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    EverActiveTheme {
        RegisterScreen(onRegisterSuccess = { _, _ -> }, onBackToLoginClick = {})
    }
}
