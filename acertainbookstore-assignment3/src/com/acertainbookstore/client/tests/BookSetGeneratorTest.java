package com.acertainbookstore.client.tests;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.workloads.BookSetGenerator;

public class BookSetGeneratorTest {

    private BookSetGenerator bookSetGenerator;

    @BeforeClass
    public void setUp() {
        bookSetGenerator = new BookSetGenerator();
    }

    @Test
    public void shouldReturnRandomISBNS() {
        Set<Integer> isbns = IntStream.rangeClosed(1, 9000).boxed().collect(Collectors.toSet());
        assertEquals(30,bookSetGenerator.sampleFromSetOfISBNs(isbns,30).size());
    }
    @Test
    public void shouldReturnRandomBooks() {
        Set<StockBook> bookSet = bookSetGenerator.nextSetOfStockBooks(100);
        assertEquals(100, bookSet.size());
    }
}