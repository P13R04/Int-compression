package io.compress.intpack;


/**
* API minimale commune à toutes les variantes.
* Le buffer compressé est un int[] autosuffisant : header + data (+ overflow éventuel).
*/
public interface IntCompressor {
/**
* Compresse src en un nouveau buffer int[] qui contient header + data.
*/
int[] compress(int[] src);


/**
* Décompresse dans dst (déjà dimensionné à n). n est lu depuis le header.
*/
void decompress(int[] compressed, int[] dst);


/**
* Accès direct au i-ème élément sans tout décompresser (O(1)).
*/
int get(int[] compressed, int index);
}
