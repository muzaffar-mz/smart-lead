package com.muzaffar.smartlead.controller;

import com.muzaffar.smartlead.config.SecurityConfig;
import com.muzaffar.smartlead.model.BaseResponse;
import com.muzaffar.smartlead.service.test.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import com.muzaffar.smartlead.service.test.TestMessagesRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test")
public class TestController {

    private final TestService testService;

    @GetMapping("/seed-messages")
    @Operation(summary = "Submit test messages",
            description = "Submits 'count' messages with a random contact and a text from test-phrases.txt. "
                    + "Besides X-API-KEY it needs the X-TEST-KEY header.",
            parameters = @Parameter(name = SecurityConfig.TEST_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Key that protects the test endpoints"))
    public BaseResponse<Integer> seedMessages(@Valid @ModelAttribute TestMessagesRequest request,
                                              HttpServletRequest httpRequest) {
        return BaseResponse.payload(
                testService.submitTestMessages(request.count(), httpRequest.getRemoteAddr()));
    }

}
