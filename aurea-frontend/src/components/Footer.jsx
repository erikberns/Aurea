export default function Footer() {
  return (
    <footer className="bg-surface-container border-t border-outline-variant/30">
      <div className="w-full px-6 md:px-12 py-16 max-w-7xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10 mb-12">
          <div className="lg:col-span-2">
            <div className="font-headline-md text-headline-md tracking-tight text-on-surface mb-3 flex items-center gap-2">
              <span className="text-primary-container font-serif italic text-3xl font-normal">Á</span>
              <span className="font-semibold tracking-normal">Áurea Joyas</span>
            </div>
            <p className="font-body-md text-body-md text-on-surface-variant max-w-sm mb-6 leading-relaxed">
              Joyería contemporánea en Plata 925 certificada y Baño de Oro 18k Vermeil. Diseñada y
              creada en Buenos Aires con pasión por los detalles y respeto por los oficios
              artesanales.
            </p>
            <div className="flex items-center gap-4 text-on-surface-variant flex-wrap">
              <span className="text-xs font-semibold">Pagos con:</span>
              <span className="font-label-sm text-[11px] px-2 py-1 bg-surface-container-lowest rounded border border-outline-variant/40">
                Mercado Pago
              </span>
              <span className="font-label-sm text-[11px] px-2 py-1 bg-surface-container-lowest rounded border border-outline-variant/40">
                MODO
              </span>
              <span className="font-label-sm text-[11px] px-2 py-1 bg-surface-container-lowest rounded border border-outline-variant/40">
                Tarjetas de Crédito
              </span>
            </div>
          </div>

          <div>
            <h4 className="font-headline-sm text-[16px] text-on-surface font-semibold mb-4">
              Compromiso Áurea
            </h4>
            <ul className="space-y-2.5 font-body-sm text-body-sm text-on-surface-variant">
              <li>Garantía 2 Años</li>
              <li>Packaging Sostenible</li>
              <li>Envíos con Seguimiento</li>
              <li>Guía de Tallas</li>
            </ul>
          </div>

          <div>
            <h4 className="font-headline-sm text-[16px] text-on-surface font-semibold mb-4">
              Cuidado &amp; Ayuda
            </h4>
            <ul className="space-y-2.5 font-body-sm text-body-sm text-on-surface-variant">
              <li>Cuidado de Joyas</li>
              <li>Aviso Legal</li>
              <li>Política de Privacidad</li>
              <li>Términos y Condiciones</li>
            </ul>
          </div>

          <div>
            <h4 className="font-headline-sm text-[16px] text-on-surface font-semibold mb-4">
              Atelier Buenos Aires
            </h4>
            <p className="text-body-sm font-body-sm text-on-surface-variant mb-2">
              Defensa 842, San Telmo
              <br />
              Ciudad Autónoma de Buenos Aires
            </p>
            <p className="text-body-sm font-body-sm text-on-surface-variant mb-4">
              Lunes a Viernes de 10 a 18 hs
              <br />
              hola@aureajoyas.com.ar
            </p>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-surface-container-lowest rounded-full border border-outline-variant/40 text-[11px] font-semibold text-secondary">
              <span className="material-symbols-outlined text-[14px]">chat</span>
              <span>WhatsApp: +54 9 11 4059-8800</span>
            </div>
          </div>
        </div>

        <div className="pt-8 border-t border-outline-variant/30 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-body-sm font-body-sm text-on-surface-variant text-center md:text-left">
            © 2026 Áurea Joyas S.L. Todos los derechos reservados. Joyería contemporánea en Plata
            925 y Baño de Oro 18k Vermeil.
          </p>
          <div className="flex items-center gap-6">
            <span className="text-xs text-outline">Argentina · ARS ($)</span>
            <span className="font-label-sm text-label-sm text-on-surface-variant hover:text-primary transition-colors cursor-pointer">
              Defensa del Consumidor
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
