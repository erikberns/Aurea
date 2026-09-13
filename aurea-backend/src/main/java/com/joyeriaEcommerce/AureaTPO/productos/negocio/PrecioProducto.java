package com.joyeriaEcommerce.AureaTPO.productos.negocio;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.descuentos.datos.Discount;
import java.time.LocalDate;
public final class PrecioProducto {
 private PrecioProducto(){}
 public static Double descuento(Product p){
  Discount d=vigente(p.getDiscount())?p.getDiscount():p.getCategory()!=null&&vigente(p.getCategory().getDiscount())?p.getCategory().getDiscount():null;
  return d==null?null:p.getPrice()*(1-d.getPercentage()/100);
 }
 private static boolean vigente(Discount d){
  LocalDate hoy=LocalDate.now();
  return d!=null&&Boolean.TRUE.equals(d.getActive())&&d.getPercentage()!=null&&d.getPercentage()>0&&d.getPercentage()<=100
   &&(d.getStartDate()==null||!hoy.isBefore(d.getStartDate()))&&(d.getEndDate()==null||!hoy.isAfter(d.getEndDate()));
 }
 public static double finalPrice(Product p){Double valor=descuento(p);return valor==null?p.getPrice():valor;}
}
