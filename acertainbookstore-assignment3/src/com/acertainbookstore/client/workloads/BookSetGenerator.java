package com.acertainbookstore.client.workloads;

import com.github.javafaker.Faker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	Faker faker;
	
	public BookSetGenerator() {
		faker = new Faker();
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		List<Integer> tempList = new ArrayList<>(isbns);
		Collections.shuffle(tempList, new Random());
		Set<Integer> result = tempList.stream().limit(num).collect(Collectors.toSet());
		return result;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> resultSet = new HashSet<>();
		for (int i = 1; i <= num; i++) {
		    resultSet.add(generateRandomBook());
		}
		return resultSet;
	}
	public ImmutableStockBook generateRandomBook() {
		
	    int isbn = faker.number().numberBetween(1000000, 9999999);
	    String title = faker.book().title();
	    String author = faker.book().author();
	    float price = (float) faker.number().numberBetween(1, 100);
	    int numCopies = faker.number().numberBetween(1, 1000);
	    long numSaleMisses = faker.number().numberBetween(0, 10000);
	    long numTimesRated = faker.number().numberBetween(0, 1000);
	    long totalRating = faker.number().numberBetween(0, 5000);
	    boolean editorPick = faker.bool().bool();
	    return new ImmutableStockBook(isbn, title, author, price, numCopies, numSaleMisses, numTimesRated, totalRating, editorPick);
	}

}
