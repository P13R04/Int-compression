package demo;


import io.compress.intpack.*;
import java.util.Arrays;


public class Main {
public static void main(String[] args) {
int[] data = {0,1,2,3,4,5,6,7,15,31,1024, 1<<20};


CompressorFactory.Options opts = new CompressorFactory.Options();
IntCompressor c1 = CompressorFactory.create(CompressionType.CROSSING, opts);
IntCompressor c2 = CompressorFactory.create(CompressionType.NO_CROSSING, opts);
IntCompressor c3 = CompressorFactory.create(CompressionType.OVERFLOW, opts);


int[] buf1 = c1.compress(data);
int[] buf2 = c2.compress(data);
int[] buf3 = c3.compress(data);


int[] out = new int[data.length];
c1.decompress(buf1, out); System.out.println("CROSSING ok="+Arrays.equals(data, out));
c2.decompress(buf2, out); System.out.println("NO_CROSSING ok="+Arrays.equals(data, out));
c3.decompress(buf3, out); System.out.println("OVERFLOW ok="+Arrays.equals(data, out));


// Test get()
System.out.println("get@10 CROSSING:" + c1.get(buf1, 10));
System.out.println("get@10 NO_CROSSING:" + c2.get(buf2, 10));
System.out.println("get@10 OVERFLOW:" + c3.get(buf3, 10));
}
}
