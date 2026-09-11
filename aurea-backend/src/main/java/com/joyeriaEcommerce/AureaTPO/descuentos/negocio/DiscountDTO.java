package com.joyeriaEcommerce.AureaTPO.descuentos.negocio;

import com.joyeriaEcommerce.AureaTPO.descuentos.datos.Discount;
import java.time.LocalDate;

public record DiscountDTO(Long id, Boolean active, LocalDate startDate, LocalDate endDate, Double percentage) {

    public static DiscountDTO desde(Discount discount) {
        if (discount == null) {
            return null;
        }
        return new DiscountDTO(
                discount.getId(),
                discount.getActive(),
                discount.getStartDate(),
                discount.getEndDate(),
                discount.getPercentage()
        );
    }
}
