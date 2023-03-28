import edu.berkeley.cs.succinct.util.vector.IntVector;
// import java.lang.Integer;

class ChunkFly {
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
        while (maskedInt > 0) {
            rank += maskedInt & 1;
            maskedInt >>= 1;
        }
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
        int subChunkBitSize = (int) Math.ceil(Math.log(end-start) / Math.log(2));
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
            //subChunkLookupTable.add(i, createLookupInt(subChunkStart, subChunkEnd));
            // add the last element of rank to the relativeSubChunksOffsets
            prevRank +=  getRankfromInt(lookupInt, subChunkSize - 1);
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

class CreateAndQueryJacobson {
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

    public CreateAndQueryJacobson(int[] array) {
        System.out.println("Creating Jacobson index");
        setSizes(array.length);

        // Create an array of chunks
        ChunkFly[] chunks = new ChunkFly[numChunks];
        // create an intVector for each chunk to store the cumulative rank of each chunk
        IntVector cumulativeChunkOffsets = new IntVector(numChunks, (int) Math.ceil(Math.log(array.length) / Math.log(2)));
        int prevRank = 0;
        // For each chunk, populate the subchunks and the cumulative rank of each chunk
        for (int i = 0; i < numChunks; i++) {
            int chunkStart = i * chunkSize;
            int chunkEnd = Math.min(chunkStart + chunkSize, array.length);
            System.out.println("chunk start: " + chunkStart + " chunk end: " + chunkEnd);
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


//            for (int j = 0; j < chunks[i].getNumSubChunks(); j++) {
//                System.out.println("  Subchunk " + j);
//                System.out.println("  Cumulative rank of last element of subchunk " + j + " : " + chunks[i].getRelativeSubChunksOffsetAt(j));
////                for (int k = 0; k < subChunkSize; k++) {
////                    System.out.println("  Rank of element " + k + " : " + chunks[i].getSubChunks()[j].getSubChunkRankAt(k));
////                }
//                System.out.println("Cumulative relative offset of the subchunks:  " +  chunks[i].getRelativeSubChunksOffsetAt(j));
//            }


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
        for (int i = 0; i < numChunks; i++) {
            overhead += chunks[i].overhead();
        }
        return overhead + Integer.SIZE * 4;
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

    public static void main(String[] args) {

        // create a bitarray of length 20
        int[] bitarray = {1,1,0,0,1,1,0,0};
        int numRepeatSize = 1000;
        int[] ground_ranks = new int[numRepeatSize];


        int[] repeat = new int[numRepeatSize];
        for (int i = 0; i < numRepeatSize; i++) {
            repeat[i] = bitarray[i % bitarray.length];
        }
        for (int i = 0; i < numRepeatSize; i++) {
            ground_ranks[i] = findRank(repeat, i);
        }

        System.out.println("Array length: " + repeat.length);
        CreateAndQueryJacobson jacobson = new CreateAndQueryJacobson(repeat);
        jacobson.display();
        System.out.println("Overhead: " + jacobson.overhead());

        int max_ones = jacobson.getCumulativeChunkOffsets().get(jacobson.getNumChunks() - 1);
        System.out.println("Max ones: " + max_ones);

        // compare rank outputs at a random position
        // System.out.println( "Actual Rank: "  + findRank(repeat, 1) );
        // System.out.println( "Cumulative rank: "  + jacobson.rank1( 1) );


//         compare outputs of ground and jacobson over all ranks
//        for (int i = 0; i < numRepeatSize; i++) {
//            System.out.println("Ground rank: " + ground_ranks[i] + " Jacobson rank: " + jacobson.rank1(i));
//            if (ground_ranks[i] != jacobson.rank1(i)) {
//                System.out.println("    --> Error at index: " + i);
//            }
//        }

        // compare outputs of select at a random position
        System.out.println( "Actual select: "  + select(repeat, 498) );
        System.out.println( "Jacobson select: "  + jacobson.select1( 498) );
//
//        // compare outputs of ground and jacobson over all select queries
//        for (int i = 0; i < 500; i++) {
//            System.out.println("Ground select: " + select(repeat, i) + " Jacobson select: " + jacobson.select1(i));
//            if (select(repeat, i) != jacobson.select1(i)) {
//                System.out.println("    --> Error at select: " + i);
//            }
//        }


    }
}
