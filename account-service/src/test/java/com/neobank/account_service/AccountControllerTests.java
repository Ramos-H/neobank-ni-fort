package com.neobank.account_service;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountRepository accountRepository;

	@Test
	void returnsBalanceForExistingAccount() throws Exception {
		when(accountRepository.findByAccountId("001"))
				.thenReturn(Optional.of(account("001", "maye", "5000.00")));

		mockMvc.perform(get("/api/accounts/001/balance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountId").value("001"))
				.andExpect(jsonPath("$.balance").value(5000.00));
	}

	@Test
	void returnsNotFoundForUnknownAccount() throws Exception {
		when(accountRepository.findByAccountId("999")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/accounts/999/balance"))
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsOwnerAndBalanceForExistingAccount() throws Exception {
		when(accountRepository.findByAccountId("002"))
				.thenReturn(Optional.of(account("002", "maria", "3200.50")));

		mockMvc.perform(get("/api/accounts/002/balance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ownerUsername").value("maria"))
				.andExpect(jsonPath("$.balance").value(3200.50));
	}

	@Test
	void creditIncreasesBalanceForExistingAccount() throws Exception {
		when(accountRepository.findByAccountId("001"))
				.thenReturn(Optional.of(account("001", "maye", "5000.00")));

		mockMvc.perform(post("/api/accounts/001/credit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":500.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(5500.00));
	}

	@Test
	void creditReturnsNotFoundForUnknownAccount() throws Exception {
		when(accountRepository.findByAccountId("999")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/accounts/999/credit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":500.00}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void creditWithZeroAmountLeavesBalanceUnchanged() throws Exception {
		when(accountRepository.findByAccountId("001"))
				.thenReturn(Optional.of(account("001", "maye", "5000.00")));

		mockMvc.perform(post("/api/accounts/001/credit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":0.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(5000.00));
	}

	@Test
	void debitDecreasesBalanceWhenFundsAreSufficient() throws Exception {
		when(accountRepository.findByAccountId("001"))
				.thenReturn(Optional.of(account("001", "maye", "1000.00")));

		mockMvc.perform(post("/api/accounts/001/debit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":300.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(700.00));
	}

	@Test
	void debitReturnsBadRequestWhenFundsAreInsufficient() throws Exception {
		when(accountRepository.findByAccountId("001"))
				.thenReturn(Optional.of(account("001", "maye", "100.00")));

		mockMvc.perform(post("/api/accounts/001/debit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":500.00}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void debitReturnsNotFoundForUnknownAccount() throws Exception {
		when(accountRepository.findByAccountId("999")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/accounts/999/debit")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":100.00}"))
				.andExpect(status().isNotFound());
	}

	@Test
    void registerAccountCreatesFirstAccountWithFormattedIdAndInitialBalance() throws Exception {
        when(accountRepository.count()).thenReturn(0L);
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/accounts/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customerId\":1,\"ownerUsername\":\"juan\",\"initialBalance\":1000.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.accountId").value("001"))
                .andExpect(jsonPath("$.ownerUsername").value("juan"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void registerAccountIncrementsGlobalAccountIdSequence() throws Exception {
        when(accountRepository.count()).thenReturn(2L); // 2 existing accounts -> yields "003"
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/accounts/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customerId\":2,\"ownerUsername\":\"maria\",\"initialBalance\":500.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.accountId").value("003"))
                .andExpect(jsonPath("$.ownerUsername").value("maria"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void registerAccountDefaultsToZeroBalanceWhenInitialBalanceNotProvided() throws Exception {
        when(accountRepository.count()).thenReturn(0L);
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/accounts/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customerId\":1,\"ownerUsername\":\"juan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value("001"))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

	private Account account(String accountId, String ownerUsername, String balance) {
		return new Account(1L, accountId, ownerUsername, new BigDecimal(balance));
	}
}
