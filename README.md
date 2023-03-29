# cmsc701_hw2
Code for the second assignment in CMSC701. Classes to do access, rank and select in Java. 


### Description of files and what they do

    1. jacobson_succinct.java 

    This is the class that implements Jacobson's rank using IntVectors, computes rank in constant time and select in O(log n) time. It computes the rank of the subchunk on the fly using bit manipulation on integers (in the ChunkFly class). 

    It contains methods for computing rank and select and well as computing the overhead of the data structure. 

    Note that both task1 and task2 are implemented in the CreateAndQueryJacobson class. 

    2. sparse_array.java

    Creates a sparse array using two arraylists, uses Jacobson's algorithm internally to compute rank and select which are used in downstream methods.
    
    3. naive_rsa.java

    A simple implementation of rank and select, used only for verification purposes.
    
    4. plotter.ipynb contains code that can be used to load outputs.txt onto a dataframe for plotting purposes [For analyzing space/ time complexities of sparse array methods].
    

## Attribution

Uses the IntVector class from https://github.com/amplab/succinct/blob/master/core/src/main/java/edu/berkeley/cs/succinct/util/vector/IntVector.java

The edu folder in src contains the external files used. 

## How to run

1. Compile the java files using the command: javac *.java
2. Run the CreateAndQueryJacobson and sparse_array using the command: java *.class. 
3. The main method in each class contains toy examples that demonstrate how to use the methods. There are also additional methods that run the experiments for the assignment.

    For example, to run the experiments for task 1 and task 2 (rank and select), run the ```test_for_plot``` method in ```jacobson_succinct.java```. To run the experiments for task 3 (sparse array), run the ```test_for_plot``` method in ```sparse_array.java```.

