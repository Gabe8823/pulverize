package com.run.module.platform.client;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.exception.BizException;
import com.run.module.platform.dto.CorosActivityResponse;
import com.run.module.platform.dto.CorosLoginResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 高驰(COROS) Training Hub 非官方API客户端
 *
 * API端点参考:
 * - https://teamcnapi.coros.com (中国区)
 * - https://teameuapi.coros.com (欧洲区)
 * - https://teamapi.coros.com (美洲区)
 *
 * 统一返回结构: { "result": "0000", "message": ..., "apiCode": ..., "data": {...} }
 * 成功标志是 result == "0000"（没有 code 字段）。
 *
 * 认证方式: 账号密码 + MD5，accountType=2(邮箱)；后续请求带 accessToken 请求头。
 * 详情接口 /activity/detail/query 返回 gzip 压缩体且不带 Content-Type，
 * 必须按 byte[] 接收后手动解压再解析（否则 Spring 报 octet-stream 无转换器）。
 * 参考项目: running_page, rCoros, coros-api, coros-to-sqlite
 *
 * 训练类接口（/training/*）在 accessToken 之外还需要 yfheader 请求头（JSON 字符串
 * {"userId":"<高驰userId>"}），见 trainingHeaders()。
 */
@Slf4j
@Component
public class CorosApiClient {

    private static final String RESULT_OK = "0000";
    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 训练类接口专用客户端：推送课表按天串行，单请求超时适当放大（读 60s） */
    private final RestTemplate trainingRestTemplate;

    public CorosApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.trainingRestTemplate = new RestTemplate(factory);
    }

    private static final Map<String, String> REGION_URL = Map.of(
            "cn", "https://teamcnapi.coros.com",
            "eu", "https://teameuapi.coros.com",
            "us", "https://teamapi.coros.com"
    );

    private String getBaseUrl(String region) {
        // Map.of 不可变映射对 null 键调用 getOrDefault 会抛 NPE，region 缺省时直接落 cn
        if (region == null || region.isBlank()) {
            return REGION_URL.get("cn");
        }
        return REGION_URL.getOrDefault(region, REGION_URL.get("cn"));
    }

    /** 浏览器特征请求头，规避高驰 WAF 拦截 */
    private HttpHeaders browserHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(JSON_UTF8);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        headers.set("Origin", "https://t.coros.com");
        headers.set("Referer", "https://t.coros.com/");
        return headers;
    }

    /** 带 accessToken 的请求头（高驰各接口统一样式） */
    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = browserHeaders();
        headers.set("accessToken", accessToken);
        return headers;
    }

    /** 训练类接口请求头：accessToken + yfheader(JSON 字符串)；corosUserId 为空则不带 yfheader */
    private HttpHeaders trainingHeaders(String accessToken, String corosUserId) {
        HttpHeaders headers = authHeaders(accessToken);
        if (corosUserId != null && !corosUserId.isBlank()) {
            headers.set("yfheader", "{\"userId\":\"" + corosUserId + "\"}");
        }
        return headers;
    }

    /** 高驰统一响应外层结构: {result, message, data}，成功标志 result == "0000" */
    @Data
    private static class CorosEnvelope {
        private String result;
        private String message;
        private Object data;
    }

    /** 训练类接口统一收发：校验 result=="0000"，失败抛 BizException（沿用 failure 风格） */
    private CorosEnvelope trainingExchange(String action, String url, HttpMethod method,
                                           String accessToken, String corosUserId, Object body) {
        try {
            ResponseEntity<CorosEnvelope> response = trainingRestTemplate.exchange(
                    url, method, new HttpEntity<>(body, trainingHeaders(accessToken, corosUserId)),
                    CorosEnvelope.class);
            CorosEnvelope env = response.getBody();
            if (env == null || !RESULT_OK.equals(env.getResult())) {
                throw failure(action, env == null ? null : env.getResult(),
                        env == null ? null : env.getMessage());
            }
            return env;
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("{}HTTP错误: url={}, status={}, body={}", action, url,
                    e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new BizException(action + ": HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("{}异常: url={}", action, url, e);
            throw new BizException(action + ": " + e.getMessage());
        }
    }

    // ==================== 账户 / 训练课表接口 ====================

    /**
     * 查询账户（身体数据）信息
     * 请求: GET /account/query  头: accessToken
     * 成功: data 含 maxHr / stature / weight / rhr / birthday(yyyyMMdd 数字) / nickname / email 等。
     * 端点已用真实 token 实测（200, result=0000, 含 maxHr/stature）。性别(sex/userProfile.gender)不可靠，勿用。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(String accessToken) {
        CorosEnvelope env = trainingExchange("查询高驰账户信息",
                getBaseUrl(null) + "/account/query", HttpMethod.GET, accessToken, null, null);
        if (env.getData() instanceof Map) {
            return (Map<String, Object>) env.getData();
        }
        return new HashMap<>();
    }

    /**
     * 查询训练日历（课表排期）
     * 请求: GET /training/schedule/query?startDate=&endDate=&supportRestExercise=1
     * 参数 startDate/endDate 为 YYYYMMDD；返回 data（含 maxIdInPlan、entities、programs 等）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchedule(String accessToken, String corosUserId,
                                           String startDate, String endDate) {
        String url = String.format("%s/training/schedule/query?startDate=%s&endDate=%s&supportRestExercise=1",
                getBaseUrl(null), startDate, endDate);
        CorosEnvelope env = trainingExchange("查询高驰训练日历", url,
                HttpMethod.GET, accessToken, corosUserId, null);
        if (env.getData() instanceof Map) {
            return (Map<String, Object>) env.getData();
        }
        return new HashMap<>();
    }

    /**
     * 新增可复用训练课表到课表库（POST /training/program/add），返回 data（新课表 id）
     * program 为构造好的课表 dict（name/sportType/exercises/... 及跑步 metadata 扩展块）
     */
    public Object addWorkoutProgram(String accessToken, String corosUserId, Map<String, Object> program) {
        CorosEnvelope env = trainingExchange("新增高驰训练课表",
                getBaseUrl(null) + "/training/program/add",
                HttpMethod.POST, accessToken, corosUserId, program);
        return env.getData();
    }

    /**
     * 排期推送（POST /training/schedule/update）
     * body 结构（参考 coros-mcp _post_schedule_inline）:
     * {entities:[{happenDay,idInPlan,sortNoInSchedule}], programs:[program(含idInPlan)],
     *  versionObjects:[{id:idInPlan,status:1}], pbVersion:2}
     * 注意: 同一天的请求必须串行（idInPlan 按 maxIdInPlan+1 解析，并发会冲突）。
     */
    public void updateSchedule(String accessToken, String corosUserId, Map<String, Object> body) {
        trainingExchange("推送课表到高驰", getBaseUrl(null) + "/training/schedule/update",
                HttpMethod.POST, accessToken, corosUserId, body);
    }

    private BizException failure(String action, String result, String message) {
        String msg = (message == null || message.isBlank())
                ? (result == null ? "响应为空" : "result=" + result)
                : message;
        return new BizException(action + ": " + msg);
    }

    /**
     * 登录高驰账号
     * 请求: POST /account/login
     * 参数: { account, pwd(MD5), accountType=2 }
     * 成功: { "result":"0000", "data": { "accessToken": "...", "userId": "..." } }
     */
    public CorosLoginResponse.LoginResult login(String email, String password, String region) {
        String baseUrl = getBaseUrl(region);

        Map<String, Object> body = new HashMap<>();
        body.put("account", email);
        body.put("pwd", DigestUtil.md5Hex(password).toLowerCase());
        body.put("accountType", 2); // 2=邮箱账号

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, browserHeaders());

        try {
            ResponseEntity<CorosLoginResponse> response = restTemplate.exchange(
                    baseUrl + "/account/login",
                    HttpMethod.POST,
                    entity,
                    CorosLoginResponse.class
            );

            CorosLoginResponse resp = response.getBody();
            if (resp == null || !resp.isSuccess()) {
                throw failure("高驰登录失败", resp == null ? null : resp.getResult(),
                        resp == null ? null : resp.getMessage());
            }
            CorosLoginResponse.LoginResult data = resp.getData();
            if (data == null || data.getAccessToken() == null || data.getAccessToken().isBlank()) {
                throw new BizException("高驰登录失败: 未返回访问令牌");
            }
            return data;
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("高驰登录HTTP错误: status={}, body={}", e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new BizException("高驰登录失败: HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("高驰登录异常", e);
            throw new BizException("高驰登录失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动列表
     * 请求: GET /activity/query?size=&pageNumber=&modeList=
     * 头: accessToken
     */
    public CorosActivityResponse.CorosActivityData queryActivities(
            String accessToken, String region, String modeList, Integer pageNumber, Integer pageSize) {

        String baseUrl = getBaseUrl(region);

        try {
            String url = String.format(
                    "%s/activity/query?size=%d&pageNumber=%d&modeList=%s",
                    baseUrl,
                    pageSize != null ? pageSize : 20,
                    pageNumber != null ? pageNumber : 1,
                    modeList != null ? modeList : ""
            );

            ResponseEntity<CorosActivityResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)),
                    CorosActivityResponse.class
            );

            CorosActivityResponse resp = response.getBody();
            if (resp == null || !RESULT_OK.equals(resp.getResult())) {
                throw failure("查询活动失败", resp == null ? null : resp.getResult(),
                        resp == null ? null : resp.getMessage());
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("查询高驰活动HTTP错误: status={}", e.getStatusCode());
            throw new BizException("查询活动失败: HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("查询高驰活动异常", e);
            throw new BizException("查询活动失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动详情（summary + 分段）
     * 请求: POST /activity/detail/query (form: labelId, userId, sportType)
     * 头: accessToken
     *
     * 注意：该接口响应是 gzip 压缩体且没有 Content-Type（Spring 视作 octet-stream 会拒收），
     * 因此这里按 byte[] 接收，手动判断 gzip 魔数解压后再用 Jackson 解析。
     */
    public CorosActivityResponse.CorosActivityDetailData queryActivityDetail(
            String accessToken, String userId, String region, String labelId, Integer sportType) {

        String baseUrl = getBaseUrl(region);

        HttpHeaders headers = authHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("labelId", labelId);
        if (userId != null && !userId.isBlank()) {
            form.add("userId", userId);
        }
        form.add("sportType", sportType != null ? String.valueOf(sportType) : "100");

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    baseUrl + "/activity/detail/query",
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    byte[].class
            );

            String json = decodeBody(response.getBody());
            CorosActivityResponse.CorosActivityDetail resp =
                    objectMapper.readValue(json, CorosActivityResponse.CorosActivityDetail.class);
            if (resp == null || !RESULT_OK.equals(resp.getResult())) {
                throw failure("查询活动详情失败", resp == null ? null : resp.getResult(),
                        resp == null ? null : resp.getMessage());
            }
            return resp.getData();
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("查询高驰活动详情HTTP错误: labelId={}, status={}", labelId, e.getStatusCode());
            throw new BizException("查询活动详情失败: HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("查询高驰活动详情异常: labelId={}", labelId, e);
            throw new BizException("查询活动详情失败: " + e.getMessage());
        }
    }

    /** 响应体若为 gzip（魔数 1f 8b）则解压，否则按 UTF-8 原样返回 */
    private String decodeBody(byte[] body) throws IOException {
        if (body == null || body.length == 0) {
            return "";
        }
        if (body.length >= 2 && (body[0] & 0xFF) == 0x1F && (body[1] & 0xFF) == 0x8B) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(body));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                return out.toString(StandardCharsets.UTF_8);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
