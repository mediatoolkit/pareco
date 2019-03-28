package com.mediatoolkit.pareco.model;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 24/10/2018
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("deprecation")
public enum DigestType {

	MURMUR3_32(Hashing.murmur3_32()),
	MURMUR3_128(Hashing.murmur3_128()),
	SIP_HASH_24(Hashing.sipHash24()),
	MD5(Hashing.md5()),
	SHA_1(Hashing.sha1()),
	SHA_256(Hashing.sha256()),
	SHA_384(Hashing.sha384()),
	SHA_512(Hashing.sha512()),
	CRC_32_C(Hashing.crc32c()),
	CRC_32(Hashing.crc32()),
	ADLER_32(Hashing.adler32()),
	FARM_HASH_FINGERPRINT_64(Hashing.farmHashFingerprint64());

	private final HashFunction hashFunction;

}
