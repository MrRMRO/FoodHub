package com.foodhub.dao;

import com.foodhub.model.Customer;
import com.foodhub.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Date;
import java.util.List;

public class CustomerDAO {

    // Get all customers
    public List<Customer> getAllCustomers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Customer", Customer.class).list();
        }
    }

    // Get customer by ID
    public Customer getCustomerById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Customer.class, id);
        }
    }

    // Get customer by phone
    public Customer getCustomerByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Customer> query = session.createQuery(
                    "FROM Customer WHERE phone = :phone", Customer.class);
            query.setParameter("phone", phone);
            return query.uniqueResult();
        }
    }

    // Add new customer
    public int addCustomer(Customer customer) {
        Transaction transaction = null;
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            customer.setCreatedDate(new Date());

            int id = (Integer) session.save(customer);

            transaction.commit();
            return id;

        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            e.printStackTrace();
            return -1;

        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    // Update customer
    public boolean updateCustomer(Customer customer) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(customer);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }
}