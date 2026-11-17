package com.kexu;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Connects to an already validated model endpoint without exposing upstream bodies or credentials. */
class ModelClient {
    private final RestClient client;
    ModelClient() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(180));
        client = RestClient.builder().requestFactory(factory).build();
    }

    static URI endpoint(URI uri) {
        if ("api.deepseek.com".equalsIgnoreCase(uri.getHost())) {
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/"))
                return uri.resolve("/chat/completions");
            if (path.equals("/v1") || path.equals("/v1/"))
                return uri.resolve("/v1/chat/completions");
        }
        return uri;
    }

    Object chat(URI uri, String key, String model, String prompt, String message, double temperature) {
        try {
            Map<?, ?> response = client.post().uri(endpoint(uri))
                    .header("Authorization", "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "temperature", temperature,
                            "messages", List.of(Map.of("role", "system", "content", prompt),
                                                Map.of("role", "user", "content", message))))
                    .retrieve().onStatus(status -> status.is3xxRedirection(), (request, result) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型服务返回重定向，请检查 API 地址");
                    }).body(Map.class);
            if (response == null || response.containsKey("error"))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型服务未能生成回复，请稍后重试");
            return response;
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, upstreamError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接模型服务或请求超时，请检查服务器网络后重试");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型服务返回的数据无法解析，请检查 API 地址和兼容性");
        }
    }

    static String upstreamError(int status) {
        return switch (status) {
            case 400, 422 -> "模型服务拒绝请求（HTTP " + status + "），请检查模型名称、地址及参数";
            case 401 -> "模型服务认证失败（HTTP 401），请检查 API 密钥";
            case 402 -> "模型服务账号余额不足（HTTP 402），请检查供应商账户余额";
            case 403 -> "模型服务拒绝访问（HTTP 403），请检查 API 密钥权限";
            case 404 -> "模型接口不存在（HTTP 404），请检查 API 地址；DeepSeek 地址为 https://api.deepseek.com/chat/completions";
            case 408, 504 -> "模型服务请求超时（HTTP " + status + "），请稍后重试";
            case 429 -> "模型服务请求过于频繁（HTTP 429），请稍后重试";
            case 500, 502, 503 -> "模型服务暂时不可用（HTTP " + status + "），请稍后重试";
            default -> "模型服务请求失败（HTTP " + status + "），请检查模型配置或联系服务商";
        };
    }
}
