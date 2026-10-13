$(call inherit-product, device/google/cuttlefish/vsoc_x86_64_only/phone/aosp_cf.mk)
PRODUCT_NAME := africandroid_cf_x86_64_phone
PRODUCT_BRAND := AfricAndroid
PRODUCT_MODEL := AfricAndroid Virtual Phone
PRODUCT_MANUFACTURER := AfricAndroid
PRODUCT_PACKAGES += AfricCustomizer
PRODUCT_PRODUCT_PROPERTIES += ro.africandroid.version=0.1.0
