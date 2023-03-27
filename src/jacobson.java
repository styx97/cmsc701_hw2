import edu.berkeley.cs.succinct.util.vector.IntVector;

class SubChunk {
    // for each subchunk, we calculate the rank through brute-force
    // and store it in an IntVector
    private IntVector subChunkRanks;
    public SubChunk(int[] bitarray, int start, int end) {
        int maxSize = (int) Math.ceil(Math.log(end - start) / Math.log(2));
        IntVector subChunkRanks = new IntVector(end-start, maxSize);

        int rank = 0;
        for (int i = start; i < end; i++) {
            if (bitarray[i] == 1) {
                rank++;
            }
            subChunkRanks.add(i-start, rank);
        }
        this.subChunkRanks = subChunkRanks;
    }

    //getter for subChunkRanks
    public int getSubChunkRankAt(int index) {
        return subChunkRanks.get(index);
    }

}

//class GlobalLookupTable {
//    private IntVector[] globalLookupTable;
//    private int numChunks;
//
//    public GlobalLookupTable (int size) {
//        double v = Math.log(size) / Math.log(2);
//        int globalLookupTableSize = (int) Math.ceil (Math.pow(2, v));
//        globalLookupTable = new IntVector[globalLookupTableSize, Math.ceil( v / (double) 2)];
//        numChunks = (int) Math.ceil(size / (double) chunkSize);
//        globalLookupTable = new IntVector[numChunks];
//    }
//}

class Chunk {
    private IntVector relativeSubChunksOffsets;
    private SubChunk[] subChunks;
    private int numSubChunks;

    public Chunk(int[] bitarray, int start, int end, int subChunkSize) {

        numSubChunks = (int) Math.ceil((end - start) / (double) subChunkSize);
        // System.out.println("numSubChunks: " + numSubChunks);
        subChunks = new SubChunk[numSubChunks];

        // relativeSubChunksOffsets is an array of offsets for each subchunk inside this particular chunk
        // that saves the relative cumulative rank of the last element of the subchunk
        int subChunkBitSize = (int) Math.ceil(Math.log(end-start) / Math.log(2));
        // System.out.println("subChunkBitSize: " + subChunkBitSize);

        relativeSubChunksOffsets = new IntVector(numSubChunks, subChunkBitSize);
        int prevRank = 0;

        // System.out.println("Number of subchunks in this particular chunk :  " + numSubChunks);
        for (int i = 0; i < numSubChunks; i ++) {
            int subChunkStart = i * subChunkSize + start;
            int subChunkEnd = Math.min(subChunkStart + subChunkSize, end);
            // System.out.println("subchunk start: " + subChunkStart + " subchunk end: " + subChunkEnd);
            SubChunk subChunk = new SubChunk(bitarray, subChunkStart, subChunkEnd);
            subChunks[i] = subChunk;
            // add the last element of rank to the relativeSubChunksOffsets
            prevRank +=  subChunk.getSubChunkRankAt(subChunkEnd - subChunkStart - 1);
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

    //getter for subChunks
    public SubChunk[] getSubChunks(int subChunkOffset) {
        return subChunks;
    }

    public int getChunkRank(int subChunkOffset, int subChunkSize) {
        int relativeSubChunksOffset, subChunkIndex;
        subChunkIndex = subChunkOffset / subChunkSize;
        if (subChunkIndex == 0) {
            relativeSubChunksOffset = 0;
        } else {
            relativeSubChunksOffset = getRelativeSubChunksOffsetAt(subChunkIndex - 1);
        }
        System.out.println("relativeSubChunksOffset: " + relativeSubChunksOffset);
        int exactRankIndex = subChunkOffset % subChunkSize;
        System.out.println("exactRankIndex: " + exactRankIndex);

        return relativeSubChunksOffset + subChunks[subChunkIndex].getSubChunkRankAt(exactRankIndex);
    }


}

class CreateAndQueryJacobson {
    private int chunkSize, numChunks, subChunkSize;
    private IntVector cumulativeChunkOffsets;
    private Chunk[] chunks;
    public void setSizes(int size) {
        double v = Math.log(size) / Math.log(2);
        chunkSize = (int) Math.pow(v, 2);
        numChunks = (int) Math.ceil(size / (double) chunkSize);
        subChunkSize = (int) v / 2 ;
    }

    public CreateAndQueryJacobson(int[] array) {
        System.out.println("Creating Jacobson index");
        setSizes(array.length);

        // Create an array of chunks
        Chunk[] chunks = new Chunk[numChunks];
        // create an intVector for each chunk to store the cumulative rank of each chunk
        IntVector cumulativeChunkOffsets = new IntVector(numChunks, (int) Math.ceil(Math.log(array.length) / Math.log(2)));
        int prevRank = 0;
        // For each chunk, populate the subchunks and the cumulative rank of each chunk
        for (int i = 0; i < numChunks; i++) {
            int chunkStart = i * chunkSize;
            int chunkEnd = Math.min(chunkStart + chunkSize, array.length);
            System.out.println("chunk start: " + chunkStart + " chunk end: " + chunkEnd);
            chunks[i] = new Chunk(array, chunkStart, chunkEnd, subChunkSize);
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
//            System.out.println("Chunk " + i);
//            System.out.println("Cumulative rank of last element of chunk " + i + " : " + cumulativeChunkOffsets.get(i));


            for (int j = 0; j < chunks[i].getNumSubChunks(); j++) {
                System.out.println("  Subchunk " + j);
                System.out.println("  Cumulative rank of last element of subchunk " + j + " : " + chunks[i].getRelativeSubChunksOffsetAt(j));
//                for (int k = 0; k < subChunkSize; k++) {
//                    System.out.println("  Rank of element " + k + " : " + chunks[i].getSubChunks()[j].getSubChunkRankAt(k));
//                }
                System.out.println("Cumulative relative offset of the subchunks:  " +  chunks[i].getRelativeSubChunksOffsetAt(j));
            }


        }
    }

    // getter for chunks
    public Chunk[] getChunks() {
        return chunks;
    }

    // getter for cumulativeChunkOffsets
    public IntVector getCumulativeChunkOffsets() {
        return cumulativeChunkOffsets;
    }

    public int getJacobsonRank(int index) {
        int chunkOffsetRank, relativeChunkOffset, subChunkRank, relativeSubChunkRank;
        int chunkIndex = index / chunkSize ;
        if (chunkIndex == 0) {
            chunkOffsetRank = 0;
        } else {
            chunkOffsetRank = cumulativeChunkOffsets.get(chunkIndex - 1);
        }
        System.out.println("Chunk Offset Rank:  " +  chunkOffsetRank);
        int relativeSubChunkIndex = index % chunkSize;
        System.out.println("relativeSubChunkIndex:  " +  relativeSubChunkIndex);

        subChunkRank = chunks[chunkIndex].getChunkRank(relativeSubChunkIndex, subChunkSize);
        System.out.println("Subchunk Rank:  " +  subChunkRank);

        return chunkOffsetRank + subChunkRank;
    }

}


public class jacobson {
    public static int findRank(int[] array, int index) {
        int rank = 0;
        for (int i = 0; i <= index; i++) {
            if (array[i] == 1) {
                rank++;
            }
        }
        return rank;
    }

    public static void main(String[] args) {
        int[] ground_ranks = new int[100];

        // create a bitarray of length 20
        int[] bitarray = {1,1,0,0};
        int[] repeat = new int[100];
        for (int i = 0; i < 100; i++) {
            repeat[i] = bitarray[i % bitarray.length];
        }
        for (int i = 0; i < 100; i++) {
            ground_ranks[i] = findRank(repeat, i);
        }

        // SubChunk example = new SubChunk(bitarray, 0, 20);
        //System.out.println("Rank of 5th element is: " + example.getSubChunkRankAt(5));
//        for (int i = 0; i < 20; i++) {
//            System.out.println("Rank of " + i + "th element is: " + example.getSubChunkRankAt(i));
//        }
        System.out.println("Array length: " + repeat.length);
        CreateAndQueryJacobson jacobson = new CreateAndQueryJacobson(repeat);
        jacobson.display();

        // compare outputs at a random position
        System.out.println( "Actual Rank: "  + findRank(repeat, 1) );
        System.out.println( "Cumulative rank: "  + jacobson.getJacobsonRank( 1) );


        // compare outputs of ground and jacobson over all indices
        for (int i = 0; i < 100; i++) {
            System.out.println("Ground rank: " + ground_ranks[i] + " Jacobson rank: " + jacobson.getJacobsonRank(i));
            if (ground_ranks[i] != jacobson.getJacobsonRank(i)) {
                System.out.println(" --> Error at index: " + i);
            }
        }


    }
}
