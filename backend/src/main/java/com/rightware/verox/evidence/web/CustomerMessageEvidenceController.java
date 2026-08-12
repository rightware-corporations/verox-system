package com.rightware.verox.evidence.web;

import com.rightware.verox.evidence.application.CustomerMessageEvidenceIngestionService;
import com.rightware.verox.evidence.application.CustomerMessageEvidenceView;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/v1/checkout")
public class CustomerMessageEvidenceController {

    public static final String CHECKOUT_CAPABILITY_HEADER = "VEROX-Checkout-Capability";

    private final CustomerMessageEvidenceIngestionService ingestionService;

    public CustomerMessageEvidenceController(CustomerMessageEvidenceIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(
        value = "/{checkoutSessionId}/evidence/message",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CustomerMessageEvidenceView> ingest(
        @PathVariable String checkoutSessionId,
        @RequestHeader(value = CHECKOUT_CAPABILITY_HEADER, required = false) String checkoutCapability,
        @Valid @RequestBody CustomerMessageEvidenceRequest request
    ) {
        CustomerMessageEvidenceView view = ingestionService.ingest(
            checkoutSessionId,
            checkoutCapability,
            request.content()
        );
        return ResponseEntity.accepted().body(view);
    }
}
