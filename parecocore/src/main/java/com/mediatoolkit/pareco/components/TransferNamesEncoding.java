package com.mediatoolkit.pareco.components;

import java.net.URLDecoder;
import java.net.URLEncoder;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-20
 */
@Component
public class TransferNamesEncoding {

	@SneakyThrows
	public String encode(String val) {
		return URLEncoder.encode(val, "utf-8");
	}

	@SneakyThrows
	public String decode(String val) {
		return URLDecoder.decode(val, "utf-8");
	}
}
