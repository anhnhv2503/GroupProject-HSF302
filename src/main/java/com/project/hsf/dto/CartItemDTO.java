package com.project.hsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private String itemKey; // "P_" + productId or "C_" + comboId
    private Integer productId;
    private Integer comboId;
    private String name;
    private Integer quantity;
    private Double unitPrice;
    private String imageUrl; // Primary image URL for display
    private String itemType; // "PRODUCT" or "COMBO"

    public Double getSubtotal() {
        if (quantity != null && unitPrice != null) {
            return this.quantity * this.unitPrice;
        }
        return 0.0;
    }
}