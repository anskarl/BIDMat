package edu.berkeley.bid;
import jcuda.Pointer;

public final class SLATEC {

    private SLATEC() {}

    public static native int applyfun(float[] X, float[] Y,  int N, int opn);

}
