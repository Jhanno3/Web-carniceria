-- Agrega la sección "Granja" al catálogo. Mismos ids que PRODUCTS.granja en frontend/index.html.
-- Las ofertas por paquete (x3kg, x2kg) son productos aparte vendidos "por unidad" (el paquete
-- entero a precio fijo), no un precio por kilo distinto.

alter table public.products drop constraint products_category_check;
alter table public.products add constraint products_category_check
  check (category in ('carne', 'granja', 'almacen'));

insert into public.products (id, category, name, detail, unit, price_per_unit, tag) values
('pata-muslo', 'granja', 'Pata y muslo', null, 'kg', 4900, null),
('pata-muslo-oferta-3kg', 'granja', 'Pata y muslo — oferta 3 kg', 'Pack de 3 kg', 'unidad', 12000, 'Oferta'),
('milanesa-pollo', 'granja', 'Milanesa de pollo', null, 'kg', 7000, null),
('milanesa-pollo-oferta-2kg', 'granja', 'Milanesa de pollo — oferta 2 kg', 'Pack de 2 kg', 'unidad', 13000, 'Oferta'),
('suprema', 'granja', 'Suprema', null, 'kg', 9800, null),
('pechuga-pollo', 'granja', 'Pechuga', null, 'kg', 8900, null),
('pollo-entero', 'granja', 'Pollo entero', null, 'kg', 4500, null),
('alitas', 'granja', 'Alitas', null, 'kg', 2500, null),
('menudos', 'granja', 'Menudos', null, 'kg', 2000, null),
('carcasa', 'granja', 'Carcasa', null, 'kg', 1000, null),
('paty-pollo', 'granja', 'Paty de pollo', null, 'kg', 9800, null),
('paty-carne', 'granja', 'Paty de carne', null, 'kg', 7000, null),
('milanesa-carne', 'granja', 'Milanesa de carne', null, 'kg', 14000, null)
on conflict (id) do update set
  category = excluded.category, name = excluded.name, detail = excluded.detail,
  unit = excluded.unit, price_per_unit = excluded.price_per_unit, tag = excluded.tag;
