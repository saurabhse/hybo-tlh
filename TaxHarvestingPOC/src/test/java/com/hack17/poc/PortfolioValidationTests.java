package com.hack17.poc;

import static org.junit.Assert.*;

import java.util.List;





import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.repository.PortfolioRepository;



@RunWith(SpringRunner.class)
@ContextConfiguration(classes={TaxHarvestingPocApplication.class})
public class PortfolioValidationTests {

	@Autowired
	PortfolioRepository portfolioRepository;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void portfolioAssetPercentageIsValid(){
		long portfolioId = 101;
		List<Allocation> allocations = getAllocations(portfolioId);
		
		double sumOfAllocations = allocations.stream().mapToDouble(alloc -> alloc.getPercentage()).sum();
		assertEquals(100, sumOfAllocations,0);
		
	}

	@Test
	public void portfolioAllocationETFStructure(){
		long portfolioId = 101;
		List<Allocation> allocations = getAllocations(portfolioId);
		allocations.forEach(alloc->{
			assertNotNull(alloc.getFund());
			assertNotNull(alloc.getFund().getTicker());
			assertNotNull(alloc.getQuantity());
			assertNotNull(alloc.getTransactionDate());
			assertNotNull(alloc.getExpenseRatio());
		});
	}
	
	@Test
	public void portfolioInvestorProfile(){
		long portfolioId = 101;
		Portfolio portfolio = portfolioRepository.getPortfolio(portfolioId);
		assertNotNull(portfolio.getInvestorProfile());
	}

	private List<Allocation> getAllocations(long portfolioId) {
		Portfolio portfolio = portfolioRepository.getPortfolio(portfolioId);
		List<Allocation> allocations = portfolio.getAllocations();
		return allocations;
	}
	

}
