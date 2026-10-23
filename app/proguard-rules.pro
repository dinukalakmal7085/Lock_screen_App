# Android's default R8 rules preserve the manifest activities/services and XML resources.
# Appearance JSON is written explicitly; no reflective data model needs keep rules.
-keepattributes Signature,InnerClasses,EnclosingMethod
