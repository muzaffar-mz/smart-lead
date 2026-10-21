package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.model.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Admin login for the web page. Needs no X-API-KEY: it is how the page gets one. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    public record LoginRequest(String login, String password) {
    }

    public record LoginResponse(String apiKey) {
    }

    private final String login;
    private final String password;
    private final String apiKey;

    public PublicController(@Value("${spring.security.user.name}") String login,
                            @Value("${spring.security.user.password}") String password,
                            @Value("${security.api-key}") String apiKey) {
        this.login = login;
        this.password = password;
        this.apiKey = apiKey;
    }

    /** Same credentials as Swagger. On success the page gets the API key it needs to read the data. */
    @PostMapping("/admin/login")
    public ResponseEntity<BaseResponse<?>> adminLogin(@RequestBody LoginRequest request) {
        if (request == null || !same(login, request.login()) | !same(password, request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error(ErrorConstant.UNAUTHORIZED.getCode(), "Invalid login or password"));
        }
        return ResponseEntity.ok(BaseResponse.payload(new LoginResponse(apiKey)));
    }

    private static boolean same(String expected, String provided) {
        return provided != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
}
