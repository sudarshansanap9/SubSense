package com.subsense.transaction.controller;

import com.subsense.transaction.dto.TransactionRequest;
import com.subsense.transaction.dto.TransactionResponse;
import com.subsense.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*") // Local dev fallback
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> ingestTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.processTransaction(request);
        
        if ("DUPLICATE_IGNORED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
