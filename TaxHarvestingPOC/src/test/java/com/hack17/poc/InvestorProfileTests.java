package com.hack17.poc;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.hack17.hybo.domain.InvestorProfile;
import com.hack17.hybo.domain.RiskTolerance;
import com.hack17.hybo.repository.ProfileRepository;


//import com.hack17.poc.repository.ProfileRepository;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes={TaxHarvestingPocApplication.class})
public class InvestorProfileTests {
	
	@Autowired
	ProfileRepository profileRepository;

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
	public void requiredAttributes() {
		InvestorProfile profile = profileRepository.getProfile(501);
		assertNotNull(profile.getDateOfBirth());
		assertEquals(RiskTolerance.MEDIUM, profile.getRiskTolerance());
		assertEquals(26, profile.getInvestmentHorizonInMonths(), 0);		
	}

}
