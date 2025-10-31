package io.compress.intpack;


public enum CompressionType {
CROSSING, // valeurs peuvent chevaucher deux mots int
NO_CROSSING, // valeurs n’empiètent pas (trous en fin de mot)
OVERFLOW // drapeau + payload + zone overflow
}
