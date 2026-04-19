package org.example;

import java.math.BigInteger;
import java.security.SecureRandom;

record RSAPrimePair(BigInteger p, BigInteger q) {

    public BigInteger n() {
        return p.multiply(q);
    }

    public BigInteger phiN() {
        return p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
    }

    public BigInteger d(BigInteger e) {
        return e.modInverse(phiN());
    }

}

public class RSADemo {

    public final int bitLength = 8;
    public final SecureRandom random = new SecureRandom();
    private final BigInteger e = new BigInteger("65527");

    public RSAPrimePair generatePair() {
        var p = BigInteger.probablePrime(bitLength, random);
        var q = BigInteger.probablePrime(bitLength, random);
        return new RSAPrimePair(p, q);
    }

    public void run() {
        var pair = generatePair();
        var d = pair.d(e);

        var msg = new BigInteger("10000");
        var encrypted = BigInteger.ONE;
        for (BigInteger i = BigInteger.ZERO; i.compareTo(e) <= 0; i = i.add(BigInteger.ONE)) {
            encrypted = encrypted.multiply(msg).mod(pair.n());
        }

        var decrypted = BigInteger.ONE;
        for (BigInteger i = BigInteger.ZERO; i.compareTo(d) <= 0; i = i.add(BigInteger.ONE)) {
            decrypted = decrypted.multiply(msg).mod(pair.n());
        }

        System.out.println(decrypted.toString());

    }

}
