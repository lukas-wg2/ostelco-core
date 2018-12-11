package org.ostelco.prime.apierror

enum class ApiErrorCode {
    FAILED_TO_CREATE_PAYMENT_PROFILE,
    FAILED_TO_FETCH_PAYMENT_PROFILE,
    FAILED_TO_STORE_APPLICATION_TOKEN,
    @Deprecated("Will be removed")
    FAILED_TO_FETCH_SUBSCRIPTION_STATUS,
    FAILED_TO_FETCH_SUBSCRIPTIONS,
    FAILED_TO_FETCH_BUNDLES,
    FAILED_TO_FETCH_PSEUDONYM_FOR_SUBSCRIBER,
    FAILED_TO_FETCH_PAYMENT_HISTORY,
    FAILED_TO_FETCH_PRODUCT_LIST,
    FAILED_TO_PURCHASE_PRODUCT,
    FAILED_TO_FETCH_REFERRALS,
    FAILED_TO_FETCH_REFERRED_BY_LIST,
    FAILED_TO_FETCH_PRODUCT_INFORMATION,
    FAILED_TO_STORE_PAYMENT_SOURCE,
    FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE,
    FAILED_TO_FETCH_PAYMENT_SOURCES_LIST,
    FAILED_TO_REMOVE_PAYMENT_SOURCE,
    FAILED_TO_FETCH_PROFILE,
    FAILED_TO_CREATE_PROFILE,
    FAILED_TO_UPDATE_PROFILE,
    FAILED_TO_FETCH_CONSENT,
    FAILED_TO_IMPORT_OFFER,
    FAILED_TO_REFUND_PURCHASE,
    FAILED_TO_GENERATE_STRIPE_EPHEMERAL_KEY,
    FAILED_TO_FETCH_PLAN,
    FAILED_TO_FETCH_PLANS_FOR_SUBSCRIBER,
    FAILED_TO_STORE_PLAN,
    FAILED_TO_REMOVE_PLAN,
    FAILED_TO_SUBSCRIBE_TO_PLAN,
    FAILED_TO_FETCH_SUBSCRIPTION,
    FAILED_TO_REMOVE_SUBSCRIPTION,
    FAILED_TO_CREATE_SCANID,
    FAILED_TO_UPDATE_SCAN_RESULTS
}
