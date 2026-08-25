package com.kasirpintar.kaspintest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<ProductEntity> products);

    @Query("SELECT * FROM products ORDER BY name ASC")
    LiveData<List<ProductEntity>> observeAll();

    @Query("SELECT * FROM products WHERE productId = :productId")
    ProductEntity getById(String productId);

    @Query("SELECT COUNT(*) FROM products")
    int count();

    /**
     * Only decrements when stock is sufficient; returns rows affected (0 or 1)
     * so the caller can detect a race/insufficient-stock without a separate read+write.
     */
    @Query("UPDATE products SET stock = stock - :qty WHERE productId = :productId AND stock >= :qty")
    int decrementStock(String productId, int qty);
}
