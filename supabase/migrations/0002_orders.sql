-- Tasks T1 y T2 de specs/002-pedidos-y-pagos/tasks.md
-- Correr contra la base real vía el Session pooler (ver supabase/migrations/0001_profiles.sql
-- para por qué: la conexión directa de Supabase es IPv6-only y no resuelve desde muchas redes).

-- ========== T1: tablas ==========

create table if not exists public.products (
  id text primary key,
  category text not null check (category in ('carne', 'almacen')),
  name text not null,
  detail text,
  unit text not null check (unit in ('kg', 'unidad')),
  price_per_unit numeric,
  tag text,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles (id),
  status text not null default 'pendiente_pago'
    check (status in ('pendiente_pago', 'pago_en_revision', 'confirmado', 'preparando', 'en_camino', 'entregado', 'cancelado')),
  payment_method text not null check (payment_method in ('mercadopago', 'transferencia', 'efectivo')),
  payment_status text not null default 'pendiente' check (payment_status in ('pendiente', 'completado', 'rechazado')),
  delivery_type text not null check (delivery_type in ('envio', 'retiro')),
  delivery_address_text text,
  delivery_address_lat double precision,
  delivery_address_lng double precision,
  total_estimated numeric not null,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders (id) on delete cascade,
  product_id text not null references public.products (id),
  product_name_snapshot text not null,
  unit text not null check (unit in ('kg', 'unidad')),
  quantity numeric not null check (quantity > 0),
  price_per_unit_snapshot numeric,
  subtotal_estimated numeric,
  item_notes text
);

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders (id) on delete cascade,
  method text not null check (method in ('mercadopago', 'transferencia', 'efectivo')),
  status text not null default 'pendiente' check (status in ('pendiente', 'completado', 'rechazado')),
  mercadopago_payment_id text,
  mercadopago_preference_id text,
  transfer_marked_by_client_at timestamptz,
  amount numeric not null,
  created_at timestamptz not null default now(),
  confirmed_at timestamptz
);

create table if not exists public.admin_notifications (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders (id) on delete cascade,
  type text not null check (type in ('nuevo_pedido', 'transferencia_marcada', 'mercadopago_confirmado', 'efectivo_pendiente')),
  read boolean not null default false,
  created_at timestamptz not null default now()
);

-- ========== T2: seed de productos (mismos ids que PRODUCTS en frontend/index.html) ==========

insert into public.products (id, category, name, detail, unit, price_per_unit, tag) values
('matambre', 'carne', 'Matambre', null, 'kg', 19800, null),
('asado', 'carne', 'Asado', null, 'kg', 18500, null),
('vacio', 'carne', 'Vacío', null, 'kg', 20000, null),
('falda', 'carne', 'Falda', null, 'kg', 16500, null),
('osobuco', 'carne', 'Osobuco', null, 'kg', 12000, null),
('espinazo', 'carne', 'Espinazo', null, 'kg', 9000, null),
('paleta', 'carne', 'Paleta', null, 'kg', 19000, null),
('roastbeef', 'carne', 'Roast beef', null, 'kg', 18000, null),
('chingolo', 'carne', 'Chingolo', null, 'kg', 19000, null),
('tortuga', 'carne', 'Tortuga', null, 'kg', 19000, null),
('nalga', 'carne', 'Nalga', null, 'kg', 22800, null),
('cuadrada', 'carne', 'Cuadrada', null, 'kg', 20000, null),
('bife-lomo', 'carne', 'Bife de lomo', null, 'kg', 20000, null),
('cuadril', 'carne', 'Cuadril', null, 'kg', 22600, null),
('peceto', 'carne', 'Peceto', null, 'kg', 23900, null),
('colita-cuadril', 'carne', 'Colita de cuadril', null, 'kg', 22800, null),
('entrana', 'carne', 'Entraña', null, 'kg', 24000, null),
('asado-americano', 'carne', 'Asado americano', null, 'kg', 19800, null),
('bife-chorizo', 'carne', 'Bife de chorizo', null, 'kg', 23900, null),
('lomo', 'carne', 'Lomo', null, 'kg', 24000, null),
('marucha', 'carne', 'Marucha', null, 'kg', 16800, null),
('aguja-con-hueso', 'carne', 'Aguja con hueso', null, 'kg', 16800, null),
('tapa-asado', 'carne', 'Tapa de Asado', null, 'kg', 20000, null),
('tapa-nalga', 'carne', 'Tapa de Nalga', null, 'kg', 20000, null),
('bife-ancho', 'carne', 'Bife ancho', null, 'kg', 17000, null),
('bife-angosto', 'carne', 'Bife angosto', null, 'kg', 19000, null),
('bife-con-lomo', 'carne', 'Bife con lomo', null, 'kg', 21000, null),
('higado', 'carne', 'Hígado', null, 'kg', 5500, null),
('corazon', 'carne', 'Corazón', null, 'kg', 7000, null),
('rinon', 'carne', 'Riñón', null, 'kg', 7000, null),
('lengua', 'carne', 'Lengua', null, 'kg', 11000, null),
('mondongo', 'carne', 'Mondongo', null, 'kg', 11000, null),
('rabo', 'carne', 'Rabo', null, 'kg', 12000, null),
('molleja', 'carne', 'Molleja', null, 'kg', 32000, null),
('chinchulin', 'carne', 'Chinchulín', null, 'kg', 7000, null),
('carre-cerdo', 'carne', 'Carré de cerdo', null, 'kg', 8500, null),
('pechito-cerdo', 'carne', 'Pechito de cerdo', null, 'kg', 8900, null),
('bondiola', 'carne', 'Bondiola de cerdo', null, 'kg', 9000, null),
('pollo-comun', 'carne', 'Pollo común', null, 'kg', 11000, null),
('pollo-especial', 'carne', 'Pollo especial', null, 'kg', 19000, null),
('cordero', 'carne', 'Cordero', null, 'kg', 18000, null),
('chorizo', 'carne', 'Chorizo', null, 'kg', 9900, null),
('morcilla', 'carne', 'Morcilla', null, 'kg', 7000, null),
('morcilla-vasca', 'carne', 'Morcilla vasca', null, 'kg', 8500, null),
('morcilla-fresca', 'carne', 'Morcilla fresca', null, 'kg', 9900, null),
('yerba', 'almacen', 'Yerba mate', 'Paquete de 1 kg', 'unidad', null, null),
('aceite', 'almacen', 'Aceite de oliva', 'Botella de 500 ml', 'unidad', null, null),
('arroz', 'almacen', 'Arroz largo fino', 'Paquete de 1 kg', 'unidad', null, null),
('fideos', 'almacen', 'Fideos secos', 'Paquete de 500 g', 'unidad', null, null),
('pure', 'almacen', 'Puré de tomate', 'Caja de 520 g', 'unidad', null, null),
('harina', 'almacen', 'Harina 000', 'Paquete de 1 kg', 'unidad', null, null),
('vino', 'almacen', 'Vino tinto', 'Botella de 750 ml', 'unidad', null, 'Para el asado'),
('carbon', 'almacen', 'Carbón', 'Bolsa de 4 kg', 'unidad', null, null)
on conflict (id) do update set
  category = excluded.category, name = excluded.name, detail = excluded.detail,
  unit = excluded.unit, price_per_unit = excluded.price_per_unit, tag = excluded.tag;

-- ========== RLS ==========
-- products: lectura pública (lo usa el backend con su propia conexión; no hace falta restringir).
alter table public.products enable row level security;
create policy "products_select_all" on public.products for select using (true);

alter table public.orders enable row level security;
create policy "orders_select_own" on public.orders for select using (auth.uid() = user_id);
create policy "orders_admin_select_all" on public.orders for select
  using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin'));

alter table public.order_items enable row level security;
create policy "order_items_select_own" on public.order_items for select
  using (exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid()));
create policy "order_items_admin_select_all" on public.order_items for select
  using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin'));

alter table public.payments enable row level security;
create policy "payments_select_own" on public.payments for select
  using (exists (select 1 from public.orders o where o.id = order_id and o.user_id = auth.uid()));
create policy "payments_admin_select_all" on public.payments for select
  using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin'));

alter table public.admin_notifications enable row level security;
create policy "admin_notifications_admin_only" on public.admin_notifications for select
  using (exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin'));
