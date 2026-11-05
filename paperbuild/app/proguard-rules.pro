# PDFBox's AAR supplies the reflective SecurityHandler constructor rules.
# Do not keep the entire PDFBox/BouncyCastle packages or disable obfuscation:
# that would hide the issue rather than optimise the shipped application.

# Optional JPEG-2000 codec, absent in previous releases too. PDFBox guards
# decoder discovery with Class.forName and reports MissingImageReaderException.
# PaperBuild exports JPEG, not JPEG-2000; never suppress unrelated missing types.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
