# AGENTS.md — Tienda online de Carnicería y Almacén

Guía para el agente que siga desarrollando este proyecto. Leela entera antes de hacer cambios.

Este archivo cubre diseño visual, contenido y tono del frontend. Para stack técnico, modelo de datos, roles (admin/cliente), flujos de pago (MercadoPago/transferencia/efectivo) y Supabase Auth, ver [constitution.md](./constitution.md).

## Qué es el proyecto

Una tienda online para una carnicería de barrio que también vende productos de almacén, ubicada en Argentina.
La meta es que la gente arme su pedido (cortes por kilo y productos por unidad), elija envío o retiro y pague.

`index.html` es la **base visual aprobada**: landing + catálogo con carrito en memoria/localStorage.
Todo lo nuevo tiene que verse y comportarse como parte de esa misma página.

## Principios de diseño (no negociables)

1. **Confiable, limpia y profesional.** Nada de estética "promo chillona": sin gradientes llamativos, sin emojis como íconos, sin sombras exageradas, sin texto en mayúsculas salvo los *eyebrows* chicos.
2. **Mobile first de verdad.** Todo tiene que funcionar a 360 px de ancho y verse bien hasta 1440 px. Probá ambas antes de dar algo por terminado.
3. **La confianza se gana con claridad:** precios visibles, forma de cobro por peso explicada, horarios y zona de envío claros, datos legales en el footer.
4. **Accesibilidad AA:** contraste de texto ≥ 4.5:1, objetivos táctiles ≥ 44 px, `<button>` y `<a>` reales (nunca `div` clickeables), `aria-label` en botones solo-ícono, `label` en cada input, foco visible.
5. **Movimiento sutil.** Los efectos de scroll suman, no distraen. Respetar siempre `prefers-reduced-motion`.

## Sistema visual

Los tokens están en `:root` dentro de `index.html`. **Usá siempre las variables, nunca hex sueltos.**

| Token | Valor | Uso |
|---|---|---|
| `--accent` | `#8E1B1B` (bordó) | CTAs principales, eyebrows, badges |
| `--accent-hover` | `#6E1414` | hover de CTAs |
| `--accent-soft` | `#F8ECEC` | fondos de pills / íconos |
| `--ink` | `#1C1A18` | texto principal, botón carrito, secciones oscuras |
| `--muted` | `#5F5A54` | texto secundario (pasa AA sobre blanco) |
| `--ok` | `#1F6B4F` | íconos de confianza / estados OK |
| `--surface`, `--surface-2` | `#FAF9F7`, `#F2EFEB` | franjas y placeholders |
| `--line` | `#E8E3DD` | bordes de cards |

- **Tipografía:** `Archivo` 700–800 para títulos y precios (`--font-display`); `Inter` 400–600 para texto (`--font-body`). No agregar otras fuentes.
- **Tamaños:** títulos con `clamp()` (h1 36→64 px, h2 28→42 px). Texto base 15–17 px.
- **Radios:** 10 px (inputs/botones chicos), 12 px (botones grandes), 16 px (cards), 20 px (hero).
- **Contenedor:** `.container` → `max-width: 1240px` y 16 px de margen lateral.
- **Íconos:** SVG inline de trazo (stroke 1.8–2, `stroke-linecap="round"`), estilo Lucide/Feather. Decorativos con `aria-hidden="true"`.
- **Layout responsive:** preferir `grid-template-columns: repeat(auto-fit|auto-fill, minmax(min(100%, Xpx), 1fr))` y `flex-wrap` antes que media queries. El único breakpoint fijo es `860px` (oculta nav, el buscador pasa a ancho completo).

## Componentes existentes (reutilizar, no duplicar)

- `.btn` + `.btn-primary` / `.btn-outline` / `.btn-light`
- `.eyebrow` + `.h2` dentro de `.section-head` → encabezado estándar de sección
- `.card` (producto), `.stepper` (− cantidad +), `.add-btn`
- `.tabs` / `.tab[aria-pressed]` y `.chip[aria-pressed]` para selecciones
- `.ph` → placeholder de imagen (reemplazar por `<img>` con `object-fit: cover` y `alt` descriptivo)
- `.reveal` → aparición al hacer scroll (CSS `animation-timeline` + fallback IntersectionObserver)
- `.cartbar` → barra flotante que aparece con productos en el carrito

## Reglas de negocio que ya están en la base

- **Carne por peso:** `byKg: true`; el stepper suma de a `STEP_KG` (0,5 kg). El precio se muestra "/ kg".
- **Almacén por unidad:** `byKg: false`, precio "c/u".
- **Se cobra el peso real:** el total del carrito de carne es *estimado*; mostrarlo siempre como "Total estimado" y explicar que se ajusta al pesar.
- **Precios:** `price: null` muestra `$ X.XXX` (placeholder). Formatear con `Intl.NumberFormat('es-AR', { currency: 'ARS' })`.
- El carrito se guarda en `localStorage` con la clave `cart` (`{ [id]: pasos }`).

## Contenido y tono

- Español rioplatense, voseo: "Elegí", "Armá", "Pagás". Cercano pero prolijo.
- Los datos que todavía no tenemos van entre corchetes: `[Tu Marca]`, `[ZONA DE ENVÍO]`, `[HORA]`, `[DIRECCIÓN]`, `[NÚMERO]`, `[CUIT]`, etc. **No inventar** precios, reseñas, cifras, certificaciones ni testimonios. Si hace falta, dejar placeholder.
- No usar logos ni marcas de terceros (medios de pago, etc.) sin los archivos oficiales.

## Próximos pasos sugeridos (en orden)

1. ~~**Drawer de carrito**~~ — hecho: `#cartDrawer` en `index.html`, con input de kg por corte (en vez de stepper post-agregado), subtotal estimado y botón "Continuar" que pasa un snapshot del pedido a `checkout.html` vía `sessionStorage`.
2. **Selector de presentación por corte:** grosor / entero o porcionado / con o sin hueso, guardado junto al ítem del carrito. Todavía no implementado.
3. ~~**Checkout: efectivo, transferencia y Mercado Pago**~~ — hecho (specs `002-pedidos-y-pagos` y `003-mercadopago`): `checkout.html` crea un pedido real (`POST /api/orders`) con cualquiera de los tres métodos. Transferencia: datos bancarios reales (alias `lodefacundito`, titular Jose Alberto Carnevale, con botón de copiar) + "Ya transferí" conectado. Mercado Pago: preferencia real vía el SDK oficial, redirige al checkout de MP, y un webhook público (`/api/webhooks/mercadopago`) confirma el pedido solo, sin que nadie tenga que hacer nada — nunca confía en el body del webhook, siempre vuelve a consultar la API real. El admin ve todos los pedidos en `admin.html` (mínimo, sin filtros) y puede marcar una transferencia como verificada. El **panel admin completo** (filtros, más estados, historial por cliente) sigue siendo backlog.
4. **Página de producto** y **listado por categoría** con filtros simples.
5. **Combos** configurables (4 / 8 / 12 personas) con contenido editable.
6. Separar `index.html` en `css/styles.css` y `js/app.js` cuando crezca; mover `PRODUCTS`, `CATEGORIES` y `FAQS` a `data/*.json` o a la API.
7. SEO básico: metadatos Open Graph, `schema.org` (`LocalBusiness`, `Product`), favicon.

## Cómo trabajar

- Cambios chicos y enfocados; no reescribir secciones que ya funcionan sin motivo.
- Antes de agregar un estilo nuevo, buscá si ya existe un token o componente.
- Mantener el sitio sin dependencias pesadas mientras sea posible (HTML/CSS/JS plano). Si se migra a un framework, conservar tokens, componentes y textos.
- Escapar todo texto dinámico antes de insertarlo en HTML (hay un helper `esc()`).
- Al terminar cada cambio: revisar en 360 px y 1440 px, navegar con teclado y chequear contraste.
