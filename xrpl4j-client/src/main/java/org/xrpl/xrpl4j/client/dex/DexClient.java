package org.xrpl.xrpl4j.client.dex;

import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.dex.model.Balance;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.transactions.Address;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DexClient {

  private final BigDecimal XRP_BASE_RESERVE = new BigDecimal("20");
  private final BigDecimal ACCOUNT_OBJECT_RESERVE = new BigDecimal("5");

  private XrplClient xrplClient;

  public DexClient(XrplClient xrplClient) {
    this.xrplClient = xrplClient;
  }

  public List<Balance> getBalances(Address address) throws JsonRpcClientErrorException {
    List<Balance> balances = getIssuedCurrencyBalances(address);
    balances.add(getXrpBalance(address));
    return balances;
  }

  private Balance getXrpBalance(Address address) throws JsonRpcClientErrorException {
    AccountInfoResult accountInfo = xrplClient.accountInfo(AccountInfoRequestParams.builder()
      .account(address)
      .ledgerIndex(LedgerIndex.VALIDATED)
      .build());

    BigDecimal total = accountInfo.accountData().balance().toXrp();
    BigDecimal locked = getReserve(address);
    BigDecimal available = total.subtract(locked);
    return Balance.builder()
      .currency("XRP")
      .total(total)
      .locked(locked)
      .available(available)
      .build();
  }

  private BigDecimal getReserve(Address address) throws JsonRpcClientErrorException {
    AccountInfoResult accountObjects = xrplClient.accountInfo(AccountInfoRequestParams.builder()
      .account(address)
      .ledgerIndex(LedgerIndex.VALIDATED)
      .build()
    );

    long ownerCount = accountObjects.accountData().ownerCount().longValue();
    return XRP_BASE_RESERVE
      .add(ACCOUNT_OBJECT_RESERVE.multiply(BigDecimal.valueOf(ownerCount)));
  }

  private List<Balance> getIssuedCurrencyBalances(Address address) throws JsonRpcClientErrorException {
    AccountLinesResult accountLines = xrplClient.accountLines(AccountLinesRequestParams.builder()
      .account(address)
      .ledgerIndex(LedgerIndex.VALIDATED)
      .build());

    // TODO figure out amount that is locked on order book
    Map<String, List<Balance>> balances = accountLines.lines()
      .stream()
      .map(line -> {
        BigDecimal total = new BigDecimal(line.balance());
        // TODO figure out amount that is locked on order book
        BigDecimal locked = BigDecimal.ZERO;
        BigDecimal available = total.subtract(locked);

        return Balance.builder()
          .available(available)
          .locked(locked)
          .total(total)
          .currency(line.currency())
          .build();
      })
      .collect(Collectors.groupingBy(Balance::currency));

    return balances.entrySet().stream()
      .map(entry ->
        entry.getValue().stream()
          .reduce(Balance.zeroBalance(entry.getKey()), (left, right) -> left.add(right))
      )
      .collect(Collectors.toList());
  }

}