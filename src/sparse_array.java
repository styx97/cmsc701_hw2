
import scala.Int;
import scala.collection.parallel.immutable.HashMapCombiner;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;

class ReturnObject {
    private final String elem;
    private final boolean isPresent;
    public ReturnObject(String elem, boolean isPresent) {
        this.elem = elem;
        this.isPresent = isPresent;
    }

    public String getElem() {
        return elem;
    }
    public boolean getPresence() {
        return isPresent;
    }
}

public class sparse_array {

    // define stuff you'll need to make the sparse array
    CreateAndQueryJacobson jacobson;
    ArrayList<Integer> indices;
    ArrayList<String> values;
    boolean isFinalized = false;
    int size;


    // the constructor acts as the "create" method from the spec
    public sparse_array(int size) {
        this.size = size;
        values = new ArrayList<>();
        indices = new ArrayList<>();
    }

    void append(String value, int index) {
        if (isFinalized) {
            throw new RuntimeException("Sparse array is finalized");
        }
        else if (index > size) {
            throw new RuntimeException("Index is more than size");
        }
        else if (indices.size() > 0  && indices.get(indices.size() - 1) == index) {
            throw new RuntimeException("Index already exists");
        }
        else {
            values.add(value);
            indices.add(index);
        }
    }

    void finalizeSparseArray () {
        if (isFinalized) {
            return;
        }

        int[] bitarray = new int[size];
        for (int i = 0; i < indices.size(); i++) {
            bitarray[(int) indices.get(i)] = 1;
        }
        jacobson = new CreateAndQueryJacobson(bitarray);
        isFinalized = true;
    }

    ReturnObject get_at_rank(int r) {
        if (r == 0) {
            // throw new RuntimeException("Rank cannot be 0");
            return new ReturnObject(null, false);
        }

        int maxRank = values.size();
        if (r > maxRank) {
            // throw new RuntimeException("Rank cannot be greater than max rank");
            return new ReturnObject(null, false);
        }

        return new ReturnObject(values.get(r-1), true);
    }


    ReturnObject get_at_index(int r) {
        if (r == 0 || r > size) {
            // throw new RuntimeException("Rank cannot be 0");
            return new ReturnObject(null, false);
        }

        int rank = jacobson.rank1(r);
        var elem = values.get(rank-1);
        return new ReturnObject(elem, true);
    }

    int get_index_of(int rank) {

        // System.out.println("Rank: " + rank);
        if (rank > values.size()) {
            return -1;
        }

        int selectR = jacobson.select1(rank);
        if (selectR > size || rank == 0)
            return -1;

        return selectR;
    }

    int num_elem_at(int r) {
        if (r > size) {
            throw new RuntimeException("index cannot be greater than Size of sparse array");
        }
        int rank = jacobson.rank1(r);
        return rank;
    }

    int size() {
        return size;
    }

    int num_elem() {
        return values.size();
    }

    void save(String filename) {
        return;
    }

    void load(String filename) {
        return;
    }

    int memory_usage() {
        // memory usage is the size of the sparse array + the size of the string array + the size of the indices array
        int sparseArraySize = jacobson.overhead();
        int stringArraySize = 0;
        for (String s : values) {
            stringArraySize += s.length() * Character.SIZE;
        }

        return sparseArraySize +
                stringArraySize  +
                indices.size() * Integer.SIZE +
                Integer.SIZE;
    }

    public static void main(String[] args) {
        sparse_array sparseArray = new sparse_array(10);
        sparseArray.append("foo", 1);
        sparseArray.append("bar", 5);
        // sparseArray.append("baz", 5); // should throw error, index already exists
        sparseArray.append("baz", 9);
        sparseArray.finalizeSparseArray();

//        // test append
//        System.out.println(sparseArray.values);
//        System.out.println(sparseArray.indices);
//
//        // test get_at_rank
//        System.out.println("Testing get_at_rank");
//        System.out.println(sparseArray.get_at_rank(1).getElem()); // should be foo
//        System.out.println(sparseArray.get_at_rank(4).getElem()); // should be null as rank 4 doesn't exist
//
//        //  test get_at_index
//        System.out.println("Testing get_at_index");
//        ReturnObject r1 = sparseArray.get_at_index(3);
//        System.out.println(" At index 3: " + r1.getElem() + " Return bool: " + r1.getPresence());
//        r1 = sparseArray.get_at_index(9);
//        System.out.println(" At index 5: " + r1.getElem() + " Return bool: " + r1.getPresence());
//
//
//        // test get_index_of
//        System.out.println("Testing get_index_of");
//        System.out.println(sparseArray.get_index_of(1)); // should be 1
//        System.out.println(sparseArray.get_index_of(2)); // should be 5
//        System.out.println(sparseArray.get_index_of(3)); // should be 10
//        System.out.println(sparseArray.get_index_of(0)); // should be -1
//        System.out.println(sparseArray.get_index_of(4)); // should be -1
//
//
//        // test num_elem_at
//        System.out.println("Testing num_elem_at");
//        System.out.println(sparseArray.num_elem_at(4)); // should be 1
//        System.out.println(sparseArray.num_elem_at(5)); // should be 2
//        System.out.println(sparseArray.num_elem_at(8)); // should be 2
//        System.out.println(sparseArray.num_elem_at(9)); // should be 3
//
//        // test size
//        System.out.println("Testing size");
//        System.out.println(sparseArray.size()); // should be 10
//
//        // test num_elem
//        System.out.println("Testing num_elem");
//        System.out.println(sparseArray.num_elem()); // should be 3

        // test memory_usage
        System.out.println("Testing memory_usage");
        System.out.println(sparseArray.memory_usage()); // should be 3
    }

}
