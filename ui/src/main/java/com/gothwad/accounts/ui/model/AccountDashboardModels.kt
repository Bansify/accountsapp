package com.gothwad.accounts.ui.model

import androidx.compose.runtime.Immutable

enum class AccountTab(val title: String) {
    HOME("Home"),
    PERSONAL_INFO("Personal info"),
    DATA_PRIVACY("Data & privacy"),
    SECURITY("Security"),
    PEOPLE_SHARING("People & sharing"),
    PAYMENTS_SUBSCRIPTIONS("Payments & subscriptions"),
}

sealed interface AccountSubScreen {
    data object SearchAccount : AccountSubScreen
    data object PrivacyCheckup : AccountSubScreen
    data object SecurityCheckup : AccountSubScreen
    data object DeviceManagement : AccountSubScreen
    data class ActivityControlDetail(val controlId: String) : AccountSubScreen
    data object PasswordManager : AccountSubScreen
    data object ThirdPartyApps : AccountSubScreen
    data object TwoStepVerification : AccountSubScreen
    data object StorageDetails : AccountSubScreen
    data object HelpSupport : AccountSubScreen
}

enum class EditableField(val title: String, val helper: String) {
    NAME("Name", "Changes to your name will be reflected across your Account."),
    BIRTHDAY("Birthday", "Your birthday may be used for account security and personalization."),
    GENDER("Gender", "Choose who can see your gender across services."),
    PRONOUNS("Pronouns", "Displayed next to your name on supported services."),
    RECOVERY_EMAIL("Recovery email", "Used to reach you if unusual activity is detected."),
    RECOVERY_PHONE("Recovery phone", "Used for 2-Step Verification and account recovery."),
    HOME_ADDRESS("Home address", "Used to personalize Maps, Assistant, and local weather."),
    WORK_ADDRESS("Work address", "Used for commute estimates and calendar suggestions."),
    LANGUAGE("Preferred language", "Applies across Account settings and web services."),
}

@Immutable
data class DemoAccountProfile(
    val id: String,
    val name: String,
    val email: String,
    val handle: String,
    val birthday: String,
    val gender: String,
    val pronouns: String,
    val phone: String,
    val recoveryEmail: String,
    val homeAddress: String,
    val workAddress: String,
    val language: String,
    val profilePubliclyVisible: Boolean,
    val useDemoAvatarArtwork: Boolean,
    val avatarInitials: String,
    val isWorkAccount: Boolean = false,
)

@Immutable
data class ActivityControlItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isEnabled: Boolean,
    val autoDeleteLabel: String,
    val recentEntries: List<String>,
)

@Immutable
data class SignedInDevice(
    val id: String,
    val name: String,
    val platform: String,
    val location: String,
    val lastActive: String,
    val isCurrentDevice: Boolean,
    val ipAddress: String,
    val deviceType: DeviceType,
)

enum class DeviceType { PHONE, LAPTOP, TABLET, DESKTOP }

@Immutable
data class SecurityActivityEvent(
    val id: String,
    val title: String,
    val timestamp: String,
    val location: String,
    val device: String,
    val isAlert: Boolean = false,
)

@Immutable
data class SecurityRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val isResolved: Boolean,
)

@Immutable
data class ThirdPartyAppConnection(
    val id: String,
    val appName: String,
    val developer: String,
    val accessSummary: String,
    val connectedSince: String,
    val scopes: List<String>,
)

@Immutable
data class SavedPasswordEntry(
    val id: String,
    val siteName: String,
    val domain: String,
    val username: String,
    val passwordValue: String,
    val lastUpdated: String,
)

@Immutable
data class PaymentMethodItem(
    val id: String,
    val label: String,
    val detail: String,
    val isPrimary: Boolean,
    val type: String,
)

@Immutable
data class SubscriptionItem(
    val id: String,
    val name: String,
    val plan: String,
    val price: String,
    val nextBilling: String,
    val isActive: Boolean,
)

@Immutable
data class FamilyMember(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
)

@Immutable
data class SearchableSetting(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: AccountTab,
    val targetSubScreen: AccountSubScreen? = null,
    val targetEditField: EditableField? = null,
)

@Immutable
data class AccountDashboardUiState(
    val accounts: List<DemoAccountProfile>,
    val activeAccountId: String,
    val selectedTab: AccountTab = AccountTab.HOME,
    val subScreenStack: List<AccountSubScreen> = emptyList(),
    val searchQuery: String = "",
    // Personal info & password metadata
    val passwordLastChanged: String = "Oct 12, 2026",
    val passkeyCount: Int = 2,
    val twoStepEnabled: Boolean = true,
    val authenticatorActive: Boolean = true,
    val enhancedSafeBrowsing: Boolean = true,
    val securityAlertsPhoneVerified: Boolean = true,
    // Data & privacy
    val activityControls: List<ActivityControlItem> = emptyList(),
    val personalizedAdsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = false,
    val partnerSharingEnabled: Boolean = true,
    val searchHistorySaved: Boolean = true,
    val privacyCheckupStepsDone: Set<Int> = setOf(0, 1),
    // Security
    val devices: List<SignedInDevice> = emptyList(),
    val securityEvents: List<SecurityActivityEvent> = emptyList(),
    val securityRecommendations: List<SecurityRecommendation> = emptyList(),
    val thirdPartyApps: List<ThirdPartyAppConnection> = emptyList(),
    val savedPasswords: List<SavedPasswordEntry> = emptyList(),
    val backupCodesRemaining: Int = 8,
    // People & sharing
    val contactsCount: Int = 284,
    val blockedUsers: List<String> = listOf("spam.mailer@example.net", "promo-alerts@unknown.org"),
    val familyMembers: List<FamilyMember> = emptyList(),
    val sharedEndorsementsEnabled: Boolean = true,
    // Payments & storage
    val storageUsedGb: Float = 41.2f,
    val storageTotalGb: Float = 100f,
    val driveStorageGb: Float = 18.4f,
    val mailStorageGb: Float = 9.6f,
    val photosStorageGb: Float = 13.2f,
    val paymentMethods: List<PaymentMethodItem> = emptyList(),
    val subscriptions: List<SubscriptionItem> = emptyList(),
    // Modal states
    val showAccountSwitcher: Boolean = false,
    val activeEditField: EditableField? = null,
    val showChangePasswordDialog: Boolean = false,
    val showPhotoPickerDialog: Boolean = false,
    val showTakeoutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showAddPaymentDialog: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val isLiveCoreAccountLinked: Boolean = false,
) {
    val activeAccount: DemoAccountProfile
        get() = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.first()

    val otherAccounts: List<DemoAccountProfile>
        get() = accounts.filter { it.id != activeAccountId }

    val currentSubScreen: AccountSubScreen?
        get() = subScreenStack.lastOrNull()

    val unresolvedSecurityCount: Int
        get() = securityRecommendations.count { !it.isResolved } +
            (if (!twoStepEnabled) 1 else 0) +
            (if (!enhancedSafeBrowsing) 1 else 0)
}
