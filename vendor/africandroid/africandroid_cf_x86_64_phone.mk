$(call inherit-product, device/google/cuttlefish/vsoc_x86_64_only/phone/aosp_cf.mk)
PRODUCT_NAME := africandroid_cf_x86_64_phone
PRODUCT_BRAND := AfricAndroid
PRODUCT_MODEL := AfricAndroid Virtual Phone
PRODUCT_MANUFACTURER := AfricAndroid
PRODUCT_PACKAGES += AfricCustomizer
PRODUCT_PRODUCT_PROPERTIES += ro.africandroid.version=0.1.0

# User-supplied artwork; Android uses it as the factory default, not an enforced wallpaper.
PRODUCT_COPY_FILES += vendor/africandroid/apps/Customizer/res/drawable-nodpi/africandroid_brand.jpg:$(TARGET_COPY_OUT_PRODUCT)/etc/africandroid/default_wallpaper.jpg
PRODUCT_SYSTEM_PROPERTIES += ro.config.wallpaper=/product/etc/africandroid/default_wallpaper.jpg
PRODUCT_SYSTEM_PROPERTIES += ro.config.lock_wallpaper=/product/etc/africandroid/default_wallpaper.jpg

# Narrow, owner-authenticated OS role integration; no root or blanket app privilege grant.
PRODUCT_COPY_FILES += vendor/africandroid/permissions/privapp-permissions-africandroid.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-africandroid.xml
-include vendor/africandroid/olamide/product.mk

-include vendor/africandroid/africanbrowser/product.mk
