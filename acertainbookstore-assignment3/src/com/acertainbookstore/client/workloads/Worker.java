/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
	configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
	try {
	    float percentRareStockManagerInteraction = configuration.getPercentRareStockManagerInteraction();
	    float percentFrequentStockManagerInteraction = configuration.getPercentFrequentStockManagerInteraction();

	    if (chooseInteraction < percentRareStockManagerInteraction) {
		runRareStockManagerInteraction();
	    } else if (chooseInteraction < percentRareStockManagerInteraction
		    + percentFrequentStockManagerInteraction) {
		runFrequentStockManagerInteraction();
	    } else {
		numTotalFrequentBookStoreInteraction++;
		runFrequentBookStoreInteraction();
		numSuccessfulFrequentBookStoreInteraction++;
	    }
	} catch (BookStoreException ex) {
	    return false;
	}
	return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
	int count = 1;
	long startTimeInNanoSecs = 0;
	long endTimeInNanoSecs = 0;
	int successfulInteractions = 0;
	long timeForRunsInNanoSecs = 0;

	Random rand = new Random();
	float chooseInteraction;

	// Perform the warmup runs
	while (count++ <= configuration.getWarmUpRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    runInteraction(chooseInteraction);
	}

	count = 1;
	numTotalFrequentBookStoreInteraction = 0;
	numSuccessfulFrequentBookStoreInteraction = 0;

	// Perform the actual runs
	startTimeInNanoSecs = System.nanoTime();
	while (count++ <= configuration.getNumActualRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    if (runInteraction(chooseInteraction)) {
		successfulInteractions++;
	    }
	}
	endTimeInNanoSecs = System.nanoTime();
	timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
	return new WorkerRunResult(successfulInteractions, timeForRunsInNanoSecs, configuration.getNumActualRuns(),
		numSuccessfulFrequentBookStoreInteraction, numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     * 
     * @throws BookStoreException
     */
	private void runRareStockManagerInteraction() throws BookStoreException {
		StockManager stockManager = configuration.getStockManager();
		BookSetGenerator bookSetGenerator = configuration.getBookSetGenerator();

		List<StockBook> stockBookList = stockManager.getBooks();

		List<StockBook> newBooks = new ArrayList<>(
				bookSetGenerator.nextSetOfStockBooks(configuration.getNumBooksToAdd()));

		Set<StockBook> booksMissing = new HashSet<>();
		for (StockBook book : newBooks) {
			if (!stockBookList.contains(book)) {
				booksMissing.add(book);
			}
		}

		stockManager.addBooks(booksMissing);
	}

    /**
     * Runs the stock replenishment interaction
     * 
     * @throws BookStoreException
     */
	private void runFrequentStockManagerInteraction() throws BookStoreException {
		StockManager stockManager = configuration.getStockManager();

		List<StockBook> stockBookList = stockManager.getBooks();
		Collections.sort(stockBookList, (a, b) -> b.getNumCopies() - a.getNumCopies());

		int numBooksWithLeastCopies = configuration.getNumBooksWithLeastCopies();
		List<StockBook> booksWithLeastCopies = stockBookList.subList(0, numBooksWithLeastCopies);

		Set<BookCopy> bookCopies = new HashSet<>();
		for (StockBook book : booksWithLeastCopies) {
			int isbn = book.getISBN();
			int numAddCopies = configuration.getNumAddCopies();
			bookCopies.add(new BookCopy(isbn, numAddCopies));
		}

		stockManager.addCopies(bookCopies);
	}

    /**
     * Runs the customer interaction
     * 
     * @throws BookStoreException
     */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
		BookStore bookStore = configuration.getBookStore();
		BookSetGenerator bookSetGenerator = configuration.getBookSetGenerator();

		int numEditorPicksToGet = configuration.getNumEditorPicksToGet();
		Set<Integer> editorPicksISBN = bookStore.getEditorPicks(numEditorPicksToGet).stream().map(Book::getISBN)
				.collect(Collectors.toSet());

		int numBooksToBuy = configuration.getNumBooksToBuy();
		Set<Integer> bookISBNsToBuy = bookSetGenerator.sampleFromSetOfISBNs(editorPicksISBN, numBooksToBuy);
		Set<BookCopy> booksToBuy = new HashSet<>();
		for (int isbn : bookISBNsToBuy) {
			int numBookCopiesToBuy = configuration.getNumBookCopiesToBuy();
			booksToBuy.add(new BookCopy(isbn, numBookCopiesToBuy));
		}

		bookStore.buyBooks(booksToBuy);
	}

}
