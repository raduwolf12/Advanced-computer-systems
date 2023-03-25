/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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
	static int numConcurrentWorkloadThreads;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		numConcurrentWorkloadThreads = 10;
		String serverAddress = "http://localhost:8081";
		boolean localTestAndRemote = true;

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTestAndRemote = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTestAndRemote;

		BookStore bookStore = null;
		StockManager stockManager = null;
		BookStore bookStoreRemote = null;
		StockManager stockManagerRemote = null;
		if (localTestAndRemote) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
			stockManagerRemote = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStoreRemote = new BookStoreHTTPProxy(serverAddress);
		} else {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		}

		// Generate data in the bookstore before running the workload
		List<List<WorkerRunResult>> localResults = getWorkersRunResult(bookStore, stockManager);

		List<List<WorkerRunResult>> remoteResults = getWorkersRunResult(bookStoreRemote, stockManagerRemote);

//		 Finished initialization, stop the clients if not localTest
		if (localTestAndRemote) {
			((BookStoreHTTPProxy) bookStoreRemote).stop();
			((StockManagerHTTPProxy) stockManagerRemote).stop();
		}

		reportMetric(localResults, remoteResults);
	}

	private static List<List<WorkerRunResult>> getWorkersRunResult(BookStore bookStore, StockManager stockManager)
			throws Exception {
		List<List<WorkerRunResult>> workersRunResults = new ArrayList<>();

		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			List<Future<WorkerRunResult>> runResults = new ArrayList<>();
			List<WorkerRunResult> workerRunResults = new ArrayList<>();

			for (int j = 0; j <= i; j++) {
				WorkloadConfiguration config = new WorkloadConfiguration(bookStore, stockManager);
				Worker workerTask = new Worker(config);

				runResults.add(exec.submit(workerTask));
			}

			for (Future<WorkerRunResult> futureRunResult : runResults) {
				WorkerRunResult runResult = futureRunResult.get(); 
				workerRunResults.add(runResult);
			}

			workersRunResults.add(workerRunResults);
			stockManager.removeAllBooks();
		}

		exec.shutdownNow();
		return workersRunResults;
	}

	private static JFreeChart createChart(XYDataset dataset, String title, String y) {
		JFreeChart chart = ChartFactory.createXYLineChart(title, "Number of clients", y, dataset,
				PlotOrientation.VERTICAL, true, true, false);
		return chart;
	}

	private static XYDataset createDataset(List<Double> localList, List<Double> remoteList) {
		XYSeries series1 = new XYSeries("Local");
		for (int i = 0; i < localList.size(); i++) {
			series1.add(i, localList.get(i));
		}

		XYSeries series2 = new XYSeries("Remote");
		for (int i = 0; i < remoteList.size(); i++) {
			series2.add(i, remoteList.get(i));
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);

		return dataset;
	}

	private static List<Double> calculateLatency(List<List<WorkerRunResult>> workerRunResultsLists) {
		List<Double> latencyList = new ArrayList<Double>();
		for (List<WorkerRunResult> workerRunResults : workerRunResultsLists) {
			double totalElapsedTime = 0;
			for (WorkerRunResult workerRunResult : workerRunResults) {
				totalElapsedTime += workerRunResult.getElapsedTimeInNanoSecs();
			}
			double averageLatency = totalElapsedTime / workerRunResults.size();
			latencyList.add(averageLatency);
		}
		return latencyList;
	}

	private static List<Double> calculateThroughput(List<List<WorkerRunResult>> workerRunResultsLists) {
		List<Double> throughputList = new ArrayList<Double>();
		for (List<WorkerRunResult> workerRunResults : workerRunResultsLists) {
			double totalElapsedTime = 0;
			double totalSuccessfulInteractions = 0;
			for (WorkerRunResult workerRunResult : workerRunResults) {
				totalElapsedTime += workerRunResult.getElapsedTimeInNanoSecs();
				totalSuccessfulInteractions += workerRunResult.getSuccessfulInteractions();
			}
			double aggregatedThroughput = totalSuccessfulInteractions / totalElapsedTime;
			throughputList.add(aggregatedThroughput);
		}
		return throughputList;
	}

	public static void reportMetric(List<List<WorkerRunResult>> workerRunResults,
			List<List<WorkerRunResult>> workerRunResults1) throws IOException {
		List<Double> localLatency = new ArrayList<>();
		List<Double> remoteLatency = new ArrayList<>();
		localLatency = calculateLatency(workerRunResults);
		remoteLatency = calculateLatency(workerRunResults1);

		XYDataset datasetLatency = createDataset(localLatency, remoteLatency);

		JFreeChart latencyChart = createChart(datasetLatency, "LocalvsRemote Latency", "Latency");
		ChartUtilities.saveChartAsPNG(new File("latencyChart.png"), latencyChart, 500, 300);

		List<Double> localThroughput = new ArrayList<>();
		List<Double> remoteThroughput = new ArrayList<>();
		localThroughput = calculateThroughput(workerRunResults);
		remoteThroughput = calculateThroughput(workerRunResults1);

		XYDataset datasetThroughput = createDataset(localThroughput, remoteThroughput);

		JFreeChart throughputChart = createChart(datasetThroughput, "LocalvsRemote Throughput", "Throughput");
		ChartUtilities.saveChartAsPNG(new File("throughputChart.png"), throughputChart, 500, 300);

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
		Set<StockBook> stockBookSet = bookSetGenerator.nextSetOfStockBooks(1500);

		stockManager.removeAllBooks();
		stockManager.addBooks(stockBookSet);
	}
}
