public class naive_rsa {
    // methods that compute RSA is the most naive way possible
    // only for error checking purposes


    // Return the element corresponding to an index in a binary array
    private int[] array;

    public naive_rsa(int[] array) {
        this.array = array;
    }

    public static int access(int[] array, int index) {
        return array[index];
    }

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
        int[] array = {0, 1, 0, 1, 1, 0, 1, 0, 1, 1};
        System.out.println(access(array, 3));
        System.out.println(findRank(array, 3));
        System.out.println(select(array, 3));
    }
}
