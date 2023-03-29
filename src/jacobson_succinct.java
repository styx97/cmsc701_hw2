import edu.berkeley.cs.succinct.util.vector.IntVector;
/*
Code for IntVector class taken from - https://github.com/amplab/succinct/blob/master/core/src/main/java/edu/berkeley/cs/succinct/util/vector/IntVector.java

 */


import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;



class ChunkFly implements Serializable {
    private IntVector relativeSubChunksOffsets;
    private IntVector subChunkLookupTable;
    private int numSubChunks;

    public static int createLookupInt(int[] bitarray, int subChunkStart, int subChunkEnd) {
        // convert a list of binary integers to a decimal number
        // e.g. 1011 -> 11
        int lookupInt = 0;
        for (int i = subChunkEnd-1; i >= subChunkStart; i--) {
            lookupInt = (lookupInt << 1) | bitarray[i];
        }
        return lookupInt;
    }

    // public static int getRankfromInt(int lookupInt, int offset);

    public static int getRankfromInt(int lookupInt, int offset) {
        int rank = 0;
        // mask the offset number of bits from
        int mask = (1 << (offset + 1)) - 1;
        int maskedInt = lookupInt & mask;
        // sum of bits in masked int is the rank

        // TODO: replace this with Int.bitcount
        rank = Integer.bitCount(maskedInt);
//        while (maskedInt > 0 ) {
//            rank += maskedInt & 1;
//            maskedInt >>= 1;
//        }
        return rank;
    }

    /*
    This method is used to check the correctness of the lookup method
    that uses finding the rank of a subchunk on the fly
     */
    public static void checkLookupMethod(int[] bitarray, int subChunkStart, int subChunkEnd, int offset) {
        int lookupInt = createLookupInt(bitarray, subChunkStart, subChunkEnd);
        System.out.println("lookupInt: " + lookupInt +  ", offset: " + offset);
        for (int i = subChunkStart; i < subChunkEnd; i++) {
            System.out.print(bitarray[i] + " ");
        }
        System.out.println();
        System.out.println("Rank: " + getRankfromInt(lookupInt, offset));
    }


    public ChunkFly(int[] bitarray, int start, int end, int subChunkSize) {
        numSubChunks = (int) Math.ceil((end - start) / (double) subChunkSize);
        subChunkLookupTable = new IntVector(numSubChunks, subChunkSize);
        // System.out.println("numSubChunks: " + numSubChunks);
        // relativeSubChunksOffsets is an array of offsets for each subchunk inside this particular chunk
        // that saves the relative cumulative rank of the last element of the subchunk
        int subChunkBitSize = (int) Math.ceil(Math.log(end-start+1) / Math.log(2));
        relativeSubChunksOffsets = new IntVector(numSubChunks, subChunkBitSize);
        // System.out.println("subChunkBitSize: " + subChunkBitSize);
        int prevRank = 0;

        // System.out.println("Number of subchunks in this particular chunk :  " + numSubChunks);
        for (int i = 0; i < numSubChunks; i ++) {
            int subChunkStart = i * subChunkSize + start;
            int subChunkEnd = Math.min(subChunkStart + subChunkSize, end);
            // System.out.println("subchunk start: " + subChunkStart + " subchunk end: " + subChunkEnd);


            int lookupInt = createLookupInt(bitarray, subChunkStart, subChunkEnd);
            subChunkLookupTable.add(i, lookupInt);
            // find the index of the lookup table for this subchunk
            prevRank +=  getRankfromInt(lookupInt, subChunkSize - 1);
            // add the last element of rank to the relativeSubChunksOffsets
            relativeSubChunksOffsets.add(i, prevRank);
        }
    }

    //getter for relativeSubChunksOffsets
    public int getRelativeSubChunksOffsetAt(int index) {
        // System.out.println("index: " + index);
        return relativeSubChunksOffsets.get(index);
    }

    //getter for numSubChunks
    public int getNumSubChunks() {
        return numSubChunks;
    }

    public int getChunkRank(int subChunkOffset, int subChunkSize) {
        int relativeSubChunksOffset, subChunkIndex;
        subChunkIndex = subChunkOffset / subChunkSize;
        if (subChunkIndex == 0) {
            relativeSubChunksOffset = 0;
        } else {
            relativeSubChunksOffset = getRelativeSubChunksOffsetAt(subChunkIndex - 1);
        }
        //System.out.println("relativeSubChunksOffset: " + relativeSubChunksOffset);
        int exactRankIndex = subChunkOffset % subChunkSize;
        //System.out.println("exactRankIndex: " + exactRankIndex);
        return relativeSubChunksOffset + getRankfromInt(subChunkLookupTable.get(subChunkIndex), exactRankIndex);
    }

    public int overhead() {
        return subChunkLookupTable.overhead() + relativeSubChunksOffsets.overhead() + Integer.SIZE;
    }
}

class CreateAndQueryJacobson implements Serializable {
    private int chunkSize, numChunks, subChunkSize, arraySize;
    private IntVector cumulativeChunkOffsets;
    private ChunkFly[] chunks;
    public void setSizes(int size) {
        double v = Math.log(size) / Math.log(2);
        chunkSize = (int) Math.pow(v, 2);
        numChunks = (int) Math.ceil(size / (double) chunkSize);
        subChunkSize = (int) v / 2 ;
        arraySize = size;
    }

    // constructor for the object
    public CreateAndQueryJacobson(int[] array) {
        System.out.println("Creating Jacobson index");
        setSizes(array.length);

        // Create an array of chunks
        ChunkFly[] chunks = new ChunkFly[numChunks];
        // create an intVector for each chunk to store the cumulative rank of each chunk
        IntVector cumulativeChunkOffsets = new IntVector(numChunks, (int) Math.ceil(Math.log(array.length+1) / Math.log(2)));
        int prevRank = 0;
        // For each chunk, populate the subchunks and the cumulative rank of each chunk
        for (int i = 0; i < numChunks; i++) {
            int chunkStart = i * chunkSize;
            int chunkEnd = Math.min(chunkStart + chunkSize, array.length);
            //System.out.println("chunk start: " + chunkStart + " chunk end: " + chunkEnd);
            chunks[i] = new ChunkFly(array, chunkStart, chunkEnd, subChunkSize);
            // for this chunk, add the cumulative rank of the last element of the chunk
            prevRank += chunks[i].getRelativeSubChunksOffsetAt(chunks[i].getNumSubChunks() - 1);
            // update the cumulative rank of each chunk
            cumulativeChunkOffsets.add(i, prevRank);
        }
        this.chunks = chunks;
        this.cumulativeChunkOffsets = cumulativeChunkOffsets;
    }

    public void display() {
        System.out.println("Chunk size: " + chunkSize + " Num chunks: " + numChunks + " Subchunk size: " + subChunkSize);
        for (int i = 0; i <   numChunks; i++) {
            System.out.println("Chunk " + i);
            System.out.println("Cumulative rank of last element of chunk " + i + " : " + cumulativeChunkOffsets.get(i));

        }
    }

    // getter for chunks
    public ChunkFly[] getChunks() {
        return chunks;
    }

    // getter for cumulativeChunkOffsets
    public IntVector getCumulativeChunkOffsets() {
        return cumulativeChunkOffsets;
    }

    // getter for numchunks
    public int getNumChunks() {
        return numChunks;
    }

    public int rank1(int index) {
        int chunkOffsetRank, relativeChunkOffset, subChunkRank, relativeSubChunkRank;
        int chunkIndex = index / chunkSize ;
        if (chunkIndex == 0) {
            chunkOffsetRank = 0;
        } else {
            chunkOffsetRank = cumulativeChunkOffsets.get(chunkIndex - 1);
        }
        //System.out.println("Chunk Offset Rank:  " +  chunkOffsetRank);
        int relativeSubChunkIndex = index % chunkSize;
        //System.out.println("relativeSubChunkIndex:  " +  relativeSubChunkIndex);

        subChunkRank = chunks[chunkIndex].getChunkRank(relativeSubChunkIndex, subChunkSize);
        //System.out.println("Subchunk Rank:  " +  subChunkRank);

        return chunkOffsetRank + subChunkRank;
    }

    public int select1(int rank) {
        // binary search to find the max index such that the rank of the last element of that chunk is less than or equal to the input rank
        int low = 0, high = arraySize-1, mid;
        if (rank1(high) == rank) {
            return high;
        }

        while (low < high) {
            mid = (low + high) / 2;
            int midRank = rank1(mid);
            if (midRank > rank) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low-1;
    }

    public int overhead() {
        int overhead = cumulativeChunkOffsets.overhead();
        for (int i = 0; i < chunks.length; i++) {
            overhead += chunks[i].overhead();
        }
        return overhead + Integer.SIZE * 4;
    }

    public static void save(String filename, CreateAndQueryJacobson exampleObject) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(exampleObject);
        out.close();
        fileOut.close();
    }

    public static CreateAndQueryJacobson load(String inputFile) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(inputFile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        CreateAndQueryJacobson exampleObject = (CreateAndQueryJacobson) in.readObject();
        in.close();
        fileIn.close();
        return exampleObject;
    }
}


public class jacobson_succinct {
    public static int findRank(int[] array, int index) {
        int rank = 0;
        for (int i = 0; i <= index; i++) {
            if (array[i] == 1) {
                rank++;
            }
        }
        return rank;
    }

    public static int select(int[] array, int rank) {
        int maxIndex = 0;
        for (int i = 0; i < array.length; i++) {
            if (findRank(array, i) == rank) {
                maxIndex = Math.max(maxIndex, i);
            }
            if (findRank(array, i) > rank)
                break;
        }
        return maxIndex;

    }

    static void test_for_plot() {
        int[] bitarray = new int[15];
        // set each bit in the bitarray to 1 with a probability of 30 %
        for (int i = 0; i < bitarray.length; i++) {
            bitarray[i] = Math.random() < 0.3 ? 1 : 0;
        }

        int[] numRepeatSizes = {10, 100, 1000, 10000, 100000, 1000000, 10000000};

        long[] rankTimes = new long[numRepeatSizes.length];
        long[] selectTimes = new long[numRepeatSizes.length];
        int[] overheads = new int[numRepeatSizes.length];


        for (int j=0; j < numRepeatSizes.length; j++) {
            // run the code for each numRepeatSize
            int numRepeatSize = numRepeatSizes[j];
            System.out.println("Num repeat size: " + numRepeatSize);
            int[] ground_ranks = new int[numRepeatSize];
            int[] repeat = new int[numRepeatSize];
            for (int i = 0; i < numRepeatSize; i++) {
                repeat[i] = bitarray[i % bitarray.length];
            }
            for (int i = 0; i < numRepeatSize; i++) {
                ground_ranks[i] = findRank(bitarray, i % bitarray.length);
            }

            CreateAndQueryJacobson jacobson = new CreateAndQueryJacobson(repeat);
            int overhead = jacobson.overhead();
            int max_ones = jacobson.getCumulativeChunkOffsets().get(jacobson.getNumChunks() - 1);

            // create random queries of length 500 between 1 and max_ones
            Random rand = new Random();
            int numQueries = 10000;
            int[] rankQueries = new int[numQueries];
            int[] selectQueries = new int[numQueries];
            for (int i = 0; i < numQueries; i++) {
                rankQueries[i] = rand.nextInt(numRepeatSize) + 1;
                selectQueries[i] = rand.nextInt(max_ones) + 1;
            }

            // time taken to do rank and select queries in milliseconds
            long startTime = System.nanoTime();
            for (int i = 0; i < numQueries; i++) {
                jacobson.rank1(rankQueries[i]);
            }
            long midTime = System.nanoTime();
            for (int i = 0; i < numQueries; i++) {
                jacobson.select1(selectQueries[i]);
            }
            long endTime = System.nanoTime();
            long selectTime = endTime - midTime;
            long rankTime = midTime - startTime;

            selectTimes[j] = selectTime;
            rankTimes[j] = rankTime;
            overheads[j] = overhead;

        }

        // print out the three arrays of times, overheads, and numRepeatSizes
        System.out.println("Rank Times: " + Arrays.toString(rankTimes));
        System.out.println("Select Times: " + Arrays.toString(selectTimes));
        System.out.println("Overheads: " + Arrays.toString(overheads));
        System.out.println("sizes of Bitarray: " + Arrays.toString(numRepeatSizes));

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

//         create a bitarray of length 13
        int[] bitarray = {1,1,0,1,0,1,0,0,1,0,0,0,1};
        int numRepeatSize = 100;
        int[] ground_ranks = new int[numRepeatSize];
        int[] repeat = new int[numRepeatSize];


        // populate repeat with numRepeatSize copies of bitarray
        for (int i = 0; i < numRepeatSize; i++) {
            repeat[i] = bitarray[i % bitarray.length];
        }
//
//        System.out.println("Size of repeat: " + repeat.length);
//        // populate ground_ranks with the ranks of the bitarray
//        for (int i = 0; i < numRepeatSize; i++) {
//            ground_ranks[i] = findRank(repeat, i);
//        }
//
        // create a jacobson data structure for repeat
        CreateAndQueryJacobson jacobson = new CreateAndQueryJacobson(repeat);
        System.out.println("Rank of Jacobson: " + jacobson.rank1(50));
        System.out.println("Select of Jacobson: " + jacobson.select1(30));


//      // save and load jacobson   
//        CreateAndQueryJacobson.save("jacobson.ser", jacobson);
//        CreateAndQueryJacobson jacobson2 = CreateAndQueryJacobson.load("jacobson.ser");
//
//
//        int[] jacobson_ranks = new int[numRepeatSize];
//        for (int i = 0; i < numRepeatSize; i++) {
//            jacobson_ranks[i] = jacobson2.rank1(i);
//        }
//
//        // compare outputs of gold and jacobson over all ranks
//        for (int i = 0; i < numRepeatSize; i++) {
//            System.out.println("Ground rank: " + ground_ranks[i] + " Jacobson rank: " + jacobson_ranks[i]);
//            if (ground_ranks[i] != jacobson_ranks[i]) {
//                System.out.println(" --> Mismatch at index: " + i);
//                break;
//            }
//        }

         test_for_plot();


        // compare outputs of select at a random position
//        System.out.println( "Actual select: "  + select(repeat, 498) );
//        System.out.println( "Jacobson select: "  + jacobson.select1( 498) );
////
//        // compare outputs of ground and jacobson over all select queries
//        for (int i = 0; i < 500; i++) {
//            System.out.println("Ground select: " + select(repeat, i) + " Jacobson select: " + jacobson.select1(i));
//            if (select(repeat, i) != jacobson.select1(i)) {
//                System.out.println("    --> Error at select: " + i);
//            }
//        }


    }
}
