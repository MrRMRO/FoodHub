package com.foodhub.dao;

import com.foodhub.model.Order;
import com.foodhub.model.OrderItem;
import com.foodhub.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class OrderDAO {

    // Get all orders
    public List<Order> getAllOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Order ORDER BY orderDate DESC", Order.class).list();
        }
    }

    // Get order by ID
    public Order getOrderById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Order.class, id);
        }
    }

    // Get orders by customer ID
    public List<Order> getOrdersByCustomerId(int customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "FROM Order WHERE customerId = :customerId ORDER BY orderDate DESC", Order.class);
            query.setParameter("customerId", customerId);
            return query.list();
        }
    }

    // Get orders by status
    public List<Order> getOrdersByStatus(String status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "FROM Order WHERE status = :status ORDER BY orderDate DESC", Order.class);
            query.setParameter("status", status);
            return query.list();
        }
    }

    // Add new order
    public int addOrder(Order order) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int id = (Integer) session.save(order);
            transaction.commit();
            return id;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return -1;
        }
    }

    // Update order status
    public boolean updateOrderStatus(int orderId, String status) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Order order = session.get(Order.class, orderId);
            if (order != null) {
                order.setStatus(status);
                session.update(order);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    // Get order items for an order
    public List<OrderItem> getOrderItems(int orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<OrderItem> query = session.createQuery(
                    "FROM OrderItem WHERE orderId = :orderId", OrderItem.class);
            query.setParameter("orderId", orderId);
            return query.list();
        }
    }

    // Add order item
    public boolean addOrderItem(OrderItem orderItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(orderItem);
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