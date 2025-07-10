package com.libre.spider.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.math.BigInteger;

/**
 * 小红书签名算法实现
 */
@Slf4j
@Component
public class XhsSignatureHelper {

	// ObjectMapper是线程安全的，可以作为静态常量
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
		.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);

	// CRC表作为静态常量，避免每次计算时重新创建
	private static final long[] MRC_TABLE = { 0L, 1996959894L, 3993919788L, 2567524794L, 124634137L, 1886057615L,
			3915621685L, 2657392035L, 249268274L, 2044508324L, 3772115230L, 2547177864L, 162941995L, 2125561021L,
			3887607047L, 2428444049L, 498536548L, 1789927666L, 4089016648L, 2227061214L, 450548861L, 1843258603L,
			4107580753L, 2211677639L, 325883990L, 1684777152L, 4251122042L, 2321926636L, 335633487L, 1661365465L,
			4195302755L, 2366115317L, 997073096L, 1281953886L, 3579855332L, 2724688242L, 1006888145L, 1258607687L,
			3524101629L, 2768942443L, 901097722L, 1119000684L, 3686517206L, 2898065728L, 853044451L, 1172266101L,
			3705015759L, 2882616665L, 651767980L, 1373503546L, 3369554304L, 3218104598L, 565507253L, 1454621731L,
			3485111705L, 3099436303L, 671266974L, 1594198024L, 3322730930L, 2970347812L, 795835527L, 1483230225L,
			3244367275L, 3060149565L, 1994146192L, 31158534L, 2563907772L, 4023717930L, 1907459465L, 112637215L,
			2680153253L, 3904427059L, 2013776290L, 251722036L, 2517215374L, 3775830040L, 2137656763L, 141376813L,
			2439277719L, 3865271297L, 1802195444L, 476864866L, 2238001368L, 4066508878L, 1812370925L, 453092731L,
			2181625025L, 4111451223L, 1706088902L, 314042704L, 2344532202L, 4240017532L, 1658658271L, 366619977L,
			2362670323L, 4224994405L, 1303535960L, 984961486L, 2747007092L, 3569037538L, 1256170817L, 1037604311L,
			2765210733L, 3554079995L, 1131014506L, 879679996L, 2909243462L, 3663771856L, 1141124467L, 855842277L,
			2852801631L, 3708648649L, 1342533948L, 654459306L, 3188396048L, 3373015174L, 1466479909L, 544179635L,
			3110523913L, 3462522015L, 1591671054L, 702138776L, 2966460450L, 3352799412L, 1504918807L, 783551873L,
			3082640443L, 3233442989L, 3988292384L, 2596254646L, 62317068L, 1957810842L, 3939845945L, 2647816111L,
			81470997L, 1943803523L, 3814918930L, 2489596804L, 225274430L, 2053790376L, 3826175755L, 2466906013L,
			167816743L, 2097651377L, 4027552580L, 2265490386L, 503444072L, 1762050814L, 4150417245L, 2154129355L,
			426522225L, 1852507879L, 4275313526L, 2312317920L, 282753626L, 1742555852L, 4189708143L, 2394877945L,
			397917763L, 1622183637L, 3604390888L, 2714866558L, 953729732L, 1340076626L, 3518719985L, 2797360999L,
			1068828381L, 1219638859L, 3624741850L, 2936675148L, 906185462L, 1090812512L, 3747672003L, 2825379669L,
			829329135L, 1181335161L, 3412177804L, 3160834842L, 628085408L, 1382605366L, 3423369109L, 3138078467L,
			570562233L, 1426400815L, 3317316542L, 2998733608L, 733239954L, 1555261956L, 3268935591L, 3050360625L,
			752459403L, 1541320221L, 2607071920L, 3965973030L, 1969922972L, 40735498L, 2617837225L, 3943577151L,
			1913087877L, 83908371L, 2512341634L, 3803740692L, 2075208622L, 213261112L, 2463272603L, 3855990285L,
			2094854071L, 198958881L, 2262029012L, 4057260610L, 1759359992L, 534414190L, 2176718541L, 4139329115L,
			1873836001L, 414664567L, 2282248934L, 4279200368L, 1711684554L, 285281116L, 2405801727L, 4167216745L,
			1634467795L, 376229701L, 2685067896L, 3608007406L, 1308918612L, 956543938L, 2808555105L, 3495958263L,
			1231636301L, 1047427035L, 2932959818L, 3654703836L, 1088359270L, 936918000L, 2847714899L, 3736837829L,
			1202900863L, 817233897L, 3183342108L, 3401237130L, 1404277552L, 615818150L, 3134207493L, 3453421203L,
			1423857449L, 601450431L, 3009837614L, 3294710456L, 1567103746L, 711928724L, 3020668471L, 3272380065L,
			1510334235L, 755167117L };

	/**
	 * 生成签名
	 */
	public Map<String, String> sign(String a1, String b1, String xS, String xT) {
		Map<String, Object> common = new LinkedHashMap<>();
		common.put("s0", 3);
		common.put("s1", "");
		common.put("x0", "1");
		common.put("x1", "3.7.8-2");
		common.put("x2", "Mac OS");
		common.put("x3", "xhs-pc-web");
		common.put("x4", "4.27.2");
		common.put("x5", a1);
		common.put("x6", xT);
		common.put("x7", xS);
		common.put("x8", b1);
		common.put("x9", (int) mrc(xT + xS + b1));
		common.put("x10", 154);

		String jsonStr;
		try {
			// 使用紧凑的JSON格式，与Python版本保持一致: json.dumps(common, separators=(',', ':'))
			jsonStr = OBJECT_MAPPER.writer()
				.without(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET)
				.writeValueAsString(common);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize common params", e);
		}

		byte[] encodedBytes = encodeUtf8(jsonStr);
		String xSCommon = b64Encode(encodedBytes);
		String xB3TraceId = getB3TraceId();

		Map<String, String> result = new HashMap<>();
		result.put("X-S", xS);
		result.put("X-T", xT);
		result.put("x-S-Common", xSCommon);
		result.put("X-B3-Traceid", xB3TraceId);

		return result;
	}

	/**
	 * 无符号右移
	 */
	private long rightWithoutSign(long num, int bit) {
		return (num >>> bit) & 0xFFFFFFFFL;
	}

	/**
	 * CRC计算 - 与Python版本保持一致
	 */
	private long mrc(String e) {
		long o = 0xFFFFFFFFL; // -1的无符号表示
		// Python版本固定循环57次，不管字符串长度
		int loopCount = Math.min(57, e.length());
		for (int n = 0; n < loopCount; n++) {
			int charCode = e.charAt(n);
			o = MRC_TABLE[(int) ((o & 255) ^ charCode)] ^ rightWithoutSign(o, 8);
		}
		return (o ^ 0xFFFFFFFFL ^ 3988292384L) & 0xFFFFFFFFL;
	}

	/**
	 * Base64编码字符映射表
	 */
	private static final String[] LOOKUP = { "Z", "m", "s", "e", "r", "b", "B", "o", "H", "Q", "t", "N", "P", "+", "w",
			"O", "c", "z", "a", "/", "L", "p", "n", "g", "G", "8", "y", "J", "q", "4", "2", "K", "W", "Y", "j", "0",
			"D", "S", "f", "d", "i", "k", "x", "3", "V", "T", "1", "6", "I", "l", "U", "A", "F", "M", "9", "7", "h",
			"E", "C", "v", "u", "R", "X", "5" };

	/**
	 * 自定义Base64编码
	 */
	private String b64Encode(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		int padding = bytes.length % 3;
		int mainLength = bytes.length - padding;

		// 处理主要部分
		for (int i = 0; i < mainLength; i += 3) {
			int n = ((bytes[i] & 0xFF) << 16) | ((bytes[i + 1] & 0xFF) << 8) | (bytes[i + 2] & 0xFF);
			result.append(LOOKUP[(n >> 18) & 63]);
			result.append(LOOKUP[(n >> 12) & 63]);
			result.append(LOOKUP[(n >> 6) & 63]);
			result.append(LOOKUP[n & 63]);
		}

		// 处理padding
		if (padding == 1) {
			int n = bytes[mainLength] & 0xFF;
			result.append(LOOKUP[n >> 2]);
			result.append(LOOKUP[(n << 4) & 63]);
			result.append("==");
		}
		else if (padding == 2) {
			int n = ((bytes[mainLength] & 0xFF) << 8) | (bytes[mainLength + 1] & 0xFF);
			result.append(LOOKUP[n >> 10]);
			result.append(LOOKUP[(n >> 4) & 63]);
			result.append(LOOKUP[(n << 2) & 63]);
			result.append("=");
		}

		return result.toString();
	}

	/**
	 * UTF-8编码
	 */
	private byte[] encodeUtf8(String str) {
		List<Byte> bytes = new ArrayList<>();
		String encoded = URLEncoder.encode(str, StandardCharsets.UTF_8)
			.replace("+", " ")
			.replace("%7E", "~")
			.replace("%28", "(")
			.replace("%29", ")")
			.replace("%2A", "*")
			.replace("%21", "!")
			.replace("%27", "'");

		for (int i = 0; i < encoded.length(); i++) {
			char c = encoded.charAt(i);
			if (c == '%' && i + 2 < encoded.length()) {
				String hex = encoded.substring(i + 1, i + 3);
				bytes.add((byte) Integer.parseInt(hex, 16));
				i += 2;
			}
			else {
				bytes.add((byte) c);
			}
		}

		byte[] result = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++) {
			result[i] = bytes.get(i);
		}
		return result;
	}

	/**
	 * 生成B3 Trace ID
	 */
	private String getB3TraceId() {
		String chars = "abcdef0123456789";
		StringBuilder result = new StringBuilder();
		Random random = ThreadLocalRandom.current();

		for (int i = 0; i < 16; i++) {
			result.append(chars.charAt(random.nextInt(chars.length())));
		}

		return result.toString();
	}

	/**
	 * 生成搜索ID - 与Python版本保持一致
	 */
	public String getSearchId() {
		long timestamp = System.currentTimeMillis();
		// Python版本: int(time.time() * 1000) << 64
		// 注意：timestamp已经是毫秒，不需要再乘以1000
		BigInteger e = BigInteger.valueOf(timestamp).shiftLeft(64);
		int t = ThreadLocalRandom.current().nextInt(2147483646);
		return base36Encode(e.add(BigInteger.valueOf(t)));
	}

	/**
	 * Base36编码
	 */
	private String base36Encode(long number) {
		return Long.toUnsignedString(number, 36).toUpperCase();
	}

	/**
	 * Base36编码（支持BigInteger）
	 */
	private String base36Encode(BigInteger number) {
		return number.toString(36).toUpperCase();
	}

}