package com.gothwad.accounts.ui.state

import androidx.lifecycle.ViewModel
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.ActivityControlItem
import com.gothwad.accounts.ui.model.DemoAccountProfile
import com.gothwad.accounts.ui.model.DeviceType
import com.gothwad.accounts.ui.model.EditableField
import com.gothwad.accounts.ui.model.FamilyMember
import com.gothwad.accounts.ui.model.PaymentMethodItem
import com.gothwad.accounts.ui.model.SavedPasswordEntry
import com.gothwad.accounts.ui.model.SearchableSetting
import com.gothwad.accounts.ui.model.SecurityActivityEvent
import com.gothwad.accounts.ui.model.SecurityRecommendation
import com.gothwad.accounts.ui.model.SignedInDevice
import com.gothwad.accounts.ui.model.SubscriptionItem
import com.gothwad.accounts.ui.model.ThirdPartyAppConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountDashboardViewModel : ViewModel() {

    private val mutableUiState = MutableStateFlow(createInitialDemoState())
    val uiState: StateFlow<AccountDashboardUiState> = mutableUiState.asStateFlow()

    fun syncCoreSignedInAccount(coreAccount: GothwadAccount?) {
        if (coreAccount == null) return
        mutableUiState.update { state ->
            val mapped = DemoAccountProfile(
                id = coreAccount.userId,
                name = coreAccount.name ?: coreAccount.username ?: "Pawan Gothwad",
                email = coreAccount.email ?: "pawangothwad@gmail.com",
                handle = "@${coreAccount.username ?: "pawangothwad"}",
                birthday = "14 August 1998",
                gender = "Male",
                pronouns = "he/him",
                phone = "+91 98765 ••••10",
                recoveryEmail = "pawan.recovery@gothwadtech.com",
                homeAddress = "Sector 62, Noida, Uttar Pradesh",
                workAddress = "DLF Cyber Hub, Gurugram",
                language = "English (India)",
                profilePubliclyVisible = true,
                useDemoAvatarArtwork = true,
                avatarInitials = (coreAccount.name ?: "PG").take(2).uppercase(),
            )
            val updatedAccounts = listOf(mapped) + state.accounts.filterNot { it.id == mapped.id }
            state.copy(
                accounts = updatedAccounts,
                activeAccountId = mapped.id,
                isLiveCoreAccountLinked = true,
            )
        }
    }

    fun selectTab(tab: AccountTab) {
        mutableUiState.update { it.copy(selectedTab = tab, subScreenStack = emptyList()) }
    }

    fun openSubScreen(subScreen: AccountSubScreen) {
        mutableUiState.update { it.copy(subScreenStack = it.subScreenStack + subScreen) }
    }

    fun navigateBack(): Boolean {
        var handled = false
        mutableUiState.update { state ->
            if (state.subScreenStack.isNotEmpty()) {
                handled = true
                state.copy(subScreenStack = state.subScreenStack.dropLast(1), searchQuery = "")
            } else {
                state
            }
        }
        return handled
    }

    fun updateSearchQuery(query: String) {
        mutableUiState.update { it.copy(searchQuery = query) }
    }

    fun toggleAccountSwitcher(show: Boolean) {
        mutableUiState.update { it.copy(showAccountSwitcher = show) }
    }

    fun switchAccount(accountId: String) {
        mutableUiState.update { state ->
            val target = state.accounts.firstOrNull { it.id == accountId } ?: return@update state
            state.copy(
                activeAccountId = accountId,
                showAccountSwitcher = false,
                snackbarMessage = "Switched to ${target.email}",
            )
        }
    }

    fun addDemoAccount(name: String, email: String) {
        val trimmedName = name.trim().ifEmpty { "Demo Account" }
        val trimmedEmail = email.trim().ifEmpty { "demo@gothwadtech.com" }
        val initials = trimmedName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifEmpty { "DA" }
        val newAccount = DemoAccountProfile(
            id = "demo-${System.currentTimeMillis()}",
            name = trimmedName,
            email = trimmedEmail,
            handle = "@${trimmedEmail.substringBefore("@")}",
            birthday = "1 January 2000",
            gender = "Prefer not to say",
            pronouns = "Not set",
            phone = "+91 90000 ••••01",
            recoveryEmail = "pawangothwad@gmail.com",
            homeAddress = "Not set",
            workAddress = "Not set",
            language = "English (India)",
            profilePubliclyVisible = false,
            useDemoAvatarArtwork = false,
            avatarInitials = initials,
        )
        mutableUiState.update { state ->
            state.copy(
                accounts = state.accounts + newAccount,
                activeAccountId = newAccount.id,
                showAccountSwitcher = false,
                snackbarMessage = "Added and switched to $trimmedEmail",
            )
        }
    }

    fun openEditField(field: EditableField?) {
        mutableUiState.update { it.copy(activeEditField = field) }
    }

    fun saveEditedField(field: EditableField, newValue: String) {
        val cleaned = newValue.trim()
        if (cleaned.isEmpty()) return
        mutableUiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id != state.activeAccountId) acc
                else when (field) {
                    EditableField.NAME -> acc.copy(name = cleaned)
                    EditableField.BIRTHDAY -> acc.copy(birthday = cleaned)
                    EditableField.GENDER -> acc.copy(gender = cleaned)
                    EditableField.PRONOUNS -> acc.copy(pronouns = cleaned)
                    EditableField.RECOVERY_EMAIL -> acc.copy(recoveryEmail = cleaned)
                    EditableField.RECOVERY_PHONE -> acc.copy(phone = cleaned)
                    EditableField.HOME_ADDRESS -> acc.copy(homeAddress = cleaned)
                    EditableField.WORK_ADDRESS -> acc.copy(workAddress = cleaned)
                    EditableField.LANGUAGE -> acc.copy(language = cleaned)
                }
            }
            state.copy(
                accounts = updatedAccounts,
                activeEditField = null,
                snackbarMessage = "${field.title} updated",
            )
        }
    }

    fun toggleProfileVisibility(visible: Boolean) {
        mutableUiState.update { state ->
            val updated = state.accounts.map {
                if (it.id == state.activeAccountId) it.copy(profilePubliclyVisible = visible) else it
            }
            state.copy(
                accounts = updated,
                snackbarMessage = if (visible) "Profile info visible to anyone" else "Profile info restricted",
            )
        }
    }

    fun toggleActivityControl(controlId: String) {
        mutableUiState.update { state ->
            val updated = state.activityControls.map { item ->
                if (item.id == controlId) item.copy(isEnabled = !item.isEnabled) else item
            }
            val changed = updated.firstOrNull { it.id == controlId }
            state.copy(
                activityControls = updated,
                snackbarMessage = changed?.let {
                    "${it.title} ${if (it.isEnabled) "turned on" else "paused"}"
                },
            )
        }
    }

    fun updateAutoDeletePeriod(controlId: String, period: String) {
        mutableUiState.update { state ->
            val updated = state.activityControls.map { item ->
                if (item.id == controlId) item.copy(autoDeleteLabel = period) else item
            }
            state.copy(
                activityControls = updated,
                snackbarMessage = "Auto-delete updated to $period",
            )
        }
    }

    fun togglePersonalizedAds(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                personalizedAdsEnabled = enabled,
                snackbarMessage = if (enabled) "Personalized ads enabled" else "Personalized ads turned off",
            )
        }
    }

    fun toggleLocationSharing(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                locationSharingEnabled = enabled,
                snackbarMessage = if (enabled) "Location sharing enabled" else "Location sharing turned off",
            )
        }
    }

    fun togglePartnerSharing(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                partnerSharingEnabled = enabled,
                snackbarMessage = if (enabled) "Partner sharing turned on" else "Partner sharing paused",
            )
        }
    }

    fun toggleTwoStepVerification(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                twoStepEnabled = enabled,
                snackbarMessage = if (enabled) "2-Step Verification is ON" else "2-Step Verification turned OFF",
            )
        }
    }

    fun toggleEnhancedSafeBrowsing(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                enhancedSafeBrowsing = enabled,
                snackbarMessage = if (enabled) "Enhanced Safe Browsing enabled" else "Enhanced Safe Browsing disabled",
            )
        }
    }

    fun addPasskey() {
        mutableUiState.update {
            it.copy(
                passkeyCount = it.passkeyCount + 1,
                securityEvents = listOf(
                    SecurityActivityEvent(
                        id = "evt-${System.currentTimeMillis()}",
                        title = "New hardware passkey created",
                        timestamp = "Just now",
                        location = "Noida, India",
                        device = "Pixel 9 Pro",
                    )
                ) + it.securityEvents,
                snackbarMessage = "Passkey created and synced to your Account",
            )
        }
    }

    fun regenerateBackupCodes() {
        mutableUiState.update {
            it.copy(
                backupCodesRemaining = 10,
                snackbarMessage = "10 fresh single-use backup codes generated",
            )
        }
    }

    fun resolveSecurityRecommendation(recId: String) {
        mutableUiState.update { state ->
            val updated = state.securityRecommendations.map {
                if (it.id == recId) it.copy(isResolved = true) else it
            }
            state.copy(
                securityRecommendations = updated,
                snackbarMessage = "Security recommendation resolved",
            )
        }
    }

    fun togglePrivacyCheckupStep(stepIndex: Int) {
        mutableUiState.update { state ->
            val next = if (stepIndex in state.privacyCheckupStepsDone) {
                state.privacyCheckupStepsDone - stepIndex
            } else {
                state.privacyCheckupStepsDone + stepIndex
            }
            state.copy(privacyCheckupStepsDone = next)
        }
    }

    fun signOutDevice(deviceId: String) {
        mutableUiState.update { state ->
            val target = state.devices.firstOrNull { it.id == deviceId } ?: return@update state
            if (target.isCurrentDevice) return@update state
            state.copy(
                devices = state.devices.filterNot { it.id == deviceId },
                securityEvents = listOf(
                    SecurityActivityEvent(
                        id = "evt-${System.currentTimeMillis()}",
                        title = "Signed out of ${target.name}",
                        timestamp = "Just now",
                        location = target.location,
                        device = target.name,
                    )
                ) + state.securityEvents,
                snackbarMessage = "Signed out from ${target.name}",
            )
        }
    }

    fun revokeThirdPartyApp(appId: String) {
        mutableUiState.update { state ->
            val target = state.thirdPartyApps.firstOrNull { it.id == appId } ?: return@update state
            state.copy(
                thirdPartyApps = state.thirdPartyApps.filterNot { it.id == appId },
                snackbarMessage = "Removed ${target.appName} access to your Account",
            )
        }
    }

    fun unblockUser(email: String) {
        mutableUiState.update { state ->
            state.copy(
                blockedUsers = state.blockedUsers - email,
                snackbarMessage = "Unblocked $email",
            )
        }
    }

    fun toggleSharedEndorsements(enabled: Boolean) {
        mutableUiState.update {
            it.copy(
                sharedEndorsementsEnabled = enabled,
                snackbarMessage = if (enabled) "Shared endorsements enabled" else "Shared endorsements turned off",
            )
        }
    }

    fun addPaymentMethod(label: String, detail: String, type: String) {
        if (label.isBlank() || detail.isBlank()) return
        mutableUiState.update { state ->
            val item = PaymentMethodItem(
                id = "pm-${System.currentTimeMillis()}",
                label = label.trim(),
                detail = detail.trim(),
                isPrimary = false,
                type = type,
            )
            state.copy(
                paymentMethods = state.paymentMethods + item,
                showAddPaymentDialog = false,
                snackbarMessage = "Added ${item.label}",
            )
        }
    }

    fun updatePassword() {
        mutableUiState.update { state ->
            state.copy(
                passwordLastChanged = "Just now",
                showChangePasswordDialog = false,
                securityEvents = listOf(
                    SecurityActivityEvent(
                        id = "evt-${System.currentTimeMillis()}",
                        title = "Account password changed",
                        timestamp = "Just now",
                        location = "Noida, India",
                        device = "Pixel 9 Pro",
                    )
                ) + state.securityEvents,
                snackbarMessage = "Password updated successfully",
            )
        }
    }

    fun setDialogFlags(
        changePassword: Boolean? = null,
        photoPicker: Boolean? = null,
        takeout: Boolean? = null,
        deleteAccount: Boolean? = null,
        addPayment: Boolean? = null,
        feedback: Boolean? = null,
    ) {
        mutableUiState.update { state ->
            state.copy(
                showChangePasswordDialog = changePassword ?: state.showChangePasswordDialog,
                showPhotoPickerDialog = photoPicker ?: state.showPhotoPickerDialog,
                showTakeoutDialog = takeout ?: state.showTakeoutDialog,
                showDeleteAccountDialog = deleteAccount ?: state.showDeleteAccountDialog,
                showAddPaymentDialog = addPayment ?: state.showAddPaymentDialog,
                showFeedbackDialog = feedback ?: state.showFeedbackDialog,
            )
        }
    }

    fun showToastMessage(message: String) {
        mutableUiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        mutableUiState.update { it.copy(snackbarMessage = null) }
    }

    fun getSearchIndex(): List<SearchableSetting> = listOf(
        SearchableSetting("s1", "Name", "Edit your display name", AccountTab.PERSONAL_INFO, targetEditField = EditableField.NAME),
        SearchableSetting("s2", "Birthday", "View or update date of birth", AccountTab.PERSONAL_INFO, targetEditField = EditableField.BIRTHDAY),
        SearchableSetting("s3", "Gender & pronouns", "Manage gender identity and visibility", AccountTab.PERSONAL_INFO, targetEditField = EditableField.GENDER),
        SearchableSetting("s4", "Recovery email", "Backup email for security alerts", AccountTab.PERSONAL_INFO, targetEditField = EditableField.RECOVERY_EMAIL),
        SearchableSetting("s5", "Recovery phone", "Phone number for sign-in verification", AccountTab.PERSONAL_INFO, targetEditField = EditableField.RECOVERY_PHONE),
        SearchableSetting("s6", "Home & Work addresses", "Saved locations across services", AccountTab.PERSONAL_INFO, targetEditField = EditableField.HOME_ADDRESS),
        SearchableSetting("s7", "Language preferences", "English (India), Hindi, input tools", AccountTab.PERSONAL_INFO, targetEditField = EditableField.LANGUAGE),
        SearchableSetting("s8", "Privacy Checkup", "Step-by-step guide to privacy settings", AccountTab.DATA_PRIVACY, targetSubScreen = AccountSubScreen.PrivacyCheckup),
        SearchableSetting("s9", "Web & App Activity", "Activity controls & auto-delete", AccountTab.DATA_PRIVACY, targetSubScreen = AccountSubScreen.ActivityControlDetail("web_app")),
        SearchableSetting("s10", "Timeline (Location History)", "Places you visit and routes", AccountTab.DATA_PRIVACY, targetSubScreen = AccountSubScreen.ActivityControlDetail("timeline")),
        SearchableSetting("s11", "YouTube History", "Videos watched and search history", AccountTab.DATA_PRIVACY, targetSubScreen = AccountSubScreen.ActivityControlDetail("youtube")),
        SearchableSetting("s12", "My Ad Center", "Personalized ads and topic controls", AccountTab.DATA_PRIVACY),
        SearchableSetting("s13", "Download your data", "Create an archive of your Account data", AccountTab.DATA_PRIVACY),
        SearchableSetting("s14", "Security Checkup", "Review recommendations & sign-in protection", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.SecurityCheckup),
        SearchableSetting("s15", "2-Step Verification", "Authenticator, prompts, and backup codes", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.TwoStepVerification),
        SearchableSetting("s16", "Passkeys and security keys", "Passwordless biometric sign-in", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.TwoStepVerification),
        SearchableSetting("s17", "Your devices", "Manage signed-in phones and laptops", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.DeviceManagement),
        SearchableSetting("s18", "Third-party apps & services", "Apps with OAuth access to your Account", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.ThirdPartyApps),
        SearchableSetting("s19", "Password Manager", "Saved passwords & security audit", AccountTab.SECURITY, targetSubScreen = AccountSubScreen.PasswordManager),
        SearchableSetting("s20", "Family group", "Gothwad Family sharing & controls", AccountTab.PEOPLE_SHARING),
        SearchableSetting("s21", "Blocked users", "Manage blocked email addresses", AccountTab.PEOPLE_SHARING),
        SearchableSetting("s22", "Account storage", "41.2 GB of 100 GB used across Drive, Mail & Photos", AccountTab.PAYMENTS_SUBSCRIPTIONS, targetSubScreen = AccountSubScreen.StorageDetails),
        SearchableSetting("s23", "Payment methods", "Cards, UPI & billing profiles", AccountTab.PAYMENTS_SUBSCRIPTIONS),
        SearchableSetting("s24", "Subscriptions", "Google One, YouTube Premium", AccountTab.PAYMENTS_SUBSCRIPTIONS),
    )

    private fun createInitialDemoState(): AccountDashboardUiState {
        val primary = DemoAccountProfile(
            id = "pawan-main",
            name = "Pawan Gothwad",
            email = "pawangothwad@gmail.com",
            handle = "@pawangothwad",
            birthday = "14 August 1998",
            gender = "Male",
            pronouns = "he/him",
            phone = "+91 98765 ••••10",
            recoveryEmail = "pawan.recovery@gothwadtech.com",
            homeAddress = "Sector 62, Noida, Uttar Pradesh",
            workAddress = "DLF Cyber Hub, Gurugram",
            language = "English (India)",
            profilePubliclyVisible = true,
            useDemoAvatarArtwork = true,
            avatarInitials = "PG",
        )
        val work = DemoAccountProfile(
            id = "pawan-work",
            name = "Pawan Gothwad (Work)",
            email = "pawan@gothwadtech.com",
            handle = "@gothwadtech",
            birthday = "14 August 1998",
            gender = "Male",
            pronouns = "he/him",
            phone = "+91 98765 ••••10",
            recoveryEmail = "pawangothwad@gmail.com",
            homeAddress = "Sector 62, Noida, Uttar Pradesh",
            workAddress = "Gothwad Tech Tower, Gurugram",
            language = "English (India)",
            profilePubliclyVisible = true,
            useDemoAvatarArtwork = false,
            avatarInitials = "GW",
            isWorkAccount = true,
        )
        val studio = DemoAccountProfile(
            id = "pawan-studio",
            name = "Gothwad Studio",
            email = "studio@gothwadtech.com",
            handle = "@gothwadstudio",
            birthday = "5 May 2021",
            gender = "Organization",
            pronouns = "they/them",
            phone = "+91 98110 ••••42",
            recoveryEmail = "pawangothwad@gmail.com",
            homeAddress = "Noida, UP",
            workAddress = "DLF Cyber Hub, Gurugram",
            language = "English (India)",
            profilePubliclyVisible = false,
            useDemoAvatarArtwork = false,
            avatarInitials = "GS",
        )

        return AccountDashboardUiState(
            accounts = listOf(primary, work, studio),
            activeAccountId = primary.id,
            activityControls = listOf(
                ActivityControlItem(
                    id = "web_app",
                    title = "Web & App Activity",
                    subtitle = "Saves your activity on sites and apps, including Search and Maps",
                    isEnabled = true,
                    autoDeleteLabel = "Auto-delete (18 months)",
                    recentEntries = listOf(
                        "Searched 'Jetpack Compose Material 3 expressive' • Today, 6:42 PM",
                        "Opened Android Studio Docs • Today, 4:15 PM",
                        "Directions to Sector 62 Metro Station • Yesterday",
                    ),
                ),
                ActivityControlItem(
                    id = "timeline",
                    title = "Timeline",
                    subtitle = "Saves where you go with your devices, even when you aren't using Maps",
                    isEnabled = false,
                    autoDeleteLabel = "Auto-delete (3 months)",
                    recentEntries = listOf(
                        "Timeline is currently paused. Existing visits remain encrypted on-device.",
                    ),
                ),
                ActivityControlItem(
                    id = "youtube",
                    title = "YouTube History",
                    subtitle = "Saves the YouTube videos you watch and what you search on YouTube",
                    isEnabled = true,
                    autoDeleteLabel = "Auto-delete (36 months)",
                    recentEntries = listOf(
                        "Watched 'Kotlin Multiplatform & Compose State Deep Dive' • Today",
                        "Watched 'Material Design 3 Architecture on Android' • 2 days ago",
                    ),
                ),
            ),
            devices = listOf(
                SignedInDevice(
                    id = "dev-1",
                    name = "Pixel 9 Pro",
                    platform = "Android 16",
                    location = "Noida, India",
                    lastActive = "Active now",
                    isCurrentDevice = true,
                    ipAddress = "103.82.•••.14",
                    deviceType = DeviceType.PHONE,
                ),
                SignedInDevice(
                    id = "dev-2",
                    name = "MacBook Pro 14\"",
                    platform = "macOS Sequoia • Chrome",
                    location = "Gurugram, India",
                    lastActive = "2 hours ago",
                    isCurrentDevice = false,
                    ipAddress = "103.82.•••.91",
                    deviceType = DeviceType.LAPTOP,
                ),
                SignedInDevice(
                    id = "dev-3",
                    name = "Galaxy Tab S9",
                    platform = "Android 15",
                    location = "Noida, India",
                    lastActive = "Nov 13, 2026",
                    isCurrentDevice = false,
                    ipAddress = "49.36.•••.204",
                    deviceType = DeviceType.TABLET,
                ),
                SignedInDevice(
                    id = "dev-4",
                    name = "Windows Workstation",
                    platform = "Windows 11 • Edge",
                    location = "New Delhi, India",
                    lastActive = "Nov 09, 2026",
                    isCurrentDevice = false,
                    ipAddress = "122.161.•••.78",
                    deviceType = DeviceType.DESKTOP,
                ),
            ),
            securityEvents = listOf(
                SecurityActivityEvent(
                    id = "se-1",
                    title = "New sign-in on Pixel 9 Pro",
                    timestamp = "Today, 7:12 PM",
                    location = "Noida, India",
                    device = "Pixel 9 Pro",
                ),
                SecurityActivityEvent(
                    id = "se-2",
                    title = "Passkey verified for Gothwad Cloud",
                    timestamp = "Nov 13, 2026",
                    location = "Gurugram, India",
                    device = "MacBook Pro 14\"",
                ),
                SecurityActivityEvent(
                    id = "se-3",
                    title = "Recovery email verified",
                    timestamp = "Nov 08, 2026",
                    location = "Noida, India",
                    device = "Pixel 9 Pro",
                ),
            ),
            securityRecommendations = listOf(
                SecurityRecommendation(
                    id = "rec-1",
                    title = "Review recent sign-in on Windows Workstation",
                    description = "Verify that you signed in from New Delhi on Nov 09, 2026.",
                    actionLabel = "Confirm it was me",
                    isResolved = false,
                ),
                SecurityRecommendation(
                    id = "rec-2",
                    title = "Recovery phone and email up to date",
                    description = "Both recovery options are verified and reachable.",
                    actionLabel = "Done",
                    isResolved = true,
                ),
                SecurityRecommendation(
                    id = "rec-3",
                    title = "2-Step Verification active",
                    description = "Device prompts and passkeys protect your sign-in.",
                    actionLabel = "Done",
                    isResolved = true,
                ),
            ),
            thirdPartyApps = listOf(
                ThirdPartyAppConnection(
                    id = "tp-1",
                    appName = "GitHub",
                    developer = "GitHub, Inc.",
                    accessSummary = "Basic profile info, verified email",
                    connectedSince = "Jan 18, 2025",
                    scopes = listOf("View your email address", "View your basic profile info"),
                ),
                ThirdPartyAppConnection(
                    id = "tp-2",
                    appName = "Figma",
                    developer = "Figma, Inc.",
                    accessSummary = "Profile info, Drive file picker",
                    connectedSince = "Apr 02, 2025",
                    scopes = listOf("View your email address", "See and download specific Drive files"),
                ),
                ThirdPartyAppConnection(
                    id = "tp-3",
                    appName = "Spotify",
                    developer = "Spotify AB",
                    accessSummary = "Sign in with Account",
                    connectedSince = "Aug 11, 2025",
                    scopes = listOf("Associate you with your personal info"),
                ),
                ThirdPartyAppConnection(
                    id = "tp-4",
                    appName = "Notion",
                    developer = "Notion Labs, Inc.",
                    accessSummary = "Calendar events & Drive previews",
                    connectedSince = "Feb 24, 2026",
                    scopes = listOf("View your email address", "View your Calendar events"),
                ),
            ),
            savedPasswords = listOf(
                SavedPasswordEntry("pw-1", "GitHub", "github.com", "pawangothwad", "Gh#94!kL20xQ", "Oct 2026"),
                SavedPasswordEntry("pw-2", "Figma", "figma.com", "pawangothwad@gmail.com", "Fg_88mP!vZ91", "Sep 2026"),
                SavedPasswordEntry("pw-3", "Vercel", "vercel.com", "pawan@gothwadtech.com", "Vc$71nW@44pR", "Aug 2026"),
                SavedPasswordEntry("pw-4", "Amazon India", "amazon.in", "+91 9876543210", "Amz!55tK#19d", "Jul 2026"),
            ),
            familyMembers = listOf(
                FamilyMember("fm-1", "Pawan Gothwad", "pawangothwad@gmail.com", "Family manager"),
                FamilyMember("fm-2", "Ananya Gothwad", "ananya.gothwad@gmail.com", "Member"),
                FamilyMember("fm-3", "Rajesh Gothwad", "rajesh.gothwad@gmail.com", "Parent"),
                FamilyMember("fm-4", "Meera Gothwad", "meera.gothwad@gmail.com", "Member"),
            ),
            paymentMethods = listOf(
                PaymentMethodItem("pm-1", "Visa •••• 4821", "Expires 09/29 • Default", true, "Card"),
                PaymentMethodItem("pm-2", "UPI • pawan@okaxis", "Instant bank transfer", false, "UPI"),
            ),
            subscriptions = listOf(
                SubscriptionItem("sub-1", "Cloud Storage 100 GB", "Monthly plan", "₹130 / month", "Renews Dec 01, 2026", true),
                SubscriptionItem("sub-2", "Media Premium Individual", "Ad-free & offline", "₹149 / month", "Renews Nov 24, 2026", true),
            ),
        )
    }
}
