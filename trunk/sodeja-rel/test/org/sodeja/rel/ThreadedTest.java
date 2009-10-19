package org.sodeja.rel;

import org.sodeja.lang.Range;

public class ThreadedTest {
	private static Object GLOBAL_LOCK = new Object();
	
	private static class TestThread extends Thread {
		private final Domain domain;
		private final Range insertRange;
		private final Range insertAnDeleteRange;
		
		public TestThread(Domain domain, Range insertRange, Range insertAnDeleteRange) {
			this.domain = domain;
			this.insertRange = insertRange;
			this.insertAnDeleteRange = insertAnDeleteRange;
		}

		@Override
		public void run() {
			synchronized (GLOBAL_LOCK) {
				try {
					GLOBAL_LOCK.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for(Integer i : insertRange) {
				domain.getTransactionManager().begin();
				
				System.out.println("Insert: " + i);
				domain.insertPlain("Department", 
						"department_id", i, 
						"name", "fairlyLongName", 
						"manager", "fairlyLongManagerName");
				
				domain.getTransactionManager().commit();
			}

			for(Integer i : insertAnDeleteRange) {
				domain.getTransactionManager().begin();

				System.out.println("Insert: " + i);
				domain.insertPlain("Department", 
						"department_id", i, 
						"name", "fairlyLongName", 
						"manager", "fairlyLongManagerName");
				
				domain.getTransactionManager().commit();

				domain.getTransactionManager().begin();

				System.out.println("Delete: " + i);
				domain.deletePlain("Department", "department_id", i); 

				domain.getTransactionManager().commit();
			}
			
			System.out.println("Finished");
		}
	}
	
	public static void main(String[] args) {
		Domain domain = IntegrityTests.createDomain();
		
		TestThread[] threads = new TestThread[10];
		int sz = 10;
		int delBase = sz * threads.length;
		
		for(Integer i : Range.of(threads)) {
			threads[i] = new TestThread(domain, new Range(i * sz, (i + 1) * sz), new Range(delBase + i * sz, delBase + (i + 1) * sz));
			threads[i].start();
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long begin = System.currentTimeMillis();
		synchronized (GLOBAL_LOCK) {
			GLOBAL_LOCK.notifyAll();
		}
		
		OUTER: while(true) {
			for(TestThread t : threads) {
				if(t.isAlive()) {
					continue OUTER;
				}
			}
			break;
		}
		long end = System.currentTimeMillis();
		
		System.out.println("COUNT: " + (domain.resolveBase("Department").select().size()));
		System.out.println("TIME: " + (end - begin));
	}
}
