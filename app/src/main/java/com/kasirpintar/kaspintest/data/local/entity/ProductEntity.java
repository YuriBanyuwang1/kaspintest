package com.kasirpintar.kaspintest.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {

    @PrimaryKey
    @NonNull
    public String productId;

    public String name;
    public long price;
    public int stock;

    public ProductEntity(@NonNull String productId, String name, long price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
