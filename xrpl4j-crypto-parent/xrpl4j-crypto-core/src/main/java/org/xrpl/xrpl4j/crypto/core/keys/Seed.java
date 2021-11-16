package org.xrpl.xrpl4j.crypto.core.keys;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedInteger;
import org.xrpl.xrpl4j.codec.addresses.AddressBase58;
import org.xrpl.xrpl4j.codec.addresses.Base58;
import org.xrpl.xrpl4j.codec.addresses.Decoded;
import org.xrpl.xrpl4j.codec.addresses.SeedCodec;
import org.xrpl.xrpl4j.codec.addresses.UnsignedByteArray;
import org.xrpl.xrpl4j.codec.addresses.Version;

import java.util.Objects;

/**
 * A compact value that is used to derive the actual private and public keys for an account.
 */
public class Seed implements javax.security.auth.Destroyable {

  // This holds all of the bytes of a Seed (e.g., entropy bytes, version type, etc).
  private final UnsignedByteArray value;
  private boolean destroyed;

  /**
   * Construct an Ed25519-compatible {@link Seed} from the supplied {@code passphrase}.
   *
   * @param passphrase A {@link Passphrase} to generate a seed from.
   *
   * @return A {@link Seed}.
   */
  public static Seed ed25519SeedFromPassphrase(final Passphrase passphrase) {
    Objects.requireNonNull(passphrase);

    final byte[] entropyBytes = new byte[16];

    // 16 bytes of deterministic entropy.
    Hashing.sha512()
      .hashBytes(passphrase.value())
      .writeBytesTo(entropyBytes, 0, 16);

    return ed25519SeedFromEntropy(Entropy.of(entropyBytes));
  }

  /**
   * Construct an SECP256K1-compatible {@link Seed} from the supplied {@link Passphrase}.
   *
   * @param passphrase A {@link Passphrase} to generate a seed from.
   *
   * @return A {@link Seed}.
   */
  public static Seed secp256k1SeedFromPassphrase(final Passphrase passphrase) {
    Objects.requireNonNull(passphrase);

    final byte[] entropyBytes = new byte[16];

    // 16 bytes of deterministic entropy.
    Hashing.sha512()
      .hashBytes(passphrase.value())
      .writeBytesTo(entropyBytes, 0, 16);

    return secp256k1SeedFromEntropy(Entropy.of(entropyBytes));
  }

  /**
   * Construct an Ed25519-compatible {@link Seed} from the supplied {@code passphrase}.
   *
   * @param entropy A {@link Entropy} to generate a {@link Seed} from.
   *
   * @return A {@link Seed}.
   */
  public static Seed ed25519SeedFromEntropy(final Entropy entropy) {
    Objects.requireNonNull(entropy);

    final String base58EncodedSeed = AddressBase58.encode(
      entropy.value(),
      Lists.newArrayList(Version.ED25519_SEED),
      UnsignedInteger.valueOf(entropy.value().length())
    );

    return new Seed(UnsignedByteArray.of(AddressBase58.decode(base58EncodedSeed)));
  }

  /**
   * Construct an SECP256K1-compatible {@link Seed} from the supplied {@link Passphrase}.
   *
   * @param entropy A {@link Entropy} to generate a {@link Seed} from.
   *
   * @return A {@link Seed}.
   */
  public static Seed secp256k1SeedFromEntropy(final Entropy entropy) {
    Objects.requireNonNull(entropy);

    final String base58EncodedSeed = AddressBase58.encode(
      entropy.value(),
      Lists.newArrayList(Version.FAMILY_SEED),
      UnsignedInteger.valueOf(entropy.value().length())
    );

    return new Seed(UnsignedByteArray.of(Base58.decode(base58EncodedSeed)));
  }

  /**
   * Required-args Constructor. Purposefully package-private (use a static method instead for construction).
   *
   * @param value This seed's full binary value (including the entropy bytes and versionType).
   */
  public Seed(final UnsignedByteArray value) {
    this.value = Objects.requireNonNull(value);
  }

  /**
   * Copy constructor.
   *
   * @param seed This seed's full binary value (including the entropy bytes and versionType).
   */
  @VisibleForTesting
  Seed(final Seed seed) {
    this.value = Objects.requireNonNull(seed.decodedSeed().bytes());
  }

  /**
   * The decoded details of this seed.
   *
   * @return An instance of {@link Decoded}.
   */
  public Decoded decodedSeed() {
    final byte[] copiedByteValue = new byte[this.value.length()];
    System.arraycopy(this.value.toByteArray(), 0, copiedByteValue, 0, value.length());

    return SeedCodec.getInstance().decodeSeed(
      Base58.encode(copiedByteValue)
    );
  }

  @Override
  public final void destroy() {
    this.value.destroy();
    this.destroyed = true;
  }

  @Override
  public final boolean isDestroyed() {
    return this.destroyed;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Seed)) {
      return false;
    }

    Seed that = (Seed) obj;
    return that.value.equals(this.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return "Seed{" +
      "value=[redacted]" +
      ", destroyed=" + destroyed +
      '}';
  }
}