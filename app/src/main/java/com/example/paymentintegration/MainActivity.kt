package com.example.paymentintegration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.paymentintegration.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private val addToGoogleWalletRequestCode = 1000
    private lateinit var paymentsClient: PaymentsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)
        binding.btnPay.setOnClickListener {
        requestPayment()
        }
    }



    fun requestPayment() {
        binding.btnPay.visibility = View.GONE
        val totalAmountToBePaid = 10L
        val shippingCostCents = 9L
        val task = getLoadPaymentDataTask(totalAmountToBePaid)
        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                completedTask.result.let(::handlePaymentSuccess)
            } else {
                when (val exception = completedTask.exception) {
                    is ResolvableApiException -> {
                        resolvePaymentForResult.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    }

                    is ApiException -> {
                        handleError(exception.statusCode, exception.message.toString())
                    }

                    else -> {
                        handleError(
                            CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                                    " exception when trying to deliver the task result to an activity!"
                        )
                    }
                }
            }
// Re-enables the Google Pay payment button.
            binding.btnPay.visibility = View.VISIBLE
        }
    }

    private val resolvePaymentForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK ->
                    result.data?.let {
                        PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                    }

                RESULT_CANCELED -> {
// The user cancelled the payment attempt
                }
            }
        }
    private val _canUseGooglePay: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            fetchCanUseGooglePay()
        }
    }

    private fun fetchCanUseGooglePay() {
        val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest()
        if (isReadyToPayJson == null) _canUseGooglePay.value = false
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                _canUseGooglePay.value = completedTask.getResult(ApiException::class.java)
            } catch (exception: ApiException) {
                Log.w("isReadyToPay failed", exception)
                _canUseGooglePay.value = false
            }
        }
    }

    fun getLoadPaymentDataTask(priceCents: Long): Task<PaymentData> {
        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(priceCents)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return paymentsClient.loadPaymentData(request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == addToGoogleWalletRequestCode) {
                when (resultCode) {
                    RESULT_OK -> Toast
                        .makeText(this, "add_google_wallet_success", Toast.LENGTH_LONG)
                        .show()

                    RESULT_CANCELED -> {
                    }

                    else -> handleError(
                        CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                                " exception when trying to deliver the task result to an activity!"
                    )
                }
                binding.btnPay.visibility = View.VISIBLE
            }
    }catch (e: Exception)
    {
        Log.e("Error",e.toString())
    }
    }
}

private fun handleError(statusCode: Int, message: String) {
    Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
}

private fun handlePaymentSuccess(paymentData: PaymentData) {
    val paymentInformation = paymentData.toJson() ?: return
    try {
        val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
        val billingName = paymentMethodData.getJSONObject("info")
            .getJSONObject("billingAddress").getString("name")
        Log.d("BillingName", billingName)

        Log.d(
            "GooglePaymentToken", paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token")
        )
    } catch (e: JSONException) {
        Log.e("handlePaymentSuccess", "Error: $e")
    }
}