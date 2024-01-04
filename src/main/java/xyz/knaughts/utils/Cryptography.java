package xyz.knaughts.utils;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.LibraryLoader;
import xyz.knaughts.KnaughtsBot;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A utility class for dealing with cryptography. Knaughts bot uses <a href="https://github.com/terl/lazysodium-java">lazysodium</a>
 * which is a Java wrapper for the cryptography library <a href="https://libsodium.gitbook.io/doc/">libsodium</a>.
 */
public final class Cryptography {

    /**
     * The almighty.
     */
    private static Key KEY = null;

    /**
     * The sodium instance.
     */
    private static final LazySodiumJava sodium = new LazySodiumJava(new SodiumJava(LibraryLoader.Mode.BUNDLED_ONLY), StandardCharsets.UTF_8);

    private Cryptography() {
    }

    /**
     * Initializes the cryptography key by reading a console input with echoing disabled.
     * Alternatively, you can read it from elsewhere such as a config/env file or a dedicated
     * on/offline keystore.
     */
    public static void init() {
        KnaughtsBot.LOG.info("Initializing cryptography key...");

        Console console = System.console();
        if (console == null)
            throw new RuntimeException("No console found. Please run this program from a terminal (not an IDE).");

        System.console().printf("Enter the private cryptography key in hexadecimal format:");
        String hexKey = Arrays.toString(console.readPassword());
        KEY = Key.fromHexString(hexKey);

        KnaughtsBot.LOG.info("Cryptography key initialized.");
    }

    /**
     * Encrypts a string using the cryptography key.
     * @param str the string to encrypt
     * @return the encrypted string
     * @throws SodiumException if the encryption fails
     */
    public static String encrypt(String str) throws SodiumException {
        final byte[] nonce = randNonce();
        final var nonceStr = sodium.sodiumBin2Hex(nonce);
        return nonceStr + sodium.cryptoSecretBoxEasy(str, nonce, KEY);
    }

    /**
     * Decrypts a string using the cryptography key.
     * @param str the string to decrypt
     * @return the decrypted string
     * @throws SodiumException if the decryption fails
     */
    public static String decrypt(String str) throws SodiumException {
        final var nonce = sodium.sodiumHex2Bin(str.substring(0, 64));
        return sodium.cryptoSecretBoxOpenEasy(str.substring(64), nonce, KEY);
    }

    /**
     * Generates a random nonce.
     * @return the nonce
     */
    public static byte[] randNonce() {
        return sodium.randomBytesBuf(32);
    }
}
