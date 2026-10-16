package com.muzaffar.smartlead.model.request;

import com.muzaffar.smartlead.validation.ValidContact;
import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
        @NotBlank(message = "Contact details cannot be blank") @ValidContact String contact,
        @NotBlank(message = "Message cannot be blank") String message
) {
}
