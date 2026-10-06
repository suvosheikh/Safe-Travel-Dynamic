package com.safetravel.tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetravel.tracker.R
import com.safetravel.tracker.ui.components.GlassBackground
import com.safetravel.tracker.ui.components.GlassColors
import com.safetravel.tracker.ui.theme.SafeTravelTheme
import com.safetravel.tracker.ui.theme.Slate300
import com.safetravel.tracker.ui.theme.Slate400
import com.safetravel.tracker.ui.theme.Slate500
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    SafeTravelTheme {
        LoginView(vm = viewModel())
    }
}

enum class AuthState {
    LOGIN,
    SIGN_UP,
    EMAIL_SENT,
    VERIFICATION
}

@Composable
fun BrandHeaderRow(isCompact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.safetravel),
            contentDescription = "Safe Travel Logo",
            modifier = Modifier.size(if (isCompact) 26.dp else 30.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Safe Travel",
            fontSize = if (isCompact) 18.sp else 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun EnvelopeIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scale = 0.75f
            val ew = w * scale
            val eh = h * scale * 0.7f
            val left = (w - ew) / 2
            val top = (h - eh) / 2

            val envelopePath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = left + ew,
                        bottom = top + eh,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                    )
                )
            }

            // Draw Backdrop shadow
            drawPath(
                path = envelopePath,
                color = Color(0x22FFFFFF)
            )

            // Draw envelope border stroke
            drawPath(
                path = envelopePath,
                color = Color.White.copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            // Draw flap folding
            val flapPath = Path().apply {
                moveTo(left, top)
                lineTo(w / 2, top + (eh * 0.45f))
                lineTo(left + ew, top)
            }
            drawPath(
                path = flapPath,
                color = Color.White.copy(alpha = 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            // Draw lower cross shadows
            val foldPath = Path().apply {
                moveTo(left, top + eh)
                lineTo(w / 2, top + (eh * 0.42f))
                lineTo(left + ew, top + eh)
            }
            drawPath(
                path = foldPath,
                color = Color.White.copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )

            // Active node badge
            val badgeX = left + ew - 6f
            val badgeY = top + 6f
            val badgeRadius = 13f

            drawCircle(
                color = Color(0xFFEC4899).copy(alpha = 0.3f),
                radius = badgeRadius * 2.0f,
                center = androidx.compose.ui.geometry.Offset(badgeX, badgeY)
            )
            drawCircle(
                color = Color(0xFFEC4899),
                radius = badgeRadius,
                center = androidx.compose.ui.geometry.Offset(badgeX, badgeY)
            )
        }
    }
}

// Glassmorphic Modern Text Input (100% Borderless)
@Composable
fun SafeTravelAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 3.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp, color = Color.White),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x24FFFFFF),
                unfocusedContainerColor = Color(0x14FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// Glassmorphic Google Button (100% Borderless)
@Composable
fun GlassGoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleLogoIcon(modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Non-shifting primary action button
@Composable
fun PrimaryActionButton(
    text: String,
    isLoading: Boolean,
    loadingText: String,
    height: Dp = 44.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6B53FF), Color(0xFF4F83FF))
                )
            )
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loadingText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            )
        }
    }
}

// Glassmorphic OTP Box (100% Borderless)
@Composable
fun GlassDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 1) onValueChange(input)
        },
        modifier = modifier
            .size(48.dp, 52.dp)
            .clip(RoundedCornerShape(12.dp)),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0x2EFFFFFF),
            unfocusedContainerColor = Color(0x18FFFFFF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.google),
        contentDescription = "Google Logo",
        modifier = modifier.size(18.dp),
        tint = Color.Unspecified
    )
}

@Composable
fun LoginView(vm: SafeTravelViewModel) {
    val email by vm.emailInput.collectAsState()
    val password by vm.passwordInput.collectAsState()
    val name by vm.nameInput.collectAsState()

    val isAuthLoading by vm.isAuthLoading.collectAsState()
    val feedbackMessage by vm.actionFeedbackMessage.collectAsState()

    var authState by remember { mutableStateOf(AuthState.LOGIN) }
    var confirmPassword by remember { mutableStateOf("") }

    // Intercept hardware/gesture back press when not on primary LOGIN state
    BackHandler(enabled = authState != AuthState.LOGIN) {
        authState = AuthState.LOGIN
        vm.actionFeedbackMessage.value = null
    }

    // Keep VM sign up mode in sync with the interactive UI AuthState
    LaunchedEffect(authState) {
        vm.isSignUpMode.value = (authState == AuthState.SIGN_UP)
    }

    // OTP digits initialized clean (empty strings)
    var d1 by remember { mutableStateOf("") }
    var d2 by remember { mutableStateOf("") }
    var d3 by remember { mutableStateOf("") }
    var d4 by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    val isCompact = screenHeight < 720 || screenWidth < 360

    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = if (isCompact) 20.dp else 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = if (isCompact) 10.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                    // Logo and branding text
                    BrandHeaderRow(
                        isCompact = isCompact,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Feedback message pill (100% Borderless)
                    if (feedbackMessage != null) {
                        feedbackMessage?.let { msg ->
                            val isErr = msg.startsWith("Error") || msg.contains("Failed") || msg.contains("do not match") || msg.contains("least 6")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isErr) Color(0x33EF4444) else Color(0x3310B981))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isErr) Icons.Default.Error else Icons.Default.CheckCircle,
                                        contentDescription = "Status",
                                        tint = if (isErr) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        fontSize = 11.5.sp,
                                        color = if (isErr) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // SWITCHABLE AUTH STATE RENDERING
                    when (authState) {
                        AuthState.LOGIN -> {
                            Text(
                                text = "Welcome!",
                                fontSize = if (isCompact) 19.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Please enter your details to sign in",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            SafeTravelAuthField(
                                value = email,
                                onValueChange = { vm.emailInput.value = it },
                                label = "Email",
                                placeholder = "Email address",
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            SafeTravelAuthField(
                                value = password,
                                onValueChange = { vm.passwordInput.value = it },
                                label = "Password",
                                placeholder = "•••••••••••••",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            PrimaryActionButton(
                                text = "Log In",
                                isLoading = isAuthLoading,
                                loadingText = "Signing in...",
                                height = 44.dp,
                                onClick = {
                                    if (email.isBlank()) {
                                        vm.actionFeedbackMessage.value = "Error: Please enter your email address"
                                    } else if (password.isBlank()) {
                                        vm.actionFeedbackMessage.value = "Error: Please enter your password"
                                    } else {
                                        vm.submitAuthentication()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Forgotten Password?",
                                fontSize = 12.sp,
                                color = Color(0xFFF472B6),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        vm.actionFeedbackMessage.value = null
                                        authState = AuthState.EMAIL_SENT
                                    }
                                    .padding(vertical = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            GlassGoogleButton(
                                text = "Sign in with Google",
                                onClick = {
                                    vm.actionFeedbackMessage.value = "Google Sign-in is coming soon in the next update."
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Don't have an account? ", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text(
                                    text = "Sign up for free",
                                    color = Color(0xFFF472B6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        vm.actionFeedbackMessage.value = null
                                        authState = AuthState.SIGN_UP
                                    }
                                )
                            }
                        }

                        AuthState.SIGN_UP -> {
                            Text(
                                text = "Create Account",
                                fontSize = if (isCompact) 19.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Join Safe Travel to secure your journeys",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            SafeTravelAuthField(
                                value = name,
                                onValueChange = { vm.nameInput.value = it },
                                label = "Full Name",
                                placeholder = "Enter your full name",
                                leadingIcon = Icons.Default.Person,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            SafeTravelAuthField(
                                value = email,
                                onValueChange = { vm.emailInput.value = it },
                                label = "Email",
                                placeholder = "example@domain.com",
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            SafeTravelAuthField(
                                value = password,
                                onValueChange = { vm.passwordInput.value = it },
                                label = "Password",
                                placeholder = "At least 6 characters",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            SafeTravelAuthField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Confirm Password",
                                placeholder = "Repeat password",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            PrimaryActionButton(
                                text = "Sign Up",
                                isLoading = isAuthLoading,
                                loadingText = "Creating account...",
                                height = 44.dp,
                                onClick = {
                                    if (name.isBlank()) {
                                        vm.actionFeedbackMessage.value = "Error: Please enter your name"
                                    } else if (email.isBlank()) {
                                        vm.actionFeedbackMessage.value = "Error: Please enter your email address"
                                    } else if (password.length < 6) {
                                        vm.actionFeedbackMessage.value = "Error: Password must be at least 6 characters"
                                    } else if (password != confirmPassword) {
                                        vm.actionFeedbackMessage.value = "Error: Passwords do not match!"
                                    } else {
                                        vm.submitAuthentication()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            GlassGoogleButton(
                                text = "Sign up with Google",
                                onClick = {
                                    vm.actionFeedbackMessage.value = "Google Sign-in is coming soon in the next update."
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Already have an account? ", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text(
                                    text = "Log in",
                                    color = Color(0xFFF472B6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        vm.actionFeedbackMessage.value = null
                                        authState = AuthState.LOGIN
                                    }
                                )
                            }
                        }

                        AuthState.EMAIL_SENT -> {
                            EnvelopeIllustration(modifier = Modifier.size(68.dp).padding(vertical = 2.dp))

                            Text(
                                text = "Email Sent",
                                fontSize = if (isCompact) 19.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "The email may take a few minutes to arrive. Follow the link inside to verify and reset your password.",
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            PrimaryActionButton(
                                text = "Continue",
                                isLoading = false,
                                loadingText = "",
                                height = 44.dp,
                                onClick = {
                                    vm.actionFeedbackMessage.value = null
                                    authState = AuthState.VERIFICATION
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Back to Login",
                                fontSize = 12.5.sp,
                                color = Color(0xFFF472B6),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    vm.actionFeedbackMessage.value = null
                                    authState = AuthState.LOGIN
                                }
                            )
                        }

                        AuthState.VERIFICATION -> {
                            EnvelopeIllustration(
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(vertical = 2.dp)
                            )

                            Text(
                                text = "We just emailed you",
                                fontSize = if (isCompact) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Please enter the 4-digit code sent to your email.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Text(
                                text = "Confirmation code",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                textAlign = TextAlign.Start
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassDigitBox(value = d1, onValueChange = { d1 = it })
                                GlassDigitBox(value = d2, onValueChange = { d2 = it })
                                GlassDigitBox(value = d3, onValueChange = { d3 = it })
                                GlassDigitBox(value = d4, onValueChange = { d4 = it })
                            }

                            PrimaryActionButton(
                                text = "Verify Code",
                                isLoading = isAuthLoading,
                                loadingText = "Verifying...",
                                height = 44.dp,
                                onClick = {
                                    val code = "$d1$d2$d3$d4"
                                    if (code.length < 4) {
                                        vm.actionFeedbackMessage.value = "Error: Please enter all 4 digits"
                                    } else {
                                        vm.actionFeedbackMessage.value = "Verification successful! Welcome back."
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resend code",
                                    color = Color(0xFFF472B6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        vm.actionFeedbackMessage.value = "New verification token sent to registered email!"
                                    }
                                )
                                Text(" • ", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                Text(
                                    text = "Back to Login",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.clickable {
                                        vm.actionFeedbackMessage.value = null
                                        authState = AuthState.LOGIN
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}
