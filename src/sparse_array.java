import java.io.*;
import java.util.ArrayList;

class ReturnObject {
    // an object that stores the reference element
    // This needs a class in java because pointers are not a thing in java

    private String elem;

    public ReturnObject(String elem) {
        this.elem = elem;
    }
    public void setElem(String elem) {
        this.elem = elem;
    }

    public String getElem() {
        return elem;
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

    // implements the "finalize" method from the spec
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

    boolean get_at_rank(int r, ReturnObject e) {
        if (r <= 0) {
            // throw new RuntimeException("Rank cannot be 0");
            return false;
        }

        int maxRank = values.size();
        if (r > maxRank) {
            // throw new RuntimeException("Rank cannot be greater than max rank");
            return false;
        }
        e.setElem(values.get(r-1));
        return true;
    }


    boolean get_at_index(int index, ReturnObject e) {
        if (index == 0 || index > size) {
            // throw new RuntimeException("Rank cannot be 0 or greater than size
            return false;
        }

        int rank = jacobson.rank1(index);
        var elem = values.get(rank-1);
        e.setElem(elem);
        return true;
    }

    int get_index_of(int rank) {

        // System.out.println("Rank: " + rank);
        if (rank > values.size() || rank <=0) {
            return -1;
        }

        int selectR = jacobson.select1(rank-1) + 1 ;
        if (selectR > size || rank <= 0)
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

    public static void save(String filename, sparse_array exampleObject) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(exampleObject);
        out.close();
        fileOut.close();
    }
    public static sparse_array load(String inputFile) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(inputFile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        sparse_array exampleObject = (sparse_array) in.readObject();
        in.close();
        fileIn.close();
        return exampleObject;
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

    int memory_usage_if_string_list() {
        // memory usage if the list of strings was used without any fancy bitvectors
        int totalbits = 0;
        int num_values = values.size();
        int num_empty = size - num_values;
        for (String s : values) {
            totalbits += s.length() * Character.SIZE;
        }
        return totalbits + num_empty * Character.SIZE;
    }

    public static void test_for_plot() throws IOException {
        int numQueries = 10000;
        float[] sparseArrayRatios = {0.01f, 0.05f, 0.1f, 0.5f};
        int[] sparseArraySizes = {100, 1000, 10000, 100000, 1000000};
        // list of random strings
        String[] random_strings = {"foasdaso", "basdar", "baasdz", "qSADASux", "quASDAux", "quSDASDuz", "corge"};
        // pick a random string from random_strings
        String random_string;

        /*
        We will query the functions num_elem_at() and get_index_of() for each sparse array size and sparsity ratio.
        This is because we want to test the performance of calling rank and select on the sparse array for each size and sparsity ratio.
        While num_elem_at() requires a rank query, get_index_of() requires a select query

        The domain of get_elem_at() is [1, size]
        The domain of get_index_of() is [1, num_elem()]

        If we want to add more functions --
        Domain of get_at_rank() is [1, num_elem()]
        Domain of get_at_index() is [1, size]

        */
        // for each size and sparse array ratio, create a sparse array and test the memory usage

        FileWriter fileWriter = new FileWriter("output.txt");   // create a file writer

        for (int array_size : sparseArraySizes ) {
            for (float sparsity : sparseArrayRatios) {
                // create the queries for the two methods
                int[] numElemAtQueries =  new int[numQueries];
                int[] getIndexOfQueries = new int[numQueries];

                // create the sparse array:
                ArrayList<Integer> indices = new ArrayList<>();
                ArrayList<String> values = new ArrayList<>();
                sparse_array sparseArray = new sparse_array(array_size);

                long appendStartTime = System.nanoTime();
                int[] array = new int[array_size];
                for (int i = 0; i < array_size; i++) {
                    if (Math.random() < sparsity) {
                        array[i] = 1;
                        random_string = random_strings[(int) (Math.random() * random_strings.length)];
                        sparseArray.append(random_string, i);
                    }
                    else {
                        array[i] = 0;
                    }
                }
                long appendEndTime = System.nanoTime();

                // create and populate the sparse array
                sparseArray.finalizeSparseArray();

                long t0 = System.nanoTime();
                int num_elem = sparseArray.num_elem();
                // populate numElemAtQueries with random numbers in the range [1, array_size]
                for (int i = 0; i < numQueries; i++) {
                    numElemAtQueries[i] = (int) (Math.random() * array_size) + 1;
                }
                // populate getIndexOfQueries with random numbers in the range [1, num_elem()]
                for (int i = 0; i < numQueries; i++) {
                    getIndexOfQueries[i] = (int) (Math.random() * num_elem) + 1;
                }

                // measure mem usage
                long sparseArrayOverhead = sparseArray.memory_usage();
                long stringListOverhead = sparseArray.memory_usage_if_string_list();
                System.out.println("Array size: " + array_size + ", sparsity: " + sparsity);
                System.out.println("Sparse array overhead: " + sparseArrayOverhead);
                System.out.println("String list overhead: " + stringListOverhead);

                ReturnObject e = new ReturnObject(null);
                // measure time for num_elem_at
                long t1 = System.nanoTime();
                for (int i = 0; i < numQueries; i++) {
                    int temp = sparseArray.num_elem_at(numElemAtQueries[i]);
                }
                long t2 = System.nanoTime();
                // measure time for get_index_of
                for (int i = 0; i < numQueries; i++) {
                    int temp = sparseArray.get_index_of(getIndexOfQueries[i]);
                }
                long t3 = System.nanoTime();
                // measure time of get_at_rank
                for (int i = 0; i < numQueries; i++) {
                    boolean temp = sparseArray.get_at_rank(getIndexOfQueries[i], e);
                }
                long t4 = System.nanoTime();


                // Save a comma separated string containing all the results for this test
                String results = array_size + "," + sparsity + "," + sparseArrayOverhead + "," + stringListOverhead + "," + (t2 - t1) + "," + (t3 - t2) + "," + (t4 - t3) + "," + (appendEndTime - appendStartTime) + "," + (t0 - appendEndTime);
                fileWriter.write(results + "\n");

            }
        }
        fileWriter.close();
        System.out.println("Wrote to file");
    }


    public static void main(String[] args) throws IOException {
        //uncomment the following line to test the methods on queries of length 10000
        // the outputs of these were used to create the plots in the report
        // test_for_plot();

         // the following lines test the methods and space occupied on a toy example
        sparse_array sparseArray = new sparse_array(13);
        sparseArray.append("foo", 1);
        sparseArray.append("bar", 5);
        // sparseArray.append("baz", 5); // should throw error, index already exists
        sparseArray.append("baz", 9);
        sparseArray.append("bar", 12);
        sparseArray.finalizeSparseArray();

//        // test append
//        System.out.println(sparseArray.values);
//        System.out.println(sparseArray.indices);
//
        // test get_at_rank
        ReturnObject e = new ReturnObject(null);
        System.out.println("Testing get_at_rank");
        System.out.println(sparseArray.get_at_rank(1, e)); // should be foo
        System.out.println("Return object: " + e.getElem());
//        System.out.println(sparseArray.get_at_rank(4, returnObject)); // should be bar
//        System.out.println("Return object: " + returnObject.getElem());
//
//
        //  test get_at_index
//        System.out.println("Testing get_at_index");
//        System.out.println(sparseArray.get_at_index(3, e));
//
//
        // test get_index_of
//        System.out.println("Testing get_index_of");
//        System.out.println(sparseArray.get_index_of(2)); // should be 1
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
/*
        System.out.println("Testing memory_usage");
        System.out.println(sparseArray.memory_usage());
*/



    }

}
