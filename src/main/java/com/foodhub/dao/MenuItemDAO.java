package com.foodhub.dao;

import com.foodhub.model.MenuItem;
import com.foodhub.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class MenuItemDAO {

    // Get all menu items
    public List<MenuItem> getAllMenuItems() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM MenuItem", MenuItem.class).list();
        }
    }

    // Get menu items by category
    public List<MenuItem> getMenuItemsByCategory(String category) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<MenuItem> query = session.createQuery(
                    "FROM MenuItem WHERE category = :category", MenuItem.class);
            query.setParameter("category", category);
            return query.list();
        }
    }

    // Get available menu items
    public List<MenuItem> getAvailableMenuItems() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<MenuItem> query = session.createQuery(
                    "FROM MenuItem WHERE available = true", MenuItem.class);
            return query.list();
        }
    }

    // Get menu item by ID
    public MenuItem getMenuItemById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(MenuItem.class, id);
        }
    }

    // Add new menu item
    public boolean addMenuItem(MenuItem menuItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(menuItem);
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

    // Update menu item
    public boolean updateMenuItem(MenuItem menuItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(menuItem);
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

    // Delete menu item
    public boolean deleteMenuItem(int id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            MenuItem menuItem = session.get(MenuItem.class, id);
            if (menuItem != null) {
                session.delete(menuItem);
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
}