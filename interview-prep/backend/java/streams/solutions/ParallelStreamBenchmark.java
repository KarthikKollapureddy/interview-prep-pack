import java.util.*;
import java.util.stream.*;

/**
 * Challenge 4: Parallel Stream Benchmark
 * 
 * Given: A list of 1 million random integers.
 * Find all prime numbers and return their sum.
 * 
 * Implement both sequential and parallel versions, then benchmark.
 */
public class ParallelStreamBenchmark {

    /**
     * 1. Find all primes and return their sum — sequential stream.
     */
    public long sumPrimesSequential(List<Integer> numbers) {
        // YOUR CODE HERE
        return 0;
    }

    /**
     * 2. Find all primes and return their sum — parallel stream.
     */
    public long sumPrimesParallel(List<Integer> numbers) {
        // YOUR CODE HERE
        return 0;
    }

    /**
     * Helper: Check if a number is prime.
     * You may implement this however you want.
     */
    private boolean isPrime(int n) {
        // YOUR CODE HERE
        return false;
    }

    /**
     * 3. Benchmark both and print execution times.
     *    Discuss in comments: when does parallel actually win?
     */
    public static void main(String[] args) {
        // YOUR CODE HERE
        // Generate 1 million random integers
        // Benchmark sequential vs parallel
        // Print results and timing
    }
}
