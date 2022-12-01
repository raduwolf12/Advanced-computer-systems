package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.acertainbookstore.business.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

// TODO: Auto-generated Javadoc
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

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
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
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * Test buy default book.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testBuyOnlyOneDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));

		// Try to buy the book
		client.buyBooks(booksToBuy);

		List<StockBook> bookList = storeManager.getBooks();
		assertTrue(bookList.size() == 1);
		StockBook firstBookInList = bookList.get(0);

		assertTrue(firstBookInList.getNumCopies() == (NUM_COPIES - 1));
	}
	@Test
	public void testBuyNoBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();

		// Try to buy the book
		client.buyBooks(booksToBuy);

		List<StockBook> bookList = storeManager.getBooks();
		assertTrue(bookList.size() == 1);
		StockBook firstBookInList = bookList.get(0);

		assertTrue(firstBookInList.getNumCopies() == (NUM_COPIES));
	}
	/**
	 * Test rate books.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetInvalidRateBooks() throws BookStoreException {
		HashSet<BookRating> rateBooks = new HashSet<>();
		rateBooks.add(new BookRating(0, -1 ));

		try {
			client.rateBooks(rateBooks);
			fail();
		} catch (BookStoreException ex) {
			;
		}
		//TBD
	}
	
	/**
	 * Test rate books.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testRateBooks() throws BookStoreException {
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 5));

		client.rateBooks(rateBooks);

		List<StockBook> bookList = storeManager.getBooks();
		assertTrue(bookList.size() == 1);
		StockBook stockBook = bookList.get(0);

		assertTrue(stockBook.getTotalRating() == 5);
		assertTrue(stockBook.getNumTimesRated() == 1);
		assertTrue(stockBook.getAverageRating() - 5 == 0);
	}
	
	/**
	 * Test rate books multiple.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testRateBooksMultiple() throws BookStoreException {
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 5));
		client.rateBooks(rateBooks);
		
		rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 4));
		client.rateBooks(rateBooks);
		
		rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 3));
		client.rateBooks(rateBooks);
		
		rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 5));
		client.rateBooks(rateBooks);

		List<StockBook> bookList = storeManager.getBooks();
		assertTrue(bookList.size() == 1);
		StockBook stockBook = bookList.get(0);

		assertTrue(stockBook.getTotalRating() == 17);
		assertTrue(stockBook.getNumTimesRated() == 4);
		assertTrue(stockBook.getAverageRating() == 4.25);
	}
	
	/**
	 * Test rate books invalid ISBN.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	 public void testRateBooksInvalidISBN() throws BookStoreException {
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(-1, 1)); // invalid

		 try{
			 client.rateBooks(rateBooks);
			 fail();
		 } catch(BookStoreException ex){
			 ;
		 }

		 List<StockBook> bookList = storeManager.getBooks();
		 assertTrue(bookList.size() == 1);
		 StockBook stockBook = bookList.get(0);

		 assertTrue(stockBook.getTotalRating() == 0);
		 assertTrue(stockBook.getNumTimesRated() == 0);
		 assertTrue(stockBook.getAverageRating() == -1.0f);
	 }
	
	/**
	 * Test rate books negative.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test 
	 public void testRateBooksNegative() throws BookStoreException{
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, -1)); // Invalid


		 try{
			 client.rateBooks(rateBooks);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }

		 List<StockBook> bookList = storeManager.getBooks();
		 StockBook stockBook = bookList.get(0);

		 assertTrue(stockBook.getTotalRating() == 0);
		 assertTrue(stockBook.getNumTimesRated() == 0);
		 assertTrue(stockBook.getAverageRating() == -1.0f);
	 }
	
	/**
	 * Test rate books over five rating.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test 
	 public void testRateBooksOverFiveRating() throws BookStoreException{
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 100)); // Invalid


		 try{
			 client.rateBooks(rateBooks);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }

		 List<StockBook> bookList = storeManager.getBooks();
		 StockBook stockBook = bookList.get(0);

		 assertTrue(stockBook.getTotalRating() == 0);
		 assertTrue(stockBook.getNumTimesRated() == 0);
		 assertTrue(stockBook.getAverageRating() == -1.0f);
	 }
	
	/**
	 * Test rate books negative and positive.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test 
	 public void testRateBooksNegativeAndPositive() throws BookStoreException{
		Set<BookRating> rateBooks = new HashSet<BookRating>();
		rateBooks.add(new BookRating(TEST_ISBN, 2)); // Valid
		
		Set<BookRating> rateBooks1 = new HashSet<BookRating>();
		rateBooks1.add(new BookRating(TEST_ISBN, -1)); // Invalid
		
		
		client.rateBooks(rateBooks);
		
		 try{
			 client.rateBooks(rateBooks1);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }

		 List<StockBook> bookList = storeManager.getBooks();
		 StockBook stockBook = bookList.get(0);

		 assertTrue(stockBook.getTotalRating() == 2);
		 assertTrue(stockBook.getNumTimesRated() == 1);
		 assertTrue(stockBook.getAverageRating() == 2);
	 }
	
	/**
	 * Test rate books null input.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test 
	 public void testRateBooksNullInput() throws BookStoreException{
		Set<BookRating> rateBooks = null; 
		
		try{
			 client.rateBooks(rateBooks);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }

		 List<StockBook> bookList = storeManager.getBooks();
		 StockBook stockBook = bookList.get(0);

		 assertTrue(stockBook.getTotalRating() == 0);
		 assertTrue(stockBook.getNumTimesRated() == 0);
		 assertTrue(stockBook.getAverageRating() == -1.0f);
	 }
	
	 /**
 	 * Test get top rated books.
 	 *
 	 * @throws BookStoreException the book store exception
 	 */
 	@Test
	 public void testGetTopRatedBooks() throws BookStoreException {
		 int new_isbn = 9999;
		 int new_isbn1 = 7777;

		 // Add new books
		 addBooks(new_isbn, 15);
		 addBooks(new_isbn1, 6);

		 // rate books
		 Set<BookRating> rateBooks = new HashSet<BookRating>();
		 rateBooks.add(new BookRating(TEST_ISBN, 5));
		 rateBooks.add(new BookRating(new_isbn, 2));
		 rateBooks.add(new BookRating(new_isbn1, 4));

		 

		 client.rateBooks(rateBooks);

		 List<Book> topRatedBooks = client.getTopRatedBooks(3);
		 Book book = topRatedBooks.get(0);
		 StockBook defaultBook = getDefaultBook();

		 assertTrue(book.getISBN() == defaultBook.getISBN());
		 
		 Book lastRatedBook = topRatedBooks.get(2);
		 
		 assertTrue(lastRatedBook.getISBN() == new_isbn);
	 }
 	@Test
	 public void testGetTopRatedBooksNegative() throws BookStoreException {
		 int new_isbn = 9999;

		 // Add new book
		 addBooks(new_isbn, 15);

		 // rate books
		 Set<BookRating> rateBooks = new HashSet<BookRating>();
		 rateBooks.add(new BookRating(TEST_ISBN, 5));
		 rateBooks.add(new BookRating(new_isbn, 2));
		 

		 client.rateBooks(rateBooks);

		 List<Book> topRatedBooks = new ArrayList<Book>();
		 try{
			 topRatedBooks = client.getTopRatedBooks(-1);
				fail();
			} catch(BookStoreException ex){
				;
			}

		 assertTrue(topRatedBooks.isEmpty());
	 }

	/**
	 * Test get books in demand.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetBooksInDemand() throws BookStoreException {
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES+1));


		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}
		List<StockBook> booksInDemandList = storeManager.getBooksInDemand();
		
		assertTrue(booksInDemandList.size()>0);
		
		StockBook bookInDemand = booksInDemandList.get(0);
		StockBook insertedBook = getDefaultBook();
		
		assertTrue(bookInDemand.getISBN() == insertedBook.getISBN());
	}
	
	/**
	 * Test get books in demand empty.
	 *
	 * @throws BookStoreException the book store exception
	 */
	@Test
	public void testGetBooksInDemandEmpty() throws BookStoreException {
		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		
		assertTrue(booksInDemand.size() == 0);
	}
	
	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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
	 * @throws BookStoreException
	 *             the book store exception
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

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
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
