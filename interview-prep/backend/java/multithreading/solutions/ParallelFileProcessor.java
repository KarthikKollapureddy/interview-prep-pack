import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Challenge 2: Parallel File Processor
 * 
 * 1. Use ExecutorService with pool of 5 threads
 * 2. Each "file" takes random 100-500ms to process
 * 3. Chain with CompletableFuture: read → transform → write
 * 4. Collect results with CompletableFuture.allOf()
 * 5. Print total time (should be ~4x faster than sequential)
 */
public class ParallelFileProcessor {

    // YOUR CODE HERE

    public static void main(String[] args) {
        // Process 20 files, compare sequential vs parallel time
    }
}
