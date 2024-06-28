package io.github.amrjlg.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.amrjlg.AzureEndpointUtil;
import io.github.amrjlg.entity.AzureAccessToken;
import io.github.amrjlg.entity.AzureEnterpriseAppManagement;
import io.github.amrjlg.repository.AzureEnterpriseAppRepository;
import io.github.amrjlg.response.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lingjiang
 */
@Controller
@RequestMapping("${sso.azure.base-path:/azure}")
public class AzureSsoController {
    private final RestTemplate restTemplate;


    private final RedisTemplate<String, String> redisTemplate;

    private final AzureEnterpriseAppRepository enterpriseAppRepository;

    private final ObjectMapper objectMapper;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AzureSsoController(RestTemplate restTemplate, RedisTemplate<String, String> redisTemplate, AzureEnterpriseAppRepository enterpriseAppRepository, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.enterpriseAppRepository = enterpriseAppRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("${sso.azure.refresh:refresh}")
    @ResponseBody
    public R<String> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken, @RequestParam("appid") Long appid) throws JsonProcessingException {
        accessToken = accessToken.replace("Bearer ", "");
        String token = redisTemplate.opsForValue().get(accessToken);
        if (Objects.isNull(token)) {
            throw new RuntimeException("已过期不能刷新token");
        }
        AzureEnterpriseAppManagement app = enterpriseAppRepository.findByAppid(appid);
        if (Objects.isNull(app)) {
            throw new RuntimeException("App配置异常，无法刷新");
        }

        AzureAccessToken azureAccessToken = objectMapper.readValue(token, AzureAccessToken.class);
        String tokenEndpoint = AzureEndpointUtil.token(app.getTenant());
        ResponseEntity<AzureAccessToken> response = restTemplate.postForEntity(tokenEndpoint, buildRefreshEntity(app, azureAccessToken.getRefreshToken()), AzureAccessToken.class);

        return R.success(response.getBody().getAccessToken());
    }

    @GetMapping("${sso.azure.logout:logout}")
    public String logout(@RequestParam("redirectUrl") String redirectUrl, @RequestParam("appid") Long appid) throws UnsupportedEncodingException {

        AzureEnterpriseAppManagement app = enterpriseAppRepository.findByAppid(appid);
        if (Objects.isNull(app)) {
            throw new RuntimeException("App配置异常，无法登出");
        }
        String url = app.getLogoutCallbackUrl();
        String encode = Base64.getEncoder().encodeToString(redirectUrl.getBytes(StandardCharsets.UTF_8));

        String s = url + "?redirect=" + encode;
        return "redirect:https://login.microsoftonline.com/common/oauth2/v2.0/logout?post_logout_redirect_uri=" + URLEncoder.encode(s, "utf8");
    }

    @PostMapping("${sso.azure.end_session:end_session}")
    public String logoutCallback(@RequestParam(value = "redirect", required = false) String redirect, HttpServletRequest request) {
        Map<String, String[]> map = request.getParameterMap();
        logger.info("end_session params names: {}", map.keySet());
        logger.info("end_session params: {}", map);
        byte[] decode = Base64.getDecoder().decode(redirect);
        return "redirect:" + new String(decode, StandardCharsets.UTF_8);
    }

    /**
     * @param code     获取token凭证
     * @param state    请求验证值，由服务的传递，回调时原样传回。可以验证请求是否有服务端发出
     * @param response
     * @throws IOException
     */
    @RequestMapping("${sso.azure.callback:callback}")
    public void callback(@RequestParam("code") String code, @RequestParam("state") String state, HttpServletResponse response) throws IOException {

        Pair<String, AzureEnterpriseAppManagement> pair = decodeState(state);
        if (pair == null) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
            response.getOutputStream().write("状态已过期或者app异常，请稍后重试".getBytes(StandardCharsets.UTF_8));
            return;
        }
        AzureEnterpriseAppManagement app = pair.getSecond();
        ResponseEntity<AzureAccessToken> responseEntity = restTemplate.postForEntity(AzureEndpointUtil.token(app.getTenant()), buildTokenEntity(app, code), AzureAccessToken.class);
        AzureAccessToken azureAccessToken = responseEntity.getBody();

        redisTemplate.opsForValue().set(azureAccessToken.getAccessToken(), objectMapper.writeValueAsString(azureAccessToken), azureAccessToken.getExpiresIn() / 4 * 3, TimeUnit.SECONDS);

        String first = pair.getFirst();
        response.sendRedirect(first + "?token=" + azureAccessToken.getAccessToken());

    }

    /**
     * 重定向到授权地址
     *
     * @param redirectUrl 获取token的回调地址
     * @param appid       系统分配的appid
     * @param response
     * @throws IOException
     */
    @GetMapping("${sso.azure.authorize:authorize}")
    public void authorize(@RequestParam("redirectUrl") String redirectUrl, Long appid, HttpServletResponse response) throws IOException {

        String state = UUID.randomUUID().toString();

        state = buildState(state, appid);
        String buildAuthorization = buildAuthorization(state, appid);
        if (buildAuthorization == null) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
            response.getOutputStream().write("未找到相关App".getBytes(StandardCharsets.UTF_8));
        } else {
            redisTemplate.opsForValue().set(state, redirectUrl, 10, TimeUnit.MINUTES);
            response.sendRedirect(buildAuthorization);
        }

    }

    @ResponseBody
    @GetMapping("${sso.azure.get-redirect:/get/redirect}")
    public R<String> redirect(@RequestParam("redirectUrl") String redirectUrl, Long appid) throws IOException {

        R<String> result;
        String state = UUID.randomUUID().toString();
        state = buildState(state, appid);
        String buildAuthorization = buildAuthorization(state, appid);
        if (buildAuthorization == null) {
            result = R.failed("未找到相关App");
            logger.error(result.getMessage());
        } else {
            redisTemplate.opsForValue().set(state, redirectUrl, 10, TimeUnit.MINUTES);
            result = R.success(buildAuthorization);
        }
        return result;
    }

    private String buildAuthorization(String state, Long appid) throws UnsupportedEncodingException {

        AzureEnterpriseAppManagement app = enterpriseAppRepository.findByAppid(appid);

        if (app == null) {
            return null;
        }

        return AzureEndpointUtil.authorization(app, state);

    }

    private String buildState(String state, Long appid) {
        return Base64.getEncoder().encodeToString((state + "," + appid).getBytes(StandardCharsets.UTF_8));
    }

    private Pair<String, AzureEnterpriseAppManagement> decodeState(String state) {
        byte[] decode = Base64.getDecoder().decode(state);

        String[] states = new String(decode, StandardCharsets.UTF_8).split(",");

        String redirect = redisTemplate.opsForValue().get(states[0]);
        if (redirect == null) {
            return null;
        }


        AzureEnterpriseAppManagement appid = enterpriseAppRepository.findByAppid(Long.valueOf(states[1]));
        if (appid == null) {
            return null;
        }
        return Pair.of(redirect, appid);
    }


    private HttpEntity<String> buildRefreshEntity(AzureEnterpriseAppManagement app, String refreshToken) {
        /**
         * // Line breaks for legibility only
         *
         * POST /{tenant}/oauth2/v2.0/token HTTP/1.1
         * Host: https://login.microsoftonline.com
         * Content-Type: application/x-www-form-urlencoded
         *
         * client_id=535fb089-9ff3-47b6-9bfb-4f1264799865
         * &scope=https%3A%2F%2Fgraph.microsoft.com%2Fmail.read
         * &refresh_token=OAAABAAAAiL9Kn2Z27UubvWFPbm0gLWQJVzCTE9UkP3pSx1aXxUjq...
         * &grant_type=refresh_token
         * &client_secret=sampleCredentia1s    // NOTE: Only required for web apps. This secret needs to be URL-Encoded
         */
        String scope = AzureEndpointUtil.scope(app.getClient());
        Map<String, String> params = new HashMap<>();
        params.put("scope", scope);
        params.put("client_id", app.getClient());
        params.put("client_secret", app.getSecret());
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        return entityOfFormUrlencoded(params);
    }

    private HttpEntity<String> buildTokenEntity(AzureEnterpriseAppManagement app, String code) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("scope", AzureEndpointUtil.scope(app.getClient()));
        params.put("client_secret", app.getSecret());
        params.put("client_id", app.getClient());
        String encode = URLEncoder.encode(app.getRedirectUrl(), "utf8");
        params.put("redirect_uri", encode);
        return entityOfFormUrlencoded(params);
    }

    private HttpEntity<String> entityOfFormUrlencoded(Map<String, String> params) {

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        String postParameters = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + Optional.ofNullable(e.getValue()).map(Objects::toString).orElse(""))
                .collect(Collectors.joining("&"));
        return new HttpEntity<>(postParameters, headers);
    }
}
