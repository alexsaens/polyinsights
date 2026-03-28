package com.polyinsights.nfccloner.nfc.keys

import com.polyinsights.nfccloner.util.HexUtil

/**
 * Well-known default MIFARE Classic keys.
 * Sourced from common factory defaults and widely-used deployments.
 */
object DefaultKeys {
    val KEYS: List<ByteArray> = listOf(
        // Factory defaults — try these first
        "FFFFFFFFFFFF",
        "000000000000",
        "A0A1A2A3A4A5",
        "B0B1B2B3B4B5",
        "D3F7D3F7D3F7",
        "AABBCCDDEEFF",
        "4D3A99C351DD",
        "1A982C7E459A",
        "714C5C886E97",
        "587EE5F9350F",
        "A0478CC39091",
        "533CB6C723F6",
        "8FD0A4F256E9",
        // NXP application directory
        "A0A1A2A3A4A5",
        "C1C2C3C4C5C6",
        "C7C8C9CACBCC",
        "CDCECFD0D1D2",
        "D3D4D5D6D7D8",
        "D9DADBDCDDDE",
        // Common access control keys
        "000000000001",
        "000000000002",
        "111111111111",
        "222222222222",
        "333333333333",
        "444444444444",
        "555555555555",
        "666666666666",
        "777777777777",
        "888888888888",
        "999999999999",
        "AAAAAAAAAAAA",
        "BBBBBBBBBBBB",
        "CCCCCCCCCCCC",
        "DDDDDDDDDDDD",
        "EEEEEEEEEEEE",
        // Transport / common deployments
        "484558414354",  // HEXACT
        "A64598A77478",
        "26940B21FF5D",
        "FC00018778F7",
        "00000FFE2211",
        "44AB0B4D0BEC",
        "1B3C73783F9C",
        "7F33625BC129",
        "484944204953",
        "3E65E4FB65B3",
        "18072B1B8E07",
        "2735FC181807",
        "3F542A5F4E6E",
        "010203040506",
        "0102030405060708090A0B0C".take(12),
        "123456789ABC",
        "B127C6F41436",
        "12F2EE3478C1",
        "34D1DF9934C5",
        "55F5A5DD38C9",
        "F1A97341A2EC",
        "31E67B5FC4A8",
        "48FFE71C6F64",
        "2AE3B207C277",
        "BFE2D36BF3B8",
        "EC9B2EFCA5E9",
        "E7C2B69E8F5A",
        "FBF225DC8F18",
        "2B7F3253FAC5",
        "5AA1B25DAE4F",
        "E3917B983AB0",
        "B23A00000000",
        "229A00000000",
        "A22AE129C013",
        "49FAE4E3849F",
        "38FCF33072E0",
        "000000000042",
        "4279505F504B",
        "534349545A31",
        "544549434F31",
        "434F4D504154",
        "504153534F31",
    ).map { HexUtil.hexToBytes(it) }
        .distinctBy { HexUtil.bytesToHex(it) } // Remove duplicates
}
