package com.pTech.trustTheBox.util

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object BillingManager : PurchasesUpdatedListener, ProductDetailsResponseListener {

    private lateinit var billingClient: BillingClient

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private const val REMOVE_ADS_PRODUCT_ID = "remove_ads_permanent"

    private var removeAdsProductDetails: ProductDetails? = null

    fun init(context: Context, lifecycleOwner: LifecycleOwner) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                lifecycleOwner.lifecycleScope.launch {
                    delay(5000)
                    init(context, lifecycleOwner)
                }
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            val hasPurchased = purchases.any {
                it.products.contains(REMOVE_ADS_PRODUCT_ID) && it.isAcknowledged
            }
            _isPremium.value = hasPurchased
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params, this)
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsResult: QueryProductDetailsResult
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            removeAdsProductDetails = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = removeAdsProductDetails ?: run {
            queryProductDetails()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            var shouldRefresh = false
            for (purchase in purchases) {
                if (purchase.products.contains(REMOVE_ADS_PRODUCT_ID) && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                    shouldRefresh = true
                }
            }
            if (shouldRefresh) queryPurchases()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) {
            queryPurchases()
        }
    }
}