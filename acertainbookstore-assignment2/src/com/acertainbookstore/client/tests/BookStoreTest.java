package com.acertainbookstore.client.tests;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** Single lock test */
	private static boolean singleLock = false;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn   the isbn
	 * @param copies the copies
	 * @throws BookStoreException the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	public class buyBooks implements Runnable {
		Set<BookCopy> books;
		int opNum;

		buyBooks(int opNum, Set<BookCopy> books) {
			this.opNum = opNum;
			this.books = books;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < opNum; i++) {
					client.buyBooks(books);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
			}

		}
	}

	public class addCopies implements Runnable {
		Set<BookCopy> books;
		int opNum;

		addCopies(int opNum, Set<BookCopy> books) {
			this.opNum = opNum;
			this.books = books;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < opNum; i++) {
					storeManager.addCopies(books);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
			}

		}
	}
	
	// Test scenario 1 from the assignment
	@Test
	public void test1() throws BookStoreException {
		// starting from scratch
		storeManager.removeAllBooks();

		// Add some books to the store
		addBooks(TEST_ISBN, NUM_COPIES * 100);

		Set<BookCopy> books = new HashSet<>();
		books.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		Thread c1 = new Thread(new buyBooks(NUM_COPIES * 10, books));
		Thread c2 = new Thread(new addCopies(NUM_COPIES * 10, books));

		c1.start();
		c2.start();

		try {
			c1.join();
			c2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(500, storeManager.getBooksByISBN(new HashSet<>(singletonList(TEST_ISBN))).get(0).getNumCopies());
	}
	
	public class buyAndRestock implements Runnable {
		Set<BookCopy> books;
		int opNum;

		buyAndRestock(int opNum, Set<BookCopy> books) {
			this.opNum = opNum;
			this.books = books;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < opNum; i++) {
					client.buyBooks(books);

					storeManager.addCopies(books);
				}
			} catch (BookStoreException e) {
				fail("Unexpected exception while buying and restocking books:" + e);
			}

		}
	}
	
	public class checkSnapshot implements Runnable {
		Map<Integer, Integer> expectedQuantitiesMap;
		int opNum;

		checkSnapshot(int opNum, Map<Integer, Integer> expectedQuantitiesMap) {
			this.opNum = opNum;
			this.expectedQuantitiesMap = expectedQuantitiesMap;
		}

		@Override
		public void run() {

			for (int i = 0; i < opNum; i++) {
				try {
					List<StockBook> books = storeManager.getBooks();
					for (StockBook book : books) {
						int expectedQuantity = this.expectedQuantitiesMap.get(book.getISBN());
						assertEquals(expectedQuantity, book.getNumCopies());
					}

				} catch (BookStoreException e) {
					fail("Unexpected exception while buying and restocking books:" + e);
				}
			}

		}
	}
	
	// Test scenario 2 from the assignment
	@Test
    public void test2() throws BookStoreException {

        storeManager.removeAllBooks();

        addBooks(TEST_ISBN, NUM_COPIES);
        addBooks(TEST_ISBN+1,NUM_COPIES);

		Set<BookCopy> booksToBuy = new HashSet<>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES));

        Map<Integer, Integer> expectedQuantities = new HashMap<Integer, Integer>();
    	expectedQuantities.put(TEST_ISBN, NUM_COPIES);
		expectedQuantities.put(TEST_ISBN + 1, NUM_COPIES);

		int opNum = 100;

		Thread c1 = new Thread(new buyAndRestock(opNum, booksToBuy));
		Thread c2 = new Thread(new checkSnapshot(opNum, expectedQuantities));

		c1.start();
        c2.start();

        try {
            c1.join();
            c2.join();
        } catch (InterruptedException e) {
        	fail("Interrupted while waiting for threads to finish: " + e);
        }

        assertTrue(true);

    }

	// test what happens when 2 clients buy all the books in the same time
	@Test
    public void test3() throws BookStoreException {

        storeManager.removeAllBooks();

        addBooks(TEST_ISBN, NUM_COPIES*100);
        addBooks(TEST_ISBN+1,NUM_COPIES*100);

		Set<BookCopy> books = new HashSet<>();
		books.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		books.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES));

       

		int opNum = 100;

		Thread c1 = new Thread(new buyBooks(opNum, books));
		Thread c2 = new Thread(new buyBooks(opNum, books));

		c1.start();
        c2.start();

        try {
            c1.join();
            c2.join();
        } catch (InterruptedException e) {
        	fail("Interrupted while waiting for threads to finish: " + e);
        }

        assertEquals(0, storeManager.getBooksByISBN(new HashSet<>(singletonList(TEST_ISBN))).get(0).getNumCopies());
        assertEquals(0, storeManager.getBooksByISBN(new HashSet<>(singletonList(TEST_ISBN+1))).get(0).getNumCopies());

    }
	// test what happens when a client is buying some books and the stock is replenish while other client add copies into the stock
	@Test
	public void test4() throws BookStoreException {

		storeManager.removeAllBooks();

		addBooks(TEST_ISBN, NUM_COPIES);
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		Set<BookCopy> books = new HashSet<>();
		books.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		books.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES));

		int opNum = 40;

		Thread c1 = new Thread(new buyAndRestock(opNum, books));
		Thread c2 = new Thread(new addCopies(opNum, books));

		c1.start();
		c2.start();

		try {
			c1.join();
			c2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int copies = opNum * NUM_COPIES + NUM_COPIES;

		assertEquals(copies,
				storeManager.getBooksByISBN(new HashSet<>(singletonList(TEST_ISBN))).get(0).getNumCopies());
		assertEquals(copies,
				storeManager.getBooksByISBN(new HashSet<>(singletonList(TEST_ISBN + 1))).get(0).getNumCopies());
	}
	
	
	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
