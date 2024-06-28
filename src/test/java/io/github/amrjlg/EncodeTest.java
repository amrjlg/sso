package io.github.amrjlg;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author lingjiang
 */
public class EncodeTest {
    @Test
    void encode() throws UnsupportedEncodingException {
        String url = "https://lingjiang@deloitte.com.cn@gitlab.diapp.siemens.com.cn?a=1&b=2";

        byte[] bytes = url.getBytes(StandardCharsets.UTF_8);
        System.out.println(Base64.getEncoder().encodeToString(bytes));

        System.out.println(Base64.getUrlEncoder().encodeToString(bytes));

        System.out.println(URLEncoder.encode(url,"utf8"));
    }
}
