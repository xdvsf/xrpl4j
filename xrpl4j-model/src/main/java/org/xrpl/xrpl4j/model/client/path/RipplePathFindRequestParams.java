package org.xrpl.xrpl4j.model.client.path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.xrpl.xrpl4j.model.client.LegacyLedgerSpecifierUtils;
import org.xrpl.xrpl4j.model.client.XrplRequestParams;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerIndexShortcut;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.CurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.Hash256;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Request parameters for a "ripple_path_find" rippled API method call.
 *
 * <p>This method is only enabled in the JSON RPC API.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableRipplePathFindRequestParams.class)
@JsonDeserialize(as = ImmutableRipplePathFindRequestParams.class)
public interface RipplePathFindRequestParams extends XrplRequestParams {

  /**
   * Construct a builder for this class.
   *
   * @return An {@link ImmutableRipplePathFindRequestParams.Builder}.
   */
  static ImmutableRipplePathFindRequestParams.Builder builder() {
    return ImmutableRipplePathFindRequestParams.builder();
  }

  /**
   * Unique {@link Address} of the account that would send funds in a transaction.
   *
   * @return The unique {@link Address} of the source account.
   */
  @JsonProperty("source_account")
  Address sourceAccount();

  /**
   * Unique {@link Address} of the account that would receive funds in a transaction.
   *
   * @return The unique {@link Address} of the destination account.
   */
  @JsonProperty("destination_account")
  Address destinationAccount();

  /**
   * {@link CurrencyAmount} that the destination account would receive in a transaction.
   *
   * <p>Special case: You can specify "-1" (for XRP) or provide "-1" as the contents of
   * {@link org.xrpl.xrpl4j.model.transactions.IssuedCurrencyAmount#value()} (for non-XRP currencies).
   * This requests a path to deliver as much as possible, while spending no more than the amount specified in
   * {@link #sendMax()} (if provided).
   *
   * @return A {@link CurrencyAmount} denoting the destination amount.
   */
  // TODO: XrpCurrencyAmount currently doesn't support negative values, so one would not be able to set this to
  //  "-1" for XRP.  We should still type XrpCurrencyAmount as a wrapper of a String to allow for this.
  @JsonProperty("destination_amount")
  CurrencyAmount destinationAmount();

  /**
   * {@link CurrencyAmount} that would be spent in the transaction. Cannot be used with {@link #sourceCurrencies()}.
   *
   * @return A {@link CurrencyAmount} denoting the send max.
   */
  @JsonProperty("send_max")
  Optional<CurrencyAmount> sendMax();

  /**
   * A {@link List} of {@link PathCurrency} that the source account might want to spend.
   *
   * <p>Cannot contain more than 18 source currencies.
   *
   * @return A {@link List} of {@link PathCurrency} containing all of the source currencies.
   */
  @JsonProperty("source_currencies")
  List<PathCurrency> sourceCurrencies();

  /**
   * A 20-byte hex string for the ledger version to use.
   *
   * @return An optionally-present {@link Hash256}.
   * @deprecated Ledger hash should be specified in {@link #ledgerSpecifier()}.
   */
  @JsonIgnore
  @Deprecated
  @Value.Auxiliary
  Optional<Hash256> ledgerHash();

  /**
   * The ledger index of the ledger to use, or a shortcut string to choose a ledger automatically.
   *
   * @return A {@link LedgerIndex}.  Defaults to {@link LedgerIndex#CURRENT}.
   * @deprecated Ledger index and any shortcut values should be specified in {@link #ledgerSpecifier()}.
   */
  @JsonIgnore
  @Deprecated
  @Nullable
  @Value.Auxiliary
  LedgerIndex ledgerIndex();

  /**
   * Specifies the ledger version to request. A ledger version can be specified by ledger hash,
   * numerical ledger index, or a shortcut value.
   *
   * @return A {@link LedgerSpecifier} specifying the ledger version to request.
   */
  @Value.Default
  @JsonUnwrapped
  default LedgerSpecifier ledgerSpecifier() {
    return LegacyLedgerSpecifierUtils.computeLedgerSpecifier(ledgerHash(), ledgerIndex());
  }
}
