package com.example.integrations

import com.example.data.model.GlobalSettings
import com.example.data.model.PaymentMethod

/**
 * Modular Rewarded Ad provider contract.
 * Never grants free rewards on button click — requires verified ad SDK credentials
 * and a genuine provider completion callback.
 */
sealed interface RewardedAdStatus {
    data class ConfigurationRequired(
        val requiredCredentials: List<String>,
        val explanation: String
    ) : RewardedAdStatus

    data object Ready : RewardedAdStatus
    data class VerifiedRewardEarned(val verificationToken: String) : RewardedAdStatus
    data class Failed(val reason: String) : RewardedAdStatus
}

class RewardedAdProvider(
    private val adMobAppId: String = "",
    private val rewardedAdUnitId: String = ""
) {
    fun getStatus(settings: GlobalSettings): RewardedAdStatus {
        val hasCredentials = adMobAppId.isNotBlank() && rewardedAdUnitId.isNotBlank() && settings.rewardedAdConfigured
        return if (!hasCredentials) {
            RewardedAdStatus.ConfigurationRequired(
                requiredCredentials = listOf(
                    "ADMOB_APP_ID (AndroidManifest meta-data)",
                    "ADMOB_REWARDED_AD_UNIT_ID",
                    "Server-Side Verification (SSV) callback URL"
                ),
                explanation = "Rewarded Ad credentials are not configured yet. Continuing via ad requires a live Google Mobile Ads SDK integration and a verified ad-completion callback — free instant continues are disabled for fairness."
            )
        } else {
            RewardedAdStatus.Ready
        }
    }
}

/**
 * Modular Pakistan Payment Gateway integration contract (JazzCash, Easypaisa, Bank Transfer).
 * Clearly distinguishes Sandbox / Manual Admin Verification mode from live API integrations
 * and never fabricates successful live payments.
 */
data class PaymentMethodDescriptor(
    val method: PaymentMethod,
    val enabled: Boolean,
    val sandboxMode: Boolean,
    val instructions: String,
    val requiredApiCredentials: List<String>,
    val complianceNote: String
)

object PaymentGatewayRegistry {
    fun describeAll(settings: GlobalSettings): List<PaymentMethodDescriptor> = listOf(
        PaymentMethodDescriptor(
            method = PaymentMethod.JAZZCASH,
            enabled = settings.jazzCashEnabled,
            sandboxMode = settings.paymentSandboxMode,
            instructions = settings.jazzCashInstructions,
            requiredApiCredentials = listOf(
                "JAZZCASH_MERCHANT_ID",
                "JAZZCASH_PASSWORD",
                "JAZZCASH_INTEGRITY_SALT",
                "JAZZCASH_WEBHOOK_SECRET (Server-side Cloud Function)"
            ),
            complianceNote = "Manual Admin verification required before PKR Main Wallet credit. Live automated settlement requires signed JazzCash Merchant API webhooks."
        ),
        PaymentMethodDescriptor(
            method = PaymentMethod.EASYPAISA,
            enabled = settings.easypaisaEnabled,
            sandboxMode = settings.paymentSandboxMode,
            instructions = settings.easypaisaInstructions,
            requiredApiCredentials = listOf(
                "EASYPAISA_STORE_ID",
                "EASYPAISA_HASH_KEY",
                "EASYPAISA_USERNAME / PASSWORD (Server-side Cloud Function)"
            ),
            complianceNote = "Manual Admin verification required before PKR Main Wallet credit. Automated verification requires Easypaisa Open API credentials."
        ),
        PaymentMethodDescriptor(
            method = PaymentMethod.BANK_TRANSFER,
            enabled = settings.bankTransferEnabled,
            sandboxMode = settings.paymentSandboxMode,
            instructions = settings.bankTransferInstructions,
            requiredApiCredentials = listOf(
                "CORPORATE_IBAN",
                "BANK_RECONCILIATION_API_KEY (Optional RAAST / 1Link integration)"
            ),
            complianceNote = "Admin reconciles bank transfer reference against bank statement before approving Deposit or marking Withdrawal as Paid."
        )
    )
}
