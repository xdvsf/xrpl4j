package org.xrpl.xrpl4j.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.dex.DexClient;
import org.xrpl.xrpl4j.client.dex.model.Balance;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.flags.Flags;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.IssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.OfferCreate;
import org.xrpl.xrpl4j.model.transactions.Transaction;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;
import org.xrpl.xrpl4j.wallet.Wallet;

import java.math.BigDecimal;
import java.util.List;

public class DexClientIT extends AbstractIT {

  private DexClient dexClient = new DexClient(xrplClient);

  private static final String USD = "USD";
  private static final String XRP = "XRP";

  @Test
  public void testSingleCurrencyBalance() throws JsonRpcClientErrorException {
    Wallet issuer = this.createRandomAccount();
    Wallet purchaser = this.createRandomAccount();

    issueCurrency(issuer, USD);

    buyIssuedCurrency(purchaser, USD, issuer.classicAddress());

    List<Balance> balances = dexClient.getBalances(purchaser.classicAddress());

    BigDecimal usdBalance = new BigDecimal("0.5");
    BigDecimal xrpBalance = new BigDecimal("998.99999");
    BigDecimal reserve = new BigDecimal("25");

    assertThat(balances).isNotEmpty()
      .containsExactlyInAnyOrder(
        Balance.builder().currency(XRP)
          .available(xrpBalance.subtract(reserve))
          .total(xrpBalance)
          .locked(reserve)
          .build(),
        Balance.builder().currency(USD)
          .available(usdBalance)
          .total(usdBalance)
          .locked(BigDecimal.ZERO)
          .build()
      );
  }

  @Test
  public void testSingleCurrencyMultipleIssuerBalance() throws JsonRpcClientErrorException {
    Wallet issuerOne = this.createRandomAccount();
    Wallet issuerTwo = this.createRandomAccount();
    Wallet purchaser = this.createRandomAccount();

    String USD = "USD";
    issueCurrency(issuerOne, USD);
    issueCurrency(issuerTwo, USD);

    buyIssuedCurrency(purchaser, USD, issuerOne.classicAddress());
    buyIssuedCurrency(purchaser, USD, issuerTwo.classicAddress());

    List<Balance> balances = dexClient.getBalances(purchaser.classicAddress());
    BigDecimal usdBalance = new BigDecimal("1");
    BigDecimal xrpBalance = new BigDecimal("997.99998");
    BigDecimal reserve = new BigDecimal("30");

    assertThat(balances).isNotEmpty()
      .containsExactlyInAnyOrder(
        Balance.builder().currency(XRP)
          .available(xrpBalance.subtract(reserve).stripTrailingZeros())
          .total(xrpBalance)
          .locked(reserve)
          .build(),
        Balance.builder().currency(USD)
          .available(usdBalance)
          .total(usdBalance)
          .locked(BigDecimal.ZERO)
          .build()
      );
  }

  private void buyIssuedCurrency(Wallet purchaser, String currency, Address issuer) throws JsonRpcClientErrorException {
    FeeResult feeResult = xrplClient.fee();
    AccountInfoResult accountInfoResult =
      this.scanForResult(() -> this.getCurrentAccountInfo(purchaser.classicAddress()));

    //////////////////////
    // Create an Offer
    UnsignedInteger sequence = accountInfoResult.accountData().sequence();
    OfferCreate offerCreate = OfferCreate.builder()
      .account(purchaser.classicAddress())
      .fee(feeResult.drops().minimumFee())
      .sequence(sequence)
      .signingPublicKey(purchaser.publicKey())
      .takerGets(XrpCurrencyAmount.ofXrp(BigDecimal.ONE)
      )
      .takerPays(
        IssuedCurrencyAmount.builder()
          .currency(currency)
          .issuer(issuer)
          .value("0.01")
          .build()
      )
      .flags(Flags.OfferCreateFlags.builder()
        .tfFullyCanonicalSig(true)
        .tfSell(true)
        .build())
      .build();

    // OFFER anyone who pays 0.01 USD can get 1 XRP  (at most 100 XRP per USD)

    SubmitResult<OfferCreate> response = xrplClient.submit(purchaser, offerCreate);
    logger.info("offer transaction {} response {}", response.transactionResult().transaction().hash(), response.result());
    assertThat(response.result()).isEqualTo("tesSUCCESS");

    awaitValidatedTransaction(response, OfferCreate.class);
  }

  private void awaitValidatedTransaction(SubmitResult<?> response, Class<? extends Transaction> trxClass) {
    this.scanForResult(
      () -> getValidatedTransaction(
        response.transactionResult().transaction().hash().orElseThrow(() -> new IllegalStateException("no hash found")), trxClass)
    );
  }

  private void issueCurrency(Wallet issuerWallet, String currency) throws JsonRpcClientErrorException {
    AccountInfoResult accountInfoResult =
      this.scanForResult(() -> this.getCurrentAccountInfo(issuerWallet.classicAddress()));

    //////////////////////
    // Create an Offer
    UnsignedInteger sequence = accountInfoResult.accountData().sequence();
    OfferCreate offerCreate = OfferCreate.builder()
      .account(issuerWallet.classicAddress())
      .fee(XrpCurrencyAmount.of(UnsignedLong.valueOf(10)))
      .sequence(sequence)
      .signingPublicKey(issuerWallet.publicKey())
      .takerGets(IssuedCurrencyAmount.builder()
        .currency(currency)
        .issuer(issuerWallet.classicAddress())
        .value("100")
        .build()
      )
      .takerPays(XrpCurrencyAmount.ofXrp(BigDecimal.valueOf(200.0)))
      .flags(Flags.OfferCreateFlags.builder()
        .tfFullyCanonicalSig(true)
        .tfSell(true)
        .build())
      .build();


    // OFFER anyone who pays 200 XRP can get 100 USD (at least 2 XRP PER USD)

    SubmitResult<OfferCreate> response = xrplClient.submit(issuerWallet, offerCreate);
    logger.info("sell offer transaction {} response {}", response.transactionResult().transaction().hash(), response.result());
    assertThat(response.result()).isEqualTo("tesSUCCESS");

    awaitValidatedTransaction(response, OfferCreate.class);
  }


}