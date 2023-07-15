package com.driver.services.impl;

import com.driver.model.*;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		Optional<Customer> optionalCustomer = customerRepository2.findById(customerId);
		if(!optionalCustomer.isPresent())
			return;
		Customer customer = optionalCustomer.get();
		customerRepository2.delete(customer);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query

		// Check customer exist or not
		Optional<Customer> optionalCustomer = customerRepository2.findById(customerId);
//		if(!optionalCustomer.isPresent())
//			throw  new Exception("No cab available!");

		// Check any cab-driver available or not
		List<Driver> driverList = driverRepository2.findAll();
		Driver driver = null;
		int driverId = Integer.MAX_VALUE;
		for(Driver currDriver : driverList){
			Cab currCab = currDriver.getCab();
			if(currCab.getAvailable()){
				if(currDriver.getDriverId() < driverId){
					driverId = currDriver.getDriverId();
					driver = currDriver;
				}
			}
		}
		if(driver == null){
			throw new Exception("No cab available!");
		}

		// Now get the customer and get the total bill
		Customer customer = optionalCustomer.get();
		Cab cab = driver.getCab();
		int bill = distanceInKm * cab.getPerKmRate();

		// Now prepare the tripBooking object;
		TripBooking tripToBeBooked = new TripBooking();
		tripToBeBooked.setFromLocation(fromLocation);
		tripToBeBooked.setToLocation(toLocation);
		tripToBeBooked.setDistanceInKm(distanceInKm);
		tripToBeBooked.setStatus(TripStatus.CONFIRMED);
		tripToBeBooked.setBill(bill);
		tripToBeBooked.setDriver(driver);
		tripToBeBooked.setCustomer(customer);

		cab.setAvailable(false);

		// Save the trip in Database
		TripBooking bookedTrip = tripBookingRepository2.save(tripToBeBooked);

		// Now check for the Driver Entity
		List<TripBooking> tripBookingListOfDriver = driver.getTripBookingList();
		if(tripBookingListOfDriver == null)
			tripBookingListOfDriver = new ArrayList<>();
		tripBookingListOfDriver.add(bookedTrip);
		driver.setTripBookingList(tripBookingListOfDriver);
		driverRepository2.save(driver);

		// Now check for the Customer Entity
		List<TripBooking> tripBookingListOfCustomer = customer.getTripBookingList();
		if(tripBookingListOfCustomer == null)
			tripBookingListOfCustomer = new ArrayList<>();
		tripBookingListOfCustomer.add(bookedTrip);
		customer.setTripBookingList(tripBookingListOfCustomer);
		customerRepository2.save(customer);

		return bookedTrip;

	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		Optional<TripBooking> optionalTripBooking = tripBookingRepository2.findById(tripId);
		if(!optionalTripBooking.isPresent())
			return;

		TripBooking bookedTrip = optionalTripBooking.get();
		bookedTrip.setStatus(TripStatus.CANCELED);
		bookedTrip.setBill(0);
		Driver driver = bookedTrip.getDriver();
		Cab cab = driver.getCab();
		cab.setAvailable(true);

		driverRepository2.save(driver);
		tripBookingRepository2.save(bookedTrip);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		Optional<TripBooking> optionalTripBooking = tripBookingRepository2.findById(tripId);
		if(!optionalTripBooking.isPresent())
			return;

		TripBooking bookedTrip = optionalTripBooking.get();
		bookedTrip.setStatus(TripStatus.COMPLETED);
		Driver driver = bookedTrip.getDriver();
		Cab cab = driver.getCab();
		cab.setAvailable(true);

		driverRepository2.save(driver);
		tripBookingRepository2.save(bookedTrip);
	}
}
