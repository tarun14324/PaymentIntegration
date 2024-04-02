package com.example.paymentintegration


import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

object PaymentsUtil {
    val CENTS = BigDecimal(100)

    private val baseRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)


    private val gatewayTokenizationSpecification: JSONObject =
        JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject(Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))

    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)

    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    private fun baseCardPaymentMethod(): JSONObject =
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", allowedCardAuthMethods)
                    .put("allowedCardNetworks", allowedCardNetworks)
                    .put("billingAddressRequired", true)
                    .put(
                        "billingAddressParameters", JSONObject()
                            .put("format", "FULL")
                    )
            )


    private val cardPaymentMethod: JSONObject = baseCardPaymentMethod()
        .put("tokenizationSpecification", gatewayTokenizationSpecification)

    val allowedPaymentMethods: JSONArray = JSONArray().put(cardPaymentMethod)


    fun isReadyToPayRequest(): JSONObject? =
        try {
            baseRequest
                .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
        } catch (e: JSONException) {
            null
        }

    private val merchantInfo: JSONObject =
        JSONObject().put("Demo Payment", "Payment Demo")


    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }


    private fun getTransactionInfo(price: String): JSONObject =
        JSONObject()
            .put("totalPrice", price)
            .put("totalPriceStatus", "FINAL")
            .put("countryCode", Constants.COUNTRY_CODE)
            .put("currencyCode", Constants.CURRENCY_CODE)


    fun getPaymentDataRequest(priceCents: Long): JSONObject =
        baseRequest
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .put("transactionInfo", getTransactionInfo(priceCents.centsToString()))
            .put("merchantInfo", merchantInfo)
            .put("shippingAddressRequired", true)
            .put(
                "shippingAddressParameters", JSONObject()
                    .put("phoneNumberRequired", false)
                    .put("allowedCountryCodes", JSONArray(listOf("US", "GB")))
            )
}

/**
 * Converts cents to a string format accepted by [PaymentsUtil.getPaymentDataRequest].
 */
fun Long.centsToString() = BigDecimal(this)
    .divide(PaymentsUtil.CENTS)
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()