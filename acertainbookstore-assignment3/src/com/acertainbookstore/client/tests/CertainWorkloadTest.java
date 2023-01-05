package com.acertainbookstore.client.tests;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.workloads.CertainWorkload;
import com.acertainbookstore.utils.BookStoreException;

public class CertainWorkloadTest {

    private static CertainBookStore bookStore;
    private static CertainBookStore stockManager;

    @BeforeClass
    public static void setUp() {
        CertainBookStore store = new CertainBookStore();
        try {
			bookStore = store;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			stockManager = store;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    @Test
    public void shouldInitializeBookStore() throws BookStoreException {
        CertainWorkload.initializeBookStoreData(bookStore, stockManager);
        // initialize 1000 books
        assertEquals(1000, bookStore.getBooks().size());
    }

}