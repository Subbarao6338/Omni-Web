package com.omniweb.app.util.adblock.hash;
import java.io.Serializable;
public class MurmurHashStringAdapter implements HashingAlgorithm<String>, Serializable {
    @Override
    public int hash(String item) { return MurmurHash.hash32(item); }
}
