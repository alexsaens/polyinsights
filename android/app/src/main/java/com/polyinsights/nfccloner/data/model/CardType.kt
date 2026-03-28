package com.polyinsights.nfccloner.data.model

enum class CardType(val displayName: String, val isEmulatable: Boolean) {
    MIFARE_CLASSIC_1K("MIFARE Classic 1K", false),
    MIFARE_CLASSIC_4K("MIFARE Classic 4K", false),
    MIFARE_ULTRALIGHT("MIFARE Ultralight", false),
    MIFARE_ULTRALIGHT_C("MIFARE Ultralight C", false),
    NTAG_213("NTAG 213", false),
    NTAG_215("NTAG 215", false),
    NTAG_216("NTAG 216", false),
    NDEF_GENERIC("NDEF", false),
    ISODEP("ISO-DEP (ISO 14443-4)", true),
    NFC_A("NFC-A (ISO 14443-3A)", false),
    NFC_B("NFC-B (ISO 14443-3B)", false),
    NFC_F("NFC-F (FeliCa)", false),
    NFC_V("NFC-V (ISO 15693)", false),
    UNKNOWN("Unknown", false);
}
