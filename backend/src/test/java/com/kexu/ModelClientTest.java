package com.kexu;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class ModelClientTest {
    @Test void deepseekBaseAddressesResolveToCompletions() {
        assertEquals("https://api.deepseek.com/chat/completions", ModelClient.endpoint(URI.create("https://api.deepseek.com")).toString());
        assertEquals("https://api.deepseek.com/v1/chat/completions", ModelClient.endpoint(URI.create("https://api.deepseek.com/v1/")).toString());
        assertEquals("https://api.deepseek.com/chat/completions", ModelClient.endpoint(URI.create("https://api.deepseek.com/chat/completions")).toString());
    }
    @Test void forwardsSuccessfulResponsesAndExplainsErrorsWithoutLeakingBody() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/success", exchange -> {
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body="{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.createContext("/failure", exchange -> {
            byte[] body="sensitive test-key".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(402,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.start();
        try {
            ModelClient client=new ModelClient();String base="http://127.0.0.1:"+server.getAddress().getPort();
            assertTrue(((Map<?,?>)client.chat(URI.create(base+"/success"),"test-key","model","role","hello",0.7)).containsKey("choices"));
            var e=assertThrows(ResponseStatusException.class,()->client.chat(URI.create(base+"/failure"),"test-key","model","role","hello",0.7));
            assertTrue(e.getReason().contains("余额不足"));assertFalse(e.getReason().contains("test-key"));
        } finally {server.stop(0);}
    }
}
