/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 10;
		String serverAddress = "http://localhost:8081";
		boolean localTest = true;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		int numSuccessfulInteractions = 0;
		long elapsedTime = 0;
		int numTotalRuns = 0;
		int numSuccessfulFrequentInteractions = 0;
		int numTotalFrequentInteractions = 0;

		double aggregatedThroughput = 0;
		double avgLatency = 0;

		for (WorkerRunResult workerResult : workerRunResults) {
			numSuccessfulInteractions += workerResult.getSuccessfulInteractions();
			numTotalRuns += workerResult.getTotalRuns();
			elapsedTime += workerResult.getElapsedTimeInNanoSecs();
			numSuccessfulFrequentInteractions += workerResult.getSuccessfulFrequentBookStoreInteractionRuns();
			numTotalFrequentInteractions += workerResult.getTotalFrequentBookStoreInteractionRuns();

			double throughput = workerResult.getSuccessfulFrequentBookStoreInteractionRuns()
					/ (double) workerResult.getElapsedTimeInNanoSecs();
			aggregatedThroughput += throughput;
			avgLatency += 1 / throughput;
		}

		System.out.println("Successful Interactions: " + numSuccessfulInteractions);
		System.out.println("Successful Frequent Bookstore Interaction Runs: " + numSuccessfulFrequentInteractions);

		System.out.println("Total runs: " + numTotalRuns);
		System.out.println("Total Frequent Bookstore Interaction Runs: " + numTotalFrequentInteractions);

		System.out.println("Elapsed Time: " + elapsedTime + "ns");

		System.out.println("Aggregated Throughput: " + aggregatedThroughput);
		System.out.println("Average Latency: " + avgLatency);
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore, StockManager stockManager)
			throws BookStoreException {

		BookSetGenerator bookSetGenerator = new BookSetGenerator();
		Set<StockBook> stockBookSet = bookSetGenerator.nextSetOfStockBooks(1000);

		stockManager.removeAllBooks();
		stockManager.addBooks(stockBookSet);
	}
}
