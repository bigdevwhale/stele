# Room entities and DAOs are kept by the Room artifacts' consumer rules.
# aircompressor reaches for sun.misc.Unsafe and Hadoop codecs it never needs here.
-dontwarn sun.misc.Unsafe
-dontwarn org.apache.hadoop.**
# Broadcast receivers are referenced from the manifest and kept automatically.
