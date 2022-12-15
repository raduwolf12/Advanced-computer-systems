package com.acertainbookstore.business;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * {@link TwoLevelLockingConcurrentCertainBookStore} implements the
 * {@link BookStore} and {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private Lock exclusiveLock = rwLock.writeLock();
	private Lock sharedLock = rwLock.readLock();

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<Integer, BookStoreBook> bookMap = null;
	private Map<Integer, ReadWriteLock> bookLocks;

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
		bookLocks = new HashMap<>();
	}

	private void getLocalLock(int isbn, boolean readLock) throws BookStoreException {
		if (!bookLocks.containsKey(isbn)) {
			throw new BookStoreException("ISBN" + isbn + BookStoreConstants.NOT_AVAILABLE);
		}
		ReadWriteLock lock = bookLocks.get(isbn);
		sharedLock.lock();
		if (readLock) {
			lock.readLock().lock();
		} else {
			lock.writeLock().lock();
		}
	}

	private void releaseLocalLock(int isbn, boolean readLock) {
		ReadWriteLock lock = bookLocks.get(isbn);
		if (readLock) {
			lock.readLock().unlock();
		} else {
			lock.writeLock().unlock();
		}
		sharedLock.unlock();
	}

	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookPrice < 0.0) { // Check if the price of the book is valid
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookMap.containsKey(isbn)) {// Check if the book is not in stock
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
		}
	}

	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

		if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
			throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
		}
	}

	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
	}

	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
		}
		if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		exclusiveLock.lock();
		try {

			// Check if all are there
			for (StockBook book : bookSet) {
				validate(book);
			}

			for (StockBook book : bookSet) {
				int isbn = book.getISBN();
				bookMap.put(isbn, new BookStoreBook(book));
			}
			for (StockBook book : bookSet) {
				bookLocks.put(book.getISBN(), new ReentrantReadWriteLock());
			}
		} finally {
			exclusiveLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		int isbn;
		int numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		try {
			for (BookCopy bookCopy : bookCopiesSet) {
				getLocalLock(bookCopy.getISBN(), false);
				validate(bookCopy);
			}

			BookStoreBook book;

			// Update the number of copies
			for (BookCopy bookCopy : bookCopiesSet) {
				isbn = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				book = bookMap.get(isbn);
				book.addCopies(numCopies);
			}
		} finally {
			for (BookCopy bookCopy : bookCopiesSet) {
				releaseLocalLock(bookCopy.getISBN(), false);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() throws BookStoreException {

		Collection<BookStoreBook> bookMapValues = bookMap.values();
		try {
			for (BookStoreBook book : bookMapValues) {
				getLocalLock(book.getISBN(), true);
			}

			return bookMapValues.stream().map(book -> book.immutableStockBook()).collect(Collectors.toList());
		} finally {
			for (BookStoreBook book : bookMapValues) {
				releaseLocalLock(book.getISBN(), true);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {

		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int isbnValue;

		try {
			for (BookEditorPick editorPickArg : editorPicks) {
				getLocalLock(editorPickArg.getISBN(), false);
				validate(editorPickArg);
			}

			for (BookEditorPick editorPickArg : editorPicks) {
				bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
			}
		} finally {
			for (BookEditorPick editorPick : editorPicks) {
				releaseLocalLock(editorPick.getISBN(), false);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all ISBNs that we buy are there first.
		int isbn;
		BookStoreBook book;
		Boolean saleMiss = false;

		Map<Integer, Integer> salesMisses = new HashMap<>();
		try {
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				isbn = bookCopyToBuy.getISBN();
				getLocalLock(isbn, false);
				validate(bookCopyToBuy);

				book = bookMap.get(isbn);

				if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
					// If we cannot sell the copies of the book, it is a miss.
					salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
					saleMiss = true;
				}
			}

			// We throw exception now since we want to see how many books in the
			// order incurred misses which is used by books in demand
			if (saleMiss) {
				for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
					book = bookMap.get(saleMissEntry.getKey());
					book.addSaleMiss(saleMissEntry.getValue());
				}
				throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
			}

			// Then make the purchase.
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				book = bookMap.get(bookCopyToBuy.getISBN());
				book.buyCopies(bookCopyToBuy.getNumCopies());
			}
		} finally {
			for (BookCopy bookCopy : bookCopiesToBuy) {
				releaseLocalLock(bookCopy.getISBN(), false);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
	 * Set)
	 */
	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		try {
			for (Integer ISBN : isbnSet) {
				getLocalLock(ISBN, true);
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream().map(isbn -> bookMap.get(isbn).immutableStockBook()).collect(Collectors.toList());
		} finally {
			for (Integer ISBN : isbnSet) {
				releaseLocalLock(ISBN, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		try {
			// Check that all ISBNs that we rate are there to start with.
			for (Integer ISBN : isbnSet) {
				getLocalLock(ISBN, true);
				validateISBNInStock(ISBN);
			}

			return isbnSet.stream().map(isbn -> bookMap.get(isbn).immutableBook()).collect(Collectors.toList());
		} finally {
			for (Integer ISBN : isbnSet) {
				releaseLocalLock(ISBN, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		try {
			List<BookStoreBook> listAllEditorPicks = bookMap.entrySet().stream().map(pair -> pair.getValue())
					.filter(book -> book.isEditorPick()).collect(Collectors.toList());

			for (Book book : listAllEditorPicks) {
				getLocalLock(book.getISBN(), true);
			}

			// Find numBooks random indices of books that will be picked.
			Random rand = new Random();
			Set<Integer> tobePicked = new HashSet<>();
			int rangePicks = listAllEditorPicks.size();

			if (rangePicks <= numBooks) {

				// We need to add all books.
				for (int i = 0; i < listAllEditorPicks.size(); i++) {
					tobePicked.add(i);
				}
			} else {

				// We need to pick randomly the books that need to be returned.
				int randNum;

				while (tobePicked.size() < numBooks) {
					randNum = rand.nextInt(rangePicks);
					tobePicked.add(randNum);
				}
			}

			// Return all the books by the randomly chosen indices.
			return tobePicked.stream().map(index -> listAllEditorPicks.get(index).immutableBook())
					.collect(Collectors.toList());
		} finally {
			for (Book book : bookMap.values()) {
				releaseLocalLock(book.getISBN(), true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public void removeAllBooks() throws BookStoreException {
		exclusiveLock.lock();
		try {
			bookMap.clear();
			bookLocks.clear();
		} finally {
			exclusiveLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		exclusiveLock.lock();
		try {
			if (isbnSet == null) {
				throw new BookStoreException(BookStoreConstants.NULL_INPUT);
			}

			for (Integer ISBN : isbnSet) {
				if (BookStoreUtility.isInvalidISBN(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
				}

				if (!bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
				}
			}

			for (int isbn : isbnSet) {
				bookMap.remove(isbn);
			}
		} finally {
			exclusiveLock.unlock();
		}

	}
}
