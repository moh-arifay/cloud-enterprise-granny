package com.sap.hana.cloud.samples.granny.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sap.hana.cloud.samples.granny.model.Address;
import com.sap.hana.cloud.samples.granny.model.AddressType;
import com.sap.hana.cloud.samples.granny.model.CommunicationType;
import com.sap.hana.cloud.samples.granny.model.Contact;
import com.sap.hana.cloud.samples.granny.model.EmailAddress;
import com.sap.hana.cloud.samples.granny.model.PhoneNumber;
import com.sap.hana.cloud.samples.granny.model.Salutation;

/**
 * Tests for the {@link ContactDAO} class.  
 */
@ContextConfiguration("classpath:/META-INF/spring/app-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles(profiles = "test")
public class TestContactDAO
{
	@Autowired
	ContactDAO contactDAO = null;
	
	/**
	 * Tests the CRUD operations.
	 */
	@Test
	@Transactional
	public void testCRUD() 
	{
		boolean transactional = TransactionSynchronizationManager.isActualTransactionActive();
		assertTrue("CRUD operations test should run within a transaction!", transactional);
		
		Contact testPerson = createTestContact();
		
		long noOfRecords = contactDAO.count();
		
		// create
		Contact contact = contactDAO.save(testPerson);
		assertNotNull("Contact should have been created!", contact);
		assertTrue("Contact should have been created!", (noOfRecords == (contactDAO.count() - 1)));
		
		// read
		Contact anotherContactRef = contactDAO.findOne(contact.getId());
		assertNotNull("Contact should have been read!", anotherContactRef);
		
		// update
		anotherContactRef.setFirstName("NEO");
		
		Contact updatedContactRef = contactDAO.save(anotherContactRef);
		assertTrue("Contact name should be updated!", anotherContactRef.getFirstName().equals(updatedContactRef.getFirstName()));
		
		// delete
		contactDAO.delete(updatedContactRef);
		assertTrue("Contact should have been deleted!", (noOfRecords == contactDAO.count()));
		
	}
	
	/** 
	 * Tests concurrent modification of an object.
	 */
	public void testOptimisticLocking() 
	{
		boolean transactional = TransactionSynchronizationManager.isActualTransactionActive();
		assertFalse("Optimistic Locking behavior cannot be verified within a transaction!", transactional);
		
		Contact testPerson = createTestContact();
		
		long noOfRecords = contactDAO.count();
		
		// create
		Contact contact = contactDAO.save(testPerson);
		assertNotNull("Contact should have been created!", contact);
		assertTrue("Contact should have been created!", (noOfRecords == (contactDAO.count() - 1)));
		
		// another user is obtaining a reference to the same contact
		Contact anotherContactRef = contactDAO.findOne(contact.getId());	
		assertNotNull("Contact should have been read!", anotherContactRef);
		
		// update
		contact.setFirstName("NEO");
		Contact updatedContact = contactDAO.save(contact);
		assertTrue("Contact name should be updated!", contact.getFirstName().equals(updatedContact.getFirstName()));
		
		// now, the version attribute of the two object references should be different
		assertTrue("Version numbers should be different!", (anotherContactRef.getVersion() != updatedContact.getVersion()));
		
		// 2nd update - should fail
		try
		{
			anotherContactRef = contactDAO.save(anotherContactRef);
			fail("OtimisticLocking excpetion should have been thrown!");
		}
		catch (Exception ex)
		{
			assertTrue("Expected OptimisticException", ex instanceof JpaOptimisticLockingFailureException);
		}
		finally
		{
			// clean up the created contact
			contactDAO.delete(contact);
		}
		
	}
	
	/**
	 * Tests the query for contacts based on a given country.
	 */
	@Test
	@Transactional
	public void testfindByAddressCountryQuery()
	{
		final String AUSTRALIA = "AU"; 
		
		Contact testPerson = createTestContact();
		Address address = testPerson.getAddresses().get(0);
		address.setCountry(AUSTRALIA);
		
		long noOfRecords = contactDAO.count();
		
		// create
		Contact contact = contactDAO.save(testPerson);
		assertNotNull("Contact should have been created!", contact);
		assertTrue("Contact should have been created!", (noOfRecords == (contactDAO.count() - 1)));
				
		// spring data JPA query
		List<Contact> aussies = contactDAO.findByAddressesCountry(AUSTRALIA);
		assertTrue("There should be at least one contact", (aussies != null && aussies.size() > 0));
	
		// query all (optimized via fetch) 
		aussies = contactDAO.findAll();
		assertTrue("There should be at least one contact", (aussies != null && aussies.size() > 0));
		
	}
	
	/**
	 * Creates a demo test {@link Contact}.
	 * 
	 * @return A test {@link Contact}
	 */
	static Contact createTestContact()
	{
		Contact contact = new Contact();
		Address address = new Address();
		EmailAddress email = new EmailAddress();
		PhoneNumber homePhone = new PhoneNumber();
		PhoneNumber cellPhone = new PhoneNumber();
	
		homePhone.setType(CommunicationType.HOME);
		homePhone.setNumber("+1 610 661 1000");
		
		cellPhone.setType(CommunicationType.CELL);
		cellPhone.setNumber("+1 555 123 4567");
		
		email.setType(AddressType.WORK);
		email.setEmail("thomas.anderson@sap.com");
		
		address.setType(AddressType.WORK);
		address.setCity("Newton Square");
		address.setCountry("US");
		address.setState("PA");
		address.setStreet("3999 W Chester Pike");
		
		contact.setFirstName("Thomas");
		contact.setLastName("Anderson");
		contact.setSalutation(Salutation.MR);
		
		List<Address> addressList = new ArrayList<Address>(1);
		List<EmailAddress> emailList = new ArrayList<EmailAddress>(1);
		List<PhoneNumber> phoneNumberList = new ArrayList<PhoneNumber>(2);
	
		addressList.add(address);
		emailList.add(email);
		phoneNumberList.add(homePhone);
		phoneNumberList.add(cellPhone);
		
		contact.setAddresses(addressList);
		contact.setEmailAdresses(emailList);
		contact.setPhoneNumbers(phoneNumberList);
		
		return contact;
	}
}
