package com.joyeriaEcommerce.AureaTPO.ordenes.negocio.strategy;
import org.springframework.stereotype.Service;
/** Stateless: cada calculo depende exclusivamente del subtotal recibido. */
@Service
public class CostoEnvio {
 private final FreeShippingStrategy gratis; private final StandardShippingStrategy estandar;
 public CostoEnvio(FreeShippingStrategy gratis,StandardShippingStrategy estandar){this.gratis=gratis;this.estandar=estandar;}
 public double calcular(double subtotal){
  ShippingStrategy estrategia=subtotal>=60000?gratis:estandar;
  return estrategia.calculateShippingCost(subtotal);
 }
}
