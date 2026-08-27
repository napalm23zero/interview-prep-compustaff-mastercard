package dev.hustletech.interview.compustaff.account.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.repository.AccountRepository;
import dev.hustletech.interview.compustaff.shared.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    public Account findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<Account> findAll() {
        return repository.findAllByOrderByIdAsc();
    }

}
