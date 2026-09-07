package com.neobank.transaction_service;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TransactionServiceApplicationTests {

	private static final String FROM_ACCOUNT_ID = "003";
	private static final String TO_ACCOUNT_ID = "004";
	private static final BigDecimal AMOUNT = new BigDecimal("125.50");

	@Mock
	private TransactionRepository repository;

	@Mock
	private RestTemplate restTemplate;

	private TransactionController controller;

	@BeforeEach
	void setUp() {
		controller = new TransactionController(repository, restTemplate);
	}

	@Test
	void transfer_debitsCreditsAndSavesSuccessfulTransaction() {
		TransferRequest request = transferRequest();

		ResponseEntity<?> response = controller.transfer(request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		Transaction transaction = (Transaction) response.getBody();
		assertNotNull(transaction);
		assertEquals(FROM_ACCOUNT_ID, transaction.getFromAccountId());
		assertEquals(TO_ACCOUNT_ID, transaction.getToAccountId());
		assertEquals(AMOUNT, transaction.getAmount());
		assertEquals("SUCCESS", transaction.getStatus());

		verify(restTemplate).postForObject(
				eq("http://account-service/api/accounts/1/debit"),
				eq(Map.of("amount", AMOUNT)),
				eq(Object.class));
		verify(restTemplate).postForObject(
				eq("http://account-service/api/accounts/2/credit"),
				eq(Map.of("amount", AMOUNT)),
				eq(Object.class));
		verify(repository).save(transaction);
	}

	@Test
	void transfer_whenDebitFails_savesFailedTransactionAndDoesNotCredit() {
		TransferRequest request = transferRequest();
		when(restTemplate.postForObject(
				eq("http://account-service/api/accounts/1/debit"),
				any(Map.class),
				eq(Object.class)))
				.thenThrow(new RuntimeException("insufficient funds"));

		ResponseEntity<?> response = controller.transfer(request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Transfer failed: insufficient funds", response.getBody());
		verify(restTemplate, never()).postForObject(
				eq("http://account-service/api/accounts/2/credit"),
				any(Map.class),
				eq(Object.class));
		verifyFailedTransactionSaved();
	}

	@Test
	void transfer_whenCreditFails_savesFailedTransaction() {
		TransferRequest request = transferRequest();
		when(restTemplate.postForObject(
				eq("http://account-service/api/accounts/2/credit"),
				any(Map.class),
				eq(Object.class)))
				.thenThrow(new RuntimeException("destination account unavailable"));

		ResponseEntity<?> response = controller.transfer(request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Transfer failed: destination account unavailable", response.getBody());
		verifyFailedTransactionSaved();
	}

	private TransferRequest transferRequest() {
		TransferRequest request = new TransferRequest();
		request.setFromAccountId(FROM_ACCOUNT_ID);
		request.setToAccountId(TO_ACCOUNT_ID);
		request.setAmount(AMOUNT);
		return request;
	}

	private void verifyFailedTransactionSaved() {
		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(repository).save(transactionCaptor.capture());
		Transaction transaction = transactionCaptor.getValue();
		assertEquals(FROM_ACCOUNT_ID, transaction.getFromAccountId());
		assertEquals(TO_ACCOUNT_ID, transaction.getToAccountId());
		assertEquals(AMOUNT, transaction.getAmount());
		assertEquals("FAILED", transaction.getStatus());
	}
}
