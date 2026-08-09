package com.rightware.verox.bridge.web;

import com.rightware.verox.bridge.application.BridgeEvidenceIngestionService;
import com.rightware.verox.bridge.application.BridgeEvidenceView;
import com.rightware.verox.bridge.application.BridgePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bridges")
public class BridgeEvidenceController {

    private final BridgeEvidenceIngestionService ingestionService;

    public BridgeEvidenceController(BridgeEvidenceIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/{bridgeId}/evidence")
    public ResponseEntity<BridgeEvidenceView> ingest(
        @PathVariable String bridgeId,
        @AuthenticationPrincipal BridgePrincipal principal,
        @Valid @RequestBody BridgeEvidenceRequest request
    ) {
        BridgeEvidenceView view = ingestionService.ingest(
            principal,
            bridgeId,
            request.content(),
            request.receivedAt()
        );
        return ResponseEntity.accepted().body(view);
    }
}
